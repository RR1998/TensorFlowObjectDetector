package com.example.TensorFlowObjectDetector.di

import com.example.TensorFlowObjectDetector.tensorchat.SendPromptUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatEntryPoint {
    fun sendPromptUseCase(): SendPromptUseCase
}
