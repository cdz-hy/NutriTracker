package com.example.nutritracker

import com.example.nutritracker.feature.camera.AiFoodAnalyzer
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

class AiFoodAnalyzerTest {

    private val analyzer = AiFoodAnalyzer(
        client = OkHttpClient(),
        gson = Gson(),
        apiKey = "test-key",
        baseUrl = "https://example.com/v1",
        model = "test-model"
    )

    @Test
    fun `extractJsonFromContent strips think tags markdown fences and prose`() {
        val raw = """
            <think>
            Analyzing image weights carefully...
            </think>
            Here is your JSON result:
            ```json
            {
              "meal_name": "番茄炒蛋盖饭",
              "total_calories": 580
            }
            ```
            Hope this helps!
        """.trimIndent()

        val extracted = AiFoodAnalyzer.extractJsonFromContent(raw)
        assertTrue(extracted.startsWith("{"))
        assertTrue(extracted.endsWith("}"))
        assertTrue(extracted.contains("番茄炒蛋盖饭"))
    }

    @Test
    fun `parseResponse parses macros micronutrients and food items`() {
        val apiJson = """
            {
              "choices": [
                {
                  "message": {
                    "content": "```json\n{\"meal_name\":\"牛肉面\",\"total_calories\":620,\"total_protein\":28.5,\"total_fat\":16.0,\"total_carbs\":88.0,\"total_fiber_g\":4.2,\"total_sodium_mg\":1450,\"food_items\":[{\"name\":\"拉面\",\"weight_g\":300,\"calories\":420,\"protein\":14,\"fat\":3,\"carbs\":80}]}\n```"
                  },
                  "finish_reason": "stop"
                }
              ]
            }
        """.trimIndent()

        val parsed = analyzer.parseResponse(apiJson)
        assertEquals("牛肉面", parsed.mealName)
        assertEquals(620.0, parsed.totalCalories, 0.01)
        assertEquals(4.2, parsed.totalFiberG ?: 0.0, 0.01)
        assertEquals(1450.0, parsed.totalSodiumMg ?: 0.0, 0.01)
        assertEquals(1, parsed.foodItems.size)
        assertEquals("拉面", parsed.foodItems[0].name)
    }

    @Test(expected = Exception::class)
    fun `parseResponse throws on API error payload`() {
        analyzer.parseResponse("""{"error":{"message":"Invalid API key"}}""")
    }
}
