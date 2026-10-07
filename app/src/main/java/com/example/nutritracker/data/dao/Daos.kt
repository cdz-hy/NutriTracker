package com.example.nutritracker.data.dao

import androidx.room.*
import com.example.nutritracker.data.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

// ── Relations ────────────────────────────────────────────────────────────────

data class IntakeWithMeal(
    @Embedded val intake: Intake,
    @Relation(parentColumn = "mealId", entityColumn = "id")
    val meal: Meal
)

data class RecipeIngredientWithMeal(
    @Embedded val ingredient: RecipeIngredient,
    @Relation(parentColumn = "mealId", entityColumn = "id")
    val meal: Meal
)

data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(
        entity = RecipeIngredient::class,
        parentColumn = "id",
        entityColumn = "recipeId"
    )
    val ingredients: List<RecipeIngredientWithMeal>
)

// ── User DAO ─────────────────────────────────────────────────────────────────

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE id = 1")
    suspend fun getUser(): User?

    @Query("SELECT * FROM user WHERE id = 1")
    fun getUserFlow(): Flow<User?>

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

    @Query("SELECT * FROM meals ORDER BY id DESC")
    suspend fun getAll(): List<Meal>

    @Query("SELECT * FROM meals WHERE isArchived = 0 ORDER BY isFavorite DESC, id DESC")
    fun getLibraryFlow(): Flow<List<Meal>>

    @Query("SELECT * FROM meals WHERE isArchived = 0 AND name LIKE '%' || :query || '%' ORDER BY isFavorite DESC, id DESC")
    suspend fun search(query: String): List<Meal>

    @Query("UPDATE meals SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE meals SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Delete
    suspend fun delete(meal: Meal)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteById(id: Long)
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

    @Transaction
    @Query("SELECT * FROM intakes WHERE id = :id")
    suspend fun getWithMealById(id: Long): IntakeWithMeal?

    @Query("SELECT * FROM intakes WHERE mealId = :mealId ORDER BY dateTime DESC LIMIT 1")
    suspend fun getByMealId(mealId: Long): Intake?

    @Query("SELECT * FROM intakes WHERE mealId = :mealId ORDER BY dateTime DESC")
    suspend fun getByMealIdAll(mealId: Long): List<Intake>

    @Query("SELECT COUNT(*) FROM intakes WHERE mealId = :mealId")
    suspend fun countByMealId(mealId: Long): Int

    @Query("SELECT * FROM intakes ORDER BY dateTime DESC")
    suspend fun getAll(): List<Intake>

    @Query("""
        SELECT * FROM intakes
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime): List<Intake>

    @Transaction
    @Query("""
        SELECT * FROM intakes
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    fun getWithMealByRangeFlow(start: LocalDateTime, end: LocalDateTime): Flow<List<IntakeWithMeal>>

    @Transaction
    @Query("""
        SELECT * FROM intakes
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    suspend fun getWithMealByRange(start: LocalDateTime, end: LocalDateTime): List<IntakeWithMeal>

    @Query("""
        SELECT * FROM intakes
        WHERE intakeType = :type AND dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    suspend fun getByTypeAndRange(type: IntakeType, start: LocalDateTime, end: LocalDateTime): List<Intake>

    @Transaction
    @Query("""
        SELECT * FROM intakes
        WHERE intakeType = :type AND dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    fun getWithMealByTypeAndRangeFlow(type: IntakeType, start: LocalDateTime, end: LocalDateTime): Flow<List<IntakeWithMeal>>

    @Query("SELECT * FROM intakes ORDER BY dateTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<Intake>

    @Transaction
    @Query("SELECT * FROM intakes WHERE intakeType = :type ORDER BY dateTime DESC LIMIT :limit")
    fun getRecentWithMealByTypeFlow(type: IntakeType, limit: Int = 30): Flow<List<IntakeWithMeal>>

    @Query("SELECT * FROM intakes WHERE intakeType = :type ORDER BY dateTime DESC LIMIT :limit")
    suspend fun getRecentByType(type: IntakeType, limit: Int = 10): List<Intake>
}

// ── Recipe DAO ───────────────────────────────────────────────────────────────

@Dao
interface RecipeDao {
    @Upsert
    suspend fun upsertRecipe(recipe: Recipe): Long

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredient>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsByRecipeId(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Long)

    @Query("SELECT * FROM recipes ORDER BY id DESC")
    suspend fun getAllRecipes(): List<Recipe>

    @Query("SELECT * FROM recipe_ingredients")
    suspend fun getAllIngredients(): List<RecipeIngredient>

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun getAllWithIngredientsFlow(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getWithIngredientsById(id: Long): RecipeWithIngredients?
}

// ── TrackedDay DAO ───────────────────────────────────────────────────────────

@Dao
interface TrackedDayDao {
    @Upsert
    suspend fun upsert(day: TrackedDay)

    @Query("SELECT * FROM tracked_days WHERE date = :date")
    suspend fun getByDate(date: LocalDate): TrackedDay?

    @Query("SELECT * FROM tracked_days WHERE date = :date")
    fun getByDateFlow(date: LocalDate): Flow<TrackedDay?>

    @Query("SELECT * FROM tracked_days ORDER BY date DESC")
    suspend fun getAll(): List<TrackedDay>

    @Query("SELECT * FROM tracked_days WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getByDateRange(start: LocalDate, end: LocalDate): List<TrackedDay>

    @Query("SELECT * FROM tracked_days WHERE date BETWEEN :start AND :end ORDER BY date ASC")
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

    @Query("SELECT * FROM activities ORDER BY dateTime DESC")
    suspend fun getAll(): List<UserActivityEntity>

    @Query("""
        SELECT * FROM activities
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime): List<UserActivityEntity>

    @Query("""
        SELECT * FROM activities
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    fun getByDateTimeRangeFlow(start: LocalDateTime, end: LocalDateTime): Flow<List<UserActivityEntity>>

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

    @Query("SELECT * FROM weight_logs ORDER BY date DESC")
    suspend fun getAll(): List<WeightLog>

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

    @Query("SELECT * FROM water_intakes ORDER BY dateTime DESC")
    suspend fun getAll(): List<WaterIntake>

    @Query("""
        SELECT * FROM water_intakes
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime): List<WaterIntake>

    @Query("""
        SELECT * FROM water_intakes
        WHERE dateTime >= :start AND dateTime < :end
        ORDER BY dateTime ASC
    """)
    fun getByDateTimeRangeFlow(start: LocalDateTime, end: LocalDateTime): Flow<List<WaterIntake>>

    @Query("SELECT * FROM water_intakes ORDER BY dateTime DESC LIMIT 1")
    suspend fun getLatest(): WaterIntake?
}
