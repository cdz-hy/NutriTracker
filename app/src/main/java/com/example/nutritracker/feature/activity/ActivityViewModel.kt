package com.example.nutritracker.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.*
import com.example.nutritracker.data.repository.*
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.util.MetCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val activityRepo: ActivityRepository,
    private val trackedDayRepo: TrackedDayRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) : ViewModel() {

    val user: StateFlow<User?> = flow { emit(userRepo.getUser()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addActivity(name: String, mets: Double, duration: Double, burnedKcal: Double) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            activityRepo.upsert(UserActivityEntity(name = name, mets = mets, durationMinutes = duration, burnedKcal = burnedKcal, dateTime = now))
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val day = dayBoundaryCalc.logicalDayOf(now, offset)
            trackedDayRepo.ensureDay(day, 0.0, 0.0, 0.0, 0.0)
            trackedDayRepo.increaseGoal(day, burnedKcal)
        }
    }

    fun addCustomActivity(name: String, kcal: Double, duration: Double) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            activityRepo.upsert(UserActivityEntity(name = name, mets = 0.0, durationMinutes = duration, burnedKcal = kcal, dateTime = now, isCustom = true))
            val offset = settingsRepo.dayBoundaryMinutes.first()
            val day = dayBoundaryCalc.logicalDayOf(now, offset)
            trackedDayRepo.ensureDay(day, 0.0, 0.0, 0.0, 0.0)
            trackedDayRepo.increaseGoal(day, kcal)
        }
    }
}
