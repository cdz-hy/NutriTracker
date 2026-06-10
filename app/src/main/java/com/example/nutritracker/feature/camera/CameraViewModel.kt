package com.example.nutritracker.feature.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutritracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    suspend fun analyzeImage(context: Context, uri: Uri): Result<NutritionResult> {
        val apiKey = settingsRepo.aiApiKey.first()
        val baseUrl = settingsRepo.aiBaseUrl.first()
        val model = settingsRepo.aiModel.first()
        if (apiKey.isBlank()) return Result.failure(Exception("请先在设置中配置 AI API Key"))
        val analyzer = AiFoodAnalyzer(apiKey, baseUrl, model)
        return analyzer.analyzeImage(context, uri)
    }
}
