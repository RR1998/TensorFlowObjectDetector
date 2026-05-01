package com.example.TensorFlowObjectDetector.tensorchat

class SendPromptUseCase(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(prompt: String, imageUri: String? = null): String {
        return chatRepository.sendPrompt(prompt, imageUri)
    }
}
