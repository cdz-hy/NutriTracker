package com.example.nutritracker.data.dao

import androidx.room.*
import com.example.nutritracker.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

// ── User DAO ─────────────────────────────────────────────────────────────────

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE id = 1")
    suspend fun getUser(): User?

    @Upsert
    suspend fun upsertUser(user: User)
}

// ── Meal DAO ─────────────────────────────────────────────────────────────────

@Dao
interface MealDao {
    @Upsert
    suspend fun upsert(meal: Meal): Long

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getById(id: Long): Meal?

    @Query("SELECT * FROM meals ORDER BY id DESC")
    fun getAllFlow(): Flow<List<Meal>>

    @Query("SELECT * FROM meals WHERE name LIKE '%' || :query || '%' OR brands LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Meal>

    @Delete
    suspend fun delete(meal: Meal)
}

// ── Intake DAO ───────────────────────────────────────────────────────────────

@Dao
interface IntakeDao {
    @Upsert
    suspend fun upsert(intake: Intake): Long

    @Delete
    suspend fun delete(intake: Intake)

    @Query("DELETE FROM intakes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM intakes WHERE id = :id")
    suspend fun getById(id: Long): Intake?

    @Query("SELECT * FROM intakes WHERE mealId = :mealId LIMIT 1")
    suspend fun getByMealId(mealId: Long): Intake?

    @Query("""
        SELECT * FROM intakes
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime): List<Intake>

    @Query("""
        SELECT * FROM intakes
        WHERE intakeType = :type AND dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    suspend fun getByTypeAndRange(type: IntakeType, start: LocalDateTime, end: LocalDateTime): List<Intake>

    @Query("SELECT * FROM intakes ORDER BY dateTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<Intake>
}

// ── TrackedDay DAO ───────────────────────────────────────────────────────────

@Dao
interface TrackedDayDao {
    @Upsert
    suspend fun upsert(day: TrackedDay)

    @Query("SELECT * FROM tracked_days WHERE date = :date")
    suspend fun getByDate(date: LocalDate): TrackedDay?

    @Query("SELECT * FROM tracked_days WHERE date BETWEEN :start AND :end")
    suspend fun getByDateRange(start: LocalDate, end: LocalDate): List<TrackedDay>

    @Query("SELECT * FROM tracked_days WHERE date BETWEEN :start AND :end")
    fun getByDateRangeFlow(start: LocalDate, end: LocalDate): Flow<List<TrackedDay>>

    @Query("SELECT * FROM tracked_days ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): TrackedDay?
}

// ── Activity DAO ─────────────────────────────────────────────────────────────

@Dao
interface ActivityDao {
    @Upsert
    suspend fun upsert(activity: UserActivityEntity): Long

    @Delete
    suspend fun delete(activity: UserActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT * FROM activities
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime): List<UserActivityEntity>

    @Query("SELECT * FROM activities ORDER BY dateTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<UserActivityEntity>
}

// ── WeightLog DAO ────────────────────────────────────────────────────────────

@Dao
interface WeightLogDao {
    @Upsert
    suspend fun upsert(log: WeightLog)

    @Query("DELETE FROM weight_logs WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)

    @Query("SELECT * FROM weight_logs ORDER BY date DESC")
    fun getAllFlow(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): WeightLog?

    @Query("SELECT * FROM weight_logs WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getByRange(start: LocalDate, end: LocalDate): List<WeightLog>
}

// ── WaterIntake DAO ──────────────────────────────────────────────────────────

@Dao
interface WaterIntakeDao {
    @Upsert
    suspend fun upsert(entry: WaterIntake): Long

    @Delete
    suspend fun delete(entry: WaterIntake)

    @Query("DELETE FROM water_intakes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT * FROM water_intakes
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime): List<WaterIntake>

    @Query("SELECT * FROM water_intakes ORDER BY dateTime DESC LIMIT 1")
    suspend fun getLatest(): WaterIntake?
}
