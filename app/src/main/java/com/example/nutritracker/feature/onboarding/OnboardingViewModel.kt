package com.example.nutritracker.feature.onboarding

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.SettingsRepository
import com.example.nutritracker.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    var gender by mutableStateOf(Gender.MALE)
    var caloriesProfile by mutableStateOf(CaloriesProfile.AVERAGED)
    var birthdayStr by mutableStateOf("2000-01-01")
    var heightStr by mutableStateOf("")
    var weightStr by mutableStateOf("")
    var targetWeightStr by mutableStateOf("")
    var activityLevel by mutableStateOf(ActivityLevel.SEDENTARY)
    var weightGoal by mutableStateOf(WeightGoal.MAINTAIN)
    var weeklyGoalStr by mutableStateOf("")

    fun canProceed(page: Int): Boolean = when (page) {
        0 -> parseBirthday() != null
        1 -> heightStr.toDoubleOrNull() != null && weightStr.toDoubleOrNull() != null
        2 -> true
        3 -> true
        4 -> buildUser() != null
        else -> false
    }

    private fun parseBirthday(): LocalDate? = try {
        LocalDate.parse(birthdayStr)
    } catch (_: Exception) { null }

    fun buildUser(): User? {
        val birthday = parseBirthday() ?: return null
        val height = heightStr.toDoubleOrNull() ?: return null
        val weight = weightStr.toDoubleOrNull() ?: return null
        if (height < 30 || height > 300 || weight < 2 || weight > 640) return null
        return User(
            birthday = birthday, heightCm = height, weightKg = weight,
            gender = gender, activityLevel = activityLevel, weightGoal = weightGoal,
            caloriesProfile = if (gender == Gender.NON_BINARY) caloriesProfile else CaloriesProfile.AVERAGED,
            weeklyWeightGoalKg = weeklyGoalStr.toDoubleOrNull(),
            targetWeightKg = targetWeightStr.toDoubleOrNull()?.takeIf { it > 0 }
        )
    }

    fun save() {
        val user = buildUser() ?: return
        viewModelScope.launch {
            userRepo.upsert(user)
            settingsRepo.setOnboardingDone(true)
        }
    }

    fun createInitialWeightLog(weightKg: Double) {
        viewModelScope.launch {
            // Weight log will be created when profile updates the weight
        }
    }
}
