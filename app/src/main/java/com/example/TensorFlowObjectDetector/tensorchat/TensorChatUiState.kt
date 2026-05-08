package com.example.TensorFlowObjectDetector.tensorchat

data class TensorChatUiState(
    val chatContext: ChatContext? = null,
    val hasSentInitialContextPrompt: Boolean = false,
    val selectedProvider: ChatProviderOption = ChatProviderOption.GEMINI_DIRECT,
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

enum class ChatProviderOption(val label: String) {
    GEMINI_DIRECT("Gemini Direct"),
    GEMINI_CLOUD("Gemini Cloud")
}
