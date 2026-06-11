package com.example.nutritracker.data

import android.content.Context
import android.net.Uri
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ExportData(
    val version: Int = 1,
    val exportedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val settings: Map<String, Any> = emptyMap(),
    val meals: List<Meal> = emptyList(),
    val intakes: List<Intake> = emptyList(),
    val trackedDays: List<TrackedDay> = emptyList(),
    val activities: List<UserActivityEntity> = emptyList(),
    val weightLogs: List<WeightLog> = emptyList(),
    val waterIntakes: List<WaterIntake> = emptyList(),
    val user: User? = null
)

data class ImportResult(
    val mealsImported: Int = 0,
    val mealsSkipped: Int = 0,
    val intakesImported: Int = 0,
    val trackedDaysImported: Int = 0,
    val trackedDaysSkipped: Int = 0,
    val activitiesImported: Int = 0,
    val weightLogsImported: Int = 0,
    val weightLogsSkipped: Int = 0,
    val waterIntakesImported: Int = 0,
    val userImported: Boolean = false,
    val settingsImported: Boolean = false,
    val imagesImported: Int = 0,
    val imagesSkipped: Int = 0
)

@Singleton
class DataExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val activityRepo: ActivityRepository,
    private val weightLogRepo: WeightLogRepository,
    private val waterRepo: WaterIntakeRepository,
    private val userRepo: UserRepository,
    private val settingsRepo: SettingsRepository
) {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, object : TypeAdapter<LocalDate>() {
            override fun write(out: JsonWriter, value: LocalDate) { out.value(value.toString()) }
            override fun read(input: JsonReader): LocalDate = LocalDate.parse(input.nextString())
        })
        .registerTypeAdapter(LocalDateTime::class.java, object : TypeAdapter<LocalDateTime>() {
            override fun write(out: JsonWriter, value: LocalDateTime) { out.value(value.toString()) }
            override fun read(input: JsonReader): LocalDateTime = LocalDateTime.parse(input.nextString())
        })
        .setPrettyPrinting()
        .create()

    private val thumbnailDir = File(context.filesDir, "meal_thumbnails")

    suspend fun exportData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val meals = mealRepo.getAllFlow().first()
            val allIntakes = intakeRepo.getAll()

            val settings = mapOf(
                "dayBoundaryMinutes" to settingsRepo.dayBoundaryMinutes.first(),
                "kcalAdjustment" to settingsRepo.kcalAdjustment.first(),
                "carbPct" to settingsRepo.carbPct.first(),
                "fatPct" to settingsRepo.fatPct.first(),
                "proteinPct" to settingsRepo.proteinPct.first(),
                "waterGoalMl" to settingsRepo.waterGoalMl.first(),
                "onboardingDone" to settingsRepo.onboardingDone.first()
            )

            val exportData = ExportData(
                settings = settings,
                meals = meals,
                intakes = allIntakes,
                trackedDays = trackedDayRepo.getAll(),
                activities = activityRepo.getAll(),
                weightLogs = weightLogRepo.getAllFlow().first(),
                waterIntakes = waterRepo.getAll(),
                user = userRepo.getUser()
            )

            val dataJson = gson.toJson(exportData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zip ->
                    zip.putNextEntry(ZipEntry("data.json"))
                    zip.write(dataJson.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    if (thumbnailDir.exists()) {
                        thumbnailDir.listFiles()?.forEach { file ->
                            zip.putNextEntry(ZipEntry("images/${file.name}"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importData(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            var result = ImportResult()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val entries = mutableMapOf<String, ByteArray>()
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            entries[entry.name] = zip.readBytes()
                        }
                        entry = zip.nextEntry
                    }
                }

                val dataJson = entries["data.json"]?.toString(Charsets.UTF_8)
                    ?: return@withContext Result.failure(Exception("ZIP 中未找到 data.json"))

                @Suppress("UNCHECKED_CAST")
                val root = gson.fromJson(dataJson, Map::class.java) as Map<String, Any>

                // 导入图片
                val imagesDir = File(context.filesDir, "meal_thumbnails")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                var imgImported = 0
                var imgSkipped = 0
                entries.filter { it.key.startsWith("images/") }.forEach { (path, bytes) ->
                    val fileName = path.removePrefix("images/")
                    val targetFile = File(imagesDir, fileName)
                    if (!targetFile.exists()) {
                        targetFile.writeBytes(bytes)
                        imgImported++
                    } else {
                        imgSkipped++
                    }
                }

                // 解析 meals
                @Suppress("UNCHECKED_CAST")
                val mealsList = (root["meals"] as? List<Map<String, Any>>)?.map { m ->
                    Meal(
                        id = (m["id"] as? Number)?.toLong() ?: 0L,
                        name = m["name"] as? String ?: "",
                        brands = m["brands"] as? String,
                        barcode = m["barcode"] as? String,
                        imageUrl = m["imageUrl"] as? String,
                        localImagePath = m["localImagePath"] as? String,
                        source = try { MealSource.valueOf(m["source"] as? String ?: "MANUAL") } catch (_: Exception) { MealSource.MANUAL },
                        energyKcal100 = (m["energyKcal100"] as? Number)?.toDouble() ?: 0.0,
                        carbohydrates100 = (m["carbohydrates100"] as? Number)?.toDouble() ?: 0.0,
                        fat100 = (m["fat100"] as? Number)?.toDouble() ?: 0.0,
                        proteins100 = (m["proteins100"] as? Number)?.toDouble() ?: 0.0,
                        sugars100 = (m["sugars100"] as? Number)?.toDouble(),
                        saturatedFat100 = (m["saturatedFat100"] as? Number)?.toDouble(),
                        fiber100 = (m["fiber100"] as? Number)?.toDouble(),
                        sodium100 = (m["sodium100"] as? Number)?.toDouble(),
                        cholesterol100 = (m["cholesterol100"] as? Number)?.toDouble(),
                        servingSize = m["servingSize"] as? String,
                        servingQuantityG = (m["servingQuantityG"] as? Number)?.toDouble(),
                        foodItemsJson = m["foodItemsJson"] as? String
                    )
                } ?: emptyList()

                // 修正图片路径
                val adjustedMeals = mealsList.map { meal ->
                    meal.localImagePath?.let { path ->
                        val fileName = File(path).name
                        val localPath = File(imagesDir, fileName).absolutePath
                        meal.copy(localImagePath = if (File(localPath).exists()) localPath else null)
                    } ?: meal
                }

                // 导入 Meals（按 name+source 去重，建立 oldId -> newId 映射）
                val existingMeals = mealRepo.getAllFlow().first()
                val existingMealKeys = existingMeals.map { "${it.name}_${it.source}" }.toSet()
                val idMap = mutableMapOf<Long, Long>()
                var mealsImported = 0
                var mealsSkipped = 0

                for (meal in adjustedMeals) {
                    val key = "${meal.name}_${meal.source}"
                    if (key in existingMealKeys) {
                        mealsSkipped++
                        continue
                    }
                    val newId = mealRepo.upsert(meal.copy(id = 0))
                    idMap[meal.id] = newId
                    mealsImported++
                }

                // 解析 intakes
                @Suppress("UNCHECKED_CAST")
                val intakesList = (root["intakes"] as? List<Map<String, Any>>)?.map { m ->
                    Intake(
                        id = (m["id"] as? Number)?.toLong() ?: 0L,
                        mealId = (m["mealId"] as? Number)?.toLong() ?: 0L,
                        intakeType = try { IntakeType.valueOf(m["intakeType"] as? String ?: "BREAKFAST") } catch (_: Exception) { IntakeType.BREAKFAST },
                        amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                        unit = m["unit"] as? String ?: "g",
                        dateTime = try { LocalDateTime.parse(m["dateTime"] as? String ?: "") } catch (_: Exception) { LocalDateTime.now() }
                    )
                } ?: emptyList()

                // 导入 Intakes（用新的 mealId）
                val adjustedIntakes = intakesList.mapNotNull { intake ->
                    val newMealId = idMap[intake.mealId] ?: return@mapNotNull null
                    intake.copy(id = 0, mealId = newMealId)
                }
                var intakesImported = 0
                for (intake in adjustedIntakes) {
                    intakeRepo.upsert(intake)
                    intakesImported++
                }

                // 解析 trackedDays
                @Suppress("UNCHECKED_CAST")
                val trackedDaysList = (root["trackedDays"] as? List<Map<String, Any>>)?.map { m ->
                    TrackedDay(
                        date = try { LocalDate.parse(m["date"] as? String ?: "") } catch (_: Exception) { LocalDate.now() },
                        calorieGoal = (m["calorieGoal"] as? Number)?.toDouble() ?: 0.0,
                        caloriesTracked = (m["caloriesTracked"] as? Number)?.toDouble() ?: 0.0,
                        carbsGoal = (m["carbsGoal"] as? Number)?.toDouble(),
                        carbsTracked = (m["carbsTracked"] as? Number)?.toDouble(),
                        fatGoal = (m["fatGoal"] as? Number)?.toDouble(),
                        fatTracked = (m["fatTracked"] as? Number)?.toDouble(),
                        proteinGoal = (m["proteinGoal"] as? Number)?.toDouble(),
                        proteinTracked = (m["proteinTracked"] as? Number)?.toDouble()
                    )
                } ?: emptyList()

                // 导入 TrackedDays（按 date 去重）
                var tdImported = 0
                var tdSkipped = 0
                for (td in trackedDaysList) {
                    val existing = trackedDayRepo.getByDate(td.date)
                    if (existing == null) {
                        trackedDayRepo.upsert(td)
                        tdImported++
                    } else {
                        tdSkipped++
                    }
                }

                // 解析 activities
                @Suppress("UNCHECKED_CAST")
                val activitiesList = (root["activities"] as? List<Map<String, Any>>)?.map { m ->
                    UserActivityEntity(
                        id = (m["id"] as? Number)?.toLong() ?: 0L,
                        name = m["name"] as? String ?: "",
                        mets = (m["mets"] as? Number)?.toDouble() ?: 0.0,
                        durationMinutes = (m["durationMinutes"] as? Number)?.toDouble() ?: 0.0,
                        burnedKcal = (m["burnedKcal"] as? Number)?.toDouble() ?: 0.0,
                        dateTime = try { LocalDateTime.parse(m["dateTime"] as? String ?: "") } catch (_: Exception) { LocalDateTime.now() },
                        isCustom = m["isCustom"] as? Boolean ?: false,
                        category = m["category"] as? String
                    )
                } ?: emptyList()

                // 导入 Activities（直接导入）
                var actImported = 0
                for (act in activitiesList) {
                    activityRepo.upsert(act.copy(id = 0))
                    actImported++
                }

                // 解析 weightLogs
                @Suppress("UNCHECKED_CAST")
                val weightLogsList = (root["weightLogs"] as? List<Map<String, Any>>)?.map { m ->
                    WeightLog(
                        date = try { LocalDate.parse(m["date"] as? String ?: "") } catch (_: Exception) { LocalDate.now() },
                        weightKg = (m["weightKg"] as? Number)?.toDouble() ?: 0.0
                    )
                } ?: emptyList()

                // 导入 WeightLogs（按 date 去重）
                var wlImported = 0
                var wlSkipped = 0
                for (wl in weightLogsList) {
                    val existing = weightLogRepo.getByRange(wl.date, wl.date).firstOrNull()
                    if (existing == null) {
                        weightLogRepo.upsert(wl)
                        wlImported++
                    } else {
                        wlSkipped++
                    }
                }

                // 解析 waterIntakes
                @Suppress("UNCHECKED_CAST")
                val waterIntakesList = (root["waterIntakes"] as? List<Map<String, Any>>)?.map { m ->
                    WaterIntake(
                        id = (m["id"] as? Number)?.toLong() ?: 0L,
                        amountMl = (m["amountMl"] as? Number)?.toInt() ?: 0,
                        dateTime = try { LocalDateTime.parse(m["dateTime"] as? String ?: "") } catch (_: Exception) { LocalDateTime.now() }
                    )
                } ?: emptyList()

                // 导入 WaterIntakes（直接导入）
                var wiImported = 0
                for (wi in waterIntakesList) {
                    waterRepo.upsert(wi.copy(id = 0))
                    wiImported++
                }

                // 导入 User（覆盖）
                @Suppress("UNCHECKED_CAST")
                val userMap = root["user"] as? Map<String, Any>
                if (userMap != null) {
                    val user = User(
                        id = (userMap["id"] as? Number)?.toInt() ?: 1,
                        birthday = try { LocalDate.parse(userMap["birthday"] as? String ?: "") } catch (_: Exception) { LocalDate.now() },
                        heightCm = (userMap["heightCm"] as? Number)?.toDouble() ?: 170.0,
                        weightKg = (userMap["weightKg"] as? Number)?.toDouble() ?: 70.0,
                        gender = try { Gender.valueOf(userMap["gender"] as? String ?: "MALE") } catch (_: Exception) { Gender.MALE },
                        activityLevel = try { ActivityLevel.valueOf(userMap["activityLevel"] as? String ?: "SEDENTARY") } catch (_: Exception) { ActivityLevel.SEDENTARY },
                        weightGoal = try { WeightGoal.valueOf(userMap["weightGoal"] as? String ?: "MAINTAIN") } catch (_: Exception) { WeightGoal.MAINTAIN },
                        caloriesProfile = try { CaloriesProfile.valueOf(userMap["caloriesProfile"] as? String ?: "AVERAGED") } catch (_: Exception) { CaloriesProfile.AVERAGED },
                        weeklyWeightGoalKg = (userMap["weeklyWeightGoalKg"] as? Number)?.toDouble(),
                        targetWeightKg = (userMap["targetWeightKg"] as? Number)?.toDouble(),
                        caloriesTaperEnabled = userMap["caloriesTaperEnabled"] as? Boolean ?: false,
                        usesImperialUnits = userMap["usesImperialUnits"] as? Boolean ?: false
                    )
                    userRepo.upsert(user)
                }

                // 导入 Settings
                @Suppress("UNCHECKED_CAST")
                val settings = root["settings"] as? Map<String, Any> ?: emptyMap()
                settings["dayBoundaryMinutes"]?.let { settingsRepo.setDayBoundaryMinutes((it as Number).toInt()) }
                settings["kcalAdjustment"]?.let { settingsRepo.setKcalAdjustment((it as Number).toDouble()) }
                settings["carbPct"]?.let { settingsRepo.setCarbPct((it as Number).toDouble()) }
                settings["fatPct"]?.let { settingsRepo.setFatPct((it as Number).toDouble()) }
                settings["proteinPct"]?.let { settingsRepo.setProteinPct((it as Number).toDouble()) }
                settings["waterGoalMl"]?.let { settingsRepo.setWaterGoalMl((it as Number).toInt()) }
                settings["onboardingDone"]?.let { settingsRepo.setOnboardingDone(it as Boolean) }

                result = ImportResult(
                    mealsImported = mealsImported,
                    mealsSkipped = mealsSkipped,
                    intakesImported = intakesImported,
                    trackedDaysImported = tdImported,
                    trackedDaysSkipped = tdSkipped,
                    activitiesImported = actImported,
                    weightLogsImported = wlImported,
                    weightLogsSkipped = wlSkipped,
                    waterIntakesImported = wiImported,
                    userImported = userMap != null,
                    settingsImported = settings.isNotEmpty(),
                    imagesImported = imgImported,
                    imagesSkipped = imgSkipped
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
