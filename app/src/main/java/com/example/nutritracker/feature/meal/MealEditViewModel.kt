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

    init {
        if (isEditing) {
            viewModelScope.launch {
                mealRepo.getById(mealId)?.let { meal ->
                    state = MealEditState(
                        name = meal.name,
                        energyStr = meal.energyKcal100.toString(),
                        carbsStr = meal.carbohydrates100.toString(),
                        fatStr = meal.fat100.toString(),
                        proteinStr = meal.proteins100.toString(),
                        sugarsStr = meal.sugars100?.toString() ?: "",
                        satFatStr = meal.saturatedFat100?.toString() ?: "",
                        fiberStr = meal.fiber100?.toString() ?: "",
                        sodiumStr = meal.sodium100?.toString() ?: ""
                    )
                }
            }
        }
    }

    fun updateName(v: String) { state = state.copy(name = v) }
    fun updateAmount(v: String) { state = state.copy(amountStr = v) }
    fun updateEnergy(v: String) { state = state.copy(energyStr = v) }
    fun updateCarbs(v: String) { state = state.copy(carbsStr = v) }
    fun updateFat(v: String) { state = state.copy(fatStr = v) }
    fun updateProtein(v: String) { state = state.copy(proteinStr = v) }
    fun updateSugars(v: String) { state = state.copy(sugarsStr = v) }
    fun updateSatFat(v: String) { state = state.copy(satFatStr = v) }
    fun updateFiber(v: String) { state = state.copy(fiberStr = v) }
    fun updateSodium(v: String) { state = state.copy(sodiumStr = v) }

    private fun buildMeal(): Meal? {
        if (state.name.isBlank()) return null
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
            sodium100 = state.sodiumStr.toDoubleOrNull()
        )
    }

    fun save() {
        val meal = buildMeal() ?: return
        viewModelScope.launch { mealRepo.upsert(meal) }
    }

    fun saveIntake() {
        val meal = buildMeal() ?: return
        val amount = state.amountStr.toDoubleOrNull() ?: 100.0
        viewModelScope.launch {
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
    }
}
