package com.example.nutritracker.feature.camera

import android.content.Context
import android.net.Uri
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.util.ImageUtils
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class AnalysisMode { IMAGE, TEXT }

enum class TaskStatus { PENDING, ANALYZING, AWAITING_CONFIRM, SUCCESS, FAILED, CANCELLED }

data class AnalysisTask(
    val id: Int,
    val mode: AnalysisMode,
    val uris: List<Uri> = emptyList(),
    val textQuery: String = "",
    val intakeType: IntakeType,
    val notes: String = "",
    val date: LocalDate = LocalDate.now(),
    val status: TaskStatus = TaskStatus.PENDING,
    val result: AnalysisResult? = null,
    val errorMessage: String? = null
)

@Singleton
class AiAnalysisManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val taskIdCounter = AtomicInteger(0)
    private val runningJobs = mutableMapOf<Int, Job>()

    private val _tasks = MutableStateFlow<List<AnalysisTask>>(emptyList())
    val tasks: StateFlow<List<AnalysisTask>> = _tasks.asStateFlow()

    val isAnalyzing: StateFlow<Boolean> = _tasks
        .map { list -> list.any { it.status == TaskStatus.PENDING || it.status == TaskStatus.ANALYZING } }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val activeJobCount: StateFlow<Int> = _tasks
        .map { list -> list.count { it.status == TaskStatus.PENDING || it.status == TaskStatus.ANALYZING } }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    val pendingConfirmTasks: StateFlow<List<AnalysisTask>> = _tasks
        .map { list -> list.filter { it.status == TaskStatus.AWAITING_CONFIRM && it.result != null } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val failedTasks: StateFlow<List<AnalysisTask>> = _tasks
        .map { list -> list.filter { it.status == TaskStatus.FAILED } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    private val _analysisSuccess = MutableSharedFlow<String>(replay = 0)
    val analysisSuccess: SharedFlow<String> = _analysisSuccess.asSharedFlow()

    fun clearError() {
        _analysisError.value = null
    }

    fun analyzeAndCreateMeals(
        context: Context,
        uris: List<Uri>,
        intakeType: IntakeType,
        notes: String = "",
        date: LocalDate = LocalDate.now()
    ) {
        if (uris.isEmpty()) return
        val taskId = taskIdCounter.incrementAndGet()
        val task = AnalysisTask(
            id = taskId,
            mode = AnalysisMode.IMAGE,
            uris = uris,
            intakeType = intakeType,
            notes = notes,
            date = date
        )
        _tasks.update { it + task }
        launchTask(task)
    }

    fun analyzeTextMeal(
        description: String,
        intakeType: IntakeType,
        date: LocalDate = LocalDate.now()
    ) {
        if (description.isBlank()) return
        val taskId = taskIdCounter.incrementAndGet()
        val task = AnalysisTask(
            id = taskId,
            mode = AnalysisMode.TEXT,
            textQuery = description.trim(),
            intakeType = intakeType,
            date = date
        )
        _tasks.update { it + task }
        launchTask(task)
    }

    fun cancelTask(taskId: Int) {
        runningJobs.remove(taskId)?.cancel()
        val task = _tasks.value.find { it.id == taskId }
        if (task != null) {
            if (task.uris.isNotEmpty()) ImageUtils.cleanupTempFiles(appContext, task.uris)
            ImageUtils.deleteThumbnail(appContext, task.result?.thumbnailPath)
        }
        _tasks.update { list -> list.filterNot { it.id == taskId } }
    }

    fun dismissTask(taskId: Int) {
        val task = _tasks.value.find { it.id == taskId }
        if (task != null) {
            if (task.uris.isNotEmpty()) ImageUtils.cleanupTempFiles(appContext, task.uris)
            ImageUtils.deleteThumbnail(appContext, task.result?.thumbnailPath)
        }
        _tasks.update { list -> list.filterNot { it.id == taskId } }
    }

    fun retryTask(taskId: Int) {
        val existing = _tasks.value.find { it.id == taskId } ?: return
        if (existing.status != TaskStatus.FAILED) return
        val reset = existing.copy(status = TaskStatus.PENDING, errorMessage = null)
        _tasks.update { list -> list.map { if (it.id == taskId) reset else it } }
        launchTask(reset)
    }

    private fun launchTask(task: AnalysisTask) {
        val job = scope.launch {
            updateTask(task.id) { it.copy(status = TaskStatus.ANALYZING, errorMessage = null) }
            try {
                val apiKey = settingsRepo.aiApiKey.first()
                val baseUrl = settingsRepo.aiBaseUrl.first()
                val model = settingsRepo.aiModel.first()
                val autoConfirm = settingsRepo.aiAutoConfirm.first()

                if (apiKey.isBlank()) {
                    val isZh = LocaleHelper.isChinese(appContext)
                    val msg = LocaleHelper.tr(isZh, "请先在设置中配置 AI API Key", "Please configure your AI API Key in Settings first")
                    _analysisError.value = msg
                    updateTask(task.id) { it.copy(status = TaskStatus.FAILED, errorMessage = msg) }
                    return@launch
                }

                val isZh = LocaleHelper.isChinese(appContext)
                val result = withContext(Dispatchers.IO) {
                    val analyzer = AiFoodAnalyzer(okHttpClient, gson, apiKey, baseUrl, model, isZh = isZh)
                    when (task.mode) {
                        AnalysisMode.IMAGE -> {
                            analyzer.analyzeImages(appContext, task.uris, task.notes).map { nutrition ->
                                val thumbnailPath = ImageUtils.saveThumbnail(appContext, task.uris.first())
                                AnalysisResult(nutritionResult = nutrition, thumbnailPath = thumbnailPath)
                            }
                        }
                        AnalysisMode.TEXT -> {
                            analyzer.analyzeText(task.textQuery).map { nutrition ->
                                AnalysisResult(nutritionResult = nutrition, thumbnailPath = null)
                            }
                        }
                    }
                }

                result.fold(
                    onSuccess = { analysisResult ->
                        if (task.uris.isNotEmpty()) {
                            ImageUtils.cleanupTempFiles(appContext, task.uris)
                        }
                        if (autoConfirm) {
                            val nowZh = LocaleHelper.isChinese(appContext)
                            persistAnalysisResult(
                                mealName = resolveMealName(analysisResult.nutritionResult),
                                items = analysisResult.nutritionResult.foodItems,
                                fallbackTotals = analysisResult.nutritionResult,
                                thumbnailPath = analysisResult.thumbnailPath,
                                intakeType = task.intakeType,
                                date = task.date
                            )
                            _tasks.update { list -> list.filterNot { it.id == task.id } }
                            val typeLabel = LocaleHelper.mealTypeLabel(task.intakeType, nowZh)
                            _analysisSuccess.emit(
                                LocaleHelper.tr(nowZh, "AI 识别成功！已记录$typeLabel", "AI logged to $typeLabel")
                            )
                        } else {
                            updateTask(task.id) {
                                it.copy(status = TaskStatus.AWAITING_CONFIRM, result = analysisResult)
                            }
                        }
                    },
                    onFailure = { error ->
                        val msg = error.message ?: LocaleHelper.tr(
                            LocaleHelper.isChinese(appContext),
                            "识别失败，请重试！",
                            "Analysis failed, please retry."
                        )
                        _analysisError.value = msg
                        updateTask(task.id) { it.copy(status = TaskStatus.FAILED, errorMessage = msg) }
                    }
                )
            } catch (_: CancellationException) {
                // Handled by cancelTask
            } catch (e: Exception) {
                val msg = e.message ?: LocaleHelper.tr(
                    LocaleHelper.isChinese(appContext),
                    "网络或配置异常，请重试！",
                    "Network or configuration error, please retry."
                )
                _analysisError.value = msg
                updateTask(task.id) { it.copy(status = TaskStatus.FAILED, errorMessage = msg) }
            } finally {
                runningJobs.remove(task.id)
            }
        }
        runningJobs[task.id] = job
    }

    /**
     * Commits an interactively reviewed/edited AI analysis task into Room.
     */
    fun confirmTask(
        taskId: Int,
        mealName: String,
        intakeType: IntakeType,
        date: LocalDate,
        multiplier: Double,
        editedItems: List<NutritionResult.FoodItem>,
        fallbackResult: NutritionResult,
        thumbnailPath: String?
    ) {
        scope.launch {
            val scale = multiplier.coerceIn(0.1, 10.0)
            val scaledItems = editedItems.map { item ->
                item.copy(
                    weightG = item.weightG * scale,
                    calories = item.calories * scale,
                    protein = item.protein * scale,
                    fat = item.fat * scale,
                    carbs = item.carbs * scale,
                    fiberG = item.fiberG?.times(scale),
                    sugarsG = item.sugarsG?.times(scale),
                    saturatedFatG = item.saturatedFatG?.times(scale),
                    sodiumMg = item.sodiumMg?.times(scale),
                    cholesterolMg = item.cholesterolMg?.times(scale)
                )
            }
            val scaledFallback = fallbackResult.copy(
                mealName = mealName,
                totalCalories = fallbackResult.totalCalories * scale,
                totalProtein = fallbackResult.totalProtein * scale,
                totalFat = fallbackResult.totalFat * scale,
                totalCarbs = fallbackResult.totalCarbs * scale,
                totalFiberG = fallbackResult.totalFiberG?.times(scale),
                totalSugarsG = fallbackResult.totalSugarsG?.times(scale),
                totalSaturatedFatG = fallbackResult.totalSaturatedFatG?.times(scale),
                totalSodiumMg = fallbackResult.totalSodiumMg?.times(scale),
                totalCholesterolMg = fallbackResult.totalCholesterolMg?.times(scale)
            )
            persistAnalysisResult(
                mealName = mealName.ifBlank { resolveMealName(fallbackResult) },
                items = scaledItems,
                fallbackTotals = scaledFallback,
                thumbnailPath = thumbnailPath,
                intakeType = intakeType,
                date = date
            )
            _tasks.update { list -> list.filterNot { it.id == taskId } }
            val isZh = LocaleHelper.isChinese(appContext)
            val typeLabel = LocaleHelper.mealTypeLabel(intakeType, isZh)
            _analysisSuccess.emit(LocaleHelper.tr(isZh, "已记录到$typeLabel", "Logged to $typeLabel"))
        }
    }

    private suspend fun persistAnalysisResult(
        mealName: String,
        items: List<NutritionResult.FoodItem>,
        fallbackTotals: NutritionResult,
        thumbnailPath: String?,
        intakeType: IntakeType,
        date: LocalDate
    ) {
        val hasItems = items.isNotEmpty()
        val totalWeight = if (hasItems) {
            items.sumOf { it.weightG }.takeIf { it > 0.0 } ?: 100.0
        } else {
            100.0
        }
        val factorTo100 = 100.0 / totalWeight

        val totalKcal = if (hasItems) items.sumOf { it.calories } else fallbackTotals.totalCalories
        val totalCarbs = if (hasItems) items.sumOf { it.carbs } else fallbackTotals.totalCarbs
        val totalFat = if (hasItems) items.sumOf { it.fat } else fallbackTotals.totalFat
        val totalProtein = if (hasItems) items.sumOf { it.protein } else fallbackTotals.totalProtein

        fun sumOptional(
            itemPick: (NutritionResult.FoodItem) -> Double?,
            fallbackPick: Double?
        ): Double? {
            if (!hasItems) return fallbackPick?.times(factorTo100)
            val present = items.mapNotNull(itemPick)
            return if (present.isNotEmpty()) present.sum() * factorTo100 else fallbackPick?.times(factorTo100)
        }

        val foodItemsJson = if (hasItems) gson.toJson(items) else null

        val mealId = mealRepo.upsert(
            Meal(
                name = mealName,
                source = MealSource.AI_ANALYSIS,
                energyKcal100 = totalKcal * factorTo100,
                carbohydrates100 = totalCarbs * factorTo100,
                fat100 = totalFat * factorTo100,
                proteins100 = totalProtein * factorTo100,
                fiber100 = sumOptional({ it.fiberG }, fallbackTotals.totalFiberG),
                sugars100 = sumOptional({ it.sugarsG }, fallbackTotals.totalSugarsG),
                saturatedFat100 = sumOptional({ it.saturatedFatG }, fallbackTotals.totalSaturatedFatG),
                sodium100 = sumOptional({ it.sodiumMg }, fallbackTotals.totalSodiumMg),
                cholesterol100 = sumOptional({ it.cholesterolMg }, fallbackTotals.totalCholesterolMg),
                localImagePath = thumbnailPath,
                foodItemsJson = foodItemsJson
            )
        )

        val offset = settingsRepo.dayBoundaryMinutes.first()
        val logicalToday = dayBoundaryCalc.currentLogicalDay(offset)
        val dateTime = if (date == logicalToday) LocalDateTime.now() else date.atTime(12, 0)
        intakeRepo.upsert(Intake(mealId = mealId, intakeType = intakeType, amount = totalWeight, dateTime = dateTime))
        trackedDayRepo.saveGoalSnapshot(date, 0.0, 0.0, 0.0, 0.0)
        NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
    }

    private fun resolveMealName(nutrition: NutritionResult): String =
        nutrition.mealName.ifBlank {
            if (nutrition.foodItems.isNotEmpty()) {
                nutrition.foodItems.take(3).joinToString(" + ") { it.name }
            } else {
                LocaleHelper.tr(LocaleHelper.isChinese(appContext), "AI 识别食物", "AI Analyzed Meal")
            }
        }

    private fun updateTask(taskId: Int, transform: (AnalysisTask) -> AnalysisTask) {
        _tasks.update { current ->
            current.map { if (it.id == taskId) transform(it) else it }
        }
    }
}
