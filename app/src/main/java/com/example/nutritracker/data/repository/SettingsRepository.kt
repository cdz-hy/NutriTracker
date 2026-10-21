package com.example.nutritracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.nutritracker.data.entity.AppLanguage
import com.example.nutritracker.data.entity.ThemeMode
import com.example.nutritracker.util.CryptoManager
import com.example.nutritracker.util.LocaleHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {
    private object Keys {
        val DAY_BOUNDARY_MINUTES = intPreferencesKey("day_boundary_minutes")
        val KCAL_ADJUSTMENT = doublePreferencesKey("kcal_adjustment")
        val CARB_PCT = doublePreferencesKey("carb_pct")
        val FAT_PCT = doublePreferencesKey("fat_pct")
        val PROTEIN_PCT = doublePreferencesKey("protein_pct")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val WATER_QUICK_AMOUNTS = stringPreferencesKey("water_quick_amounts")
        val AI_API_KEY_LEGACY = stringPreferencesKey("ai_api_key")
        val AI_API_KEY_ENC = stringPreferencesKey("ai_api_key_enc")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_AUTO_CONFIRM = booleanPreferencesKey("ai_auto_confirm")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val USE_KJ = booleanPreferencesKey("use_kj")
        val MEAL_REMINDER_ENABLED = booleanPreferencesKey("meal_reminder_enabled")
        val BREAKFAST_REMINDER_MIN = intPreferencesKey("breakfast_reminder_min")
        val LUNCH_REMINDER_MIN = intPreferencesKey("lunch_reminder_min")
        val DINNER_REMINDER_MIN = intPreferencesKey("dinner_reminder_min")
        val WATER_REMINDER_ENABLED = booleanPreferencesKey("water_reminder_enabled")
        val WATER_REMINDER_INTERVAL_MIN = intPreferencesKey("water_reminder_interval_min")
    }

    val dayBoundaryMinutes: Flow<Int> = context.dataStore.data.map {
        (it[Keys.DAY_BOUNDARY_MINUTES] ?: 0).coerceIn(0, 1439)
    }
    val kcalAdjustment: Flow<Double> = context.dataStore.data.map { it[Keys.KCAL_ADJUSTMENT] ?: 0.0 }
    val carbPct: Flow<Double> = context.dataStore.data.map { it[Keys.CARB_PCT] ?: 0.60 }
    val fatPct: Flow<Double> = context.dataStore.data.map { it[Keys.FAT_PCT] ?: 0.25 }
    val proteinPct: Flow<Double> = context.dataStore.data.map { it[Keys.PROTEIN_PCT] ?: 0.15 }
    val waterGoalMl: Flow<Int> = context.dataStore.data.map { (it[Keys.WATER_GOAL_ML] ?: 2000).coerceAtLeast(100) }
    val waterQuickAmounts: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        parseQuickAmounts(prefs[Keys.WATER_QUICK_AMOUNTS])
    }

    val aiApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        val encrypted = prefs[Keys.AI_API_KEY_ENC]
        if (!encrypted.isNullOrBlank()) {
            cryptoManager.decrypt(encrypted)
        } else {
            prefs[Keys.AI_API_KEY_LEGACY] ?: ""
        }
    }
    val aiBaseUrl: Flow<String> = context.dataStore.data.map { it[Keys.AI_BASE_URL] ?: "" }
    val aiModel: Flow<String> = context.dataStore.data.map { it[Keys.AI_MODEL] ?: "" }
    val aiAutoConfirm: Flow<Boolean> = context.dataStore.data.map { it[Keys.AI_AUTO_CONFIRM] ?: false }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }
    val appLanguage: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.APP_LANGUAGE]
        val lang = runCatching { AppLanguage.valueOf(raw ?: AppLanguage.SYSTEM.name) }
            .getOrDefault(AppLanguage.SYSTEM)
        if (raw != null && LocaleHelper.getSavedLanguageSync(context) != lang) {
            LocaleHelper.persistSync(context, lang)
        }
        lang
    }
    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR_ENABLED] ?: true }
    val useKj: Flow<Boolean> = context.dataStore.data.map { it[Keys.USE_KJ] ?: false }

    val mealReminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.MEAL_REMINDER_ENABLED] ?: false }
    val breakfastReminderMin: Flow<Int> = context.dataStore.data.map { it[Keys.BREAKFAST_REMINDER_MIN] ?: (8 * 60) }
    val lunchReminderMin: Flow<Int> = context.dataStore.data.map { it[Keys.LUNCH_REMINDER_MIN] ?: (12 * 60 + 30) }
    val dinnerReminderMin: Flow<Int> = context.dataStore.data.map { it[Keys.DINNER_REMINDER_MIN] ?: (18 * 60 + 30) }
    val waterReminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.WATER_REMINDER_ENABLED] ?: false }
    val waterReminderIntervalMin: Flow<Int> = context.dataStore.data.map { (it[Keys.WATER_REMINDER_INTERVAL_MIN] ?: 120).coerceAtLeast(30) }

    suspend fun setDayBoundaryMinutes(v: Int) = context.dataStore.edit {
        it[Keys.DAY_BOUNDARY_MINUTES] = v.coerceIn(0, 1439)
    }
    suspend fun setKcalAdjustment(v: Double) = context.dataStore.edit { it[Keys.KCAL_ADJUSTMENT] = v }
    suspend fun setCarbPct(v: Double) = context.dataStore.edit { it[Keys.CARB_PCT] = v }
    suspend fun setFatPct(v: Double) = context.dataStore.edit { it[Keys.FAT_PCT] = v }
    suspend fun setProteinPct(v: Double) = context.dataStore.edit { it[Keys.PROTEIN_PCT] = v }
    suspend fun setWaterGoalMl(v: Int) = context.dataStore.edit {
        it[Keys.WATER_GOAL_ML] = v.coerceIn(100, 20000)
    }
    suspend fun setWaterQuickAmounts(amounts: List<Int>) = context.dataStore.edit {
        val cleaned = amounts.filter { a -> a in 10..5000 }.distinct().take(5)
        it[Keys.WATER_QUICK_AMOUNTS] = if (cleaned.isNotEmpty()) cleaned.joinToString(",") else "150,250,350,500"
    }

    suspend fun setAiApiKey(v: String) = context.dataStore.edit { prefs ->
        val trimmed = v.trim()
        if (trimmed.isEmpty()) {
            prefs.remove(Keys.AI_API_KEY_ENC)
        } else {
            prefs[Keys.AI_API_KEY_ENC] = cryptoManager.encrypt(trimmed)
        }
        prefs.remove(Keys.AI_API_KEY_LEGACY)
    }

    suspend fun migrateLegacyApiKeyIfNeeded() {
        context.dataStore.edit { prefs ->
            val legacy = prefs[Keys.AI_API_KEY_LEGACY]
            if (!legacy.isNullOrBlank() && prefs[Keys.AI_API_KEY_ENC].isNullOrBlank()) {
                prefs[Keys.AI_API_KEY_ENC] = cryptoManager.encrypt(legacy)
                prefs.remove(Keys.AI_API_KEY_LEGACY)
            }
        }
    }

    suspend fun setAiBaseUrl(v: String) = context.dataStore.edit { it[Keys.AI_BASE_URL] = v.trim() }
    suspend fun setAiModel(v: String) = context.dataStore.edit { it[Keys.AI_MODEL] = v.trim() }
    suspend fun setAiAutoConfirm(v: Boolean) = context.dataStore.edit { it[Keys.AI_AUTO_CONFIRM] = v }
    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_DONE] = v }
    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAppLanguage(language: AppLanguage) {
        LocaleHelper.persistSync(context, language)
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }
    suspend fun setDynamicColorEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    suspend fun setUseKj(useKj: Boolean) = context.dataStore.edit { it[Keys.USE_KJ] = useKj }
    suspend fun setMealReminderEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.MEAL_REMINDER_ENABLED] = enabled }
    suspend fun setBreakfastReminderMin(min: Int) = context.dataStore.edit { it[Keys.BREAKFAST_REMINDER_MIN] = min.coerceIn(0, 1439) }
    suspend fun setLunchReminderMin(min: Int) = context.dataStore.edit { it[Keys.LUNCH_REMINDER_MIN] = min.coerceIn(0, 1439) }
    suspend fun setDinnerReminderMin(min: Int) = context.dataStore.edit { it[Keys.DINNER_REMINDER_MIN] = min.coerceIn(0, 1439) }
    suspend fun setWaterReminderEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.WATER_REMINDER_ENABLED] = enabled }
    suspend fun setWaterReminderIntervalMin(min: Int) = context.dataStore.edit { it[Keys.WATER_REMINDER_INTERVAL_MIN] = min.coerceIn(30, 480) }

    private fun parseQuickAmounts(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return listOf(150, 250, 350, 500)
        val parsed = raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 10..5000 }.distinct()
        return parsed.ifEmpty { listOf(150, 250, 350, 500) }
    }
}
