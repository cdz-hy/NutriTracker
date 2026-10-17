package com.example.nutritracker

import com.example.nutritracker.data.DataExportManager
import com.example.nutritracker.data.dao.*
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.DayBoundaryCalc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DataExportManagerTest {

    private class FakeDbState {
        var user: User? = null
        val meals = mutableListOf<Meal>()
        val intakes = mutableListOf<Intake>()
        val recipes = mutableListOf<Recipe>()
        val recipeIngredients = mutableListOf<RecipeIngredient>()
        val trackedDays = mutableMapOf<LocalDate, TrackedDay>()
        val activities = mutableListOf<UserActivityEntity>()
        val weightLogs = mutableMapOf<LocalDate, WeightLog>()
        val waterIntakes = mutableListOf<WaterIntake>()

        var mealSeq = 1L
        var intakeSeq = 1L
        var recipeSeq = 1L
        var recipeIngSeq = 1L
        var activitySeq = 1L
        var waterSeq = 1L
    }

    private fun buildManager(state: FakeDbState): DataExportManager {
        val dbc = DayBoundaryCalc()
        val txRunner = object : DatabaseTransactionRunner {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T = block()
        }
        val userDao = object : UserDao {
            override suspend fun getUser() = state.user
            override fun getUserFlow(): Flow<User?> = MutableStateFlow(state.user)
            override suspend fun upsertUser(user: User) { state.user = user }
        }
        val mealDao = object : MealDao {
            override suspend fun upsert(meal: Meal): Long {
                val id = if (meal.id == 0L) state.mealSeq++ else meal.id
                state.meals.removeAll { it.id == id }
                state.meals.add(meal.copy(id = id))
                return id
            }
            override suspend fun getById(id: Long) = state.meals.find { it.id == id }
            override fun getAllFlow(): Flow<List<Meal>> = MutableStateFlow(state.meals.toList())
            override suspend fun getAll() = state.meals.toList()
            override fun getLibraryFlow(): Flow<List<Meal>> = MutableStateFlow(state.meals.filter { !it.isArchived })
            override suspend fun search(query: String) = state.meals.filter { it.name.contains(query, true) }
            override suspend fun setFavorite(id: Long, favorite: Boolean) {}
            override suspend fun setArchived(id: Long, archived: Boolean) {}
            override suspend fun delete(meal: Meal) { state.meals.remove(meal) }
            override suspend fun deleteById(id: Long) { state.meals.removeAll { it.id == id } }
        }
        val intakeDao = object : IntakeDao {
            override suspend fun upsert(intake: Intake): Long {
                val id = if (intake.id == 0L) state.intakeSeq++ else intake.id
                state.intakes.removeAll { it.id == id }
                state.intakes.add(intake.copy(id = id))
                return id
            }
            override suspend fun delete(intake: Intake) { state.intakes.remove(intake) }
            override suspend fun deleteById(id: Long) { state.intakes.removeAll { it.id == id } }
            override suspend fun getById(id: Long) = state.intakes.find { it.id == id }
            override suspend fun getWithMealById(id: Long): IntakeWithMeal? = null
            override suspend fun getByMealId(mealId: Long) = state.intakes.find { it.mealId == mealId }
            override suspend fun getByMealIdAll(mealId: Long) = state.intakes.filter { it.mealId == mealId }
            override suspend fun countByMealId(mealId: Long) = state.intakes.count { it.mealId == mealId }
            override suspend fun getAll() = state.intakes.toList()
            override suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime) =
                state.intakes.filter { !it.dateTime.isBefore(start) && it.dateTime.isBefore(end) }
            override fun getWithMealByRangeFlow(start: LocalDateTime, end: LocalDateTime): Flow<List<IntakeWithMeal>> =
                MutableStateFlow(emptyList())
            override suspend fun getWithMealByRange(start: LocalDateTime, end: LocalDateTime): List<IntakeWithMeal> = emptyList()
            override suspend fun getByTypeAndRange(type: IntakeType, start: LocalDateTime, end: LocalDateTime) = emptyList<Intake>()
            override fun getWithMealByTypeAndRangeFlow(type: IntakeType, start: LocalDateTime, end: LocalDateTime): Flow<List<IntakeWithMeal>> =
                MutableStateFlow(emptyList())
            override suspend fun getRecent(limit: Int) = state.intakes.take(limit)
            override fun getRecentWithMealByTypeFlow(type: IntakeType, limit: Int): Flow<List<IntakeWithMeal>> = MutableStateFlow(emptyList())
            override suspend fun getRecentByType(type: IntakeType, limit: Int) = emptyList<Intake>()
        }
        val recipeDao = object : RecipeDao {
            override suspend fun upsertRecipe(recipe: Recipe): Long {
                val id = if (recipe.id == 0L) state.recipeSeq++ else recipe.id
                state.recipes.removeAll { it.id == id }
                state.recipes.add(recipe.copy(id = id))
                return id
            }
            override suspend fun insertIngredients(ingredients: List<RecipeIngredient>) {
                ingredients.forEach { state.recipeIngredients.add(it.copy(id = state.recipeIngSeq++)) }
            }
            override suspend fun deleteIngredientsByRecipeId(recipeId: Long) {
                state.recipeIngredients.removeAll { it.recipeId == recipeId }
            }
            override suspend fun deleteRecipeById(id: Long) { state.recipes.removeAll { it.id == id } }
            override suspend fun getAllRecipes() = state.recipes.toList()
            override suspend fun getAllIngredients() = state.recipeIngredients.toList()
            override fun getAllWithIngredientsFlow(): Flow<List<RecipeWithIngredients>> = MutableStateFlow(emptyList())
            override suspend fun getWithIngredientsById(id: Long): RecipeWithIngredients? = null
        }
        val trackedDayDao = object : TrackedDayDao {
            override suspend fun upsert(day: TrackedDay) { state.trackedDays[day.date] = day }
            override suspend fun getByDate(date: LocalDate) = state.trackedDays[date]
            override fun getByDateFlow(date: LocalDate): Flow<TrackedDay?> = MutableStateFlow(state.trackedDays[date])
            override suspend fun getAll() = state.trackedDays.values.toList()
            override suspend fun getByDateRange(start: LocalDate, end: LocalDate) =
                state.trackedDays.values.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
            override fun getByDateRangeFlow(start: LocalDate, end: LocalDate): Flow<List<TrackedDay>> = MutableStateFlow(emptyList())
            override suspend fun getLatest() = state.trackedDays.values.maxByOrNull { it.date }
        }
        val activityDao = object : ActivityDao {
            override suspend fun upsert(activity: UserActivityEntity): Long {
                val id = if (activity.id == 0L) state.activitySeq++ else activity.id
                state.activities.removeAll { it.id == id }
                state.activities.add(activity.copy(id = id))
                return id
            }
            override suspend fun delete(activity: UserActivityEntity) { state.activities.remove(activity) }
            override suspend fun deleteById(id: Long) { state.activities.removeAll { it.id == id } }
            override suspend fun getAll() = state.activities.toList()
            override suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime) = emptyList<UserActivityEntity>()
            override fun getByDateTimeRangeFlow(start: LocalDateTime, end: LocalDateTime): Flow<List<UserActivityEntity>> = MutableStateFlow(emptyList())
            override suspend fun getRecent(limit: Int) = state.activities.take(limit)
        }
        val weightLogDao = object : WeightLogDao {
            override suspend fun upsert(log: WeightLog) { state.weightLogs[log.date] = log }
            override suspend fun deleteByDate(date: LocalDate) { state.weightLogs.remove(date) }
            override fun getAllFlow(): Flow<List<WeightLog>> = MutableStateFlow(state.weightLogs.values.toList())
            override suspend fun getAll() = state.weightLogs.values.toList()
            override suspend fun getLatest() = state.weightLogs.values.maxByOrNull { it.date }
            override suspend fun getByRange(start: LocalDate, end: LocalDate) =
                state.weightLogs.values.filter { !it.date.isBefore(start) && !it.date.isAfter(end) }
        }
        val waterDao = object : WaterIntakeDao {
            override suspend fun upsert(entry: WaterIntake): Long {
                val id = if (entry.id == 0L) state.waterSeq++ else entry.id
                state.waterIntakes.removeAll { it.id == id }
                state.waterIntakes.add(entry.copy(id = id))
                return id
            }
            override suspend fun delete(entry: WaterIntake) { state.waterIntakes.remove(entry) }
            override suspend fun deleteById(id: Long) { state.waterIntakes.removeAll { it.id == id } }
            override suspend fun getAll() = state.waterIntakes.toList()
            override suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime) = emptyList<WaterIntake>()
            override fun getByDateTimeRangeFlow(start: LocalDateTime, end: LocalDateTime): Flow<List<WaterIntake>> = MutableStateFlow(emptyList())
            override suspend fun getLatest() = state.waterIntakes.maxByOrNull { it.dateTime }
        }

        return DataExportManager(
            context = null,
            mealRepo = MealRepository(mealDao, intakeDao),
            intakeRepo = IntakeRepository(intakeDao, dbc),
            recipeRepo = RecipeRepository(recipeDao, txRunner),
            trackedDayRepo = TrackedDayRepository(trackedDayDao),
            activityRepo = ActivityRepository(activityDao, dbc),
            weightLogRepo = WeightLogRepository(weightLogDao),
            waterRepo = WaterIntakeRepository(waterDao, dbc),
            userRepo = UserRepository(userDao),
            settingsRepo = null,
            txRunner = txRunner
        )
    }

    @Test
    fun `export and import round trip preserves all entities even with existing duplicate meals`() = runTest {
        val srcState = FakeDbState()
        srcState.user = User(
            birthday = LocalDate.of(1992, 3, 10),
            heightCm = 172.0,
            weightKg = 68.5,
            gender = Gender.FEMALE,
            activityLevel = ActivityLevel.ACTIVE,
            weightGoal = WeightGoal.LOSE
        )
        srcState.meals.add(Meal(id = 10, name = "燕麦牛奶", source = MealSource.MANUAL, energyKcal100 = 150.0, fiber100 = 3.5))
        srcState.intakes.add(
            Intake(
                id = 5,
                mealId = 10,
                intakeType = IntakeType.BREAKFAST,
                amount = 250.0,
                dateTime = LocalDateTime.of(2026, 11, 10, 8, 30)
            )
        )
        srcState.recipes.add(Recipe(id = 3, name = "减脂早餐组合", defaultServings = 1.0))
        srcState.recipeIngredients.add(RecipeIngredient(id = 1, recipeId = 3, mealId = 10, amountGrams = 200.0))
        srcState.trackedDays[LocalDate.of(2026, 11, 10)] = TrackedDay(date = LocalDate.of(2026, 11, 10), calorieGoal = 1800.0)
        srcState.activities.add(
            UserActivityEntity(id = 2, name = "慢跑", mets = 7.0, durationMinutes = 30.0, burnedKcal = 240.0, dateTime = LocalDateTime.of(2026, 11, 10, 18, 0))
        )
        srcState.weightLogs[LocalDate.of(2026, 11, 10)] = WeightLog(date = LocalDate.of(2026, 11, 10), weightKg = 68.5)
        srcState.waterIntakes.add(WaterIntake(id = 7, amountMl = 300, dateTime = LocalDateTime.of(2026, 11, 10, 10, 0)))

        val srcDir = Files.createTempDirectory("nt_export_src").toFile()
        val dstDir = Files.createTempDirectory("nt_export_dst").toFile()
        try {
            val zipOut = ByteArrayOutputStream()
            buildManager(srcState).exportToStream(zipOut, srcDir)

            // Pre-populate destination with the SAME meal under a different ID (99L) to verify ID remapping!
            val dstState = FakeDbState()
            dstState.meals.add(Meal(id = 99, name = "燕麦牛奶", source = MealSource.MANUAL, energyKcal100 = 150.0))
            dstState.mealSeq = 100L

            val result = buildManager(dstState).importFromStream(ByteArrayInputStream(zipOut.toByteArray()), dstDir)

            assertEquals(0, result.mealsImported)
            assertEquals(1, result.mealsSkipped)
            assertEquals(1, result.intakesImported) // MUST NOT be dropped when meal is skipped!
            assertEquals(99L, dstState.intakes.first().mealId)
            assertEquals(1, result.recipesImported)
            assertEquals(99L, dstState.recipeIngredients.first().mealId)
            assertEquals(1, result.trackedDaysImported)
            assertEquals(1, result.activitiesImported)
            assertEquals(1, result.weightLogsImported)
            assertEquals(1, result.waterIntakesImported)
            assertTrue(result.userImported)
        } finally {
            srcDir.deleteRecursively()
            dstDir.deleteRecursively()
        }
    }

    @Test
    fun `import rejects zip slip path traversal`() = runTest {
        val dstDir = Files.createTempDirectory("nt_zipslip").toFile()
        val parentEscapeTarget = File(dstDir.parentFile, "evil_pwned.txt")
        parentEscapeTarget.delete()

        try {
            val baos = ByteArrayOutputStream()
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                zip.write("""{"version":2,"meals":[]}""".toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("images/../../evil_pwned.txt"))
                zip.write("malicious".toByteArray())
                zip.closeEntry()
            }

            buildManager(FakeDbState()).importFromStream(ByteArrayInputStream(baos.toByteArray()), dstDir)
            assertFalse("Zip slip escaped outside target directory", parentEscapeTarget.exists())
        } finally {
            parentEscapeTarget.delete()
            dstDir.deleteRecursively()
        }
    }
}
