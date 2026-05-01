package com.example.TensorFlowObjectDetector.tensorchat

data class ChatContext(
    val currentResult: RecognitionResult,
    val extraMetadata: Map<String, String> = emptyMap()
)
