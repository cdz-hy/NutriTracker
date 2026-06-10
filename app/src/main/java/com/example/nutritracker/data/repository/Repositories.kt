package com.example.nutritracker.data.repository

import com.example.nutritracker.data.dao.*
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.util.DayBoundaryCalc
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

// ── User ─────────────────────────────────────────────────────────────────────

class UserRepository(private val dao: UserDao) {
    suspend fun getUser(): User? = dao.getUser()
    suspend fun upsert(user: User) = dao.upsertUser(user)
    suspend fun hasUser(): Boolean = dao.getUser() != null
}

// ── Meal ─────────────────────────────────────────────────────────────────────

class MealRepository(private val dao: MealDao) {
    suspend fun upsert(meal: Meal): Long = dao.upsert(meal)
    suspend fun getById(id: Long): Meal? = dao.getById(id)
    fun getAllFlow(): Flow<List<Meal>> = dao.getAllFlow()
    suspend fun search(query: String): List<Meal> = dao.search(query)
    suspend fun delete(meal: Meal) = dao.delete(meal)
}

// ── Intake ───────────────────────────────────────────────────────────────────

class IntakeRepository(
    private val dao: IntakeDao,
    private val dbc: DayBoundaryCalc
) {
    suspend fun upsert(intake: Intake): Long = dao.upsert(intake)
    suspend fun delete(intake: Intake) = dao.delete(intake)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun getById(id: Long): Intake? = dao.getById(id)
    suspend fun getRecent(limit: Int = 20): List<Intake> = dao.getRecent(limit)

    suspend fun getByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): List<Intake> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByDateTimeRange(start, end)
    }

    suspend fun getByTypeAndLogicalDay(type: IntakeType, date: LocalDate, offsetMinutes: Int = 0): List<Intake> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByTypeAndRange(type, start, end)
    }
}

// ── TrackedDay ───────────────────────────────────────────────────────────────

class TrackedDayRepository(private val dao: TrackedDayDao) {
    suspend fun getByDate(date: LocalDate): TrackedDay? = dao.getByDate(date)
    suspend fun getByDateRange(start: LocalDate, end: LocalDate): List<TrackedDay> = dao.getByDateRange(start, end)
    fun getByDateRangeFlow(start: LocalDate, end: LocalDate): Flow<List<TrackedDay>> = dao.getByDateRangeFlow(start, end)
    suspend fun upsert(day: TrackedDay) = dao.upsert(day)
    suspend fun getLatest(): TrackedDay? = dao.getLatest()

    suspend fun ensureDay(date: LocalDate, calorieGoal: Double, carbGoal: Double, fatGoal: Double, proteinGoal: Double): TrackedDay {
        val existing = dao.getByDate(date)
        if (existing != null) return existing
        val day = TrackedDay(
            date = date, calorieGoal = calorieGoal,
            carbsGoal = carbGoal, fatGoal = fatGoal, proteinGoal = proteinGoal
        )
        dao.upsert(day)
        return day
    }

    suspend fun addCalories(date: LocalDate, kcal: Double, carbs: Double, fat: Double, protein: Double) {
        val day = dao.getByDate(date) ?: return
        dao.upsert(day.copy(
            caloriesTracked = day.caloriesTracked + kcal,
            carbsTracked = (day.carbsTracked ?: 0.0) + carbs,
            fatTracked = (day.fatTracked ?: 0.0) + fat,
            proteinTracked = (day.proteinTracked ?: 0.0) + protein
        ))
    }

    suspend fun removeCalories(date: LocalDate, kcal: Double, carbs: Double, fat: Double, protein: Double) {
        val day = dao.getByDate(date) ?: return
        dao.upsert(day.copy(
            caloriesTracked = (day.caloriesTracked - kcal).coerceAtLeast(0.0),
            carbsTracked = ((day.carbsTracked ?: 0.0) - carbs).coerceAtLeast(0.0),
            fatTracked = ((day.fatTracked ?: 0.0) - fat).coerceAtLeast(0.0),
            proteinTracked = ((day.proteinTracked ?: 0.0) - protein).coerceAtLeast(0.0)
        ))
    }

    suspend fun increaseGoal(date: LocalDate, kcal: Double, carbs: Double = 0.0, fat: Double = 0.0, protein: Double = 0.0) {
        val day = dao.getByDate(date) ?: return
        dao.upsert(day.copy(
            calorieGoal = day.calorieGoal + kcal,
            carbsGoal = (day.carbsGoal ?: 0.0) + carbs,
            fatGoal = (day.fatGoal ?: 0.0) + fat,
            proteinGoal = (day.proteinGoal ?: 0.0) + protein
        ))
    }

    suspend fun reconcileDay(date: LocalDate, kcal: Double, carbs: Double, fat: Double, protein: Double) {
        val day = dao.getByDate(date) ?: return
        dao.upsert(day.copy(
            caloriesTracked = kcal,
            carbsTracked = carbs,
            fatTracked = fat,
            proteinTracked = protein
        ))
    }
}

// ── Activity ─────────────────────────────────────────────────────────────────

class ActivityRepository(
    private val dao: ActivityDao,
    private val dbc: DayBoundaryCalc
) {
    suspend fun upsert(activity: UserActivityEntity): Long = dao.upsert(activity)
    suspend fun delete(activity: UserActivityEntity) = dao.delete(activity)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun getRecent(limit: Int = 20): List<UserActivityEntity> = dao.getRecent(limit)

    suspend fun getByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): List<UserActivityEntity> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByDateTimeRange(start, end)
    }

    suspend fun getTotalBurnedByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): Double =
        getByLogicalDay(date, offsetMinutes).sumOf { it.burnedKcal }
}

// ── WeightLog ────────────────────────────────────────────────────────────────

class WeightLogRepository(private val dao: WeightLogDao) {
    suspend fun upsert(log: WeightLog) = dao.upsert(log)
    suspend fun deleteByDate(date: LocalDate) = dao.deleteByDate(date)
    fun getAllFlow(): Flow<List<WeightLog>> = dao.getAllFlow()
    suspend fun getLatest(): WeightLog? = dao.getLatest()
    suspend fun getByRange(start: LocalDate, end: LocalDate) = dao.getByRange(start, end)
}

// ── WaterIntake ──────────────────────────────────────────────────────────────

class WaterIntakeRepository(
    private val dao: WaterIntakeDao,
    private val dbc: DayBoundaryCalc
) {
    suspend fun upsert(entry: WaterIntake): Long = dao.upsert(entry)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun getLatest(): WaterIntake? = dao.getLatest()

    suspend fun getByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): List<WaterIntake> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByDateTimeRange(start, end)
    }

    suspend fun getTotalMlByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): Int =
        getByLogicalDay(date, offsetMinutes).sumOf { it.amountMl }
}
