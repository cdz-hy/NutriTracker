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
import com.example.nutritracker.feature.camera.AiAnalysisManager

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
    private val dayBoundaryCalc: DayBoundaryCalc,
    private val aiAnalysisManager: AiAnalysisManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val aiIsAnalyzing: StateFlow<Boolean> = aiAnalysisManager.isAnalyzing
    val aiAnalysisError: StateFlow<String?> = aiAnalysisManager.analysisError
    val aiSuccessEvent: SharedFlow<String> = aiAnalysisManager.analysisSuccess

    fun clearAiError() {
        aiAnalysisManager.clearError()
    }

    val onboardingDone: Flow<Boolean> = settingsRepo.onboardingDone

    init {
        // 监听设置变化，自动刷新首页
        viewModelScope.launch {
            combine(
                settingsRepo.kcalAdjustment,
                settingsRepo.carbPct,
                settingsRepo.fatPct
            ) { a, b, c -> Triple(a, b, c) }
            .collect { refresh() }
        }
        viewModelScope.launch {
            combine(
                settingsRepo.proteinPct,
                settingsRepo.waterGoalMl,
                settingsRepo.dayBoundaryMinutes
            ) { a, b, c -> Triple(a, b, c) }
            .collect { refresh() }
        }
        // 监听后台 AI 识图分析成功事件，自动刷新首页数据
        viewModelScope.launch {
            aiAnalysisManager.analysisSuccess.collect {
                refresh()
            }
        }
        refresh()
    }

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
            val calorieGoal = CalorieGoalCalc.getTotalKcalGoal(
                user = user,
                userKcalAdjustment = kcalAdj,
                totalKcalActivities = activityBurn
            )
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

    fun undoLastWaterIntake() {
        viewModelScope.launch {
            val latest = waterRepo.getLatest()
            if (latest != null) {
                waterRepo.deleteById(latest.id)
                refresh()
            }
        }
    }

    fun deleteIntake(intake: Intake) {
        viewModelScope.launch {
            // 先获取 Meal 数据
            val meal = mealRepo.getById(intake.mealId)
            // 先从 TrackedDay 移除卡路里
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
            // 再删除摄入记录
            intakeRepo.delete(intake)
            refresh()
        }
    }

    /**
     * 更新摄入记录的份量
     */
    fun updateIntakeAmount(intake: Intake, newAmount: Double) {
        viewModelScope.launch {
            val meal = mealRepo.getById(intake.mealId) ?: return@launch
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val today = dayBoundaryCalc.logicalDayOf(intake.dateTime, offset)

            // 移除旧的卡路里
            trackedDayRepo.removeCalories(
                today,
                meal.energyKcal100 * intake.amount / 100.0,
                meal.carbohydrates100 * intake.amount / 100.0,
                meal.fat100 * intake.amount / 100.0,
                meal.proteins100 * intake.amount / 100.0
            )

            // 更新摄入记录
            val updatedIntake = intake.copy(amount = newAmount)
            intakeRepo.upsert(updatedIntake)

            // 添加新的卡路里
            trackedDayRepo.addCalories(
                today,
                meal.energyKcal100 * newAmount / 100.0,
                meal.carbohydrates100 * newAmount / 100.0,
                meal.fat100 * newAmount / 100.0,
                meal.proteins100 * newAmount / 100.0
            )

            refresh()
        }
    }

    fun analyzeAndCreateMeals(context: android.content.Context, uris: List<android.net.Uri>, intakeType: IntakeType) {
        aiAnalysisManager.analyzeAndCreateMeals(context, uris, intakeType)
    }

    fun deleteActivity(activity: UserActivityEntity) {
        viewModelScope.launch {
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
