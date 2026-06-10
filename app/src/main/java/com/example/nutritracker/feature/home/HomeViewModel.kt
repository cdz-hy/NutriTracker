package com.example.nutritracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class MealSection(
    val type: IntakeType,
    val intakes: List<Intake>,
    val meals: Map<Long, Meal>,
    val totalKcal: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalProtein: Double
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val calorieGoal: Double = 0.0,
    val caloriesSupplied: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val caloriesLeft: Double = 0.0,
    val carbsGoal: Double = 0.0,
    val fatGoal: Double = 0.0,
    val proteinGoal: Double = 0.0,
    val carbsTracked: Double = 0.0,
    val fatTracked: Double = 0.0,
    val proteinTracked: Double = 0.0,
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val mealSections: List<MealSection> = emptyList(),
    val activities: List<UserActivityEntity> = emptyList(),
    val user: User? = null,
    val isBelowKcalFloor: Boolean = false,
    val recommendedFloor: Double = 0.0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val activityRepo: ActivityRepository,
    private val waterRepo: WaterIntakeRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val onboardingDone: Flow<Boolean> = settingsRepo.onboardingDone

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val user = userRepo.getUser() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val today = dayBoundaryCalc.currentLogicalDay(offset)
            val kcalAdj = settingsRepo.kcalAdjustment.first()
            val carbPct = settingsRepo.carbPct.first()
            val fatPct = settingsRepo.fatPct.first()
            val proteinPct = settingsRepo.proteinPct.first()
            val waterGoal = settingsRepo.waterGoalMl.first()

            val activityBurn = activityRepo.getTotalBurnedByLogicalDay(today, offset)
            val calorieGoal = CalorieGoalCalc.getTotalKcalGoal(user, activityBurn, kcalAdj)
            val carbsGoal = MacroCalc.getCarbsGoal(calorieGoal, carbPct)
            val fatGoal = MacroCalc.getFatGoal(calorieGoal, fatPct)
            val proteinGoal = MacroCalc.getProteinGoal(calorieGoal, proteinPct)

            // Ensure TrackedDay exists with correct goals
            trackedDayRepo.ensureDay(today, calorieGoal, carbsGoal, fatGoal, proteinGoal)

            // Build meal sections
            val sections = IntakeType.entries.map { type ->
                val intakes = intakeRepo.getByTypeAndLogicalDay(type, today, offset)
                val mealIds = intakes.map { it.mealId }.distinct()
                val meals = mealIds.mapNotNull { id -> mealRepo.getById(id) }.associateBy { it.id }
                MealSection(
                    type = type, intakes = intakes, meals = meals,
                    totalKcal = intakes.sumOf { calcKcal(it, meals) },
                    totalCarbs = intakes.sumOf { calcCarbs(it, meals) },
                    totalFat = intakes.sumOf { calcFat(it, meals) },
                    totalProtein = intakes.sumOf { calcProtein(it, meals) }
                )
            }

            val totalSupplied = sections.sumOf { it.totalKcal }
            val totalCarbsTracked = sections.sumOf { it.totalCarbs }
            val totalFatTracked = sections.sumOf { it.totalFat }
            val totalProteinTracked = sections.sumOf { it.totalProtein }
            val activities = activityRepo.getByLogicalDay(today, offset)
            val waterMl = waterRepo.getTotalMlByLogicalDay(today, offset)

            // Reconcile TrackedDay
            trackedDayRepo.reconcileDay(today, totalSupplied, totalCarbsTracked, totalFatTracked, totalProteinTracked)

            _state.update {
                HomeUiState(
                    isLoading = false,
                    calorieGoal = calorieGoal,
                    caloriesSupplied = totalSupplied,
                    caloriesBurned = activityBurn,
                    caloriesLeft = calorieGoal - totalSupplied,
                    carbsGoal = carbsGoal, fatGoal = fatGoal, proteinGoal = proteinGoal,
                    carbsTracked = totalCarbsTracked, fatTracked = totalFatTracked, proteinTracked = totalProteinTracked,
                    waterMl = waterMl, waterGoalMl = waterGoal,
                    mealSections = sections, activities = activities, user = user,
                    isBelowKcalFloor = CalorieGoalCalc.isBelowRecommendedFloor(calorieGoal, user),
                    recommendedFloor = CalorieGoalCalc.recommendedFloor(user)
                )
            }
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            waterRepo.upsert(WaterIntake(amountMl = amountMl, dateTime = LocalDateTime.now()))
            refresh()
        }
    }

    fun deleteIntake(intake: Intake) {
        viewModelScope.launch {
            val meal = mealRepo.getById(intake.mealId)
            if (meal != null) {
                val offset = settingsRepo.dayBoundaryMinutes.first()
                val today = dayBoundaryCalc.logicalDayOf(intake.dateTime, offset)
                trackedDayRepo.removeCalories(
                    today,
                    meal.energyKcal100 * intake.amount / 100.0,
                    meal.carbohydrates100 * intake.amount / 100.0,
                    meal.fat100 * intake.amount / 100.0,
                    meal.proteins100 * intake.amount / 100.0
                )
            }
            intakeRepo.delete(intake)
            refresh()
        }
    }

    fun deleteActivity(activity: UserActivityEntity) {
        viewModelScope.launch {
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val today = dayBoundaryCalc.logicalDayOf(activity.dateTime, offset)
            // Reduce day's calorie goal to reverse the activity's boost
            val user = userRepo.getUser() ?: return@launch
            val kcalAdj = settingsRepo.kcalAdjustment.first()
            val carbPct = settingsRepo.carbPct.first()
            val fatPct = settingsRepo.fatPct.first()
            val proteinPct = settingsRepo.proteinPct.first()
            val remainingBurn = activityRepo.getTotalBurnedByLogicalDay(today, offset) - activity.burnedKcal
            val newGoal = CalorieGoalCalc.getTotalKcalGoal(user, remainingBurn, kcalAdj)
            val td = trackedDayRepo.getByDate(today)
            if (td != null) {
                trackedDayRepo.upsert(td.copy(
                    calorieGoal = newGoal,
                    carbsGoal = MacroCalc.getCarbsGoal(newGoal, carbPct),
                    fatGoal = MacroCalc.getFatGoal(newGoal, fatPct),
                    proteinGoal = MacroCalc.getProteinGoal(newGoal, proteinPct)
                ))
            }
            activityRepo.delete(activity)
            refresh()
        }
    }

    private fun calcKcal(intake: Intake, meals: Map<Long, Meal>): Double {
        val meal = meals[intake.mealId] ?: return 0.0
        return meal.energyKcal100 * intake.amount / 100.0
    }
    private fun calcCarbs(intake: Intake, meals: Map<Long, Meal>): Double {
        val meal = meals[intake.mealId] ?: return 0.0
        return meal.carbohydrates100 * intake.amount / 100.0
    }
    private fun calcFat(intake: Intake, meals: Map<Long, Meal>): Double {
        val meal = meals[intake.mealId] ?: return 0.0
        return meal.fat100 * intake.amount / 100.0
    }
    private fun calcProtein(intake: Intake, meals: Map<Long, Meal>): Double {
        val meal = meals[intake.mealId] ?: return 0.0
        return meal.proteins100 * intake.amount / 100.0
    }
}
