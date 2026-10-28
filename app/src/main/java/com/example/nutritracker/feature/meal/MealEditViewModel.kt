package com.example.nutritracker.feature.meal

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.feature.camera.NutritionResult
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.util.UnitCalc
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject

data class EditableFoodItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val weightG: String,
    val calories: String,
    val carbs: String,
    val protein: String,
    val fat: String
)

data class MealEditState(
    val name: String = "",
    val amountStr: String = "100",
    val energyStr: String = "",
    val carbsStr: String = "",
    val fatStr: String = "",
    val proteinStr: String = "",
    val sugarsStr: String = "",
    val satFatStr: String = "",
    val fiberStr: String = "",
    val sodiumStr: String = "",
    val cholesterolStr: String = "",
    val portionLabel: String = "",
    val portionGramsStr: String = "",
    val isFavorite: Boolean = false
)

@HiltViewModel
class MealEditViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc,
    private val gson: Gson
) : ViewModel() {

    private val intakeId: Long = savedStateHandle["intakeId"] ?: -1L
    private val mealId: Long = savedStateHandle["mealId"] ?: -1L
    private val intakeTypeId: Int = savedStateHandle["intakeTypeId"] ?: 0
    private val dateEpochDay: Long = savedStateHandle["date"] ?: LocalDate.now().toEpochDay()

    val isTemplateOnlyEdit: Boolean = intakeId <= 0 && mealId > 0
    val isEditing: Boolean = intakeId > 0 || mealId > 0

    val useKj: StateFlow<Boolean> = settingsRepo.useKj
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    var selectedDate by mutableStateOf(LocalDate.ofEpochDay(dateEpochDay))
        private set

    var state by mutableStateOf(MealEditState())
        private set

    var intakeType by mutableStateOf(IntakeType.entries.getOrElse(intakeTypeId) { IntakeType.BREAKFAST })

    var sharedIntakeCount by mutableIntStateOf(0)
        private set

    var cloneAsNewMeal by mutableStateOf(true)

    var isSaving by mutableStateOf(false)
        private set

    var isSaveCompleted by mutableStateOf(false)
        private set

    var validationError by mutableStateOf<String?>(null)
        private set

    val foodItems = mutableStateListOf<EditableFoodItem>()

    private var loadedMeal: Meal? = null
    private var loadedIntake: Intake? = null

    init {
        if (isEditing) {
            viewModelScope.launch {
                val intakeWithMeal = if (intakeId > 0) intakeRepo.getWithMealById(intakeId) else null
                val intake = intakeWithMeal?.intake ?: if (mealId > 0 && intakeId > 0) intakeRepo.getByMealId(mealId) else null
                val meal = intakeWithMeal?.meal ?: if (mealId > 0) mealRepo.getById(mealId) else null

                loadedMeal = meal
                loadedIntake = intake

                if (meal != null) {
                    val count = intakeRepo.countByMealId(meal.id)
                    sharedIntakeCount = count
                    cloneAsNewMeal = count > 1 && !isTemplateOnlyEdit

                    meal.foodItemsJson?.let { json ->
                        try {
                            val itemsType = object : TypeToken<List<NutritionResult.FoodItem>>() {}.type
                            val parsedItems: List<NutritionResult.FoodItem> = gson.fromJson(json, itemsType)
                            parsedItems.forEach { item ->
                                foodItems.add(
                                    EditableFoodItem(
                                        name = item.name,
                                        weightG = UnitCalc.formatDecimal(item.weightG, 1),
                                        calories = UnitCalc.formatDecimal(item.calories, 1),
                                        carbs = UnitCalc.formatDecimal(item.carbs, 1),
                                        protein = UnitCalc.formatDecimal(item.protein, 1),
                                        fat = UnitCalc.formatDecimal(item.fat, 1)
                                    )
                                )
                            }
                        } catch (_: Exception) {
                        }
                    }

                    state = MealEditState(
                        name = meal.name,
                        amountStr = intake?.amount?.let { UnitCalc.formatDecimal(it, 0) }
                            ?: meal.portionGrams?.let { UnitCalc.formatDecimal(it, 0) }
                            ?: "100",
                        energyStr = UnitCalc.formatDecimal(meal.energyKcal100, 1),
                        carbsStr = UnitCalc.formatDecimal(meal.carbohydrates100, 1),
                        fatStr = UnitCalc.formatDecimal(meal.fat100, 1),
                        proteinStr = UnitCalc.formatDecimal(meal.proteins100, 1),
                        sugarsStr = meal.sugars100?.let { UnitCalc.formatDecimal(it, 1) } ?: "",
                        satFatStr = meal.saturatedFat100?.let { UnitCalc.formatDecimal(it, 1) } ?: "",
                        fiberStr = meal.fiber100?.let { UnitCalc.formatDecimal(it, 1) } ?: "",
                        sodiumStr = meal.sodium100?.let { UnitCalc.formatDecimal(it, 1) } ?: "",
                        cholesterolStr = meal.cholesterol100?.let { UnitCalc.formatDecimal(it, 1) } ?: "",
                        portionLabel = meal.portionLabel ?: "",
                        portionGramsStr = meal.portionGrams?.let { UnitCalc.formatDecimal(it, 0) } ?: "",
                        isFavorite = meal.isFavorite
                    )

                    if (foodItems.isNotEmpty() && intake == null) {
                        recalculateTotals()
                    }

                    intake?.let {
                        intakeType = it.intakeType
                        val offset = settingsRepo.dayBoundaryMinutes.first()
                        selectedDate = dayBoundaryCalc.logicalDayOf(it.dateTime, offset)
                    }
                }
            }
        }
    }

    fun clearValidationError() { validationError = null }
    fun updateDate(date: LocalDate) { selectedDate = date }
    fun updateName(v: String) { state = state.copy(name = v) }
    fun updateAmount(v: String) { if (foodItems.isEmpty()) state = state.copy(amountStr = v) }
    fun updateEnergy(v: String) { if (foodItems.isEmpty()) state = state.copy(energyStr = v) }
    fun updateCarbs(v: String) { if (foodItems.isEmpty()) state = state.copy(carbsStr = v) }
    fun updateFat(v: String) { if (foodItems.isEmpty()) state = state.copy(fatStr = v) }
    fun updateProtein(v: String) { if (foodItems.isEmpty()) state = state.copy(proteinStr = v) }
    fun updateSugars(v: String) { state = state.copy(sugarsStr = v) }
    fun updateSatFat(v: String) { state = state.copy(satFatStr = v) }
    fun updateFiber(v: String) { state = state.copy(fiberStr = v) }
    fun updateSodium(v: String) { state = state.copy(sodiumStr = v) }
    fun updateCholesterol(v: String) { state = state.copy(cholesterolStr = v) }
    fun updatePortionLabel(v: String) { state = state.copy(portionLabel = v) }
    fun updatePortionGrams(v: String) { state = state.copy(portionGramsStr = v) }
    fun toggleFavorite() { state = state.copy(isFavorite = !state.isFavorite) }

    fun addFoodItem() {
        val defaultName = LocaleHelper.tr(LocaleHelper.isChinese(appContext), "新食物", "New item")
        foodItems.add(EditableFoodItem(name = defaultName, weightG = "100", calories = "0", carbs = "0", protein = "0", fat = "0"))
        recalculateTotals()
    }

    fun removeFoodItem(id: String) {
        foodItems.removeAll { it.id == id }
        recalculateTotals()
    }

    fun updateFoodItem(id: String, update: (EditableFoodItem) -> EditableFoodItem) {
        val idx = foodItems.indexOfFirst { it.id == id }
        if (idx != -1) {
            foodItems[idx] = update(foodItems[idx])
            recalculateTotals()
        }
    }

    private fun recalculateTotals() {
        if (foodItems.isEmpty()) return
        var totalWeight = 0.0
        var totalCals = 0.0
        var totalCarbs = 0.0
        var totalPro = 0.0
        var totalFat = 0.0

        for (item in foodItems) {
            totalWeight += (item.weightG.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
            totalCals += (item.calories.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
            totalCarbs += (item.carbs.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
            totalPro += (item.protein.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
            totalFat += (item.fat.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
        }

        val safeWeight = totalWeight.coerceAtLeast(1.0)
        val per100 = 100.0 / safeWeight

        state = state.copy(
            amountStr = String.format(Locale.US, "%.0f", safeWeight),
            energyStr = UnitCalc.formatDecimal(totalCals * per100, 2),
            carbsStr = UnitCalc.formatDecimal(totalCarbs * per100, 2),
            proteinStr = UnitCalc.formatDecimal(totalPro * per100, 2),
            fatStr = UnitCalc.formatDecimal(totalFat * per100, 2)
        )
    }

    private fun buildMealEntity(targetMealId: Long): Meal? {
        val isZh = LocaleHelper.isChinese(appContext)
        if (state.name.isBlank()) {
            validationError = LocaleHelper.tr(isZh, "请输入食物名称", "Please enter a food name")
            return null
        }

        val energy = state.energyStr.toDoubleOrNull() ?: 0.0
        val carbs = state.carbsStr.toDoubleOrNull() ?: 0.0
        val fat = state.fatStr.toDoubleOrNull() ?: 0.0
        val protein = state.proteinStr.toDoubleOrNull() ?: 0.0

        if (energy < 0 || carbs < 0 || fat < 0 || protein < 0) {
            validationError = LocaleHelper.tr(isZh, "营养成分数值不能为负数", "Nutrient values cannot be negative")
            return null
        }

        val newFoodItemsJson = if (foodItems.isNotEmpty()) {
            val unnamed = LocaleHelper.tr(isZh, "未命名", "Unnamed")
            val items = foodItems.map {
                NutritionResult.FoodItem(
                    name = it.name.ifBlank { unnamed },
                    weightG = (it.weightG.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                    calories = (it.calories.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                    carbs = (it.carbs.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                    protein = (it.protein.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
                    fat = (it.fat.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                )
            }
            gson.toJson(items)
        } else null

        val original = loadedMeal
        return Meal(
            id = targetMealId,
            name = state.name.trim(),
            localImagePath = original?.localImagePath,
            source = original?.source ?: MealSource.MANUAL,
            energyKcal100 = energy,
            carbohydrates100 = carbs,
            fat100 = fat,
            proteins100 = protein,
            sugars100 = state.sugarsStr.toDoubleOrNull()?.coerceAtLeast(0.0),
            saturatedFat100 = state.satFatStr.toDoubleOrNull()?.coerceAtLeast(0.0),
            fiber100 = state.fiberStr.toDoubleOrNull()?.coerceAtLeast(0.0),
            sodium100 = state.sodiumStr.toDoubleOrNull()?.coerceAtLeast(0.0),
            cholesterol100 = state.cholesterolStr.toDoubleOrNull()?.coerceAtLeast(0.0),
            portionLabel = state.portionLabel.trim().takeIf { it.isNotEmpty() },
            portionGrams = state.portionGramsStr.toDoubleOrNull()?.takeIf { it > 0 },
            isFavorite = state.isFavorite,
            isArchived = original?.isArchived ?: false,
            foodItemsJson = newFoodItemsJson
        )
    }

    fun save() = saveIntake()

    fun saveIntake() {
        val isZh = LocaleHelper.isChinese(appContext)
        val amount = state.amountStr.toDoubleOrNull()
        if (!isTemplateOnlyEdit && (amount == null || amount <= 0.0)) {
            validationError = LocaleHelper.tr(isZh, "请输入大于 0 的食用份量 (g)", "Please enter serving size > 0g")
            return
        }

        val targetMealIdForBuild = when {
            isTemplateOnlyEdit -> loadedMeal?.id ?: mealId
            loadedIntake != null && loadedMeal != null -> {
                if (sharedIntakeCount > 1 && cloneAsNewMeal) 0L else loadedMeal!!.id
            }
            else -> 0L
        }

        val meal = buildMealEntity(targetMealIdForBuild) ?: return

        viewModelScope.launch {
            isSaving = true
            try {
                if (isTemplateOnlyEdit) {
                    mealRepo.upsert(meal)
                } else if (loadedIntake != null) {
                    val savedMealId = mealRepo.upsert(meal)
                    val offset = settingsRepo.dayBoundaryMinutes.first()
                    val oldIntake = loadedIntake!!
                    val oldLogicalDay = dayBoundaryCalc.logicalDayOf(oldIntake.dateTime, offset)
                    val newDateTime = if (selectedDate == oldLogicalDay) {
                        oldIntake.dateTime
                    } else {
                        val logicalToday = dayBoundaryCalc.currentLogicalDay(offset)
                        if (selectedDate == logicalToday) LocalDateTime.now() else selectedDate.atTime(12, 0)
                    }
                    intakeRepo.upsert(
                        oldIntake.copy(
                            mealId = savedMealId,
                            intakeType = intakeType,
                            amount = amount!!,
                            dateTime = newDateTime
                        )
                    )
                    trackedDayRepo.saveGoalSnapshot(selectedDate, 0.0, 0.0, 0.0, 0.0)
                } else {
                    val savedMealId = mealRepo.upsert(meal)
                    val offset = settingsRepo.dayBoundaryMinutes.first()
                    val logicalToday = dayBoundaryCalc.currentLogicalDay(offset)
                    val dateTime = if (selectedDate == logicalToday) LocalDateTime.now() else selectedDate.atTime(12, 0)
                    intakeRepo.upsert(
                        Intake(
                            mealId = savedMealId,
                            intakeType = intakeType,
                            amount = amount!!,
                            dateTime = dateTime
                        )
                    )
                    trackedDayRepo.saveGoalSnapshot(selectedDate, 0.0, 0.0, 0.0, 0.0)
                }
                NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
                isSaveCompleted = true
            } catch (e: Exception) {
                validationError = LocaleHelper.tr(isZh, "保存失败: ${e.message}", "Save failed: ${e.message}")
            } finally {
                isSaving = false
            }
        }
    }
}
