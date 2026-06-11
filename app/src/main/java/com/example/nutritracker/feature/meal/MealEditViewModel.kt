package com.example.nutritracker.feature.meal

import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.DayBoundaryCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
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
    val sodiumStr: String = ""
)

// Helper for parsing JSON using Gson
private val gson = com.google.gson.Gson()

@HiltViewModel
class MealEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) : ViewModel() {

    private val mealId: Long = savedStateHandle["mealId"] ?: -1L
    private val intakeTypeId: Int = savedStateHandle["intakeTypeId"] ?: 0
    val isEditing = mealId > 0

    var state by mutableStateOf(MealEditState())
        private set
    var intakeType by mutableStateOf(IntakeType.entries.getOrElse(intakeTypeId) { IntakeType.BREAKFAST })

    var isSaving by mutableStateOf(false)
        private set
    var isSaveCompleted by mutableStateOf(false)
        private set

    val foodItems = mutableStateListOf<EditableFoodItem>()

    // 保存原始图片路径，编辑后保留
    private var originalImagePath: String? = null

    init {
        if (isEditing) {
            viewModelScope.launch {
                val meal = mealRepo.getById(mealId)
                val intake = intakeRepo.getByMealId(mealId)
                if (meal != null) {
                    originalImagePath = meal.localImagePath
                    
                    meal.foodItemsJson?.let { json ->
                        try {
                            val itemsType = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                            val parsedItems: List<Map<String, Any>> = gson.fromJson(json, itemsType)
                            parsedItems.forEach { itemMap ->
                                foodItems.add(
                                    EditableFoodItem(
                                        name = (itemMap["name"] as? String) ?: "",
                                        weightG = (itemMap["weight_g"] as? Double)?.toString() ?: "0",
                                        calories = (itemMap["calories"] as? Double)?.toString() ?: "0",
                                        carbs = (itemMap["carbs"] as? Double)?.toString() ?: "0",
                                        protein = (itemMap["protein"] as? Double)?.toString() ?: "0",
                                        fat = (itemMap["fat"] as? Double)?.toString() ?: "0"
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }

                    state = MealEditState(
                        name = meal.name,
                        amountStr = intake?.amount?.toInt()?.toString() ?: "100",
                        energyStr = meal.energyKcal100.toString(),
                        carbsStr = meal.carbohydrates100.toString(),
                        fatStr = meal.fat100.toString(),
                        proteinStr = meal.proteins100.toString(),
                        sugarsStr = meal.sugars100?.toString() ?: "",
                        satFatStr = meal.saturatedFat100?.toString() ?: "",
                        fiberStr = meal.fiber100?.toString() ?: "",
                        sodiumStr = meal.sodium100?.toString() ?: ""
                    )
                    
                    if (foodItems.isNotEmpty()) {
                        recalculateTotals()
                    }

                    intake?.let {
                        intakeType = it.intakeType
                    }
                }
            }
        }
    }

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

    fun addFoodItem() {
        foodItems.add(EditableFoodItem(name = "新食物", weightG = "100", calories = "0", carbs = "0", protein = "0", fat = "0"))
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
            totalWeight += item.weightG.toDoubleOrNull() ?: 0.0
            totalCals += item.calories.toDoubleOrNull() ?: 0.0
            totalCarbs += item.carbs.toDoubleOrNull() ?: 0.0
            totalPro += item.protein.toDoubleOrNull() ?: 0.0
            totalFat += item.fat.toDoubleOrNull() ?: 0.0
        }

        totalWeight = totalWeight.coerceAtLeast(0.1) // prevent div by zero
        val per100Factor = 100.0 / totalWeight

        state = state.copy(
            amountStr = totalWeight.toInt().toString(),
            energyStr = "%.2f".format(totalCals * per100Factor).replace(",", "."),
            carbsStr = "%.2f".format(totalCarbs * per100Factor).replace(",", "."),
            proteinStr = "%.2f".format(totalPro * per100Factor).replace(",", "."),
            fatStr = "%.2f".format(totalFat * per100Factor).replace(",", ".")
        )
    }

    private fun buildMeal(): Meal? {
        if (state.name.isBlank()) return null
        
        val newFoodItemsJson = if (foodItems.isNotEmpty()) {
            val jsonList = foodItems.map {
                mapOf(
                    "name" to it.name,
                    "weight_g" to (it.weightG.toDoubleOrNull() ?: 0.0),
                    "calories" to (it.calories.toDoubleOrNull() ?: 0.0),
                    "carbs" to (it.carbs.toDoubleOrNull() ?: 0.0),
                    "protein" to (it.protein.toDoubleOrNull() ?: 0.0),
                    "fat" to (it.fat.toDoubleOrNull() ?: 0.0)
                )
            }
            gson.toJson(jsonList)
        } else null

        return Meal(
            id = if (isEditing) mealId else 0,
            name = state.name,
            source = if (isEditing) MealSource.CUSTOM else MealSource.MANUAL,
            energyKcal100 = state.energyStr.toDoubleOrNull() ?: 0.0,
            carbohydrates100 = state.carbsStr.toDoubleOrNull() ?: 0.0,
            fat100 = state.fatStr.toDoubleOrNull() ?: 0.0,
            proteins100 = state.proteinStr.toDoubleOrNull() ?: 0.0,
            sugars100 = state.sugarsStr.toDoubleOrNull(),
            saturatedFat100 = state.satFatStr.toDoubleOrNull(),
            fiber100 = state.fiberStr.toDoubleOrNull(),
            sodium100 = state.sodiumStr.toDoubleOrNull(),
            localImagePath = originalImagePath,
            foodItemsJson = newFoodItemsJson
        )
    }

    fun save() {
        saveIntake()
    }

    fun saveIntake() {
        val meal = buildMeal() ?: return
        val amount = state.amountStr.toDoubleOrNull() ?: 100.0
        viewModelScope.launch {
            isSaving = true
            try {
                if (isEditing) {
                    val oldMeal = mealRepo.getById(mealId)
                    val oldIntake = intakeRepo.getByMealId(mealId)
                    if (oldMeal != null && oldIntake != null) {
                        val offset = settingsRepo.dayBoundaryMinutes.first()
                        val day = dayBoundaryCalc.logicalDayOf(oldIntake.dateTime, offset)

                        // 1. 移除旧的卡路里贡献
                        val oldFactor = oldIntake.amount / 100.0
                        trackedDayRepo.removeCalories(
                            day,
                            oldMeal.energyKcal100 * oldFactor,
                            oldMeal.carbohydrates100 * oldFactor,
                            oldMeal.fat100 * oldFactor,
                            oldMeal.proteins100 * oldFactor
                        )

                        // 2. 保存/更新 Meal
                        mealRepo.upsert(meal)

                        // 3. 更新 Intake
                        val updatedIntake = oldIntake.copy(
                            intakeType = intakeType,
                            amount = amount
                        )
                        intakeRepo.upsert(updatedIntake)

                        // 4. 添加新的卡路里贡献
                        val newFactor = amount / 100.0
                        trackedDayRepo.ensureDay(day, 0.0, 0.0, 0.0, 0.0)
                        trackedDayRepo.addCalories(
                            day,
                            meal.energyKcal100 * newFactor,
                            meal.carbohydrates100 * newFactor,
                            meal.fat100 * newFactor,
                            meal.proteins100 * newFactor
                        )
                    }
                } else {
                    val savedMealId = mealRepo.upsert(meal)
                    val now = LocalDateTime.now()
                    intakeRepo.upsert(Intake(mealId = savedMealId, intakeType = intakeType, amount = amount, dateTime = now))
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
                isSaveCompleted = true
            } catch (_: Exception) {
                // Ignore or log error
            } finally {
                isSaving = false
            }
        }
    }
}
