package com.example.nutritracker.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.nutritracker.data.entity.AppLanguage
import com.example.nutritracker.data.entity.IntakeType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocalIsChinese = staticCompositionLocalOf { true }
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.SYSTEM }

@Composable
@ReadOnlyComposable
fun tr(zh: String, en: String): String = if (LocalIsChinese.current) zh else en

object LocaleHelper {
    private const val PREFS_NAME = "nutritracker_locale"
    private const val KEY_APP_LANGUAGE = "app_language"

    @Volatile
    private var cachedLanguage: AppLanguage? = null

    fun persistSync(context: Context, language: AppLanguage) {
        cachedLanguage = language
        val prefsCtx = context.applicationContext ?: context
        runCatching {
            prefsCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_APP_LANGUAGE, language.name)
                .apply()
        }
    }

    fun getSavedLanguageSync(context: Context): AppLanguage {
        cachedLanguage?.let { return it }
        val prefsCtx = context.applicationContext ?: context
        return runCatching {
            val raw = prefsCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_APP_LANGUAGE, AppLanguage.SYSTEM.name)
            val loaded = runCatching { AppLanguage.valueOf(raw ?: AppLanguage.SYSTEM.name) }
                .getOrDefault(AppLanguage.SYSTEM)
            cachedLanguage = loaded
            loaded
        }.getOrDefault(AppLanguage.SYSTEM)
    }

    fun isSystemChinese(): Boolean {
        val sysLocale = runCatching {
            val list = Resources.getSystem().configuration.locales
            if (!list.isEmpty) list[0] else null
        }.getOrNull()
        val lang = (sysLocale ?: Locale.getDefault()).language
        return lang.equals("zh", ignoreCase = true)
    }

    fun isChinese(language: AppLanguage): Boolean = when (language) {
        AppLanguage.ZH -> true
        AppLanguage.EN -> false
        AppLanguage.SYSTEM -> isSystemChinese()
    }

    fun isChinese(context: Context): Boolean = isChinese(getSavedLanguageSync(context))

    fun isCurrentLocaleChinese(): Boolean = Locale.getDefault().language.equals("zh", ignoreCase = true)

    fun targetLocale(language: AppLanguage): Locale = when (language) {
        AppLanguage.ZH -> Locale.SIMPLIFIED_CHINESE
        AppLanguage.EN -> Locale.ENGLISH
        AppLanguage.SYSTEM -> runCatching {
            val list = Resources.getSystem().configuration.locales
            if (!list.isEmpty) list[0] else Locale.getDefault()
        }.getOrElse { Locale.getDefault() }
    }

    fun applyDefaultLocale(language: AppLanguage): Locale {
        val locale = targetLocale(language)
        Locale.setDefault(locale)
        return locale
    }

    fun localizedContext(context: Context, language: AppLanguage = getSavedLanguageSync(context)): Context {
        val locale = targetLocale(language)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun wrapBaseContext(base: Context): Context {
        val lang = getSavedLanguageSync(base)
        applyDefaultLocale(lang)
        return if (lang == AppLanguage.SYSTEM) base else localizedContext(base, lang)
    }

    fun wrapActivityContext(activity: Context, language: AppLanguage): Context {
        applyDefaultLocale(language)
        val configCtx = localizedContext(activity, language)
        return LocalizedActivityContextWrapper(activity, configCtx.resources)
    }

    private class LocalizedActivityContextWrapper(
        base: Context,
        private val localizedResources: Resources
    ) : ContextWrapper(base) {
        override fun getResources(): Resources = localizedResources
        override fun getAssets(): AssetManager = localizedResources.assets
        override fun createConfigurationContext(overrideConfiguration: Configuration): Context =
            baseContext.createConfigurationContext(overrideConfiguration)
    }

    fun tr(isZh: Boolean, zh: String, en: String): String = if (isZh) zh else en

    fun tr(context: Context, zh: String, en: String): String =
        if (isChinese(context)) zh else en

    fun mealTypeLabel(type: IntakeType, isZh: Boolean): String = when (type) {
        IntakeType.BREAKFAST -> tr(isZh, "早餐", "Breakfast")
        IntakeType.LUNCH -> tr(isZh, "午餐", "Lunch")
        IntakeType.DINNER -> tr(isZh, "晚餐", "Dinner")
        IntakeType.SNACK -> tr(isZh, "加餐", "Snack")
    }

    fun appLanguageLabel(language: AppLanguage, isZh: Boolean): String = when (language) {
        AppLanguage.SYSTEM -> tr(isZh, "跟随系统", "System")
        AppLanguage.ZH -> "中文"
        AppLanguage.EN -> "English"
    }

    fun formatFullDate(date: LocalDate, isZh: Boolean): String = if (isZh) {
        date.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.SIMPLIFIED_CHINESE))
    } else {
        date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH))
    }

    fun formatMonthDay(date: LocalDate, isZh: Boolean): String = if (isZh) {
        date.format(DateTimeFormatter.ofPattern("MM月dd日", Locale.SIMPLIFIED_CHINESE))
    } else {
        date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
    }

    fun formatDateWithWeekday(date: LocalDate, isZh: Boolean): String = if (isZh) {
        date.format(DateTimeFormatter.ofPattern("M月d日 E", Locale.SIMPLIFIED_CHINESE))
    } else {
        date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))
    }

    fun formatDayCaloriesTitle(date: LocalDate, isZh: Boolean): String = if (isZh) {
        date.format(DateTimeFormatter.ofPattern("MM月dd日热量", Locale.SIMPLIFIED_CHINESE))
    } else {
        "${date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))} Calories"
    }

    fun formatBackupDateTime(dt: LocalDateTime, isZh: Boolean): String = if (isZh) {
        dt.format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm", Locale.SIMPLIFIED_CHINESE))
    } else {
        dt.format(DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH))
    }
}
