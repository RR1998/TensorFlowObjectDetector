package com.example.TensorFlowObjectDetector.tensorchat

interface ChatCloudRepository {
    suspend fun sendPrompt(prompt: String, imageUri: String? = null): String
}
