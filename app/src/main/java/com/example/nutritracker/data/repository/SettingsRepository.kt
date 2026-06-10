package com.example.nutritracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DAY_BOUNDARY_MINUTES = intPreferencesKey("day_boundary_minutes")
        val KCAL_ADJUSTMENT = doublePreferencesKey("kcal_adjustment")
        val CARB_PCT = doublePreferencesKey("carb_pct")
        val FAT_PCT = doublePreferencesKey("fat_pct")
        val PROTEIN_PCT = doublePreferencesKey("protein_pct")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val dayBoundaryMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.DAY_BOUNDARY_MINUTES] ?: 0 }
    val kcalAdjustment: Flow<Double> = context.dataStore.data.map { it[Keys.KCAL_ADJUSTMENT] ?: 0.0 }
    val carbPct: Flow<Double> = context.dataStore.data.map { it[Keys.CARB_PCT] ?: 0.60 }
    val fatPct: Flow<Double> = context.dataStore.data.map { it[Keys.FAT_PCT] ?: 0.25 }
    val proteinPct: Flow<Double> = context.dataStore.data.map { it[Keys.PROTEIN_PCT] ?: 0.15 }
    val waterGoalMl: Flow<Int> = context.dataStore.data.map { it[Keys.WATER_GOAL_ML] ?: 2000 }
    val aiApiKey: Flow<String> = context.dataStore.data.map { it[Keys.AI_API_KEY] ?: "" }
    val aiBaseUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.AI_BASE_URL] ?: "https://api.openai.com/v1"
    }
    val aiModel: Flow<String> = context.dataStore.data.map { it[Keys.AI_MODEL] ?: "gpt-4o" }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setDayBoundaryMinutes(v: Int) = context.dataStore.edit { it[Keys.DAY_BOUNDARY_MINUTES] = v }
    suspend fun setKcalAdjustment(v: Double) = context.dataStore.edit { it[Keys.KCAL_ADJUSTMENT] = v }
    suspend fun setCarbPct(v: Double) = context.dataStore.edit { it[Keys.CARB_PCT] = v }
    suspend fun setFatPct(v: Double) = context.dataStore.edit { it[Keys.FAT_PCT] = v }
    suspend fun setProteinPct(v: Double) = context.dataStore.edit { it[Keys.PROTEIN_PCT] = v }
    suspend fun setWaterGoalMl(v: Int) = context.dataStore.edit { it[Keys.WATER_GOAL_ML] = v }
    suspend fun setAiApiKey(v: String) = context.dataStore.edit { it[Keys.AI_API_KEY] = v }
    suspend fun setAiBaseUrl(v: String) = context.dataStore.edit { it[Keys.AI_BASE_URL] = v }
    suspend fun setAiModel(v: String) = context.dataStore.edit { it[Keys.AI_MODEL] = v }
    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_DONE] = v }
}
