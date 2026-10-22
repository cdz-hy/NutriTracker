package com.example.nutritracker.feature.meal

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.dao.RecipeWithIngredients
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.util.ImageUtils
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

enum class LibraryFilter(val label: String, val labelEn: String) {
    ALL("全部", "All"),
    FAVORITE("收藏", "Favorites"),
    AI("AI 识别", "AI"),
    MANUAL("手动/自制", "Manual");

    fun displayLabel(isZh: Boolean): String = if (isZh) label else labelEn
}

@HiltViewModel
class FoodLibraryViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val recipeRepo: RecipeRepository,
    private val recipeIntakeManager: RecipeIntakeManager,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
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
            val result = mealRepo.deleteOrArchive(meal.id)
            if (result.physicallyDeleted) {
                ImageUtils.deleteThumbnail(appContext, result.deletedImagePath)
            }
            val isZh = LocaleHelper.isChinese(appContext)
            _toastEvent.emit(
                if (result.physicallyDeleted) {
                    LocaleHelper.tr(isZh, "已彻底删除 \"${meal.name}\"", "Deleted \"${meal.name}\"")
                } else {
                    LocaleHelper.tr(
                        isZh,
                        "该食物有历史记录引用，已从食物库隐藏",
                        "Used in past entries — archived from library"
                    )
                }
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
            _toastEvent.emit(
                LocaleHelper.tr(LocaleHelper.isChinese(appContext), "已记录 ${meal.name}", "Logged ${meal.name}")
            )
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
            _toastEvent.emit(
                LocaleHelper.tr(LocaleHelper.isChinese(appContext), "食谱已保存", "Recipe saved")
            )
        }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            recipeRepo.deleteRecipe(recipeId)
            _toastEvent.emit(
                LocaleHelper.tr(LocaleHelper.isChinese(appContext), "食谱已删除", "Recipe deleted")
            )
        }
    }

    fun logRecipe(recipeWithIngredients: RecipeWithIngredients, servings: Double, type: IntakeType) {
        viewModelScope.launch {
            if (recipeIntakeManager.logRecipe(recipeWithIngredients, servings, type, targetDate)) {
                _toastEvent.emit(
                    LocaleHelper.tr(
                        LocaleHelper.isChinese(appContext),
                        "已记入食谱 ${recipeWithIngredients.recipe.name}",
                        "Logged recipe ${recipeWithIngredients.recipe.name}"
                    )
                )
            }
        }
    }
}
