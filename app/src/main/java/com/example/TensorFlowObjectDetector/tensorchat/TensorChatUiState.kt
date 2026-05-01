package com.example.TensorFlowObjectDetector.tensorchat

data class TensorChatUiState(
    val chatContext: ChatContext? = null,
    val hasSentInitialContextPrompt: Boolean = false,
    val messageInput: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val lastResponse: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ChatMessage(
    val text: String,
    val type: ChatMessageType
)

enum class ChatMessageType {
    SENT,
    RECEIVED
}
