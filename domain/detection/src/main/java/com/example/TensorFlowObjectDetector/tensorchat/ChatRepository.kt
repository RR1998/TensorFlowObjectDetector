package com.example.TensorFlowObjectDetector.tensorchat

interface ChatRepository {
    suspend fun sendPrompt(prompt: String, imageUri: String? = null): String
}
