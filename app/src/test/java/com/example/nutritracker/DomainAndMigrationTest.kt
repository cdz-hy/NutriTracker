package com.example.nutritracker

import com.example.nutritracker.data.AppDatabase
import com.example.nutritracker.data.ExportBundle
import com.example.nutritracker.data.dao.IntakeDao
import com.example.nutritracker.data.dao.IntakeWithMeal
import com.example.nutritracker.data.dao.MealDao
import com.example.nutritracker.data.entity.AppLanguage
import com.example.nutritracker.data.entity.Intake
import com.example.nutritracker.data.entity.IntakeType
import com.example.nutritracker.data.entity.Meal
import com.example.nutritracker.data.entity.PhysicalActivityDatabase
import com.example.nutritracker.data.repository.MealRepository
import com.example.nutritracker.util.BmiCalc
import com.example.nutritracker.util.BmiStandard
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.util.MicronutrientKey
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DomainAndMigrationTest {

    @Test
    fun `intakeType suggestForTime covers meal windows cleanly`() {
        assertEquals(IntakeType.SNACK, IntakeType.suggestForTime(LocalTime.of(4, 59)))
        assertEquals(IntakeType.BREAKFAST, IntakeType.suggestForTime(LocalTime.of(5, 0)))
        assertEquals(IntakeType.BREAKFAST, IntakeType.suggestForTime(LocalTime.of(10, 29)))
        assertEquals(IntakeType.LUNCH, IntakeType.suggestForTime(LocalTime.of(10, 30)))
        assertEquals(IntakeType.LUNCH, IntakeType.suggestForTime(LocalTime.of(14, 29)))
        assertEquals(IntakeType.SNACK, IntakeType.suggestForTime(LocalTime.of(14, 30)))
        assertEquals(IntakeType.SNACK, IntakeType.suggestForTime(LocalTime.of(16, 59)))
        assertEquals(IntakeType.DINNER, IntakeType.suggestForTime(LocalTime.of(17, 0)))
        assertEquals(IntakeType.DINNER, IntakeType.suggestForTime(LocalTime.of(21, 29)))
        assertEquals(IntakeType.SNACK, IntakeType.suggestForTime(LocalTime.of(21, 30)))
    }

    @Test
    fun `mealRepository deleteOrArchive cleans unreferenced meals and archives referenced ones`() = runTest {
        val meals = mutableListOf(
            Meal(id = 1L, name = "鸡胸肉", localImagePath = "/tmp/meal_1.jpg"),
            Meal(id = 2L, name = "燕麦粥", localImagePath = "/tmp/meal_2.jpg")
        )
        val intakes = mutableListOf(
            Intake(id = 10L, mealId = 2L, intakeType = IntakeType.BREAKFAST, amount = 100.0, dateTime = LocalDateTime.now())
        )

        val fakeMealDao = object : MealDao {
            override suspend fun upsert(meal: Meal) = meal.id
            override suspend fun getById(id: Long) = meals.find { it.id == id }
            override fun getAllFlow(): Flow<List<Meal>> = MutableStateFlow(meals)
            override suspend fun getAll() = meals
            override fun getLibraryFlow(): Flow<List<Meal>> = MutableStateFlow(meals.filter { !it.isArchived })
            override suspend fun search(query: String) = meals
            override suspend fun setFavorite(id: Long, favorite: Boolean) {}
            override suspend fun setArchived(id: Long, archived: Boolean) {
                val idx = meals.indexOfFirst { it.id == id }
                if (idx >= 0) meals[idx] = meals[idx].copy(isArchived = archived)
            }
            override suspend fun delete(meal: Meal) { meals.remove(meal) }
            override suspend fun deleteById(id: Long) { meals.removeAll { it.id == id } }
        }

        val fakeIntakeDao = object : IntakeDao {
            override suspend fun upsert(intake: Intake) = intake.id
            override suspend fun delete(intake: Intake) {}
            override suspend fun deleteById(id: Long) {}
            override suspend fun getById(id: Long) = intakes.find { it.id == id }
            override suspend fun getWithMealById(id: Long): IntakeWithMeal? = null
            override suspend fun getByMealId(mealId: Long) = intakes.find { it.mealId == mealId }
            override suspend fun getByMealIdAll(mealId: Long) = intakes.filter { it.mealId == mealId }
            override suspend fun countByMealId(mealId: Long) = intakes.count { it.mealId == mealId }
            override suspend fun getAll() = intakes
            override suspend fun getByDateTimeRange(start: LocalDateTime, end: LocalDateTime) = emptyList<Intake>()
            override fun getWithMealByRangeFlow(start: LocalDateTime, end: LocalDateTime): Flow<List<IntakeWithMeal>> =
                MutableStateFlow(emptyList())
            override suspend fun getWithMealByRange(start: LocalDateTime, end: LocalDateTime) = emptyList<IntakeWithMeal>()
            override fun getWithMealByTypeAndRangeFlow(type: IntakeType, start: LocalDateTime, end: LocalDateTime) =
                MutableStateFlow(emptyList<IntakeWithMeal>())
            override suspend fun getByTypeAndRange(type: IntakeType, start: LocalDateTime, end: LocalDateTime) = emptyList<Intake>()
            override suspend fun getRecent(limit: Int) = emptyList<Intake>()
            override suspend fun getRecentByType(type: IntakeType, limit: Int) = emptyList<Intake>()
            override fun getRecentWithMealByTypeFlow(type: IntakeType, limit: Int): Flow<List<IntakeWithMeal>> =
                MutableStateFlow(emptyList())
        }

        val repo = MealRepository(fakeMealDao, fakeIntakeDao)

        val unreferencedResult = repo.deleteOrArchive(1L)
        assertTrue(unreferencedResult.physicallyDeleted)
        assertEquals("/tmp/meal_1.jpg", unreferencedResult.deletedImagePath)
        assertNull(fakeMealDao.getById(1L))

        val referencedResult = repo.deleteOrArchive(2L)
        assertFalse(referencedResult.physicallyDeleted)
        assertNull(referencedResult.deletedImagePath)
        assertTrue(fakeMealDao.getById(2L)!!.isArchived)
    }

    @Test
    fun `room v2 schema and migration versions stay aligned`() {
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)

        val schemaFile = listOf(
            File("schemas/com.example.nutritracker.data.AppDatabase/2.json"),
            File("app/schemas/com.example.nutritracker.data.AppDatabase/2.json")
        ).firstOrNull { it.exists() }
        assertNotNull("Exported Room schema 2.json should exist", schemaFile)

        val root = Gson().fromJson(schemaFile!!.readText(), Map::class.java)
        val db = root["database"] as Map<*, *>
        assertEquals(2.0, (db["version"] as Number).toDouble(), 0.0)

        @Suppress("UNCHECKED_CAST")
        val entities = db["entities"] as List<Map<String, Any>>
        val tableNames = entities.map { it["tableName"] as String }.toSet()
        assertEquals(
            setOf(
                "user",
                "meals",
                "intakes",
                "recipes",
                "recipe_ingredients",
                "tracked_days",
                "activities",
                "weight_logs",
                "water_intakes"
            ),
            tableNames
        )
    }

    @Test
    fun `bilingual localization helpers and search support both zh and en`() {
        assertEquals("早餐", LocaleHelper.mealTypeLabel(IntakeType.BREAKFAST, isZh = true))
        assertEquals("Breakfast", LocaleHelper.mealTypeLabel(IntakeType.BREAKFAST, isZh = false))
        assertEquals("跟随系统", LocaleHelper.appLanguageLabel(AppLanguage.SYSTEM, isZh = true))
        assertEquals("System", LocaleHelper.appLanguageLabel(AppLanguage.SYSTEM, isZh = false))
        assertEquals("English", LocaleHelper.appLanguageLabel(AppLanguage.EN, isZh = true))
        assertEquals("中文", LocaleHelper.appLanguageLabel(AppLanguage.ZH, isZh = false))

        val date = LocalDate.of(2026, 11, 18)
        assertTrue(LocaleHelper.formatFullDate(date, isZh = true).contains("2026年11月18日"))
        assertTrue(LocaleHelper.formatFullDate(date, isZh = false).contains("Nov 18, 2026"))
        assertEquals("11月18日热量", LocaleHelper.formatDayCaloriesTitle(date, isZh = true))
        assertEquals("Nov 18 Calories", LocaleHelper.formatDayCaloriesTitle(date, isZh = false))

        val zhMatch = PhysicalActivityDatabase.searchActivities("游泳")
        val enMatch = PhysicalActivityDatabase.searchActivities("Swimming")
        assertTrue(zhMatch.isNotEmpty() && zhMatch == enMatch)
        assertEquals("Swimming", enMatch.first().displayName(isZh = false))
        assertEquals("游泳", enMatch.first().displayName(isZh = true))
        assertTrue(PhysicalActivityDatabase.searchActivities("Water Sports").any { it.code == "18350" })

        assertEquals("Overweight", BmiCalc.NutritionalStatus.PRE_OBESITY.getLabel(BmiStandard.CHINA, isZh = false))
        assertEquals("Pre-obesity", BmiCalc.NutritionalStatus.PRE_OBESITY.getLabel(BmiStandard.WHO, isZh = false))
        assertEquals("Fiber", MicronutrientKey.FIBER.displayLabel(isZh = false))
        assertEquals("膳食纤维", MicronutrientKey.FIBER.displayLabel(isZh = true))

        val json = Gson().toJson(ExportBundle.ExportSettings(appLanguage = AppLanguage.EN.name))
        val parsed = Gson().fromJson(json, ExportBundle.ExportSettings::class.java)
        assertEquals(AppLanguage.EN.name, parsed.appLanguage)
    }
}
