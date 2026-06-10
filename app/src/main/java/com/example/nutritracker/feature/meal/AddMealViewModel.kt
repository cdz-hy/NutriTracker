package com.example.nutritracker.feature.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.feature.camera.NutritionResult
import com.example.nutritracker.util.DayBoundaryCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AddMealViewModel @Inject constructor(
    private val mealRepo: MealRepository,
    private val intakeRepo: IntakeRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) : ViewModel() {

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    init { search("") }

    fun search(query: String) {
        viewModelScope.launch {
            _meals.value = if (query.isBlank()) {
                mealRepo.getAllFlow().first().take(50)
            } else {
                mealRepo.search(query)
            }
        }
    }

    fun createMealFromNutrition(nr: NutritionResult) {
        viewModelScope.launch {
            mealRepo.upsert(
                Meal(
                    name = nr.name,
                    brands = nr.brands,
                    source = MealSource.AI_ANALYSIS,
                    energyKcal100 = nr.energyKcal100,
                    carbohydrates100 = nr.carbohydrates100,
                    fat100 = nr.fat100,
                    proteins100 = nr.proteins100,
                    sugars100 = nr.sugars100,
                    saturatedFat100 = nr.saturatedFat100,
                    fiber100 = nr.fiber100,
                    sodium100 = nr.sodium100
                )
            )
        }
    }

    fun createManualMeal(
        name: String, kcal: Double, carbs: Double, fat: Double, protein: Double,
        weight: Double, intakeTypeId: Int
    ) {
        viewModelScope.launch {
            val mealId = mealRepo.upsert(
                Meal(
                    name = name, source = MealSource.MANUAL,
                    energyKcal100 = kcal, carbohydrates100 = carbs,
                    fat100 = fat, proteins100 = protein
                )
            )
            addIntake(mealId, weight, IntakeType.entries[intakeTypeId])
        }
    }

    private suspend fun addIntake(mealId: Long, amount: Double, type: IntakeType) {
        val now = LocalDateTime.now()
        intakeRepo.upsert(Intake(mealId = mealId, intakeType = type, amount = amount, dateTime = now))
        val meal = mealRepo.getById(mealId) ?: return
        val offset = settingsRepo.dayBoundaryMinutes.first()
        val day = dayBoundaryCalc.logicalDayOf(now, offset)
        val goal = settingsRepo.carbPct.first() // just to get settings
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
