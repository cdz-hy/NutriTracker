package com.example.nutritracker.feature.diary

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.dao.IntakeWithMeal
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.feature.home.HomeViewModel
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

data class DiaryState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val isLogicalToday: Boolean = true,
    val calorieGoal: Double = 0.0,
    val caloriesTracked: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val carbsGoal: Double = 0.0,
    val carbsTracked: Double = 0.0,
    val fatGoal: Double = 0.0,
    val fatTracked: Double = 0.0,
    val proteinGoal: Double = 0.0,
    val proteinTracked: Double = 0.0,
    val intakes: List<IntakeWithMeal> = emptyList(),
    val activities: List<UserActivityEntity> = emptyList(),
    val waterEntries: List<WaterIntake> = emptyList(),
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val waterQuickAmounts: List<Int> = listOf(150, 250, 350, 500),
    val user: User? = null,
    val isBelowKcalFloor: Boolean = false,
    val recommendedFloor: Double = 0.0,
    val useKj: Boolean = false,
    val dayMicros: MicronutrientTotals = MicronutrientTotals(),
    val weeklyAvgMicros: MicronutrientTotals = MicronutrientTotals()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val savedStateHandle: SavedStateHandle,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val activityRepo: ActivityRepository,
    private val userRepo: UserRepository,
    private val settingsRepo: SettingsRepository,
    private val waterRepo: WaterIntakeRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(
        LocalDate.ofEpochDay(savedStateHandle["diary_date_epoch"] ?: LocalDate.now().toEpochDay())
    )
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private data class DiaryPrefs(
        val offset: Int,
        val kcalAdj: Double,
        val carbPct: Double,
        val fatPct: Double,
        val proteinPct: Double,
        val waterGoal: Int,
        val waterQuickAmounts: List<Int>,
        val useKj: Boolean
    )

    private val prefsFlow: Flow<DiaryPrefs> = combine(
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
        DiaryPrefs(
            offset = first[0] as Int,
            kcalAdj = first[1] as Double,
            carbPct = first[2] as Double,
            fatPct = first[3] as Double,
            proteinPct = second[0] as Double,
            waterGoal = second[1] as Int,
            waterQuickAmounts = second[2] as List<Int>,
            useKj = second[3] as Boolean
        )
    }

    val state: StateFlow<DiaryState> = combine(_selectedDate, prefsFlow) { date, prefs ->
        date to prefs
    }.flatMapLatest { (date, prefs) ->
        val offset = prefs.offset
        val logicalToday = dayBoundaryCalc.currentLogicalDay(offset)
        val isToday = date == logicalToday

        combine(
            userRepo.getUserFlow(),
            intakeRepo.getWithMealByLogicalDayFlow(date, offset),
            intakeRepo.getWithMealByLogicalDayRangeFlow(date.minusDays(6), date, offset),
            activityRepo.getByLogicalDayFlow(date, offset),
            waterRepo.getByLogicalDayFlow(date, offset),
            trackedDayRepo.getByDateFlow(date)
        ) { values ->
            val user = values[0] as User?
            @Suppress("UNCHECKED_CAST")
            val dayIntakes = values[1] as List<IntakeWithMeal>
            @Suppress("UNCHECKED_CAST")
            val weekIntakes = values[2] as List<IntakeWithMeal>
            @Suppress("UNCHECKED_CAST")
            val activities = values[3] as List<UserActivityEntity>
            @Suppress("UNCHECKED_CAST")
            val waterEntries = values[4] as List<WaterIntake>
            val trackedDay = values[5] as TrackedDay?

            val activityBurn = activities.sumOf { it.burnedKcal }
            val liveGoal = if (user != null) {
                CalorieGoalCalc.getTotalKcalGoal(user, prefs.kcalAdj, activityBurn, date)
            } else {
                0.0
            }

            // Preserve historical goal snapshot for past days when available
            val calorieGoal = if (!isToday && trackedDay != null && trackedDay.calorieGoal > 0.0) {
                trackedDay.calorieGoal
            } else {
                liveGoal.takeIf { it > 0.0 } ?: (trackedDay?.calorieGoal ?: 0.0)
            }

            val carbsGoal = if (!isToday && trackedDay?.carbsGoal != null && trackedDay.carbsGoal > 0) {
                trackedDay.carbsGoal
            } else {
                MacroCalc.getCarbsGoal(calorieGoal, prefs.carbPct)
            }
            val fatGoal = if (!isToday && trackedDay?.fatGoal != null && trackedDay.fatGoal > 0) {
                trackedDay.fatGoal
            } else {
                MacroCalc.getFatGoal(calorieGoal, prefs.fatPct)
            }
            val proteinGoal = if (!isToday && trackedDay?.proteinGoal != null && trackedDay.proteinGoal > 0) {
                trackedDay.proteinGoal
            } else {
                MacroCalc.getProteinGoal(calorieGoal, prefs.proteinPct)
            }

            val totalKcal = dayIntakes.sumOf { it.meal.energyKcal100 * it.intake.amount / 100.0 }
            val totalCarbs = dayIntakes.sumOf { it.meal.carbohydrates100 * it.intake.amount / 100.0 }
            val totalFat = dayIntakes.sumOf { it.meal.fat100 * it.intake.amount / 100.0 }
            val totalProtein = dayIntakes.sumOf { it.meal.proteins100 * it.intake.amount / 100.0 }

            DiaryState(
                isLoading = false,
                selectedDate = date,
                isLogicalToday = isToday,
                calorieGoal = calorieGoal,
                caloriesTracked = totalKcal,
                caloriesBurned = activityBurn,
                carbsGoal = carbsGoal,
                carbsTracked = totalCarbs,
                fatGoal = fatGoal,
                fatTracked = totalFat,
                proteinGoal = proteinGoal,
                proteinTracked = totalProtein,
                intakes = dayIntakes,
                activities = activities,
                waterEntries = waterEntries,
                waterMl = waterEntries.sumOf { it.amountMl },
                waterGoalMl = prefs.waterGoal,
                waterQuickAmounts = prefs.waterQuickAmounts,
                user = user,
                isBelowKcalFloor = user != null && calorieGoal > 0 && CalorieGoalCalc.isBelowRecommendedFloor(calorieGoal, user),
                recommendedFloor = user?.let { CalorieGoalCalc.recommendedFloor(it) } ?: 0.0,
                useKj = prefs.useKj,
                dayMicros = HomeViewModel.sumMicronutrients(dayIntakes, divisor = 1.0),
                weeklyAvgMicros = HomeViewModel.sumMicronutrients(weekIntakes, divisor = 7.0)
            )
        }
    }.onEach { s ->
        if (!s.isLoading && s.user != null && s.calorieGoal > 0) {
            trackedDayRepo.saveGoalSnapshot(
                date = s.selectedDate,
                calorieGoal = s.calorieGoal,
                carbGoal = s.carbsGoal,
                fatGoal = s.fatGoal,
                proteinGoal = s.proteinGoal,
                forceUpdateGoals = s.isLogicalToday
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryState())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        savedStateHandle["diary_date_epoch"] = date.toEpochDay()
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

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val now = LocalDateTime.now()
            val logicalDay = dayBoundaryCalc.logicalDayOf(now, offset)
            val date = _selectedDate.value
            val dateTime = if (logicalDay == date) now else date.atTime(12, 0)
            waterRepo.upsert(WaterIntake(amountMl = amountMl, dateTime = dateTime))
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun deleteWaterEntry(entry: WaterIntake) {
        viewModelScope.launch {
            waterRepo.deleteById(entry.id)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun undoLastWater() {
        viewModelScope.launch {
            val offset = settingsRepo.dayBoundaryMinutes.first()
            if (waterRepo.undoLastInLogicalDay(_selectedDate.value, offset)) {
                NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
            }
        }
    }
}
