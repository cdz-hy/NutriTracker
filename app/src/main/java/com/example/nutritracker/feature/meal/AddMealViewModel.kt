package com.example.nutritracker.feature.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import android.content.Context
import android.net.Uri
import com.example.nutritracker.feature.camera.AnalysisResult
import com.example.nutritracker.feature.camera.AiFoodAnalyzer
import com.example.nutritracker.feature.camera.AiAnalysisManager
import com.example.nutritracker.util.DayBoundaryCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AddMealViewModel @Inject constructor(
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc,
    private val aiAnalysisManager: AiAnalysisManager
) : ViewModel() {

    private val _todayIntakes = MutableStateFlow<List<Intake>>(emptyList())
    val todayIntakes: StateFlow<List<Intake>> = _todayIntakes.asStateFlow()

    private val _mealsMap = MutableStateFlow<Map<Long, Meal>>(emptyMap())
    val mealsMap: StateFlow<Map<Long, Meal>> = _mealsMap.asStateFlow()

    val isAnalyzing: StateFlow<Boolean> = aiAnalysisManager.isAnalyzing
    val analysisError: StateFlow<String?> = aiAnalysisManager.analysisError

    fun clearAnalysisError() {
        aiAnalysisManager.clearError()
    }

    /**
     * 后台执行 AI 食物分析
     */
    fun analyzeAndCreateMeals(context: Context, uris: List<Uri>, intakeType: IntakeType) {
        aiAnalysisManager.analyzeAndCreateMeals(context, uris, intakeType)
    }

    /**
     * 加载今日该餐类型的摄入记录
     */
    fun loadTodayIntakes(intakeType: IntakeType) {
        viewModelScope.launch {
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val today = dayBoundaryCalc.currentLogicalDay(offset)
            val intakes = intakeRepo.getByTypeAndLogicalDay(intakeType, today, offset)
            _todayIntakes.value = intakes

            // 加载对应的 Meal 数据
            val mealIds = intakes.map { it.mealId }.distinct()
            val meals = mealIds.mapNotNull { id -> mealRepo.getById(id) }.associateBy { it.id }
            _mealsMap.value = meals
        }
    }

    /**
     * 从 AI 分析结果创建多个 Meal 并记录摄入
     */
    fun createMealsFromAnalysis(result: AnalysisResult, intakeType: IntakeType) {
        viewModelScope.launch {
            val nutrition = result.nutritionResult
            val thumbnailPath = result.thumbnailPath

            // 为每个食物项创建 Meal 和 Intake
            nutrition.foodItems.forEach { item ->
                val kcalPer100g = if (item.weightG > 0) item.calories * 100.0 / item.weightG else 0.0
                val carbsPer100g = if (item.weightG > 0) item.carbs * 100.0 / item.weightG else 0.0
                val fatPer100g = if (item.weightG > 0) item.fat * 100.0 / item.weightG else 0.0
                val proteinPer100g = if (item.weightG > 0) item.protein * 100.0 / item.weightG else 0.0

                val mealId = mealRepo.upsert(
                    Meal(
                        name = item.name,
                        source = MealSource.AI_ANALYSIS,
                        energyKcal100 = kcalPer100g,
                        carbohydrates100 = carbsPer100g,
                        fat100 = fatPer100g,
                        proteins100 = proteinPer100g,
                        localImagePath = thumbnailPath
                    )
                )
                addIntake(mealId, item.weightG, intakeType)
            }

            // 如果没有 food_items 但有总计数据，创建一个汇总条目
            if (nutrition.foodItems.isEmpty() && nutrition.totalCalories > 0) {
                val mealId = mealRepo.upsert(
                    Meal(
                        name = "AI 识别食物",
                        source = MealSource.AI_ANALYSIS,
                        energyKcal100 = nutrition.totalCalories,
                        carbohydrates100 = nutrition.totalCarbs,
                        fat100 = nutrition.totalFat,
                        proteins100 = nutrition.totalProtein,
                        localImagePath = thumbnailPath
                    )
                )
                addIntake(mealId, 100.0, intakeType)
            }

            loadTodayIntakes(intakeType)
        }
    }

    /**
     * 手动创建食物并记录摄入
     */
    fun createManualMeal(
        name: String, kcal: Double, carbs: Double, fat: Double, protein: Double,
        weight: Double, intakeType: IntakeType
    ) {
        viewModelScope.launch {
            val mealId = mealRepo.upsert(
                Meal(
                    name = name, source = MealSource.MANUAL,
                    energyKcal100 = kcal, carbohydrates100 = carbs,
                    fat100 = fat, proteins100 = protein
                )
            )
            addIntake(mealId, weight, intakeType)
            loadTodayIntakes(intakeType)
        }
    }

    /**
     * 删除摄入记录
     * 先从 TrackedDay 移除卡路里，再删除摄入记录
     */
    fun deleteIntake(intake: Intake) {
        viewModelScope.launch {
            // 先获取 Meal 数据（删除后可能无法获取）
            val meal = mealRepo.getById(intake.mealId)
            // 先从 TrackedDay 移除卡路里
            if (meal != null) {
                val offset = settingsRepo.dayBoundaryMinutes.first()
                val today = dayBoundaryCalc.logicalDayOf(intake.dateTime, offset)
                trackedDayRepo.removeCalories(
                    today,
                    meal.energyKcal100 * intake.amount / 100.0,
                    meal.carbohydrates100 * intake.amount / 100.0,
                    meal.fat100 * intake.amount / 100.0,
                    meal.proteins100 * intake.amount / 100.0
                )
            }
            // 再删除摄入记录
            intakeRepo.delete(intake)
            loadTodayIntakes(intake.intakeType)
        }
    }

    /**
     * 更新摄入记录的份量
     * 先移除旧的卡路里，更新记录，再添加新的卡路里
     */
    fun updateIntakeAmount(intake: Intake, newAmount: Double) {
        viewModelScope.launch {
            val meal = mealRepo.getById(intake.mealId) ?: return@launch
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val today = dayBoundaryCalc.logicalDayOf(intake.dateTime, offset)

            // 移除旧的卡路里
            trackedDayRepo.removeCalories(
                today,
                meal.energyKcal100 * intake.amount / 100.0,
                meal.carbohydrates100 * intake.amount / 100.0,
                meal.fat100 * intake.amount / 100.0,
                meal.proteins100 * intake.amount / 100.0
            )

            // 更新摄入记录
            val updatedIntake = intake.copy(amount = newAmount)
            intakeRepo.upsert(updatedIntake)

            // 添加新的卡路里
            trackedDayRepo.addCalories(
                today,
                meal.energyKcal100 * newAmount / 100.0,
                meal.carbohydrates100 * newAmount / 100.0,
                meal.fat100 * newAmount / 100.0,
                meal.proteins100 * newAmount / 100.0
            )

            loadTodayIntakes(intake.intakeType)
        }
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
}
