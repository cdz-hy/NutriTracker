package com.example.nutritracker.feature.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.dao.IntakeWithMeal
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.feature.camera.AiAnalysisManager
import com.example.nutritracker.feature.camera.AnalysisTask
import com.example.nutritracker.feature.camera.NutritionResult
import com.example.nutritracker.util.*
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class MealSection(
    val type: IntakeType,
    val items: List<IntakeWithMeal>,
    val totalKcal: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val totalProtein: Double
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val logicalToday: LocalDate = LocalDate.now(),
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
    val waterEntries: List<WaterIntake> = emptyList(),
    val waterQuickAmounts: List<Int> = listOf(150, 250, 350, 500),
    val mealSections: List<MealSection> = emptyList(),
    val activities: List<UserActivityEntity> = emptyList(),
    val user: User? = null,
    val isBelowKcalFloor: Boolean = false,
    val recommendedFloor: Double = 0.0,
    val useKj: Boolean = false,
    val dayMicros: MicronutrientTotals = MicronutrientTotals(),
    val weeklyAvgMicros: MicronutrientTotals = MicronutrientTotals()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepo: UserRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val activityRepo: ActivityRepository,
    private val waterRepo: WaterIntakeRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc,
    private val aiAnalysisManager: AiAnalysisManager
) : ViewModel() {

    private data class HomePrefs(
        val dayBoundaryMinutes: Int,
        val kcalAdjustment: Double,
        val carbPct: Double,
        val fatPct: Double,
        val proteinPct: Double,
        val waterGoalMl: Int,
        val waterQuickAmounts: List<Int>,
        val useKj: Boolean
    )

    private val resumeTick = MutableStateFlow(0L)

    private val prefsFlow: Flow<HomePrefs> = combine(
        combine(
            settingsRepo.dayBoundaryMinutes,
            settingsRepo.kcalAdjustment,
            settingsRepo.carbPct,
            settingsRepo.fatPct
        ) { a, b, c, d -> listOf(a, b, c, d) },
        combine(
            settingsRepo.proteinPct,
            settingsRepo.waterGoalMl,
            settingsRepo.waterQuickAmounts,
            settingsRepo.useKj
        ) { p, wg, wq, kj -> listOf(p, wg, wq, kj) }
    ) { first, second ->
        @Suppress("UNCHECKED_CAST")
        HomePrefs(
            dayBoundaryMinutes = first[0] as Int,
            kcalAdjustment = first[1] as Double,
            carbPct = first[2] as Double,
            fatPct = first[3] as Double,
            proteinPct = second[0] as Double,
            waterGoalMl = second[1] as Int,
            waterQuickAmounts = second[2] as List<Int>,
            useKj = second[3] as Boolean
        )
    }

    val state: StateFlow<HomeUiState> = combine(prefsFlow, resumeTick) { prefs, _ ->
        prefs to dayBoundaryCalc.currentLogicalDay(prefs.dayBoundaryMinutes)
    }.flatMapLatest { (prefs, today) ->
        val dayOffset = prefs.dayBoundaryMinutes
        combine(
            userRepo.getUserFlow(),
            intakeRepo.getWithMealByLogicalDayFlow(today, dayOffset),
            intakeRepo.getWithMealByLogicalDayRangeFlow(today.minusDays(6), today, dayOffset),
            activityRepo.getByLogicalDayFlow(today, dayOffset),
            waterRepo.getByLogicalDayFlow(today, dayOffset)
        ) { user, todayIntakes, weekIntakes, activities, waterEntries ->
            if (user == null) {
                return@combine HomeUiState(
                    isLoading = false,
                    logicalToday = today,
                    waterGoalMl = prefs.waterGoalMl,
                    waterQuickAmounts = prefs.waterQuickAmounts,
                    useKj = prefs.useKj
                )
            }

            val activityBurn = activities.sumOf { it.burnedKcal }
            val calorieGoal = CalorieGoalCalc.getTotalKcalGoal(
                user = user,
                userKcalAdjustment = prefs.kcalAdjustment,
                totalKcalActivities = activityBurn,
                referenceDate = today
            )
            val carbsGoal = MacroCalc.getCarbsGoal(calorieGoal, prefs.carbPct)
            val fatGoal = MacroCalc.getFatGoal(calorieGoal, prefs.fatPct)
            val proteinGoal = MacroCalc.getProteinGoal(calorieGoal, prefs.proteinPct)

            val grouped = todayIntakes.groupBy { it.intake.intakeType }
            val sections = IntakeType.entries.map { type ->
                val list = grouped[type].orEmpty()
                MealSection(
                    type = type,
                    items = list,
                    totalKcal = list.sumOf { it.meal.energyKcal100 * it.intake.amount / 100.0 },
                    totalCarbs = list.sumOf { it.meal.carbohydrates100 * it.intake.amount / 100.0 },
                    totalFat = list.sumOf { it.meal.fat100 * it.intake.amount / 100.0 },
                    totalProtein = list.sumOf { it.meal.proteins100 * it.intake.amount / 100.0 }
                )
            }

            val totalSupplied = sections.sumOf { it.totalKcal }
            val totalCarbs = sections.sumOf { it.totalCarbs }
            val totalFat = sections.sumOf { it.totalFat }
            val totalProtein = sections.sumOf { it.totalProtein }
            val waterMl = waterEntries.sumOf { it.amountMl }

            HomeUiState(
                isLoading = false,
                logicalToday = today,
                calorieGoal = calorieGoal,
                caloriesSupplied = totalSupplied,
                caloriesBurned = activityBurn,
                caloriesLeft = calorieGoal - totalSupplied,
                carbsGoal = carbsGoal,
                fatGoal = fatGoal,
                proteinGoal = proteinGoal,
                carbsTracked = totalCarbs,
                fatTracked = totalFat,
                proteinTracked = totalProtein,
                waterMl = waterMl,
                waterGoalMl = prefs.waterGoalMl,
                waterEntries = waterEntries,
                waterQuickAmounts = prefs.waterQuickAmounts,
                mealSections = sections,
                activities = activities,
                user = user,
                isBelowKcalFloor = CalorieGoalCalc.isBelowRecommendedFloor(calorieGoal, user),
                recommendedFloor = CalorieGoalCalc.recommendedFloor(user),
                useKj = prefs.useKj,
                dayMicros = sumMicronutrients(todayIntakes, divisor = 1.0),
                weeklyAvgMicros = sumMicronutrients(weekIntakes, divisor = 7.0)
            )
        }
    }.onEach { ui ->
        if (!ui.isLoading && ui.user != null && ui.calorieGoal > 0) {
            trackedDayRepo.saveGoalSnapshot(
                date = ui.logicalToday,
                calorieGoal = ui.calorieGoal,
                carbGoal = ui.carbsGoal,
                fatGoal = ui.fatGoal,
                proteinGoal = ui.proteinGoal,
                forceUpdateGoals = true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    val aiTasks: StateFlow<List<AnalysisTask>> = aiAnalysisManager.tasks
    val aiActiveCount: StateFlow<Int> = aiAnalysisManager.activeJobCount
    val aiPendingConfirmTasks: StateFlow<List<AnalysisTask>> = aiAnalysisManager.pendingConfirmTasks
    val aiFailedTasks: StateFlow<List<AnalysisTask>> = aiAnalysisManager.failedTasks
    val aiAnalysisError: StateFlow<String?> = aiAnalysisManager.analysisError
    val aiSuccessEvent: SharedFlow<String> = aiAnalysisManager.analysisSuccess

    fun onResumed() {
        resumeTick.value = System.currentTimeMillis()
    }

    fun clearAiError() = aiAnalysisManager.clearError()

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            waterRepo.upsert(WaterIntake(amountMl = amountMl, dateTime = LocalDateTime.now()))
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun deleteWaterEntry(entry: WaterIntake) {
        viewModelScope.launch {
            waterRepo.deleteById(entry.id)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun undoLastWaterIntake() {
        viewModelScope.launch {
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val today = dayBoundaryCalc.currentLogicalDay(offset)
            if (waterRepo.undoLastInLogicalDay(today, offset)) {
                NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
            }
        }
    }

    fun deleteIntake(intake: Intake) {
        viewModelScope.launch {
            intakeRepo.deleteById(intake.id)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun deleteActivity(activity: UserActivityEntity) {
        viewModelScope.launch {
            activityRepo.deleteById(activity.id)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun analyzeAndCreateMeals(
        context: Context,
        uris: List<Uri>,
        intakeType: IntakeType,
        notes: String = "",
        date: LocalDate? = null
    ) {
        viewModelScope.launch {
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val targetDate = date ?: dayBoundaryCalc.currentLogicalDay(offset)
            aiAnalysisManager.analyzeAndCreateMeals(context, uris, intakeType, notes, targetDate)
        }
    }

    fun analyzeTextMeal(description: String, intakeType: IntakeType) {
        viewModelScope.launch {
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val targetDate = dayBoundaryCalc.currentLogicalDay(offset)
            aiAnalysisManager.analyzeTextMeal(description, intakeType, targetDate)
        }
    }

    fun cancelAiTask(taskId: Int) = aiAnalysisManager.cancelTask(taskId)
    fun retryAiTask(taskId: Int) = aiAnalysisManager.retryTask(taskId)
    fun dismissAiTask(taskId: Int) = aiAnalysisManager.dismissTask(taskId)

    fun confirmAiTask(
        taskId: Int,
        mealName: String,
        intakeType: IntakeType,
        date: LocalDate,
        multiplier: Double,
        items: List<NutritionResult.FoodItem>,
        fallbackResult: NutritionResult,
        thumbnailPath: String?
    ) {
        aiAnalysisManager.confirmTask(
            taskId = taskId,
            mealName = mealName,
            intakeType = intakeType,
            date = date,
            multiplier = multiplier,
            editedItems = items,
            fallbackResult = fallbackResult,
            thumbnailPath = thumbnailPath
        )
    }

    companion object {
        fun sumMicronutrients(intakes: List<IntakeWithMeal>, divisor: Double): MicronutrientTotals {
            val d = divisor.coerceAtLeast(1.0)
            var fiber = 0.0
            var sugars = 0.0
            var satFat = 0.0
            var sodium = 0.0
            var cholesterol = 0.0
            intakes.forEach { item ->
                val factor = item.intake.amount / 100.0
                fiber += (item.meal.fiber100 ?: 0.0) * factor
                sugars += (item.meal.sugars100 ?: 0.0) * factor
                satFat += (item.meal.saturatedFat100 ?: 0.0) * factor
                sodium += (item.meal.sodium100 ?: 0.0) * factor
                cholesterol += (item.meal.cholesterol100 ?: 0.0) * factor
            }
            return MicronutrientTotals(
                fiberG = fiber / d,
                sugarsG = sugars / d,
                saturatedFatG = satFat / d,
                sodiumMg = sodium / d,
                cholesterolMg = cholesterol / d
            )
        }
    }
}
