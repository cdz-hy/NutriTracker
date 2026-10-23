package com.example.nutritracker.data

import android.content.Context
import android.net.Uri
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.LocaleHelper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ExportBundle(
    @SerializedName("version") val version: Int = 2,
    @SerializedName("exportedAt") val exportedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    @SerializedName("settings") val settings: ExportSettings? = null,
    @SerializedName("meals") val meals: List<ExportMeal> = emptyList(),
    @SerializedName("intakes") val intakes: List<ExportIntake> = emptyList(),
    @SerializedName("recipes") val recipes: List<ExportRecipe> = emptyList(),
    @SerializedName("recipeIngredients") val recipeIngredients: List<ExportRecipeIngredient> = emptyList(),
    @SerializedName("trackedDays") val trackedDays: List<ExportTrackedDay> = emptyList(),
    @SerializedName("activities") val activities: List<ExportActivity> = emptyList(),
    @SerializedName("weightLogs") val weightLogs: List<ExportWeightLog> = emptyList(),
    @SerializedName("waterIntakes") val waterIntakes: List<ExportWaterIntake> = emptyList(),
    @SerializedName("user") val user: ExportUser? = null
) {
    data class ExportSettings(
        @SerializedName("dayBoundaryMinutes") val dayBoundaryMinutes: Int? = null,
        @SerializedName("kcalAdjustment") val kcalAdjustment: Double? = null,
        @SerializedName("carbPct") val carbPct: Double? = null,
        @SerializedName("fatPct") val fatPct: Double? = null,
        @SerializedName("proteinPct") val proteinPct: Double? = null,
        @SerializedName("waterGoalMl") val waterGoalMl: Int? = null,
        @SerializedName("waterQuickAmounts") val waterQuickAmounts: List<Int>? = null,
        @SerializedName("onboardingDone") val onboardingDone: Boolean? = null,
        @SerializedName("themeMode") val themeMode: String? = null,
        @SerializedName("dynamicColorEnabled") val dynamicColorEnabled: Boolean? = null,
        @SerializedName("useKj") val useKj: Boolean? = null,
        @SerializedName("aiAutoConfirm") val aiAutoConfirm: Boolean? = null,
        @SerializedName("appLanguage") val appLanguage: String? = null
    )

    data class ExportMeal(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("name") val name: String = "",
        @SerializedName("localImagePath") val localImagePath: String? = null,
        @SerializedName("source") val source: String = MealSource.MANUAL.name,
        @SerializedName("energyKcal100") val energyKcal100: Double = 0.0,
        @SerializedName("carbohydrates100") val carbohydrates100: Double = 0.0,
        @SerializedName("fat100") val fat100: Double = 0.0,
        @SerializedName("proteins100") val proteins100: Double = 0.0,
        @SerializedName("sugars100") val sugars100: Double? = null,
        @SerializedName("saturatedFat100") val saturatedFat100: Double? = null,
        @SerializedName("fiber100") val fiber100: Double? = null,
        @SerializedName("sodium100") val sodium100: Double? = null,
        @SerializedName("cholesterol100") val cholesterol100: Double? = null,
        @SerializedName("portionLabel") val portionLabel: String? = null,
        @SerializedName("portionGrams") val portionGrams: Double? = null,
        @SerializedName("isFavorite") val isFavorite: Boolean = false,
        @SerializedName("isArchived") val isArchived: Boolean = false,
        @SerializedName("foodItemsJson") val foodItemsJson: String? = null
    )

    data class ExportIntake(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("mealId") val mealId: Long = 0,
        @SerializedName("intakeType") val intakeType: String = IntakeType.BREAKFAST.name,
        @SerializedName("amount") val amount: Double = 0.0,
        @SerializedName("unit") val unit: String = "g",
        @SerializedName("dateTime") val dateTime: String = ""
    )

    data class ExportRecipe(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("name") val name: String = "",
        @SerializedName("description") val description: String = "",
        @SerializedName("defaultServings") val defaultServings: Double = 1.0,
        @SerializedName("createdAt") val createdAt: String = ""
    )

    data class ExportRecipeIngredient(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("recipeId") val recipeId: Long = 0,
        @SerializedName("mealId") val mealId: Long = 0,
        @SerializedName("amountGrams") val amountGrams: Double = 0.0
    )

    data class ExportTrackedDay(
        @SerializedName("date") val date: String = "",
        @SerializedName("calorieGoal") val calorieGoal: Double = 0.0,
        @SerializedName("caloriesTracked") val caloriesTracked: Double = 0.0,
        @SerializedName("carbsGoal") val carbsGoal: Double? = null,
        @SerializedName("carbsTracked") val carbsTracked: Double? = null,
        @SerializedName("fatGoal") val fatGoal: Double? = null,
        @SerializedName("fatTracked") val fatTracked: Double? = null,
        @SerializedName("proteinGoal") val proteinGoal: Double? = null,
        @SerializedName("proteinTracked") val proteinTracked: Double? = null
    )

    data class ExportActivity(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("name") val name: String = "",
        @SerializedName("mets") val mets: Double = 0.0,
        @SerializedName("durationMinutes") val durationMinutes: Double = 0.0,
        @SerializedName("burnedKcal") val burnedKcal: Double = 0.0,
        @SerializedName("dateTime") val dateTime: String = "",
        @SerializedName("isCustom") val isCustom: Boolean = false,
        @SerializedName("category") val category: String? = null
    )

    data class ExportWeightLog(
        @SerializedName("date") val date: String = "",
        @SerializedName("weightKg") val weightKg: Double = 0.0
    )

    data class ExportWaterIntake(
        @SerializedName("id") val id: Long = 0,
        @SerializedName("amountMl") val amountMl: Int = 0,
        @SerializedName("dateTime") val dateTime: String = ""
    )

    data class ExportUser(
        @SerializedName("id") val id: Int = 1,
        @SerializedName("birthday") val birthday: String = "",
        @SerializedName("heightCm") val heightCm: Double = 170.0,
        @SerializedName("weightKg") val weightKg: Double = 70.0,
        @SerializedName("gender") val gender: String = Gender.MALE.name,
        @SerializedName("activityLevel") val activityLevel: String = ActivityLevel.SEDENTARY.name,
        @SerializedName("weightGoal") val weightGoal: String = WeightGoal.MAINTAIN.name,
        @SerializedName("caloriesProfile") val caloriesProfile: String = CaloriesProfile.AVERAGED.name,
        @SerializedName("weeklyWeightGoalKg") val weeklyWeightGoalKg: Double? = null,
        @SerializedName("targetWeightKg") val targetWeightKg: Double? = null,
        @SerializedName("caloriesTaperEnabled") val caloriesTaperEnabled: Boolean = false,
        @SerializedName("usesImperialUnits") val usesImperialUnits: Boolean = false
    )
}

data class ImportResult(
    val mealsImported: Int = 0,
    val mealsSkipped: Int = 0,
    val intakesImported: Int = 0,
    val recipesImported: Int = 0,
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
    @ApplicationContext private val context: Context?,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val recipeRepo: RecipeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val activityRepo: ActivityRepository,
    private val weightLogRepo: WeightLogRepository,
    private val waterRepo: WaterIntakeRepository,
    private val userRepo: UserRepository,
    private val settingsRepo: SettingsRepository?,
    private val txRunner: DatabaseTransactionRunner
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

    suspend fun exportData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val isZh = context?.let { LocaleHelper.isChinese(it) } ?: LocaleHelper.isCurrentLocaleChinese()
            val ctx = context ?: return@withContext Result.failure(
                IllegalStateException(LocaleHelper.tr(isZh, "Context 不可用", "Context unavailable"))
            )
            val thumbnailDir = File(ctx.filesDir, "meal_thumbnails")
            val stream = ctx.contentResolver.openOutputStream(uri)
                ?: return@withContext Result.failure(
                    IOException(LocaleHelper.tr(isZh, "无法打开导出文件输出流", "Unable to open export output stream"))
                )
            stream.use { outputStream ->
                exportToStream(outputStream, thumbnailDir)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportToStream(outputStream: OutputStream, thumbnailDir: File) {
        val meals = mealRepo.getAll()
        val allIntakes = intakeRepo.getAll()
        val recipes = recipeRepo.getAllRecipes()
        val recipeIngredients = recipeRepo.getAllIngredients()
        val trackedDays = trackedDayRepo.getAll()
        val activities = activityRepo.getAll()
        val weightLogs = weightLogRepo.getAll()
        val waterIntakes = waterRepo.getAll()
        val user = userRepo.getUser()

        val exportSettings = settingsRepo?.let { s ->
            ExportBundle.ExportSettings(
                dayBoundaryMinutes = s.dayBoundaryMinutes.first(),
                kcalAdjustment = s.kcalAdjustment.first(),
                carbPct = s.carbPct.first(),
                fatPct = s.fatPct.first(),
                proteinPct = s.proteinPct.first(),
                waterGoalMl = s.waterGoalMl.first(),
                waterQuickAmounts = s.waterQuickAmounts.first(),
                onboardingDone = s.onboardingDone.first(),
                themeMode = s.themeMode.first().name,
                dynamicColorEnabled = s.dynamicColorEnabled.first(),
                useKj = s.useKj.first(),
                aiAutoConfirm = s.aiAutoConfirm.first(),
                appLanguage = s.appLanguage.first().name
            )
        }

        val bundle = ExportBundle(
            settings = exportSettings,
            meals = meals.map { m ->
                ExportBundle.ExportMeal(
                    id = m.id,
                    name = m.name,
                    localImagePath = m.localImagePath,
                    source = m.source.name,
                    energyKcal100 = m.energyKcal100,
                    carbohydrates100 = m.carbohydrates100,
                    fat100 = m.fat100,
                    proteins100 = m.proteins100,
                    sugars100 = m.sugars100,
                    saturatedFat100 = m.saturatedFat100,
                    fiber100 = m.fiber100,
                    sodium100 = m.sodium100,
                    cholesterol100 = m.cholesterol100,
                    portionLabel = m.portionLabel,
                    portionGrams = m.portionGrams,
                    isFavorite = m.isFavorite,
                    isArchived = m.isArchived,
                    foodItemsJson = m.foodItemsJson
                )
            },
            intakes = allIntakes.map { i ->
                ExportBundle.ExportIntake(
                    id = i.id,
                    mealId = i.mealId,
                    intakeType = i.intakeType.name,
                    amount = i.amount,
                    unit = i.unit,
                    dateTime = i.dateTime.toString()
                )
            },
            recipes = recipes.map { r ->
                ExportBundle.ExportRecipe(
                    id = r.id,
                    name = r.name,
                    description = r.description,
                    defaultServings = r.defaultServings,
                    createdAt = r.createdAt.toString()
                )
            },
            recipeIngredients = recipeIngredients.map { ri ->
                ExportBundle.ExportRecipeIngredient(
                    id = ri.id,
                    recipeId = ri.recipeId,
                    mealId = ri.mealId,
                    amountGrams = ri.amountGrams
                )
            },
            trackedDays = trackedDays.map { td ->
                ExportBundle.ExportTrackedDay(
                    date = td.date.toString(),
                    calorieGoal = td.calorieGoal,
                    caloriesTracked = td.caloriesTracked,
                    carbsGoal = td.carbsGoal,
                    carbsTracked = td.carbsTracked,
                    fatGoal = td.fatGoal,
                    fatTracked = td.fatTracked,
                    proteinGoal = td.proteinGoal,
                    proteinTracked = td.proteinTracked
                )
            },
            activities = activities.map { a ->
                ExportBundle.ExportActivity(
                    id = a.id,
                    name = a.name,
                    mets = a.mets,
                    durationMinutes = a.durationMinutes,
                    burnedKcal = a.burnedKcal,
                    dateTime = a.dateTime.toString(),
                    isCustom = a.isCustom,
                    category = a.category
                )
            },
            weightLogs = weightLogs.map { wl ->
                ExportBundle.ExportWeightLog(
                    date = wl.date.toString(),
                    weightKg = wl.weightKg
                )
            },
            waterIntakes = waterIntakes.map { wi ->
                ExportBundle.ExportWaterIntake(
                    id = wi.id,
                    amountMl = wi.amountMl,
                    dateTime = wi.dateTime.toString()
                )
            },
            user = user?.let { u ->
                ExportBundle.ExportUser(
                    id = u.id,
                    birthday = u.birthday.toString(),
                    heightCm = u.heightCm,
                    weightKg = u.weightKg,
                    gender = u.gender.name,
                    activityLevel = u.activityLevel.name,
                    weightGoal = u.weightGoal.name,
                    caloriesProfile = u.caloriesProfile.name,
                    weeklyWeightGoalKg = u.weeklyWeightGoalKg,
                    targetWeightKg = u.targetWeightKg,
                    caloriesTaperEnabled = u.caloriesTaperEnabled,
                    usesImperialUnits = u.usesImperialUnits
                )
            }
        )

        val dataJson = gson.toJson(bundle)
        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(dataJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            if (thumbnailDir.exists()) {
                thumbnailDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        zip.putNextEntry(ZipEntry("images/${file.name}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    suspend fun importData(uri: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            val isZh = context?.let { LocaleHelper.isChinese(it) } ?: LocaleHelper.isCurrentLocaleChinese()
            val ctx = context ?: return@withContext Result.failure(
                IllegalStateException(LocaleHelper.tr(isZh, "Context 不可用", "Context unavailable"))
            )
            val imagesDir = File(ctx.filesDir, "meal_thumbnails")
            val stream = ctx.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(
                    IOException(LocaleHelper.tr(isZh, "无法打开导入文件", "Unable to open import file"))
                )
            val result = stream.use { inputStream ->
                importFromStream(inputStream, imagesDir)
            }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromStream(inputStream: InputStream, imagesDir: File): ImportResult {
        val isZh = context?.let { LocaleHelper.isChinese(it) } ?: LocaleHelper.isCurrentLocaleChinese()
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
            ?: throw IOException(LocaleHelper.tr(isZh, "ZIP 中未找到 data.json", "data.json not found in ZIP"))

        val bundle = gson.fromJson(dataJson, ExportBundle::class.java)
            ?: throw IOException(LocaleHelper.tr(isZh, "data.json 解析为空", "data.json parsed as empty"))

        if (!imagesDir.exists()) imagesDir.mkdirs()
        val canonicalImagesRoot = imagesDir.canonicalFile

        var imgImported = 0
        var imgSkipped = 0
        entries.filterKeys { it.startsWith("images/") }.forEach { (path, bytes) ->
            val rawName = path.removePrefix("images/")
            val safeName = File(rawName).name
            if (safeName.isBlank() || safeName == "." || safeName == "..") return@forEach
            val targetFile = File(imagesDir, safeName).canonicalFile
            if (targetFile.parentFile != canonicalImagesRoot) {
                return@forEach // Zip Slip protection
            }
            if (!targetFile.exists()) {
                targetFile.writeBytes(bytes)
                imgImported++
            } else {
                imgSkipped++
            }
        }

        return txRunner.runInTransaction {
            val existingMeals = mealRepo.getAll()
            val existingByKey = existingMeals.associateBy { "${it.name}_${it.source.name}" }.toMutableMap()
            val mealIdMap = mutableMapOf<Long, Long>()
            var mealsImported = 0
            var mealsSkipped = 0

            for (em in bundle.meals) {
                val source = runCatching { MealSource.valueOf(em.source) }.getOrDefault(MealSource.MANUAL)
                val key = "${em.name}_${source.name}"
                val existing = existingByKey[key]
                if (existing != null) {
                    mealIdMap[em.id] = existing.id
                    mealsSkipped++
                    continue
                }
                val resolvedImg = em.localImagePath?.let { path ->
                    val candidate = File(imagesDir, File(path).name)
                    if (candidate.exists()) candidate.absolutePath else null
                }
                val entity = Meal(
                    id = 0,
                    name = em.name,
                    localImagePath = resolvedImg,
                    source = source,
                    energyKcal100 = em.energyKcal100,
                    carbohydrates100 = em.carbohydrates100,
                    fat100 = em.fat100,
                    proteins100 = em.proteins100,
                    sugars100 = em.sugars100,
                    saturatedFat100 = em.saturatedFat100,
                    fiber100 = em.fiber100,
                    sodium100 = em.sodium100,
                    cholesterol100 = em.cholesterol100,
                    portionLabel = em.portionLabel,
                    portionGrams = em.portionGrams,
                    isFavorite = em.isFavorite,
                    isArchived = em.isArchived,
                    foodItemsJson = em.foodItemsJson
                )
                val newId = mealRepo.upsert(entity)
                mealIdMap[em.id] = newId
                existingByKey[key] = entity.copy(id = newId)
                mealsImported++
            }

            var intakesImported = 0
            for (ei in bundle.intakes) {
                val mappedMealId = mealIdMap[ei.mealId] ?: continue
                val parsedDt = runCatching { LocalDateTime.parse(ei.dateTime) }.getOrElse { LocalDateTime.now() }
                val type = runCatching { IntakeType.valueOf(ei.intakeType) }.getOrDefault(IntakeType.BREAKFAST)
                intakeRepo.upsert(
                    Intake(
                        id = 0,
                        mealId = mappedMealId,
                        intakeType = type,
                        amount = ei.amount,
                        unit = ei.unit.ifBlank { "g" },
                        dateTime = parsedDt
                    )
                )
                intakesImported++
            }

            // Import recipes & ingredients
            val existingRecipeNames = recipeRepo.getAllRecipes().map { it.name }.toSet()
            var recipesImported = 0
            val ingredientsByRecipe = bundle.recipeIngredients.groupBy { it.recipeId }
            for (er in bundle.recipes) {
                if (er.name in existingRecipeNames) continue
                val mappedIngredients = ingredientsByRecipe[er.id].orEmpty().mapNotNull { ri ->
                    val newMealId = mealIdMap[ri.mealId] ?: return@mapNotNull null
                    newMealId to ri.amountGrams
                }
                val createdAt = runCatching { LocalDateTime.parse(er.createdAt) }.getOrElse { LocalDateTime.now() }
                recipeRepo.saveRecipe(
                    Recipe(
                        id = 0,
                        name = er.name,
                        description = er.description,
                        defaultServings = er.defaultServings,
                        createdAt = createdAt
                    ),
                    mappedIngredients
                )
                recipesImported++
            }

            var tdImported = 0
            var tdSkipped = 0
            for (etd in bundle.trackedDays) {
                val parsedDate = runCatching { LocalDate.parse(etd.date) }.getOrNull() ?: continue
                val existing = trackedDayRepo.getByDate(parsedDate)
                if (existing == null) {
                    trackedDayRepo.upsert(
                        TrackedDay(
                            date = parsedDate,
                            calorieGoal = etd.calorieGoal,
                            caloriesTracked = etd.caloriesTracked,
                            carbsGoal = etd.carbsGoal,
                            carbsTracked = etd.carbsTracked,
                            fatGoal = etd.fatGoal,
                            fatTracked = etd.fatTracked,
                            proteinGoal = etd.proteinGoal,
                            proteinTracked = etd.proteinTracked
                        )
                    )
                    tdImported++
                } else {
                    tdSkipped++
                }
            }

            var actImported = 0
            for (ea in bundle.activities) {
                val parsedDt = runCatching { LocalDateTime.parse(ea.dateTime) }.getOrElse { LocalDateTime.now() }
                activityRepo.upsert(
                    UserActivityEntity(
                        id = 0,
                        name = ea.name,
                        mets = ea.mets,
                        durationMinutes = ea.durationMinutes,
                        burnedKcal = ea.burnedKcal,
                        dateTime = parsedDt,
                        isCustom = ea.isCustom,
                        category = ea.category
                    )
                )
                actImported++
            }

            var wlImported = 0
            var wlSkipped = 0
            for (ewl in bundle.weightLogs) {
                val parsedDate = runCatching { LocalDate.parse(ewl.date) }.getOrNull() ?: continue
                val existing = weightLogRepo.getByRange(parsedDate, parsedDate).firstOrNull()
                if (existing == null) {
                    weightLogRepo.upsert(WeightLog(date = parsedDate, weightKg = ewl.weightKg))
                    wlImported++
                } else {
                    wlSkipped++
                }
            }

            var wiImported = 0
            for (ewi in bundle.waterIntakes) {
                val parsedDt = runCatching { LocalDateTime.parse(ewi.dateTime) }.getOrElse { LocalDateTime.now() }
                waterRepo.upsert(
                    WaterIntake(
                        id = 0,
                        amountMl = ewi.amountMl,
                        dateTime = parsedDt
                    )
                )
                wiImported++
            }

            bundle.user?.let { eu ->
                val birthday = runCatching { LocalDate.parse(eu.birthday) }.getOrElse { LocalDate.of(2000, 1, 1) }
                val user = User(
                    id = 1,
                    birthday = birthday,
                    heightCm = eu.heightCm,
                    weightKg = eu.weightKg,
                    gender = runCatching { Gender.valueOf(eu.gender) }.getOrDefault(Gender.MALE),
                    activityLevel = runCatching { ActivityLevel.valueOf(eu.activityLevel) }.getOrDefault(ActivityLevel.SEDENTARY),
                    weightGoal = runCatching { WeightGoal.valueOf(eu.weightGoal) }.getOrDefault(WeightGoal.MAINTAIN),
                    caloriesProfile = runCatching { CaloriesProfile.valueOf(eu.caloriesProfile) }.getOrDefault(CaloriesProfile.AVERAGED),
                    weeklyWeightGoalKg = eu.weeklyWeightGoalKg,
                    targetWeightKg = eu.targetWeightKg,
                    caloriesTaperEnabled = eu.caloriesTaperEnabled,
                    usesImperialUnits = eu.usesImperialUnits
                )
                userRepo.upsert(user)
            }

            val s = bundle.settings
            if (s != null && settingsRepo != null) {
                s.dayBoundaryMinutes?.let { settingsRepo.setDayBoundaryMinutes(it) }
                s.kcalAdjustment?.let { settingsRepo.setKcalAdjustment(it) }
                s.carbPct?.let { settingsRepo.setCarbPct(it) }
                s.fatPct?.let { settingsRepo.setFatPct(it) }
                s.proteinPct?.let { settingsRepo.setProteinPct(it) }
                s.waterGoalMl?.let { settingsRepo.setWaterGoalMl(it) }
                s.waterQuickAmounts?.let { settingsRepo.setWaterQuickAmounts(it) }
                s.onboardingDone?.let { settingsRepo.setOnboardingDone(it) }
                s.themeMode?.let { tm ->
                    runCatching { ThemeMode.valueOf(tm) }.getOrNull()?.let { settingsRepo.setThemeMode(it) }
                }
                s.dynamicColorEnabled?.let { settingsRepo.setDynamicColorEnabled(it) }
                s.useKj?.let { settingsRepo.setUseKj(it) }
                s.aiAutoConfirm?.let { settingsRepo.setAiAutoConfirm(it) }
                s.appLanguage?.let { lang ->
                    runCatching { AppLanguage.valueOf(lang) }.getOrNull()?.let { settingsRepo.setAppLanguage(it) }
                }
            }

            ImportResult(
                mealsImported = mealsImported,
                mealsSkipped = mealsSkipped,
                intakesImported = intakesImported,
                recipesImported = recipesImported,
                trackedDaysImported = tdImported,
                trackedDaysSkipped = tdSkipped,
                activitiesImported = actImported,
                weightLogsImported = wlImported,
                weightLogsSkipped = wlSkipped,
                waterIntakesImported = wiImported,
                userImported = bundle.user != null,
                settingsImported = s != null,
                imagesImported = imgImported,
                imagesSkipped = imgSkipped
            )
        }
    }
}
