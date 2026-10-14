package com.example.nutritracker.feature.activity

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.User
import com.example.nutritracker.data.entity.UserActivityEntity
import com.example.nutritracker.data.repository.ActivityRepository
import com.example.nutritracker.data.repository.SettingsRepository
import com.example.nutritracker.data.repository.UserRepository
import com.example.nutritracker.util.DayBoundaryCalc
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ActivityViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val userRepo: UserRepository,
    private val activityRepo: ActivityRepository,
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) : ViewModel() {

    private val dateEpochDay: Long = savedStateHandle["date"] ?: LocalDate.now().toEpochDay()
    val targetDate: LocalDate = LocalDate.ofEpochDay(dateEpochDay)

    val user: StateFlow<User?> = userRepo.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val useKj: StateFlow<Boolean> = settingsRepo.useKj
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun addActivity(name: String, mets: Double, duration: Double, burnedKcal: Double, category: String? = null) {
        if (duration <= 0.0 || burnedKcal <= 0.0) return
        viewModelScope.launch {
            val dateTime = resolveTargetDateTime()
            activityRepo.upsert(
                UserActivityEntity(
                    name = name,
                    mets = mets,
                    durationMinutes = duration,
                    burnedKcal = burnedKcal,
                    dateTime = dateTime,
                    category = category
                )
            )
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    fun addCustomActivity(name: String, kcal: Double, duration: Double) {
        if (name.isBlank() || kcal <= 0.0 || duration <= 0.0) return
        viewModelScope.launch {
            val dateTime = resolveTargetDateTime()
            activityRepo.upsert(
                UserActivityEntity(
                    name = name.trim(),
                    mets = 0.0,
                    durationMinutes = duration,
                    burnedKcal = kcal,
                    dateTime = dateTime,
                    isCustom = true
                )
            )
            NutriTrackerWidgetUpdater.enqueueUpdate(appContext)
        }
    }

    private suspend fun resolveTargetDateTime(): LocalDateTime {
        val offset = settingsRepo.dayBoundaryMinutes.first()
        val logicalToday = dayBoundaryCalc.currentLogicalDay(offset)
        return if (targetDate == logicalToday) LocalDateTime.now() else targetDate.atTime(12, 0)
    }
}
