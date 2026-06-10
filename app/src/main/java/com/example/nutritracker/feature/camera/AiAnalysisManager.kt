package com.example.nutritracker.feature.camera

import android.content.Context
import android.net.Uri
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.DayBoundaryCalc
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分析任务状态
 */
data class AnalysisTask(
    val id: Int,
    val uri: Uri,
    val intakeType: IntakeType,
    val status: TaskStatus = TaskStatus.PENDING
)

enum class TaskStatus { PENDING, ANALYZING, SUCCESS, FAILED, CANCELLED }

@Singleton
class AiAnalysisManager @Inject constructor(
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 使用计数器替代 boolean 标志，支持并发
    private val _activeJobCount = MutableStateFlow(0)
    val isAnalyzing: StateFlow<Boolean> = _activeJobCount.map { it > 0 }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), false)

    // 当前活跃任务数
    val activeJobCount: StateFlow<Int> = _activeJobCount.asStateFlow()

    // 错误消息
    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // 成功消息
    private val _analysisSuccess = MutableSharedFlow<String>(replay = 0)
    val analysisSuccess: SharedFlow<String> = _analysisSuccess.asSharedFlow()

    // 任务计数器
    private val taskIdCounter = AtomicInteger(0)

    // 任务队列状态
    private val _tasks = MutableStateFlow<List<AnalysisTask>>(emptyList())
    val tasks: StateFlow<List<AnalysisTask>> = _tasks.asStateFlow()

    fun clearError() {
        _analysisError.value = null
    }

    /**
     * 添加图片到分析队列，立即返回
     * 每个任务独立运行，互不阻塞
     */
    fun analyzeAndCreateMeals(context: Context, uri: Uri, intakeType: IntakeType) {
        val taskId = taskIdCounter.incrementAndGet()
        val task = AnalysisTask(id = taskId, uri = uri, intakeType = intakeType, status = TaskStatus.PENDING)

        // 添加到任务列表
        _tasks.update { current -> current + task }

        scope.launch {
            updateTaskStatus(taskId, TaskStatus.ANALYZING)
            _activeJobCount.value = _activeJobCount.value + 1

            try {
                val apiKey = settingsRepo.aiApiKey.first()
                val baseUrl = settingsRepo.aiBaseUrl.first()
                val model = settingsRepo.aiModel.first()

                if (apiKey.isBlank()) {
                    _analysisError.value = "请先在设置中配置 AI API Key"
                    updateTaskStatus(taskId, TaskStatus.FAILED)
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    val analyzer = AiFoodAnalyzer(apiKey, baseUrl, model)
                    // 保存缩略图
                    val thumbnailPath = analyzer.saveThumbnail(context, uri)
                    // 分析图片
                    val analysisResult = analyzer.analyzeImage(context, uri)
                    analysisResult.map { nutritionResult ->
                        AnalysisResult(nutritionResult = nutritionResult, thumbnailPath = thumbnailPath)
                    }
                }

                result.fold(
                    onSuccess = { ar ->
                        createMealsFromAnalysis(ar, intakeType)
                        updateTaskStatus(taskId, TaskStatus.SUCCESS)
                        _analysisSuccess.emit("AI 识别成功！已记录 ${intakeTypeName(intakeType)}")
                    },
                    onFailure = { error ->
                        _analysisError.value = error.message ?: "识别失败，请重试！"
                        updateTaskStatus(taskId, TaskStatus.FAILED)
                    }
                )
            } catch (e: Exception) {
                _analysisError.value = e.message ?: "网络或配置异常，请重试！"
                updateTaskStatus(taskId, TaskStatus.FAILED)
            } finally {
                _activeJobCount.value = (_activeJobCount.value - 1).coerceAtLeast(0)
                // 清理已完成的任务
                cleanCompletedTasks()
            }
        }
    }

    private fun updateTaskStatus(taskId: Int, status: TaskStatus) {
        _tasks.update { current ->
            current.map { if (it.id == taskId) it.copy(status = status) else it }
        }
    }

    private fun cleanCompletedTasks() {
        _tasks.update { current ->
            current.filter { it.status == TaskStatus.PENDING || it.status == TaskStatus.ANALYZING }
        }
    }

    private suspend fun createMealsFromAnalysis(result: AnalysisResult, intakeType: IntakeType) {
        val nutrition = result.nutritionResult
        val thumbnailPath = result.thumbnailPath

        // 使用 AI 返回的自然汇总名称，兜底用 foodItems 拼接
        val summaryName = nutrition.mealName.ifBlank {
            if (nutrition.foodItems.isNotEmpty()) {
                nutrition.foodItems.take(3).joinToString(" + ") { it.name }
            } else {
                "AI 识别食物"
            }
        }

        // 食物项 JSON
        val foodItemsJson = if (nutrition.foodItems.isNotEmpty()) {
            com.google.gson.Gson().toJson(nutrition.foodItems)
        } else null

        // 每100g营养值
        val totalWeight = nutrition.foodItems.sumOf { it.weightG }.coerceAtLeast(100.0)
        val kcalPer100g = nutrition.totalCalories * 100.0 / totalWeight
        val carbsPer100g = nutrition.totalCarbs * 100.0 / totalWeight
        val fatPer100g = nutrition.totalFat * 100.0 / totalWeight
        val proteinPer100g = nutrition.totalProtein * 100.0 / totalWeight

        // 创建汇总 Meal
        val mealId = mealRepo.upsert(
            Meal(
                name = summaryName,
                source = MealSource.AI_ANALYSIS,
                energyKcal100 = kcalPer100g,
                carbohydrates100 = carbsPer100g,
                fat100 = fatPer100g,
                proteins100 = proteinPer100g,
                localImagePath = thumbnailPath,
                foodItemsJson = foodItemsJson
            )
        )
        addIntake(mealId, totalWeight, intakeType)
    }

    private suspend fun addIntake(mealId: Long, amount: Double, type: IntakeType) {
        val now = LocalDateTime.now()
        intakeRepo.upsert(Intake(mealId = mealId, intakeType = type, amount = amount, dateTime = now))
        val meal = mealRepo.getById(mealId) ?: return
        val offset = settingsRepo.dayBoundaryMinutes.first()
        val day = dayBoundaryCalc.logicalDayOf(now, offset)
        trackedDayRepo.ensureDay(day, 0.0, 0.0, 0.0, 0.0)
        trackedDayRepo.addCalories(
            day,
            meal.energyKcal100 * amount / 100.0,
            meal.carbohydrates100 * amount / 100.0,
            meal.fat100 * amount / 100.0,
            meal.proteins100 * amount / 100.0
        )
    }

    private fun intakeTypeName(type: IntakeType): String = when (type) {
        IntakeType.BREAKFAST -> "早餐"
        IntakeType.LUNCH -> "午餐"
        IntakeType.DINNER -> "晚餐"
        IntakeType.SNACK -> "零食"
    }
}
