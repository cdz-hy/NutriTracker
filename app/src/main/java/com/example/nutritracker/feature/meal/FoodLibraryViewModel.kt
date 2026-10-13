package com.example.nutritracker.feature.meal

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.dao.RecipeWithIngredients
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.feature.camera.NutritionResult
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

enum class LibraryFilter(val label: String) {
    ALL("全部"),
    FAVORITE("收藏"),
    AI("AI 识别"),
    MANUAL("手动/自制")
}

@HiltViewModel
class FoodLibraryViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val recipeRepo: RecipeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc,
    private val gson: Gson
) : ViewModel() {

    private val initialTypeId: Int = savedStateHandle["intakeTypeId"] ?: 0
    private val initialDateEpoch: Long = savedStateHandle["date"] ?: LocalDate.now().toEpochDay()

    val defaultIntakeType: IntakeType = IntakeType.entries.getOrElse(initialTypeId) { IntakeType.BREAKFAST }
    val targetDate: LocalDate = LocalDate.ofEpochDay(initialDateEpoch)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(LibraryFilter.ALL)
    val filter: StateFlow<LibraryFilter> = _filter.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>(replay = 0)
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val useKj: StateFlow<Boolean> = settingsRepo.useKj
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val allLibraryMeals: StateFlow<List<Meal>> = mealRepo.getLibraryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMeals: StateFlow<List<Meal>> = combine(
        allLibraryMeals,
        _query,
        _filter
    ) { meals, q, f ->
        val trimmed = q.trim()
        meals.filter { m ->
            val matchQuery = trimmed.isEmpty() || m.name.contains(trimmed, ignoreCase = true)
            val matchFilter = when (f) {
                LibraryFilter.ALL -> true
                LibraryFilter.FAVORITE -> m.isFavorite
                LibraryFilter.AI -> m.source == MealSource.AI_ANALYSIS
                LibraryFilter.MANUAL -> m.source == MealSource.MANUAL || m.source == MealSource.CUSTOM
            }
            matchQuery && matchFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recipes: StateFlow<List<RecipeWithIngredients>> = combine(
        recipeRepo.getAllWithIngredientsFlow(),
        _query
    ) { list, q ->
        val trimmed = q.trim()
        if (trimmed.isEmpty()) list
        else list.filter { it.recipe.name.contains(trimmed, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun setFilter(f: LibraryFilter) { _filter.value = f }

    fun toggleFavorite(meal: Meal) {
        viewModelScope.launch {
            mealRepo.setFavorite(meal.id, !meal.isFavorite)
        }
    }

    fun deleteOrArchiveMeal(meal: Meal) {
        viewModelScope.launch {
            val physicallyDeleted = mealRepo.deleteOrArchive(meal.id)
            _toastEvent.emit(
                if (physicallyDeleted) "已彻底删除 \"${meal.name}\""
                else "该食物有历史记录引用，已从食物库隐藏"
            )
        }
    }

    fun logMeal(meal: Meal, grams: Double, type: IntakeType) {
        if (grams <= 0.0) return
        viewModelScope.launch {
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val logicalToday = dayBoundaryCalc.currentLogicalDay(offset)
            val dt = if (targetDate == logicalToday) LocalDateTime.now() else targetDate.atTime(12, 0)
            intakeRepo.upsert(Intake(mealId = meal.id, intakeType = type, amount = grams, dateTime = dt))
            trackedDayRepo.saveGoalSnapshot(targetDate, 0.0, 0.0, 0.0, 0.0)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
            _toastEvent.emit("已记录 ${meal.name}")
        }
    }

    fun saveRecipe(
        recipeId: Long,
        name: String,
        description: String,
        defaultServings: Double,
        ingredients: List<Pair<Long, Double>>
    ) {
        if (name.isBlank() || ingredients.isEmpty()) return
        viewModelScope.launch {
            recipeRepo.saveRecipe(
                Recipe(
                    id = recipeId,
                    name = name.trim(),
                    description = description.trim(),
                    defaultServings = defaultServings.coerceAtLeast(0.5)
                ),
                ingredients
            )
            _toastEvent.emit("食谱已保存")
        }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            recipeRepo.deleteRecipe(recipeId)
            _toastEvent.emit("食谱已删除")
        }
    }

    fun logRecipe(recipeWithIngredients: RecipeWithIngredients, servings: Double, type: IntakeType) {
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
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val logicalToday = dayBoundaryCalc.currentLogicalDay(offset)
            val dt = if (targetDate == logicalToday) LocalDateTime.now() else targetDate.atTime(12, 0)
            intakeRepo.upsert(Intake(mealId = mealId, intakeType = type, amount = totalWeight, dateTime = dt))
            trackedDayRepo.saveGoalSnapshot(targetDate, 0.0, 0.0, 0.0, 0.0)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
            _toastEvent.emit("已记入食谱 ${recipe.name}")
        }
    }
}
