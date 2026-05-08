package com.example.TensorFlowObjectDetector.data.chat

data class ChatBackendResponse(
    val answer: String,
    val conversationId: String? = null
)
