package com.example.TensorFlowObjectDetector.di

import android.app.Application
import com.example.TensorFlowObjectDetector.data.chat.BuildConfig
import com.example.TensorFlowObjectDetector.data.chat.DirectGeminiChatRepository
import com.example.TensorFlowObjectDetector.data.chat.GeminiApiService
import com.example.TensorFlowObjectDetector.data.chat.GeminiRetrofitClient
import com.example.TensorFlowObjectDetector.tensorchat.ChatRepository
import com.example.TensorFlowObjectDetector.tensorchat.ChatUseCaseFactory
import com.example.TensorFlowObjectDetector.tensorchat.SendPromptUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideGeminiApiService(): GeminiApiService = GeminiRetrofitClient.api

    @Provides
    @Singleton
    fun provideChatRepository(
        application: Application,
        api: GeminiApiService
    ): ChatRepository {
        return DirectGeminiChatRepository(
            context = application,
            api = api,
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    @Provides
    fun provideSendPromptUseCase(chatRepository: ChatRepository): SendPromptUseCase {
        return ChatUseCaseFactory.createSendPromptUseCase(chatRepository)
    }
}
