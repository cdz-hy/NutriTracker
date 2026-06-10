package com.example.nutritracker.feature.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 分析结果，包含营养数据和缩略图路径
 */
data class AnalysisResult(
    val nutritionResult: NutritionResult,
    val thumbnailPath: String?
) : java.io.Serializable

/**
 * AI 识别的食物营养结果
 */
data class NutritionResult(
    @SerializedName("total_calories") val totalCalories: Double = 0.0,
    @SerializedName("total_protein") val totalProtein: Double = 0.0,
    @SerializedName("total_fat") val totalFat: Double = 0.0,
    @SerializedName("total_carbs") val totalCarbs: Double = 0.0,
    @SerializedName("food_items") val foodItems: List<FoodItem> = emptyList()
) : java.io.Serializable {
    data class FoodItem(
        @SerializedName("name") val name: String = "",
        @SerializedName("weight_g") val weightG: Double = 0.0,
        @SerializedName("calories") val calories: Double = 0.0,
        @SerializedName("protein") val protein: Double = 0.0,
        @SerializedName("fat") val fat: Double = 0.0,
        @SerializedName("carbs") val carbs: Double = 0.0
    ) : java.io.Serializable
}

class AiFoodAnalyzer(
    private val apiKey: String,
    private val baseUrl: String,
    private val model: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /**
     * 分析图片中的食物，返回营养信息
     */
    suspend fun analyzeImage(context: Context, imageUri: Uri): Result<NutritionResult> =
        withContext(Dispatchers.IO) {
            try {
                val base64 = uriToBase64(context, imageUri)
                val requestBody = buildRequestJson(base64)
                val request = Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("API error ${response.code}: ${response.message}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val result = parseResponse(body)
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * 保存低分辨率缩略图用于追溯显示
     * @return 保存的文件路径
     */
    fun saveThumbnail(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // 缩放到最大 200px
            val maxDim = 200
            val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap

            // 保存到 app 内部存储
            val dir = File(context.filesDir, "meal_thumbnails")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(file)
            resized.compress(Bitmap.CompressFormat.JPEG, 60, fos)
            fos.flush()
            fos.close()
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun uriToBase64(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        // 缩放到最大 1024px 以减少 token 使用
        val maxDim = 1024
        val scale = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildRequestJson(base64Image: String): String {
        // 专业营养师提示词，针对中餐/复合食物优化
        val prompt = """# Role
你是一个精通全球饮食、尤其是中国本土菜系（外卖、家常菜、地方小吃）的资深营养师与多模态计算机视觉专家。

# Task
请仔细分析我上传的食物图片，识别其中的所有食物组件，估算其重量（克/g），并计算其热量及三大宏量营养素（碳水化合物、蛋白质、脂肪）。

# Execution Rules (思考链路)
1. 视觉剥离：仔细观察图片，将复合菜品（如番茄炒蛋、青椒炒肉）拆解为主要原料（如鸡蛋、番茄、油、猪肉、青椒）。
2. 本土化估计：结合中国普通盘子/碗的尺寸，合理估算食物的克数（例如：一拳头大小的米饭约为150g，一盘家常炒菜总重约200-300g）。
3. 隐性成分考量：必须考虑烹饪用油（如炒菜默认加入10-15g植物油）和调味品带来的隐性热量。

# Output Format
为了方便我的程序解析，你必须严格按照以下 JSON 格式返回数据。
注意：
1. 只能返回标准的 JSON 数据，绝对不能包含任何 Markdown 标记（例如不要用 ```json 包裹）。
2. JSON 内部不要包含任何注释（不要写 // 或 /* */）。
3. 绝对不要返回任何前言、后记、问候语或解释性文本。

{
  "total_calories": 0,
  "total_protein": 0.0,
  "total_fat": 0.0,
  "total_carbs": 0.0,
  "food_items": [
    {
      "name": "食物或原料名称",
      "weight_g": 0,
      "calories": 0,
      "protein": 0.0,
      "fat": 0.0,
      "carbs": 0.0
    }
  ]
}"""

        return gson.toJson(mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf("type" to "text", "text" to prompt),
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf(
                                "url" to "data:image/jpeg;base64,$base64Image",
                                "detail" to "low"
                            )
                        )
                    )
                )
            ),
            "max_tokens" to 1500,
            "temperature" to 0.1
        ))
    }

    private fun parseResponse(json: String): NutritionResult {
        val root = gson.fromJson(json, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        val choices = root["choices"] as? List<Map<String, Any>> ?: throw Exception("No choices")
        val message = choices.first()["message"] as? Map<*, *> ?: throw Exception("No message")
        val content = message["content"] as? String ?: throw Exception("No content")
        // 清理 markdown 代码块标记
        val cleaned = content.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        return gson.fromJson(cleaned, NutritionResult::class.java)
    }
}
