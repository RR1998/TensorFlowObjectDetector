package com.example.TensorFlowObjectDetector.tensorchat

class SendPromptCloudUseCase(
    private val chatCloudRepository: ChatCloudRepository
) {
    suspend operator fun invoke(prompt: String, imageUri: String? = null): String {
        return chatCloudRepository.sendPrompt(prompt, imageUri)
    }
}
