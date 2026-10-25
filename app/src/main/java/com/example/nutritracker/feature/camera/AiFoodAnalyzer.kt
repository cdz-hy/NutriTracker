package com.example.nutritracker.feature.camera

import android.content.Context
import android.net.Uri
import com.example.nutritracker.util.ImageUtils
import com.example.nutritracker.util.LocaleHelper
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class AnalysisResult(
    val nutritionResult: NutritionResult,
    val thumbnailPath: String?
) : java.io.Serializable

data class NutritionResult(
    @SerializedName("meal_name") val mealName: String = "",
    @SerializedName("total_calories") val totalCalories: Double = 0.0,
    @SerializedName("total_protein") val totalProtein: Double = 0.0,
    @SerializedName("total_fat") val totalFat: Double = 0.0,
    @SerializedName("total_carbs") val totalCarbs: Double = 0.0,
    @SerializedName("total_fiber_g") val totalFiberG: Double? = null,
    @SerializedName("total_sugars_g") val totalSugarsG: Double? = null,
    @SerializedName("total_saturated_fat_g") val totalSaturatedFatG: Double? = null,
    @SerializedName("total_sodium_mg") val totalSodiumMg: Double? = null,
    @SerializedName("total_cholesterol_mg") val totalCholesterolMg: Double? = null,
    @SerializedName("food_items") val foodItems: List<FoodItem> = emptyList()
) : java.io.Serializable {
    data class FoodItem(
        @SerializedName("name") val name: String = "",
        @SerializedName("weight_g") val weightG: Double = 0.0,
        @SerializedName("calories") val calories: Double = 0.0,
        @SerializedName("protein") val protein: Double = 0.0,
        @SerializedName("fat") val fat: Double = 0.0,
        @SerializedName("carbs") val carbs: Double = 0.0,
        @SerializedName("fiber_g") val fiberG: Double? = null,
        @SerializedName("sugars_g") val sugarsG: Double? = null,
        @SerializedName("saturated_fat_g") val saturatedFatG: Double? = null,
        @SerializedName("sodium_mg") val sodiumMg: Double? = null,
        @SerializedName("cholesterol_mg") val cholesterolMg: Double? = null
    ) : java.io.Serializable
}

class AiFoodAnalyzer(
    private val client: OkHttpClient,
    private val gson: Gson,
    private val apiKey: String,
    private val baseUrl: String,
    private val model: String,
    private val isZh: Boolean = LocaleHelper.isCurrentLocaleChinese()
) {

    suspend fun analyzeImages(
        context: Context,
        imageUris: List<Uri>,
        notes: String = ""
    ): Result<NutritionResult> = withContext(Dispatchers.IO) {
        try {
            if (imageUris.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException(LocaleHelper.tr(isZh, "请至少提供一张图片！", "Please provide at least one image."))
                )
            }
            val base64List = imageUris.map { ImageUtils.uriToCompressedBase64(context, it) }
            val requestBody = buildImageRequestJson(base64List, notes)
            executeChatRequest(requestBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeText(description: String): Result<NutritionResult> = withContext(Dispatchers.IO) {
        try {
            if (description.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException(LocaleHelper.tr(isZh, "请输入食物描述！", "Please enter a meal description."))
                )
            }
            val requestBody = buildTextRequestJson(description.trim())
            executeChatRequest(requestBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeChatRequest(requestBody: String): Result<NutritionResult> {
        val cleanBaseUrl = baseUrl.trim().trimEnd('/')
        if (cleanBaseUrl.isBlank() || model.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    LocaleHelper.tr(
                        isZh,
                        "请先在设置中配置完整的 Base URL 和模型名称",
                        "Please configure Base URL and model name in Settings first."
                    )
                )
            )
        }

        val request = Request.Builder()
            .url("$cleanBaseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) {
                val detail = body?.take(180)?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
                return Result.failure(
                    Exception(
                        LocaleHelper.tr(
                            isZh,
                            "API 服务返回错误 (${response.code})$detail",
                            "API returned error (${response.code})$detail"
                        )
                    )
                )
            }
            if (body.isNullOrBlank()) {
                return Result.failure(
                    Exception(LocaleHelper.tr(isZh, "AI 没有返回内容，请重试！", "AI returned empty response, please retry."))
                )
            }

            val result = try {
                parseResponse(body)
            } catch (_: JsonSyntaxException) {
                return Result.failure(
                    Exception(
                        LocaleHelper.tr(
                            isZh,
                            "AI 响应的 JSON 数据格式解析失败，请重试！",
                            "Failed to parse AI JSON response, please retry."
                        )
                    )
                )
            } catch (e: Exception) {
                return Result.failure(
                    Exception(e.message ?: LocaleHelper.tr(isZh, "分析结果解析失败", "Failed to parse analysis result"))
                )
            }

            if (result.totalCalories <= 0.0 && result.foodItems.isEmpty()) {
                return Result.failure(
                    Exception(
                        LocaleHelper.tr(
                            isZh,
                            "未检测到明显的食物或有效热量信息，请补充描述或重新拍摄！",
                            "No recognizable food or valid calorie data detected. Try adding notes or retaking the photo."
                        )
                    )
                )
            }

            return Result.success(result)
        }
    }

    private fun buildImageRequestJson(base64Images: List<String>, notes: String): String {
        val prompt = if (isZh) {
            val notesBlock = if (notes.isNotBlank()) "\n# 用户补充说明\n$notes\n" else ""
            """# Role
你是一个精通全球饮食、尤其是中国本土菜系（外卖、家常菜、地方小吃）的资深营养师与多模态计算机视觉专家。

# Task
我上传了多张同一次餐食的图片。请仔细分析这些图片，将它们视为同一次进食，识别其中的所有食物组件，估算其整体重量（克/g），并计算总热量、三大宏量营养素以及关键膳食微量指标。
$notesBlock
# Execution Rules
1. 跨图整合：综合所有图片内容，不要重复计算相同食物；不同食物合并计算。
2. 视觉剥离：将复合菜品（如番茄炒蛋、青椒炒肉）拆解为主要原料（如鸡蛋、番茄、烹饪油、肉类等）。
3. 本土化估计：结合中国普通盘子/碗的尺寸，合理估算克数（例如：一拳头米饭约150g，一份家常炒菜约200-300g）。
4. 隐性成分考量：估算烹饪用油和调味品（包括钠含量，如食盐、酱油）。

${outputSchemaPrompt()}"""
        } else {
            val notesBlock = if (notes.isNotBlank()) "\n# User Notes\n$notes\n" else ""
            """# Role
You are a senior registered dietitian and multimodal computer vision expert skilled in global cuisines, takeout, and home-cooked dishes.

# Task
I uploaded one or more photos of the same meal. Analyze all photos together as a single meal, identify all food components, estimate weights in grams (g), and compute total calories, macronutrients, and key micronutrients.
$notesBlock
# Execution Rules
1. Cross-image consolidation: Combine context across all images without double-counting the same dish across different angles.
2. Ingredient decomposition: Break composite dishes down into primary ingredients (e.g., protein, vegetables, staple carbs, cooking oil).
3. Realistic portion sizing: Infer grams using visual plate/bowl cues.
4. Hidden ingredients: Account for cooking oils, sauces, and sodium.

${outputSchemaPrompt()}"""
        }

        val contentList = mutableListOf<Map<String, Any>>()
        contentList.add(mapOf("type" to "text", "text" to prompt))
        base64Images.forEach { base64 ->
            contentList.add(
                mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf(
                        "url" to "data:image/jpeg;base64,$base64",
                        "detail" to "auto"
                    )
                )
            )
        }

        return gson.toJson(
            mapOf(
                "model" to model,
                "messages" to listOf(mapOf("role" to "user", "content" to contentList)),
                "max_tokens" to 8192,
                "temperature" to 0.1
            )
        )
    }

    private fun buildTextRequestJson(description: String): String {
        val prompt = if (isZh) {
            """# Role
你是一个精通中国本土饮食、外卖规格与营养成分估算的资深营养师。

# Task
用户用自然语言描述了一顿饭或食物组合：
"$description"

请解析其中包含的每项食物及合理份量（克/g），计算总热量、三大宏量营养素及关键微量营养素。若用户未写明份量，请按中国常见单人份标准合理估算（如一碗面约350-450g含面与浇头、一个鸡蛋约50g、一罐饮料330ml等）。

${outputSchemaPrompt()}"""
        } else {
            """# Role
You are an experienced nutritionist skilled at estimating portions and nutrients from natural language descriptions of meals.

# Task
The user described a meal or food combo:
"$description"

Parse each food component with realistic weights in grams (g), and compute total calories, macronutrients, and key micronutrients. When portion sizes are unspecified, infer standard single-serving weights.

${outputSchemaPrompt()}"""
        }

        return gson.toJson(
            mapOf(
                "model" to model,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
                "max_tokens" to 4096,
                "temperature" to 0.1
            )
        )
    }

    private fun outputSchemaPrompt(): String = if (isZh) {
        """# Output Format
只能返回标准 JSON（禁止 Markdown ```json 包裹、禁止注释、禁止任何前后说明文本）。
meal_name 应为自然概括中文名称，如"牛肉面加卤蛋"、"番茄炒蛋配米饭"。
单位说明：weight_g/protein/fat/carbs/fiber_g/sugars_g/saturated_fat_g 为克(g)，sodium_mg/cholesterol_mg 为毫克(mg)，calories 为千卡(kcal)。

{
  "meal_name": "概括性饮食名称",
  "total_calories": 0,
  "total_protein": 0.0,
  "total_fat": 0.0,
  "total_carbs": 0.0,
  "total_fiber_g": 0.0,
  "total_sugars_g": 0.0,
  "total_saturated_fat_g": 0.0,
  "total_sodium_mg": 0.0,
  "total_cholesterol_mg": 0.0,
  "food_items": [
    {
      "name": "食物或原料名称",
      "weight_g": 0,
      "calories": 0,
      "protein": 0.0,
      "fat": 0.0,
      "carbs": 0.0,
      "fiber_g": 0.0,
      "sugars_g": 0.0,
      "saturated_fat_g": 0.0,
      "sodium_mg": 0.0,
      "cholesterol_mg": 0.0
    }
  ]
}"""
    } else {
        """# Output Format
Return ONLY valid JSON (no Markdown ```json fences, no comments, no surrounding prose).
Return meal_name and every food_items[*].name in natural English (e.g. "Beef Noodle Soup with Egg", "Grilled Chicken Salad").
Units: weight_g/protein/fat/carbs/fiber_g/sugars_g/saturated_fat_g in grams (g); sodium_mg/cholesterol_mg in mg; calories in kcal.

{
  "meal_name": "Summary meal name in English",
  "total_calories": 0,
  "total_protein": 0.0,
  "total_fat": 0.0,
  "total_carbs": 0.0,
  "total_fiber_g": 0.0,
  "total_sugars_g": 0.0,
  "total_saturated_fat_g": 0.0,
  "total_sodium_mg": 0.0,
  "total_cholesterol_mg": 0.0,
  "food_items": [
    {
      "name": "Food or ingredient name in English",
      "weight_g": 0,
      "calories": 0,
      "protein": 0.0,
      "fat": 0.0,
      "carbs": 0.0,
      "fiber_g": 0.0,
      "sugars_g": 0.0,
      "saturated_fat_g": 0.0,
      "sodium_mg": 0.0,
      "cholesterol_mg": 0.0
    }
  ]
}"""
    }

    internal fun parseResponse(json: String): NutritionResult {
        val root = try {
            gson.fromJson(json, Map::class.java)
        } catch (_: Exception) {
            throw Exception(
                LocaleHelper.tr(
                    isZh,
                    "AI 返回的不是有效的 JSON，原始响应(${json.take(200)}...)",
                    "AI did not return valid JSON: ${json.take(200)}..."
                )
            )
        } ?: throw Exception(LocaleHelper.tr(isZh, "AI 响应为空", "AI response is empty"))

        val errorMap = root["error"] as? Map<*, *>
        if (errorMap != null) {
            val errorMsg = errorMap["message"] as? String ?: LocaleHelper.tr(isZh, "未知错误", "Unknown error")
            throw Exception(LocaleHelper.tr(isZh, "API 错误: $errorMsg", "API error: $errorMsg"))
        }

        @Suppress("UNCHECKED_CAST")
        val choices = root["choices"] as? List<Map<String, Any>>
            ?: throw Exception(LocaleHelper.tr(isZh, "AI 响应中没有 choices 字段", "Missing 'choices' field in AI response"))

        if (choices.isEmpty()) {
            throw Exception(
                LocaleHelper.tr(isZh, "AI 响应 choices 为空，可能模型不支持当前请求", "AI returned empty 'choices'")
            )
        }

        val choiceFirst = choices.first()
        val message = choiceFirst["message"] as? Map<*, *>
            ?: throw Exception(LocaleHelper.tr(isZh, "AI 响应中缺少 message 字段", "Missing 'message' field in AI response"))

        val contentObj = message["content"]
        val content: String = when (contentObj) {
            null -> {
                (message["refusal"] as? String)?.let {
                    throw Exception(LocaleHelper.tr(isZh, "AI 拒绝回答: $it", "AI refused: $it"))
                }
                throw Exception(
                    LocaleHelper.tr(isZh, "AI 响应中既无 content 也无 refusal 字段", "AI response has neither content nor refusal")
                )
            }
            is String -> contentObj
            is List<*> -> {
                val textPart = contentObj.find { it is Map<*, *> && it["type"] == "text" }
                (textPart as? Map<*, *>)?.get("text") as? String ?: contentObj.toString()
            }
            else -> contentObj.toString()
        }

        if (content.isBlank()) {
            val finishReason = choiceFirst["finish_reason"] as? String ?: "unknown"
            throw Exception(
                LocaleHelper.tr(
                    isZh,
                    "AI 返回了空内容 (finish_reason=$finishReason)",
                    "AI returned empty content (finish_reason=$finishReason)"
                )
            )
        }

        val cleaned = extractJsonFromContent(content)
        if (cleaned.isBlank()) {
            throw Exception(LocaleHelper.tr(isZh, "AI 返回的内容中没有 JSON 数据", "No JSON data found in AI response"))
        }

        return gson.fromJson(cleaned, NutritionResult::class.java)
            ?: throw Exception(LocaleHelper.tr(isZh, "解析结果为空", "Parsed result is empty"))
    }

    companion object {
        private val THINK_TAG_REGEX = Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE)

        /**
         * Strips reasoning `<think>` tags, markdown code fences, and surrounding prose to extract raw JSON.
         */
        fun extractJsonFromContent(content: String): String {
            var text = THINK_TAG_REGEX.replace(content, "").trim()

            while (text.startsWith("```")) {
                val firstNewline = text.indexOf('\n')
                text = if (firstNewline >= 0) {
                    text.substring(firstNewline + 1).trim()
                } else {
                    text.removePrefix("```json").removePrefix("```JSON").removePrefix("```").trim()
                }
            }
            while (text.endsWith("```")) {
                text = text.removeSuffix("```").trim()
            }

            if (!text.startsWith("{") && !text.startsWith("[")) {
                val braceStart = text.indexOf('{')
                val bracketStart = text.indexOf('[')
                val start = when {
                    braceStart >= 0 && bracketStart >= 0 -> minOf(braceStart, bracketStart)
                    braceStart >= 0 -> braceStart
                    bracketStart >= 0 -> bracketStart
                    else -> -1
                }
                if (start >= 0) {
                    text = text.substring(start)
                }
            }

            if (!text.endsWith("}") && !text.endsWith("]")) {
                val braceEnd = text.lastIndexOf('}')
                val bracketEnd = text.lastIndexOf(']')
                val end = maxOf(braceEnd, bracketEnd)
                if (end >= 0) {
                    text = text.substring(0, end + 1)
                }
            }

            return text.trim()
        }
    }
}
