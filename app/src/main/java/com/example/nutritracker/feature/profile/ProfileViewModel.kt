package com.example.nutritracker.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.UserRepository
import com.example.nutritracker.data.repository.WeightLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val weightLogRepo: WeightLogRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    val weightLogs: StateFlow<List<WeightLog>> = weightLogRepo.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _user.value = userRepo.getUser()
        }
    }

    private fun updateUser(transform: (User) -> User) {
        viewModelScope.launch {
            val current = _user.value ?: return@launch
            val updated = transform(current)
            userRepo.upsert(updated)
            _user.value = updated
        }
    }

    fun updateBirthday(birthday: LocalDate) {
        updateUser { it.copy(birthday = birthday) }
    }

    fun updateHeight(heightCm: Double) {
        updateUser { it.copy(heightCm = heightCm) }
    }

    fun updateWeight(weightKg: Double) {
        viewModelScope.launch {
            weightLogRepo.upsert(WeightLog(date = LocalDate.now(), weightKg = weightKg))
            updateUser { it.copy(weightKg = weightKg) }
        }
    }

    fun updateTargetWeight(targetWeightKg: Double?) {
        updateUser { it.copy(targetWeightKg = targetWeightKg) }
    }

    fun updateGender(gender: Gender) {
        updateUser { it.copy(gender = gender) }
    }

    fun updateActivityLevel(level: ActivityLevel) {
        updateUser { it.copy(activityLevel = level) }
    }

    fun updateWeightGoal(goal: WeightGoal) {
        updateUser { it.copy(weightGoal = goal) }
    }

    fun updateWeeklyGoal(weeklyGoalKg: Double?) {
        updateUser { it.copy(weeklyWeightGoalKg = weeklyGoalKg) }
    }

    fun updateCaloriesProfile(profile: CaloriesProfile) {
        updateUser { it.copy(caloriesProfile = profile) }
    }

    fun deleteWeightLog(date: LocalDate) {
        viewModelScope.launch {
            weightLogRepo.deleteByDate(date)
        }
    }

    fun toggleTaper(enabled: Boolean) {
        updateUser { it.copy(caloriesTaperEnabled = enabled) }
    }
}
