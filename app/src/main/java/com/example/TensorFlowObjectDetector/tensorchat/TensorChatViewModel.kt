package com.example.TensorFlowObjectDetector.tensorchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.IMAGE_URI_KEY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TensorChatViewModel @Inject constructor(
    private val sendPromptUseCase: SendPromptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TensorChatUiState())
    val uiState: StateFlow<TensorChatUiState> = _uiState.asStateFlow()

    fun setChatContext(context: ChatContext) {
        _uiState.update { state ->
            if (state.chatContext == context) {
                state
            } else {
                state.copy(
                    chatContext = context,
                    hasSentInitialContextPrompt = false
                )
            }
        }
    }

    fun onMessageInputChanged(value: String) {
        _uiState.update { it.copy(messageInput = value) }
    }

    fun buildPrompt(userMessage: String): String {
        val context = _uiState.value.chatContext ?: return userMessage
        return """
        You are an assistant inside an Android ML image recognition app.

        The local ML model detected:
        - Label: ${context.currentResult.label}
        - Confidence: ${context.currentResult.confidence}
        - Category: ${context.currentResult.category}
        - Extra metadata: ${context.extraMetadata}

        The user asks:
        "$userMessage"

        Answer clearly. If confidence is low, mention that the result may be
        uncertain and describe what you determine with more precision what do you see
    """.trimIndent()
    }

    fun sendInitialContextPromptIfNeeded() {
        val state = _uiState.value
        if (state.chatContext == null || state.hasSentInitialContextPrompt || state.isLoading) return
        sendMessageInternal(
            userMessage = "Please analyze this detection result and give me a short summary.",
            markInitialPromptAsSent = true
        )
    }

    fun sendMessage(userMessage: String) {
        sendMessageInternal(userMessage = userMessage)
    }

    private fun sendMessageInternal(
        userMessage: String,
        markInitialPromptAsSent: Boolean = false
    ) {
        val imageUri = _uiState.value.chatContext?.extraMetadata?.get(IMAGE_URI_KEY)
        if (_uiState.value.chatContext == null) return
        val trimmedMessage = userMessage.trim()
        if (trimmedMessage.isBlank()) return
        val prompt = buildPrompt(userMessage = trimmedMessage)

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage(
                        text = trimmedMessage,
                        type = ChatMessageType.SENT
                    ),
                    messageInput = "",
                    hasSentInitialContextPrompt = it.hasSentInitialContextPrompt || markInitialPromptAsSent,
                    isLoading = true,
                    errorMessage = null
                )
            }
            runCatching { sendPromptUseCase(prompt, imageUri) }
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage(
                                text = response,
                                type = ChatMessageType.RECEIVED
                            ),
                            isLoading = false,
                            lastResponse = response
                        )
                    }
                }
                .onFailure { throwable ->
                    val error = throwable.message ?: "Unable to send prompt."
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage(
                                text = error,
                                type = ChatMessageType.RECEIVED
                            ),
                            isLoading = false,
                            errorMessage = error
                        )
                    }
                }
        }
    }
}
