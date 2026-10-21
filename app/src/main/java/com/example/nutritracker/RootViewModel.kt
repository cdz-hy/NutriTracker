package com.example.nutritracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.entity.AppLanguage
import com.example.nutritracker.data.entity.ThemeMode
import com.example.nutritracker.data.repository.SettingsRepository
import com.example.nutritracker.util.DayBoundaryCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val dayBoundaryCalc: DayBoundaryCalc
) : ViewModel() {

    val onboardingDone: StateFlow<Boolean?> = settingsRepo.onboardingDone
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode: StateFlow<ThemeMode> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val appLanguage: StateFlow<AppLanguage> = settingsRepo.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.SYSTEM)

    val dynamicColorEnabled: StateFlow<Boolean> = settingsRepo.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            settingsRepo.migrateLegacyApiKeyIfNeeded()
        }
    }

    suspend fun currentLogicalDay(): LocalDate {
        val offset = settingsRepo.dayBoundaryMinutes.first()
        return dayBoundaryCalc.currentLogicalDay(offset)
    }
}
