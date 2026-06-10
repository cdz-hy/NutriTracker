package com.example.nutritracker.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.CalorieGoalCalc
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.util.MacroCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DiaryState(
    val isLoading: Boolean = true,
    val calorieGoal: Double = 0.0,
    val caloriesTracked: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val carbsGoal: Double = 0.0, val carbsTracked: Double = 0.0,
    val fatGoal: Double = 0.0, val fatTracked: Double = 0.0,
    val proteinGoal: Double = 0.0, val proteinTracked: Double = 0.0,
    val intakes: List<Intake> = emptyList(),
    val meals: Map<Long, Meal> = emptyMap(),
    val activities: List<UserActivityEntity> = emptyList(),
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2000
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val intakeRepo: IntakeRepository,
    private val mealRepo: MealRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val activityRepo: ActivityRepository,
    private val userRepo: UserRepository,
    private val settingsRepo: SettingsRepository,
    private val waterRepo: WaterIntakeRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) : ViewModel() {

    private val _state = MutableStateFlow(DiaryState())
    val state: StateFlow<DiaryState> = _state.asStateFlow()

    fun loadDay(date: LocalDate) {
        viewModelScope.launch {
            val user = userRepo.getUser()
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val intakes = intakeRepo.getByLogicalDay(date, offset)
            val mealIds = intakes.map { it.mealId }.distinct()
            val meals = mealIds.mapNotNull { id -> mealRepo.getById(id) }.associateBy { it.id }
            val activities = activityRepo.getByLogicalDay(date, offset)
            val tracked = trackedDayRepo.getByDate(date)
            val activityBurn = activities.sumOf { it.burnedKcal }
            val waterMl = waterRepo.getTotalMlByLogicalDay(date, offset)
            val waterGoal = settingsRepo.waterGoalMl.first()

            // Compute calorie goal for the day using the same logic as HomeViewModel
            val kcalAdj = settingsRepo.kcalAdjustment.first()
            val carbPct = settingsRepo.carbPct.first()
            val fatPct = settingsRepo.fatPct.first()
            val proteinPct = settingsRepo.proteinPct.first()
            val calorieGoal = if (user != null) {
                CalorieGoalCalc.getTotalKcalGoal(
                    user = user,
                    userKcalAdjustment = kcalAdj,
                    totalKcalActivities = activityBurn
                )
            } else {
                tracked?.calorieGoal ?: 0.0
            }
            val carbsGoal = MacroCalc.getCarbsGoal(calorieGoal, carbPct)
            val fatGoal = MacroCalc.getFatGoal(calorieGoal, fatPct)
            val proteinGoal = MacroCalc.getProteinGoal(calorieGoal, proteinPct)

            _state.update {
                DiaryState(
                    isLoading = false,
                    calorieGoal = calorieGoal,
                    caloriesTracked = tracked?.caloriesTracked ?: 0.0,
                    caloriesBurned = activityBurn,
                    carbsGoal = carbsGoal, carbsTracked = tracked?.carbsTracked ?: 0.0,
                    fatGoal = fatGoal, fatTracked = tracked?.fatTracked ?: 0.0,
                    proteinGoal = proteinGoal, proteinTracked = tracked?.proteinTracked ?: 0.0,
                    intakes = intakes, meals = meals, activities = activities,
                    waterMl = waterMl, waterGoalMl = waterGoal
                )
            }
        }
    }
}
