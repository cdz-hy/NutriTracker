package com.example.nutritracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val dayBoundaryStr: String = "0",
    val kcalAdjStr: String = "0",
    val carbPct: Double = 0.60,
    val fatPct: Double = 0.25,
    val proteinPct: Double = 0.15,
    val waterGoalStr: String = "2000",
    val aiApiKey: String = "",
    val aiBaseUrl: String = "https://api.openai.com/v1",
    val aiModel: String = "gpt-4o"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private data class CalcValues(val dayBnd: Int, val kcalAdj: Double, val carb: Double, val fat: Double, val protein: Double)
    private data class AiValues(val water: Int, val aiKey: String, val aiUrl: String, val aiModel: String)

    val state: StateFlow<SettingsState> = combine(
        combine(
            settingsRepo.dayBoundaryMinutes,
            settingsRepo.kcalAdjustment,
            settingsRepo.carbPct,
            settingsRepo.fatPct,
            settingsRepo.proteinPct
        ) { dayBnd, kcalAdj, carb, fat, protein ->
            CalcValues(dayBnd, kcalAdj, carb, fat, protein)
        },
        combine(
            settingsRepo.waterGoalMl,
            settingsRepo.aiApiKey,
            settingsRepo.aiBaseUrl,
            settingsRepo.aiModel
        ) { water, aiKey, aiUrl, aiModel ->
            AiValues(water, aiKey, aiUrl, aiModel)
        }
    ) { calc: CalcValues, ai: AiValues ->
        SettingsState(
            dayBoundaryStr = calc.dayBnd.toString(),
            kcalAdjStr = calc.kcalAdj.toInt().toString(),
            carbPct = calc.carb, fatPct = calc.fat, proteinPct = calc.protein,
            waterGoalStr = ai.water.toString(),
            aiApiKey = ai.aiKey, aiBaseUrl = ai.aiUrl, aiModel = ai.aiModel
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun updateDayBoundary(v: String) {
        v.toIntOrNull()?.let { viewModelScope.launch { settingsRepo.setDayBoundaryMinutes(it) } }
    }

    fun updateKcalAdj(v: String) {
        v.toDoubleOrNull()?.let { viewModelScope.launch { settingsRepo.setKcalAdjustment(it) } }
    }

    fun updateMacros(carb: Double, fat: Double, protein: Double) {
        viewModelScope.launch {
            settingsRepo.setCarbPct(carb)
            settingsRepo.setFatPct(fat)
            settingsRepo.setProteinPct(protein)
        }
    }

    fun updateWaterGoal(v: String) {
        v.toIntOrNull()?.let { viewModelScope.launch { settingsRepo.setWaterGoalMl(it) } }
    }

    fun updateAiConfig(apiKey: String, baseUrl: String, model: String) {
        viewModelScope.launch {
            settingsRepo.setAiApiKey(apiKey)
            settingsRepo.setAiBaseUrl(baseUrl)
            settingsRepo.setAiModel(model)
        }
    }
}
