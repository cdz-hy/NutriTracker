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
import java.util.concurrent.TimeUnit

data class NutritionResult(
    @SerializedName("name") val name: String = "",
    @SerializedName("brands") val brands: String? = null,
    @SerializedName("estimated_weight_g") val estimatedWeightG: Double = 100.0,
    @SerializedName("energy_kcal_per_100g") val energyKcal100: Double = 0.0,
    @SerializedName("carbohydrates_per_100g") val carbohydrates100: Double = 0.0,
    @SerializedName("fat_per_100g") val fat100: Double = 0.0,
    @SerializedName("proteins_per_100g") val proteins100: Double = 0.0,
    @SerializedName("sugars_per_100g") val sugars100: Double? = null,
    @SerializedName("saturated_fat_per_100g") val saturatedFat100: Double? = null,
    @SerializedName("fiber_per_100g") val fiber100: Double? = null,
    @SerializedName("sodium_per_100g") val sodium100: Double? = null
) : java.io.Serializable

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

    suspend fun analyzeImage(context: Context, imageUri: Uri): Result<NutritionResult> =
        withContext(Dispatchers.IO) {
            try {
                val base64 = uriToBase64(context, imageUri)
                val requestBody = buildRequestJson(base64)
                val request = Request.Builder()
                    .url("$baseUrl/chat/completions")
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

    private fun uriToBase64(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open image")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        // Resize to reduce token usage
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
        val prompt = """分析图片中的食物，返回严格JSON格式（不要markdown代码块标记）：
{
  "name": "食物名称",
  "brands": "品牌（如有）",
  "estimated_weight_g": 估计重量克数,
  "energy_kcal_per_100g": 每100g千卡,
  "carbohydrates_per_100g": 每100g碳水化合物克数,
  "fat_per_100g": 每100g脂肪克数,
  "proteins_per_100g": 每100g蛋白质克数,
  "sugars_per_100g": 每100g糖克数,
  "saturated_fat_per_100g": 每100g饱和脂肪克数,
  "fiber_per_100g": 每100g膳食纤维克数,
  "sodium_per_100g": 每100g钠毫克数
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
            "max_tokens" to 800,
            "temperature" to 0.1
        ))
    }

    private fun parseResponse(json: String): NutritionResult {
        val root = gson.fromJson(json, Map::class.java)
        @Suppress("UNCHECKED_CAST")
        val choices = root["choices"] as? List<Map<String, Any>> ?: throw Exception("No choices")
        val message = choices.first()["message"] as? Map<*, *> ?: throw Exception("No message")
        val content = message["content"] as? String ?: throw Exception("No content")
        // Strip markdown code fences if present
        val cleaned = content.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        return gson.fromJson(cleaned, NutritionResult::class.java)
    }
}
