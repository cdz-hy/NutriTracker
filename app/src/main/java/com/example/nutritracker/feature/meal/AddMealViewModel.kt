package com.example.nutritracker.feature.meal

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.dao.IntakeWithMeal
import com.example.nutritracker.data.dao.RecipeWithIngredients
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.feature.camera.AiAnalysisManager
import com.example.nutritracker.feature.camera.AnalysisTask
import com.example.nutritracker.feature.camera.NutritionResult
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

enum class ManualEntryMode(val label: String) {
    PER_100G("每100g"),
    WHOLE_PORTION("按整份"),
    QUICK_ADD("极速快记")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddMealViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val recipeRepo: RecipeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc,
    private val aiAnalysisManager: AiAnalysisManager,
    private val gson: Gson
) : ViewModel() {

    data class RecentIntake(val meal: Meal, val amount: Double, val kcal: Double)

    private val initialTypeId: Int = savedStateHandle["intakeTypeId"] ?: 0
    private val initialDateEpoch: Long = savedStateHandle["date"] ?: LocalDate.now().toEpochDay()

    private val _intakeType = MutableStateFlow(IntakeType.entries.getOrElse(initialTypeId) { IntakeType.BREAKFAST })
    private val _date = MutableStateFlow(LocalDate.ofEpochDay(initialDateEpoch))
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val useKj: StateFlow<Boolean> = settingsRepo.useKj
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val todayIntakes: StateFlow<List<IntakeWithMeal>> = combine(
        _intakeType,
        _date,
        settingsRepo.dayBoundaryMinutes
    ) { type, date, offset ->
        Triple(type, date, offset)
    }.flatMapLatest { (type, date, offset) ->
        intakeRepo.getWithMealByTypeAndLogicalDayFlow(type, date, offset)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentIntakes: StateFlow<List<RecentIntake>> = combine(
        _intakeType.flatMapLatest { type -> intakeRepo.getRecentWithMealByTypeFlow(type, 30) },
        todayIntakes
    ) { recentList, todayList ->
        val todayMealIds = todayList.map { it.meal.id }.toSet()
        recentList
            .filter { it.meal.id !in todayMealIds }
            .distinctBy { it.meal.id }
            .take(8)
            .map {
                RecentIntake(
                    meal = it.meal,
                    amount = it.intake.amount,
                    kcal = it.meal.energyKcal100 * it.intake.amount / 100.0
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLibraryMeals: StateFlow<List<Meal>> = combine(
        mealRepo.getLibraryFlow(),
        _searchQuery
    ) { meals, query ->
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else meals.filter { it.name.contains(q, ignoreCase = true) }.take(15)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipes: StateFlow<List<RecipeWithIngredients>> = combine(
        recipeRepo.getAllWithIngredientsFlow(),
        _searchQuery
    ) { all, query ->
        val q = query.trim()
        if (q.isEmpty()) all
        else all.filter { it.recipe.name.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiTasks: StateFlow<List<AnalysisTask>> = aiAnalysisManager.tasks
    val aiActiveCount: StateFlow<Int> = aiAnalysisManager.activeJobCount
    val pendingConfirmTasks: StateFlow<List<AnalysisTask>> = aiAnalysisManager.pendingConfirmTasks
    val failedTasks: StateFlow<List<AnalysisTask>> = aiAnalysisManager.failedTasks
    val analysisError: StateFlow<String?> = aiAnalysisManager.analysisError
    val analysisSuccess: SharedFlow<String> = aiAnalysisManager.analysisSuccess

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setTargetContext(type: IntakeType, date: LocalDate) {
        _intakeType.value = type
        _date.value = date
    }

    fun clearAnalysisError() = aiAnalysisManager.clearError()

    fun analyzeAndCreateMeals(
        context: Context,
        uris: List<Uri>,
        intakeType: IntakeType,
        notes: String = "",
        date: LocalDate
    ) {
        aiAnalysisManager.analyzeAndCreateMeals(context, uris, intakeType, notes, date)
    }

    fun analyzeTextMeal(description: String, intakeType: IntakeType, date: LocalDate) {
        aiAnalysisManager.analyzeTextMeal(description, intakeType, date)
    }

    fun cancelAiTask(taskId: Int) = aiAnalysisManager.cancelTask(taskId)
    fun retryAiTask(taskId: Int) = aiAnalysisManager.retryTask(taskId)
    fun dismissAiTask(taskId: Int) = aiAnalysisManager.dismissTask(taskId)

    fun confirmAiTask(
        taskId: Int,
        mealName: String,
        intakeType: IntakeType,
        date: LocalDate,
        multiplier: Double,
        items: List<NutritionResult.FoodItem>,
        fallbackResult: NutritionResult,
        thumbnailPath: String?
    ) {
        aiAnalysisManager.confirmTask(
            taskId = taskId,
            mealName = mealName,
            intakeType = intakeType,
            date = date,
            multiplier = multiplier,
            editedItems = items,
            fallbackResult = fallbackResult,
            thumbnailPath = thumbnailPath
        )
    }

    fun quickAddIntake(meal: Meal, amount: Double, intakeType: IntakeType, date: LocalDate) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val dateTime = resolveDateTime(date)
            intakeRepo.upsert(
                Intake(
                    mealId = meal.id,
                    intakeType = intakeType,
                    amount = amount,
                    unit = "g",
                    dateTime = dateTime
                )
            )
            trackedDayRepo.saveGoalSnapshot(date, 0.0, 0.0, 0.0, 0.0)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun logRecipe(recipeWithIngredients: RecipeWithIngredients, servings: Double, intakeType: IntakeType, date: LocalDate) {
        if (servings <= 0.0 || recipeWithIngredients.ingredients.isEmpty()) return
        viewModelScope.launch {
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
            val mealId = mealRepo.upsert(
                Meal(
                    name = if (servings == 1.0) recipe.name else "${recipe.name} (${servings}份)",
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
                    portionLabel = "份",
                    portionGrams = totalWeight / servings,
                    foodItemsJson = gson.toJson(subItems)
                )
            )
            intakeRepo.upsert(
                Intake(
                    mealId = mealId,
                    intakeType = intakeType,
                    amount = totalWeight,
                    dateTime = resolveDateTime(date)
                )
            )
            trackedDayRepo.saveGoalSnapshot(date, 0.0, 0.0, 0.0, 0.0)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun createManualMeal(
        mode: ManualEntryMode,
        name: String,
        kcalInput: Double,
        carbsInput: Double,
        fatInput: Double,
        proteinInput: Double,
        weightGrams: Double,
        portionLabel: String?,
        fiberInput: Double?,
        sugarsInput: Double?,
        satFatInput: Double?,
        sodiumInput: Double?,
        cholesterolInput: Double?,
        intakeType: IntakeType,
        date: LocalDate
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val safeWeight = when (mode) {
                ManualEntryMode.QUICK_ADD -> 100.0
                else -> weightGrams.coerceAtLeast(1.0)
            }
            val scaleTo100 = when (mode) {
                ManualEntryMode.PER_100G -> 1.0
                ManualEntryMode.WHOLE_PORTION -> 100.0 / safeWeight
                ManualEntryMode.QUICK_ADD -> 1.0
            }

            val mealId = mealRepo.upsert(
                Meal(
                    name = name.trim(),
                    source = MealSource.MANUAL,
                    energyKcal100 = (kcalInput * scaleTo100).coerceAtLeast(0.0),
                    carbohydrates100 = (carbsInput * scaleTo100).coerceAtLeast(0.0),
                    fat100 = (fatInput * scaleTo100).coerceAtLeast(0.0),
                    proteins100 = (proteinInput * scaleTo100).coerceAtLeast(0.0),
                    fiber100 = fiberInput?.times(scaleTo100)?.coerceAtLeast(0.0),
                    sugars100 = sugarsInput?.times(scaleTo100)?.coerceAtLeast(0.0),
                    saturatedFat100 = satFatInput?.times(scaleTo100)?.coerceAtLeast(0.0),
                    sodium100 = sodiumInput?.times(scaleTo100)?.coerceAtLeast(0.0),
                    cholesterol100 = cholesterolInput?.times(scaleTo100)?.coerceAtLeast(0.0),
                    portionLabel = if (mode == ManualEntryMode.WHOLE_PORTION) portionLabel?.takeIf { it.isNotBlank() } ?: "份" else null,
                    portionGrams = if (mode == ManualEntryMode.WHOLE_PORTION) safeWeight else null
                )
            )

            intakeRepo.upsert(
                Intake(
                    mealId = mealId,
                    intakeType = intakeType,
                    amount = safeWeight,
                    dateTime = resolveDateTime(date)
                )
            )
            trackedDayRepo.saveGoalSnapshot(date, 0.0, 0.0, 0.0, 0.0)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun deleteIntake(intake: Intake) {
        viewModelScope.launch {
            intakeRepo.deleteById(intake.id)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    private suspend fun resolveDateTime(date: LocalDate): LocalDateTime {
        val offset = settingsRepo.dayBoundaryMinutes.first()
        val logicalToday = dayBoundaryCalc.currentLogicalDay(offset)
        return if (date == logicalToday) LocalDateTime.now() else date.atTime(12, 0)
    }
}
