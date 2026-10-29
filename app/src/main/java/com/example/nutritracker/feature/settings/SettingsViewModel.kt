package com.example.nutritracker.feature.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.DataExportManager
import com.example.nutritracker.data.entity.AppLanguage
import com.example.nutritracker.data.entity.ThemeMode
import com.example.nutritracker.data.repository.SettingsRepository
import com.example.nutritracker.data.repository.UserRepository
import com.example.nutritracker.util.LocaleHelper
import com.example.nutritracker.widget.NutriTrackerWidgetUpdater
import com.example.nutritracker.worker.ReminderScheduler
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

enum class TestStatus { IDLE, TESTING, SUCCESS, FAILURE }
enum class ExportImportStatus { IDLE, RUNNING, SUCCESS, FAILURE }

data class SettingsState(
    val dayBoundaryMinutes: Int = 0,
    val kcalAdjustment: Double = 0.0,
    val carbPct: Double = 0.60,
    val fatPct: Double = 0.25,
    val proteinPct: Double = 0.15,
    val waterGoalMl: Int = 2000,
    val waterQuickAmounts: List<Int> = listOf(150, 250, 350, 500),
    val aiApiKey: String = "",
    val aiBaseUrl: String = "",
    val aiModel: String = "",
    val aiAutoConfirm: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val dynamicColorEnabled: Boolean = true,
    val useKj: Boolean = false,
    val usesImperialUnits: Boolean = false,
    val mealReminderEnabled: Boolean = false,
    val breakfastReminderMin: Int = 480,
    val lunchReminderMin: Int = 750,
    val dinnerReminderMin: Int = 1110,
    val waterReminderEnabled: Boolean = false,
    val waterReminderIntervalMin: Int = 120
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val app: Application,
    private val settingsRepo: SettingsRepository,
    private val userRepo: UserRepository,
    private val exportManager: DataExportManager,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : AndroidViewModel(app) {

    private data class CalcPart(val dayBnd: Int, val kcalAdj: Double, val carb: Double, val fat: Double, val protein: Double)
    private data class AiPart(val waterGoal: Int, val quickWater: List<Int>, val apiKey: String, val baseUrl: String, val model: String, val autoConfirm: Boolean)
    private data class PrefsPart(
        val theme: ThemeMode,
        val language: AppLanguage,
        val dynamicColor: Boolean,
        val useKj: Boolean,
        val imperial: Boolean
    )
    private data class ReminderPart(val mealOn: Boolean, val bMin: Int, val lMin: Int, val dMin: Int, val waterOn: Boolean, val waterInterval: Int)

    private val calcFlow = combine(
        settingsRepo.dayBoundaryMinutes,
        settingsRepo.kcalAdjustment,
        settingsRepo.carbPct,
        settingsRepo.fatPct,
        settingsRepo.proteinPct
    ) { db, ka, c, f, p -> CalcPart(db, ka, c, f, p) }

    private val aiFlow = combine(
        combine(settingsRepo.waterGoalMl, settingsRepo.waterQuickAmounts, settingsRepo.aiAutoConfirm) { wg, wq, ac -> Triple(wg, wq, ac) },
        combine(settingsRepo.aiApiKey, settingsRepo.aiBaseUrl, settingsRepo.aiModel) { k, u, m -> Triple(k, u, m) }
    ) { w, ai ->
        AiPart(w.first, w.second, ai.first, ai.second, ai.third, w.third)
    }

    private val prefsFlow = combine(
        settingsRepo.themeMode,
        settingsRepo.appLanguage,
        settingsRepo.dynamicColorEnabled,
        settingsRepo.useKj,
        userRepo.getUserFlow()
    ) { tm, lang, dc, kj, u ->
        PrefsPart(tm, lang, dc, kj, u?.usesImperialUnits ?: false)
    }

    private val reminderFlow = combine(
        combine(settingsRepo.mealReminderEnabled, settingsRepo.breakfastReminderMin, settingsRepo.lunchReminderMin) { on, b, l -> Triple(on, b, l) },
        combine(settingsRepo.dinnerReminderMin, settingsRepo.waterReminderEnabled, settingsRepo.waterReminderIntervalMin) { d, won, wi -> Triple(d, won, wi) }
    ) { m, w ->
        ReminderPart(m.first, m.second, m.third, w.first, w.second, w.third)
    }

    val state: StateFlow<SettingsState> = combine(
        calcFlow,
        aiFlow,
        prefsFlow,
        reminderFlow
    ) { calc, ai, prefs, rem ->
        SettingsState(
            dayBoundaryMinutes = calc.dayBnd,
            kcalAdjustment = calc.kcalAdj,
            carbPct = calc.carb,
            fatPct = calc.fat,
            proteinPct = calc.protein,
            waterGoalMl = ai.waterGoal,
            waterQuickAmounts = ai.quickWater,
            aiApiKey = ai.apiKey,
            aiBaseUrl = ai.baseUrl,
            aiModel = ai.model,
            aiAutoConfirm = ai.autoConfirm,
            themeMode = prefs.theme,
            appLanguage = prefs.language,
            dynamicColorEnabled = prefs.dynamicColor,
            useKj = prefs.useKj,
            usesImperialUnits = prefs.imperial,
            mealReminderEnabled = rem.mealOn,
            breakfastReminderMin = rem.bMin,
            lunchReminderMin = rem.lMin,
            dinnerReminderMin = rem.dMin,
            waterReminderEnabled = rem.waterOn,
            waterReminderIntervalMin = rem.waterInterval
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun updateDayBoundary(minutes: Int) {
        viewModelScope.launch {
            settingsRepo.setDayBoundaryMinutes(minutes.coerceIn(0, 1439))
            NutriTrackerWidgetUpdater.enqueueUpdate(app)
        }
    }

    fun updateKcalAdj(kcal: Double) {
        viewModelScope.launch {
            settingsRepo.setKcalAdjustment(kcal.coerceIn(-3000.0, 3000.0))
            NutriTrackerWidgetUpdater.enqueueUpdate(app)
        }
    }

    fun updateMacros(carb: Double, fat: Double, protein: Double) {
        viewModelScope.launch {
            settingsRepo.setCarbPct(carb)
            settingsRepo.setFatPct(fat)
            settingsRepo.setProteinPct(protein)
        }
    }

    fun updateWaterGoal(ml: Int) {
        viewModelScope.launch {
            settingsRepo.setWaterGoalMl(ml.coerceIn(100, 20000))
            NutriTrackerWidgetUpdater.enqueueUpdate(app)
        }
    }

    fun updateWaterQuickAmounts(amounts: List<Int>) {
        viewModelScope.launch { settingsRepo.setWaterQuickAmounts(amounts) }
    }

    fun updateAiConfig(apiKey: String, baseUrl: String, model: String) {
        viewModelScope.launch {
            settingsRepo.setAiApiKey(apiKey)
            settingsRepo.setAiBaseUrl(baseUrl)
            settingsRepo.setAiModel(model)
        }
    }

    fun setAiAutoConfirm(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setAiAutoConfirm(enabled) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepo.setAppLanguage(language)
            val localizedCtx = LocaleHelper.localizedContext(app, language)
            NutriTrackerWidgetUpdater.enqueueUpdate(localizedCtx)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setDynamicColorEnabled(enabled) }
    }

    fun setUseKj(useKj: Boolean) {
        viewModelScope.launch {
            settingsRepo.setUseKj(useKj)
            NutriTrackerWidgetUpdater.enqueueUpdate(app)
        }
    }

    fun setUsesImperialUnits(imperial: Boolean) {
        viewModelScope.launch {
            val user = userRepo.getUser() ?: return@launch
            userRepo.upsert(user.copy(usesImperialUnits = imperial))
        }
    }

    fun setMealReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setMealReminderEnabled(enabled)
            syncWorkReminders()
        }
    }

    fun setBreakfastReminderMin(min: Int) {
        viewModelScope.launch {
            settingsRepo.setBreakfastReminderMin(min)
            syncWorkReminders()
        }
    }

    fun setLunchReminderMin(min: Int) {
        viewModelScope.launch {
            settingsRepo.setLunchReminderMin(min)
            syncWorkReminders()
        }
    }

    fun setDinnerReminderMin(min: Int) {
        viewModelScope.launch {
            settingsRepo.setDinnerReminderMin(min)
            syncWorkReminders()
        }
    }

    fun setWaterReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setWaterReminderEnabled(enabled)
            syncWorkReminders()
        }
    }

    fun setWaterReminderIntervalMin(min: Int) {
        viewModelScope.launch {
            settingsRepo.setWaterReminderIntervalMin(min)
            syncWorkReminders()
        }
    }

    private suspend fun syncWorkReminders() {
        ReminderScheduler.syncReminders(
            context = app,
            mealEnabled = settingsRepo.mealReminderEnabled.first(),
            breakfastMin = settingsRepo.breakfastReminderMin.first(),
            lunchMin = settingsRepo.lunchReminderMin.first(),
            dinnerMin = settingsRepo.dinnerReminderMin.first(),
            waterEnabled = settingsRepo.waterReminderEnabled.first(),
            waterIntervalMin = settingsRepo.waterReminderIntervalMin.first()
        )
    }

    private val _testState = MutableStateFlow(TestStatus.IDLE to "")
    val testState: StateFlow<Pair<TestStatus, String>> = _testState.asStateFlow()

    fun testAiConnection(apiKey: String, baseUrl: String, model: String) {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        val cleanKey = apiKey.trim()
        val cleanModel = model.trim()
        val isZh = LocaleHelper.isChinese(app)
        if (cleanKey.isBlank() || cleanUrl.isBlank() || cleanModel.isBlank()) {
            _testState.value = TestStatus.FAILURE to LocaleHelper.tr(
                isZh,
                "请填写完整的 API Key、Base URL 和模型名称",
                "Please fill in API Key, Base URL, and model name"
            )
            return
        }

        viewModelScope.launch {
            _testState.value = TestStatus.TESTING to LocaleHelper.tr(isZh, "正在测试连接...", "Testing connection...")
            try {
                val result = withContext(Dispatchers.IO) {
                    val prompt = LocaleHelper.tr(isZh, "回复OK两个字", "Reply with OK")
                    val requestBody = gson.toJson(
                        mapOf(
                            "model" to cleanModel,
                            "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
                            "max_tokens" to 10
                        )
                    )
                    val request = Request.Builder()
                        .url("$cleanUrl/chat/completions")
                        .addHeader("Authorization", "Bearer $cleanKey")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody.toRequestBody("application/json".toMediaType()))
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string() ?: ""
                        if (!response.isSuccessful) {
                            return@withContext TestStatus.FAILURE to "HTTP ${response.code}: ${response.message}"
                        }
                        val json = gson.fromJson(body, Map::class.java)
                        @Suppress("UNCHECKED_CAST")
                        val choices = json?.get("choices") as? List<Map<String, Any>>
                        val message = choices?.firstOrNull()?.get("message") as? Map<*, *>
                        val content = message?.get("content") as? String
                        if (content == null) {
                            return@withContext TestStatus.FAILURE to LocaleHelper.tr(
                                isZh,
                                "API 返回格式异常: ${body.take(150)}",
                                "Unexpected API response: ${body.take(150)}"
                            )
                        }
                    }

                    val multimodalResult = testMultimodalCapability(cleanKey, cleanUrl, cleanModel, isZh)
                    if (multimodalResult.first) {
                        TestStatus.SUCCESS to LocaleHelper.tr(
                            isZh,
                            "连接成功！模型支持文本与多模态图片输入",
                            "Connected! Model supports text and image inputs."
                        )
                    } else {
                        TestStatus.SUCCESS to LocaleHelper.tr(
                            isZh,
                            "文本连接成功，但多模态测试未通过: ${multimodalResult.second}",
                            "Text OK, but image input check failed: ${multimodalResult.second}"
                        )
                    }
                }
                _testState.value = result
            } catch (e: Exception) {
                _testState.value = TestStatus.FAILURE to LocaleHelper.tr(
                    isZh,
                    "连接失败: ${e.message}",
                    "Connection failed: ${e.message}"
                )
            }
        }
    }

    private fun testMultimodalCapability(
        apiKey: String,
        cleanUrl: String,
        model: String,
        isZh: Boolean
    ): Pair<Boolean, String> = try {
        val testImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        val prompt = LocaleHelper.tr(isZh, "回复OK两个字", "Reply with OK")
        val requestBody = gson.toJson(
            mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to listOf(
                            mapOf("type" to "text", "text" to prompt),
                            mapOf(
                                "type" to "image_url",
                                "image_url" to mapOf(
                                    "url" to "data:image/png;base64,$testImageBase64",
                                    "detail" to "low"
                                )
                            )
                        )
                    )
                ),
                "max_tokens" to 10
            )
        )
        val request = Request.Builder()
            .url("$cleanUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) return false to "HTTP ${response.code}"
            val json = gson.fromJson(body, Map::class.java)
            @Suppress("UNCHECKED_CAST")
            val choices = json?.get("choices") as? List<Map<String, Any>>
            val message = choices?.firstOrNull()?.get("message") as? Map<*, *>
            if (message?.get("content") != null) {
                true to LocaleHelper.tr(isZh, "通过", "Passed")
            } else {
                false to LocaleHelper.tr(isZh, "返回格式异常", "Invalid format")
            }
        }
    } catch (e: Exception) {
        false to (e.message ?: LocaleHelper.tr(isZh, "未知错误", "Unknown error"))
    }

    fun resetTestState() {
        _testState.value = TestStatus.IDLE to ""
    }

    private val _exportStatus = MutableStateFlow(ExportImportStatus.IDLE to "")
    val exportStatus: StateFlow<Pair<ExportImportStatus, String>> = _exportStatus.asStateFlow()

    private val _importStatus = MutableStateFlow(ExportImportStatus.IDLE to "")
    val importStatus: StateFlow<Pair<ExportImportStatus, String>> = _importStatus.asStateFlow()

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            val isZh = LocaleHelper.isChinese(app)
            _exportStatus.value = ExportImportStatus.RUNNING to LocaleHelper.tr(isZh, "正在导出...", "Exporting...")
            val result = exportManager.exportData(uri)
            val nowZh = LocaleHelper.isChinese(app)
            _exportStatus.value = if (result.isSuccess) {
                ExportImportStatus.SUCCESS to LocaleHelper.tr(nowZh, "导出成功", "Export succeeded")
            } else {
                ExportImportStatus.FAILURE to LocaleHelper.tr(
                    nowZh,
                    "导出失败: ${result.exceptionOrNull()?.message}",
                    "Export failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            val isZh = LocaleHelper.isChinese(app)
            _importStatus.value = ExportImportStatus.RUNNING to LocaleHelper.tr(isZh, "正在导入...", "Importing...")
            val result = exportManager.importData(uri)
            val nowZh = LocaleHelper.isChinese(app)
            _importStatus.value = if (result.isSuccess) {
                NutriTrackerWidgetUpdater.enqueueUpdate(app)
                val r = result.getOrNull()!!
                val msg = buildString {
                    if (nowZh) {
                        append("导入完成: 食物 ${r.mealsImported} 条")
                        if (r.mealsSkipped > 0) append("(复用 ${r.mealsSkipped})")
                        append(", 饮食 ${r.intakesImported} 条, 活动 ${r.activitiesImported} 条")
                        if (r.recipesImported > 0) append(", 食谱 ${r.recipesImported} 个")
                        if (r.weightLogsImported > 0) append(", 体重 ${r.weightLogsImported} 条")
                        if (r.waterIntakesImported > 0) append(", 饮水 ${r.waterIntakesImported} 条")
                        if (r.imagesImported > 0) append(", 图片 ${r.imagesImported} 张")
                    } else {
                        append("Imported: ${r.mealsImported} foods")
                        if (r.mealsSkipped > 0) append(" (${r.mealsSkipped} reused)")
                        append(", ${r.intakesImported} meals, ${r.activitiesImported} activities")
                        if (r.recipesImported > 0) append(", ${r.recipesImported} recipes")
                        if (r.weightLogsImported > 0) append(", ${r.weightLogsImported} weights")
                        if (r.waterIntakesImported > 0) append(", ${r.waterIntakesImported} water logs")
                        if (r.imagesImported > 0) append(", ${r.imagesImported} photos")
                    }
                }
                ExportImportStatus.SUCCESS to msg
            } else {
                ExportImportStatus.FAILURE to LocaleHelper.tr(
                    nowZh,
                    "导入失败: ${result.exceptionOrNull()?.message}",
                    "Import failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
}
