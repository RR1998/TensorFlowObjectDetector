package com.example.TensorFlowObjectDetector.tensorchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class TensorChatViewModelFactory(
    private val sendPromptUseCase: SendPromptUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TensorChatViewModel::class.java)) {
            return TensorChatViewModel(sendPromptUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
