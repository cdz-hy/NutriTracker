package com.example.nutritracker.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.nutritracker.data.AppDatabase
import com.example.nutritracker.data.dao.*
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.feature.camera.NutritionResult
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

interface DatabaseTransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

class RoomTransactionRunner(private val db: AppDatabase) : DatabaseTransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T = db.withTransaction { block() }
}

// ── User ─────────────────────────────────────────────────────────────────────

class UserRepository(private val dao: UserDao) {
    suspend fun getUser(): User? = dao.getUser()
    fun getUserFlow(): Flow<User?> = dao.getUserFlow()
    suspend fun upsert(user: User) = dao.upsertUser(user)
    suspend fun hasUser(): Boolean = dao.getUser() != null
}

data class DeleteMealResult(
    val physicallyDeleted: Boolean,
    val deletedImagePath: String? = null
)

// ── Meal ─────────────────────────────────────────────────────────────────────

class MealRepository(
    private val dao: MealDao,
    private val intakeDao: IntakeDao
) {
    suspend fun upsert(meal: Meal): Long = dao.upsert(meal)
    suspend fun getById(id: Long): Meal? = dao.getById(id)
    fun getAllFlow(): Flow<List<Meal>> = dao.getAllFlow()
    fun getLibraryFlow(): Flow<List<Meal>> = dao.getLibraryFlow()
    suspend fun getAll(): List<Meal> = dao.getAll()
    suspend fun search(query: String): List<Meal> = dao.search(query)
    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)
    suspend fun setArchived(id: Long, archived: Boolean) = dao.setArchived(id, archived)
    suspend fun delete(meal: Meal) = dao.delete(meal)

    suspend fun deleteOrArchive(mealId: Long): DeleteMealResult {
        val existing = dao.getById(mealId)
        val refCount = intakeDao.countByMealId(mealId)
        return if (refCount == 0) {
            dao.deleteById(mealId)
            DeleteMealResult(physicallyDeleted = true, deletedImagePath = existing?.localImagePath)
        } else {
            dao.setArchived(mealId, true)
            DeleteMealResult(physicallyDeleted = false, deletedImagePath = null)
        }
    }
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
    suspend fun getWithMealById(id: Long): IntakeWithMeal? = dao.getWithMealById(id)
    suspend fun getByMealId(mealId: Long): Intake? = dao.getByMealId(mealId)
    suspend fun getByMealIdAll(mealId: Long): List<Intake> = dao.getByMealIdAll(mealId)
    suspend fun countByMealId(mealId: Long): Int = dao.countByMealId(mealId)
    suspend fun getAll(): List<Intake> = dao.getAll()
    suspend fun getRecent(limit: Int = 20): List<Intake> = dao.getRecent(limit)
    suspend fun getRecentByType(type: IntakeType, limit: Int = 10): List<Intake> = dao.getRecentByType(type, limit)

    fun getRecentWithMealByTypeFlow(type: IntakeType, limit: Int = 30): Flow<List<IntakeWithMeal>> =
        dao.getRecentWithMealByTypeFlow(type, limit)

    suspend fun getByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): List<Intake> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByDateTimeRange(start, end)
    }

    fun getWithMealByLogicalDayFlow(date: LocalDate, offsetMinutes: Int = 0): Flow<List<IntakeWithMeal>> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getWithMealByRangeFlow(start, end)
    }

    suspend fun getWithMealByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): List<IntakeWithMeal> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getWithMealByRange(start, end)
    }

    fun getWithMealByLogicalDayRangeFlow(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        offsetMinutes: Int = 0
    ): Flow<List<IntakeWithMeal>> {
        val (start, _) = dbc.logicalDayRange(startDate, offsetMinutes)
        val (_, end) = dbc.logicalDayRange(endDateInclusive, offsetMinutes)
        return dao.getWithMealByRangeFlow(start, end)
    }

    suspend fun getWithMealByLogicalDayRange(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        offsetMinutes: Int = 0
    ): List<IntakeWithMeal> {
        val (start, _) = dbc.logicalDayRange(startDate, offsetMinutes)
        val (_, end) = dbc.logicalDayRange(endDateInclusive, offsetMinutes)
        return dao.getWithMealByRange(start, end)
    }

    suspend fun getByTypeAndLogicalDay(type: IntakeType, date: LocalDate, offsetMinutes: Int = 0): List<Intake> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByTypeAndRange(type, start, end)
    }

    fun getWithMealByTypeAndLogicalDayFlow(
        type: IntakeType,
        date: LocalDate,
        offsetMinutes: Int = 0
    ): Flow<List<IntakeWithMeal>> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getWithMealByTypeAndRangeFlow(type, start, end)
    }
}

// ── Recipe ───────────────────────────────────────────────────────────────────

class RecipeRepository(
    private val dao: RecipeDao,
    private val txRunner: DatabaseTransactionRunner
) {
    fun getAllWithIngredientsFlow(): Flow<List<RecipeWithIngredients>> = dao.getAllWithIngredientsFlow()
    suspend fun getAllRecipes(): List<Recipe> = dao.getAllRecipes()
    suspend fun getAllIngredients(): List<RecipeIngredient> = dao.getAllIngredients()
    suspend fun getWithIngredientsById(id: Long): RecipeWithIngredients? = dao.getWithIngredientsById(id)
    suspend fun deleteRecipe(id: Long) = dao.deleteRecipeById(id)

    suspend fun saveRecipe(recipe: Recipe, ingredients: List<Pair<Long, Double>>): Long =
        txRunner.runInTransaction {
            val recipeId = if (recipe.id > 0) {
                dao.upsertRecipe(recipe)
                dao.deleteIngredientsByRecipeId(recipe.id)
                recipe.id
            } else {
                dao.upsertRecipe(recipe)
            }
            if (ingredients.isNotEmpty()) {
                dao.insertIngredients(
                    ingredients.map { (mealId, grams) ->
                        RecipeIngredient(recipeId = recipeId, mealId = mealId, amountGrams = grams)
                    }
                )
            }
            recipeId
        }
}

/**
 * Shared recipe-to-intake snapshot logger used by AddMealViewModel and FoodLibraryViewModel.
 */
class RecipeIntakeManager(
    private val appContext: Context,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc,
    private val gson: Gson
) {
    suspend fun logRecipe(
        recipeWithIngredients: RecipeWithIngredients,
        servings: Double,
        intakeType: IntakeType,
        date: LocalDate
    ): Boolean {
        if (servings <= 0.0 || recipeWithIngredients.ingredients.isEmpty()) return false
        val recipe = recipeWithIngredients.recipe
        val scale = servings / recipe.defaultServings.coerceAtLeast(0.1)
        val subItems = recipeWithIngredients.ingredients.map { ing ->
            val g = ing.ingredient.amountGrams * scale
            val factor = g / 100.0
            NutritionResult.FoodItem(
                name = ing.meal.name,
                weightG = g,
                calories = ing.meal.energyKcal100 * factor,
                protein = ing.meal.proteins100 * factor,
                fat = ing.meal.fat100 * factor,
                carbs = ing.meal.carbohydrates100 * factor,
                fiberG = ing.meal.fiber100?.times(factor),
                sugarsG = ing.meal.sugars100?.times(factor),
                saturatedFatG = ing.meal.saturatedFat100?.times(factor),
                sodiumMg = ing.meal.sodium100?.times(factor),
                cholesterolMg = ing.meal.cholesterol100?.times(factor)
            )
        }
        val totalWeight = subItems.sumOf { it.weightG }.coerceAtLeast(1.0)
        val to100 = 100.0 / totalWeight
        fun sumOpt(pick: (NutritionResult.FoodItem) -> Double?): Double? {
            val vals = subItems.mapNotNull(pick)
            return if (vals.isNotEmpty()) vals.sum() * to100 else null
        }
        val isZh = LocaleHelper.isChinese(appContext)
        val portionUnit = LocaleHelper.tr(isZh, "份", "serving")
        val mealId = mealRepo.upsert(
            Meal(
                name = if (servings == 1.0) recipe.name else if (isZh) "${recipe.name} (${servings}份)" else "${recipe.name} (${servings}x)",
                source = MealSource.RECIPE,
                energyKcal100 = subItems.sumOf { it.calories } * to100,
                carbohydrates100 = subItems.sumOf { it.carbs } * to100,
                fat100 = subItems.sumOf { it.fat } * to100,
                proteins100 = subItems.sumOf { it.protein } * to100,
                fiber100 = sumOpt { it.fiberG },
                sugars100 = sumOpt { it.sugarsG },
                saturatedFat100 = sumOpt { it.saturatedFatG },
                sodium100 = sumOpt { it.sodiumMg },
                cholesterol100 = sumOpt { it.cholesterolMg },
                portionLabel = portionUnit,
                portionGrams = totalWeight / servings,
                isArchived = true,
                foodItemsJson = gson.toJson(subItems)
            )
        )
        val offset = settingsRepo.dayBoundaryMinutes.first()
        val logicalToday = dayBoundaryCalc.currentLogicalDay(offset)
        val dateTime = if (date == logicalToday) LocalDateTime.now() else date.atTime(12, 0)
        intakeRepo.upsert(
            Intake(
                mealId = mealId,
                intakeType = intakeType,
                amount = totalWeight,
                dateTime = dateTime
            )
        )
        trackedDayRepo.saveGoalSnapshot(date, 0.0, 0.0, 0.0, 0.0)
        NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        return true
    }
}

// ── TrackedDay ───────────────────────────────────────────────────────────────

class TrackedDayRepository(private val dao: TrackedDayDao) {
    suspend fun getByDate(date: LocalDate): TrackedDay? = dao.getByDate(date)
    fun getByDateFlow(date: LocalDate): Flow<TrackedDay?> = dao.getByDateFlow(date)
    suspend fun getByDateRange(start: LocalDate, end: LocalDate): List<TrackedDay> = dao.getByDateRange(start, end)
    fun getByDateRangeFlow(start: LocalDate, end: LocalDate): Flow<List<TrackedDay>> = dao.getByDateRangeFlow(start, end)
    suspend fun upsert(day: TrackedDay) = dao.upsert(day)
    suspend fun getAll(): List<TrackedDay> = dao.getAll()
    suspend fun getLatest(): TrackedDay? = dao.getLatest()

    suspend fun saveGoalSnapshot(
        date: LocalDate,
        calorieGoal: Double,
        carbGoal: Double,
        fatGoal: Double,
        proteinGoal: Double,
        forceUpdateGoals: Boolean = false
    ): TrackedDay {
        val existing = dao.getByDate(date)
        if (existing != null) {
            val shouldUpdateGoals = forceUpdateGoals || (existing.calorieGoal == 0.0 && calorieGoal > 0.0)
            if (shouldUpdateGoals) {
                val updated = existing.copy(
                    calorieGoal = calorieGoal,
                    carbsGoal = carbGoal,
                    fatGoal = fatGoal,
                    proteinGoal = proteinGoal
                )
                if (updated != existing) dao.upsert(updated)
                return updated
            }
            return existing
        }
        val created = TrackedDay(
            date = date,
            calorieGoal = calorieGoal,
            carbsGoal = carbGoal,
            fatGoal = fatGoal,
            proteinGoal = proteinGoal
        )
        dao.upsert(created)
        return created
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
    suspend fun getAll(): List<UserActivityEntity> = dao.getAll()
    suspend fun getRecent(limit: Int = 20): List<UserActivityEntity> = dao.getRecent(limit)

    suspend fun getByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): List<UserActivityEntity> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByDateTimeRange(start, end)
    }

    fun getByLogicalDayFlow(date: LocalDate, offsetMinutes: Int = 0): Flow<List<UserActivityEntity>> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByDateTimeRangeFlow(start, end)
    }

    fun getByLogicalDayRangeFlow(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        offsetMinutes: Int = 0
    ): Flow<List<UserActivityEntity>> {
        val (start, _) = dbc.logicalDayRange(startDate, offsetMinutes)
        val (_, end) = dbc.logicalDayRange(endDateInclusive, offsetMinutes)
        return dao.getByDateTimeRangeFlow(start, end)
    }

    suspend fun getTotalBurnedByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): Double =
        getByLogicalDay(date, offsetMinutes).sumOf { it.burnedKcal }
}

// ── WeightLog ────────────────────────────────────────────────────────────────

class WeightLogRepository(private val dao: WeightLogDao) {
    suspend fun upsert(log: WeightLog) = dao.upsert(log)
    suspend fun deleteByDate(date: LocalDate) = dao.deleteByDate(date)
    fun getAllFlow(): Flow<List<WeightLog>> = dao.getAllFlow()
    suspend fun getAll(): List<WeightLog> = dao.getAll()
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
    suspend fun getAll(): List<WaterIntake> = dao.getAll()
    suspend fun getLatest(): WaterIntake? = dao.getLatest()

    suspend fun getByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): List<WaterIntake> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByDateTimeRange(start, end)
    }

    fun getByLogicalDayFlow(date: LocalDate, offsetMinutes: Int = 0): Flow<List<WaterIntake>> {
        val (start, end) = dbc.logicalDayRange(date, offsetMinutes)
        return dao.getByDateTimeRangeFlow(start, end)
    }

    suspend fun getTotalMlByLogicalDay(date: LocalDate, offsetMinutes: Int = 0): Int =
        getByLogicalDay(date, offsetMinutes).sumOf { it.amountMl }

    suspend fun undoLastInLogicalDay(date: LocalDate, offsetMinutes: Int = 0): Boolean {
        val entries = getByLogicalDay(date, offsetMinutes)
        val last = entries.lastOrNull() ?: return false
        dao.deleteById(last.id)
        return true
    }
}
