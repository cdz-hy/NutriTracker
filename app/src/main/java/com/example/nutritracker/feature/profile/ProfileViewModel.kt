package com.example.nutritracker.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.SettingsRepository
import com.example.nutritracker.data.repository.UserRepository
import com.example.nutritracker.data.repository.WeightLogRepository
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepo: UserRepository,
    private val weightLogRepo: WeightLogRepository,
    settingsRepo: SettingsRepository
) : ViewModel() {

    val user: StateFlow<User?> = userRepo.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weightLogs: StateFlow<List<WeightLog>> = weightLogRepo.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val useKj: StateFlow<Boolean> = settingsRepo.useKj
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private fun updateUser(transform: (User) -> User) {
        viewModelScope.launch {
            val current = userRepo.getUser() ?: return@launch
            val updated = transform(current)
            userRepo.upsert(updated)
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun updateBirthday(birthday: LocalDate) = updateUser { it.copy(birthday = birthday) }
    fun updateHeight(heightCm: Double) = updateUser { it.copy(heightCm = heightCm) }

    fun logWeight(weightKg: Double, date: LocalDate = LocalDate.now()) {
        if (weightKg !in 20.0..400.0) return
        viewModelScope.launch {
            weightLogRepo.upsert(WeightLog(date = date, weightKg = weightKg))
            val latest = weightLogRepo.getLatest()
            if (latest == null || !date.isBefore(latest.date)) {
                val current = userRepo.getUser()
                if (current != null) {
                    userRepo.upsert(current.copy(weightKg = weightKg))
                    NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
                }
            }
        }
    }

    fun updateTargetWeight(targetWeightKg: Double?) = updateUser { it.copy(targetWeightKg = targetWeightKg) }
    fun updateGender(gender: Gender) = updateUser { it.copy(gender = gender) }
    fun updateActivityLevel(level: ActivityLevel) = updateUser { it.copy(activityLevel = level) }
    fun updateWeightGoal(goal: WeightGoal) = updateUser { it.copy(weightGoal = goal) }
    fun updateWeeklyGoal(weeklyGoalKg: Double?) = updateUser { it.copy(weeklyWeightGoalKg = weeklyGoalKg) }
    fun updateCaloriesProfile(profile: CaloriesProfile) = updateUser { it.copy(caloriesProfile = profile) }
    fun toggleTaper(enabled: Boolean) = updateUser { it.copy(caloriesTaperEnabled = enabled) }

    fun deleteWeightLog(date: LocalDate) {
        viewModelScope.launch {
            weightLogRepo.deleteByDate(date)
        }
    }
}
