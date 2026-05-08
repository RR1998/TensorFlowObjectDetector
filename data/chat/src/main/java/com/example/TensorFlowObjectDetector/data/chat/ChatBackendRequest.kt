package com.example.TensorFlowObjectDetector.data.chat

data class ChatBackendRequest(
    val message: String,
    val image: ChatBackendImage? = null,
    val conversationId: String? = null
)
