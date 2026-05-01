package com.example.TensorFlowObjectDetector.tensorchat

object ChatUseCaseFactory {
    fun createSendPromptUseCase(chatRepository: ChatRepository): SendPromptUseCase {
        return SendPromptUseCase(chatRepository)
    }
}
