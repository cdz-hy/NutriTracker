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

    fun canProceed(page: Int): Boolean = getValidationError(page) == null

    /**
     * 返回验证错误信息，如果通过验证则返回 null
     */
    fun getValidationError(page: Int): String? = when (page) {
        0 -> validatePage0()
        1 -> validatePage1()
        2 -> null // 活动水平始终有效
        3 -> null // 体重目标始终有效
        4 -> validatePage4()
        else -> null
    }

    private fun validatePage0(): String? {
        if (parseBirthday() == null) return "请输入有效的生日日期 (YYYY-MM-DD)"
        val birthday = parseBirthday()!!
        val age = java.time.Period.between(birthday, LocalDate.now()).years
        if (age < 10) return "年龄不能小于 10 岁"
        if (age > 120) return "年龄不能大于 120 岁"
        return null
    }

    private fun validatePage1(): String? {
        val height = heightStr.toDoubleOrNull()
        val weight = weightStr.toDoubleOrNull()
        if (height == null) return "请输入身高"
        if (weight == null) return "请输入体重"
        if (height < 100) return "身高不能小于 100 cm"
        if (height > 250) return "身高不能大于 250 cm"
        if (weight < 30) return "体重不能小于 30 kg"
        if (weight > 300) return "体重不能大于 300 kg"
        // 验证目标体重（如果填写了）
        val targetWeight = targetWeightStr.toDoubleOrNull()
        if (targetWeight != null) {
            if (targetWeight < 30) return "目标体重不能小于 30 kg"
            if (targetWeight > 300) return "目标体重不能大于 300 kg"
        }
        return null
    }

    private fun validatePage4(): String? {
        if (buildUser() == null) return "请完善前面的信息"
        return null
    }

    private fun parseBirthday(): LocalDate? = try {
        LocalDate.parse(birthdayStr)
    } catch (_: Exception) { null }

    fun buildUser(): User? {
        val birthday = parseBirthday() ?: return null
        val height = heightStr.toDoubleOrNull() ?: return null
        val weight = weightStr.toDoubleOrNull() ?: return null
        if (height < 100 || height > 250) return null
        if (weight < 30 || weight > 300) return null
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
}
