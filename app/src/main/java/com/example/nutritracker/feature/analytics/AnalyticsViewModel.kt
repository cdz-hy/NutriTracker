package com.example.nutritracker.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.dao.IntakeWithMeal
import com.example.nutritracker.data.entity.User
import com.example.nutritracker.data.entity.UserActivityEntity
import com.example.nutritracker.data.entity.WeightLog
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.feature.home.HomeViewModel
import com.example.nutritracker.ui.components.DailyCaloriePoint
import com.example.nutritracker.ui.components.WeightChartPoint
import com.example.nutritracker.util.CalorieGoalCalc
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.util.MicronutrientTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

enum class AnalyticsRange(val days: Int, val label: String, val labelEn: String) {
    WEEK(7, "近7天", "7 Days"),
    MONTH(30, "近30天", "30 Days");

    fun displayLabel(isZh: Boolean): String = if (isZh) label else labelEn
    fun chartTitle(isZh: Boolean): String = if (isZh) "${label}热量收支" else when (this) {
        WEEK -> "7-Day Calorie Balance"
        MONTH -> "30-Day Calorie Balance"
    }
}

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val range: AnalyticsRange = AnalyticsRange.WEEK,
    val streakDays: Int = 0,
    val loggedDaysCount: Int = 0,
    val avgIntakeKcal: Double = 0.0,
    val avgGoalKcal: Double = 0.0,
    val avgBurnedKcal: Double = 0.0,
    val avgCarbsG: Double = 0.0,
    val avgFatG: Double = 0.0,
    val avgProteinG: Double = 0.0,
    val caloriePoints: List<DailyCaloriePoint> = emptyList(),
    val weightPoints: List<WeightChartPoint> = emptyList(),
    val targetWeightKg: Double? = null,
    val latestWeightKg: Double? = null,
    val usesImperial: Boolean = false,
    val useKj: Boolean = false,
    val periodAvgMicros: MicronutrientTotals = MicronutrientTotals(),
    val user: User? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val intakeRepo: IntakeRepository,
    private val activityRepo: ActivityRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val weightLogRepo: WeightLogRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) : ViewModel() {

    private val _range = MutableStateFlow(AnalyticsRange.WEEK)
    private val resumeTick = MutableStateFlow(0L)

    val state: StateFlow<AnalyticsUiState> = combine(
        _range,
        settingsRepo.dayBoundaryMinutes,
        settingsRepo.kcalAdjustment,
        settingsRepo.useKj,
        resumeTick
    ) { range, offset, kcalAdj, useKj, _ ->
        Triple(range, offset, kcalAdj to useKj)
    }.flatMapLatest { (range, offset, adjAndKj) ->
        val (kcalAdj, useKj) = adjAndKj
        val today = dayBoundaryCalc.currentLogicalDay(offset)
        val startDate = today.minusDays(range.days.toLong() - 1L)
        // Fetch 90 days for streak calculation even if viewing 7 days
        val streakWindowStart = today.minusDays(89)

        combine(
            userRepo.getUserFlow(),
            intakeRepo.getWithMealByLogicalDayRangeFlow(streakWindowStart, today, offset),
            activityRepo.getByLogicalDayRangeFlow(startDate, today, offset),
            trackedDayRepo.getByDateRangeFlow(startDate, today),
            weightLogRepo.getAllFlow()
        ) { user, allWindowIntakes, periodActivities, trackedDays, weightLogs ->
            val intakesByDay = allWindowIntakes.groupBy {
                dayBoundaryCalc.logicalDayOf(it.intake.dateTime, offset)
            }
            val activitiesByDay = periodActivities.groupBy {
                dayBoundaryCalc.logicalDayOf(it.dateTime, offset)
            }
            val trackedByDate = trackedDays.associateBy { it.date }

            val streak = computeStreak(today, intakesByDay)

            val daysList = (0 until range.days).map { i -> startDate.plusDays(i.toLong()) }
            val fallbackBaseGoal = user?.let {
                CalorieGoalCalc.getTotalKcalGoal(it, kcalAdj, 0.0, today)
            } ?: 2000.0

            val points = daysList.map { day ->
                val dayIntakes = intakesByDay[day].orEmpty()
                val dayActivities = activitiesByDay[day].orEmpty()
                val intakeKcal = dayIntakes.sumOf { it.meal.energyKcal100 * it.intake.amount / 100.0 }
                val burnedKcal = dayActivities.sumOf { it.burnedKcal }
                val goalKcal = trackedByDate[day]?.calorieGoal?.takeIf { it > 0.0 }
                    ?: (fallbackBaseGoal + burnedKcal)
                DailyCaloriePoint(
                    date = day,
                    intakeKcal = intakeKcal,
                    goalKcal = goalKcal,
                    burnedKcal = burnedKcal
                )
            }

            val activeDays = points.filter { it.intakeKcal > 0.0 }
            val periodIntakes = daysList.flatMap { intakesByDay[it].orEmpty() }
            val divisor = activeDays.size.coerceAtLeast(1).toDouble()

            val totalCarbs = periodIntakes.sumOf { it.meal.carbohydrates100 * it.intake.amount / 100.0 }
            val totalFat = periodIntakes.sumOf { it.meal.fat100 * it.intake.amount / 100.0 }
            val totalProtein = periodIntakes.sumOf { it.meal.proteins100 * it.intake.amount / 100.0 }

            val weightChartPoints = weightLogs
                .filter { !it.date.isBefore(today.minusDays(89)) }
                .sortedBy { it.date }
                .map { WeightChartPoint(it.date, it.weightKg) }

            AnalyticsUiState(
                isLoading = false,
                range = range,
                streakDays = streak,
                loggedDaysCount = activeDays.size,
                avgIntakeKcal = if (activeDays.isNotEmpty()) activeDays.sumOf { it.intakeKcal } / activeDays.size else 0.0,
                avgGoalKcal = points.map { it.goalKcal }.filter { it > 0 }.average().takeIf { !it.isNaN() } ?: 0.0,
                avgBurnedKcal = points.sumOf { it.burnedKcal } / range.days,
                avgCarbsG = totalCarbs / divisor,
                avgFatG = totalFat / divisor,
                avgProteinG = totalProtein / divisor,
                caloriePoints = points,
                weightPoints = weightChartPoints,
                targetWeightKg = user?.targetWeightKg,
                latestWeightKg = weightLogs.firstOrNull()?.weightKg ?: user?.weightKg,
                usesImperial = user?.usesImperialUnits ?: false,
                useKj = useKj,
                periodAvgMicros = HomeViewModel.sumMicronutrients(periodIntakes, divisor),
                user = user
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun selectRange(range: AnalyticsRange) {
        _range.value = range
    }

    fun onResumed() {
        resumeTick.value = System.currentTimeMillis()
    }

    private fun computeStreak(today: LocalDate, intakesByDay: Map<LocalDate, List<IntakeWithMeal>>): Int {
        var cursor = if (!intakesByDay[today].isNullOrEmpty()) today else today.minusDays(1)
        var count = 0
        while (!intakesByDay[cursor].isNullOrEmpty()) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }
}
