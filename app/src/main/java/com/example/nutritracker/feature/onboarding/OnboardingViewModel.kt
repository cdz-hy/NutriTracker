package com.example.nutritracker.feature.onboarding

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.SettingsRepository
import com.example.nutritracker.data.repository.UserRepository
import com.example.nutritracker.data.repository.WeightLogRepository
import com.example.nutritracker.util.BmiStandard
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepo: UserRepository,
    private val weightLogRepo: WeightLogRepository,
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
    var bmiStandard by mutableStateOf(
        if (java.util.Locale.getDefault().language == "zh") BmiStandard.CHINA else BmiStandard.WHO
    )
    var isSaving by mutableStateOf(false)
        private set

    fun canProceed(page: Int): Boolean = !isSaving && getValidationError(page) == null

    fun getValidationError(page: Int): String? = when (page) {
        0 -> validatePage0()
        1 -> validatePage1()
        2 -> null
        3 -> null
        4 -> validatePage4()
        else -> null
    }

    private fun validatePage0(): String? {
        val birthday = parseBirthday() ?: return "请输入有效的生日日期 (YYYY-MM-DD)"
        val age = java.time.Period.between(birthday, LocalDate.now()).years
        if (age < 10) return "年龄不能小于 10 岁"
        if (age > 120) return "年龄不能大于 120 岁"
        return null
    }

    private fun validatePage1(): String? {
        val height = heightStr.toDoubleOrNull() ?: return "请输入身高"
        val weight = weightStr.toDoubleOrNull() ?: return "请输入体重"
        if (height < 100) return "身高不能小于 100 cm"
        if (height > 250) return "身高不能大于 250 cm"
        if (weight < 30) return "体重不能小于 30 kg"
        if (weight > 300) return "体重不能大于 300 kg"
        val targetWeight = targetWeightStr.toDoubleOrNull()
        if (targetWeight != null) {
            if (targetWeight < 30) return "目标体重不能小于 30 kg"
            if (targetWeight > 300) return "目标体重不能大于 300 kg"
        }
        return null
    }

    private fun validatePage4(): String? = if (buildUser() == null) "请完善前面的信息" else null

    private fun parseBirthday(): LocalDate? = try {
        LocalDate.parse(birthdayStr)
    } catch (_: Exception) {
        null
    }

    fun buildUser(): User? {
        val birthday = parseBirthday() ?: return null
        val height = heightStr.toDoubleOrNull() ?: return null
        val weight = weightStr.toDoubleOrNull() ?: return null
        if (height !in 100.0..250.0) return null
        if (weight !in 30.0..300.0) return null
        return User(
            birthday = birthday,
            heightCm = height,
            weightKg = weight,
            gender = gender,
            activityLevel = activityLevel,
            weightGoal = weightGoal,
            caloriesProfile = if (gender == Gender.NON_BINARY) caloriesProfile else CaloriesProfile.AVERAGED,
            weeklyWeightGoalKg = weeklyGoalStr.toDoubleOrNull(),
            targetWeightKg = targetWeightStr.toDoubleOrNull()?.takeIf { it in 30.0..300.0 }
        )
    }

    fun save(onSaved: () -> Unit) {
        val user = buildUser() ?: return
        if (isSaving) return
        viewModelScope.launch {
            isSaving = true
            try {
                userRepo.upsert(user)
                weightLogRepo.upsert(WeightLog(date = LocalDate.now(), weightKg = user.weightKg))
                settingsRepo.setOnboardingDone(true)
                NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
                onSaved()
            } finally {
                isSaving = false
            }
        }
    }
}
