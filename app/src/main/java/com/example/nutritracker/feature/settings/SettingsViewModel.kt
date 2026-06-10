package com.example.nutritracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.repository.SettingsRepository
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class TestStatus { IDLE, TESTING, SUCCESS, FAILURE }

data class SettingsState(
    val dayBoundaryStr: String = "0",
    val kcalAdjStr: String = "0",
    val carbPct: Double = 0.60,
    val fatPct: Double = 0.25,
    val proteinPct: Double = 0.15,
    val waterGoalStr: String = "2000",
    val aiApiKey: String = "",
    val aiBaseUrl: String = "",
    val aiModel: String = "",
    val testStatus: TestStatus = TestStatus.IDLE,
    val testMessage: String = ""
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

    private val _testState = MutableStateFlow(TestStatus.IDLE to "")
    val testState: StateFlow<Pair<TestStatus, String>> = _testState.asStateFlow()

    fun testAiConnection(apiKey: String, baseUrl: String, model: String) {
        if (apiKey.isBlank() || baseUrl.isBlank() || model.isBlank()) {
            _testState.value = TestStatus.FAILURE to "请填写完整的 API Key、Base URL 和模型名称"
            return
        }

        viewModelScope.launch {
            _testState.value = TestStatus.TESTING to "正在测试连接..."

            try {
                val result = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val requestBody = Gson().toJson(mapOf(
                        "model" to model,
                        "messages" to listOf(
                            mapOf("role" to "user", "content" to "回复OK两个字")
                        ),
                        "max_tokens" to 10
                    ))

                    val request = Request.Builder()
                        .url("${baseUrl.trimEnd('/')}/chat/completions")
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody.toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val json = Gson().fromJson(body, Map::class.java)
                        @Suppress("UNCHECKED_CAST")
                        val choices = json["choices"] as? List<Map<String, Any>>
                        val message = choices?.firstOrNull()?.get("message") as? Map<*, *>
                        val content = message?.get("content") as? String

                        if (content != null) {
                            // 测试多模态能力
                            val multimodalResult = testMultimodalCapability(client, apiKey, baseUrl, model)
                            if (multimodalResult.first) {
                                TestStatus.SUCCESS to "连接成功！模型支持多模态输入"
                            } else {
                                TestStatus.SUCCESS to "连接成功，但多模态测试失败: ${multimodalResult.second}"
                            }
                        } else {
                            TestStatus.FAILURE to "API 返回格式异常: $body"
                        }
                    } else {
                        TestStatus.FAILURE to "HTTP ${response.code}: ${response.message}"
                    }
                }

                _testState.value = result
            } catch (e: Exception) {
                _testState.value = TestStatus.FAILURE to "连接失败: ${e.message}"
            }
        }
    }

    private suspend fun testMultimodalCapability(
        client: OkHttpClient,
        apiKey: String,
        baseUrl: String,
        model: String
    ): Pair<Boolean, String> {
        return try {
            // 创建一个最小的测试图片 (1x1 像素的 PNG)
            val testImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="

            val requestBody = Gson().toJson(mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to listOf(
                            mapOf("type" to "text", "text" to "回复OK两个字"),
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
            ))

            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = Gson().fromJson(body, Map::class.java)
                @Suppress("UNCHECKED_CAST")
                val choices = json["choices"] as? List<Map<String, Any>>
                val message = choices?.firstOrNull()?.get("message") as? Map<*, *>
                val content = message?.get("content") as? String

                if (content != null) {
                    Pair(true, "多模态测试通过")
                } else {
                    Pair(false, "返回格式异常")
                }
            } else {
                Pair(false, "HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "未知错误")
        }
    }

    fun resetTestState() {
        _testState.value = TestStatus.IDLE to ""
    }
}
