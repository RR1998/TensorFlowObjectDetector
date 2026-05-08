package com.example.TensorFlowObjectDetector.di

import android.app.Application
import com.example.TensorFlowObjectDetector.data.chat.BuildConfig
import com.example.TensorFlowObjectDetector.data.chat.CloudRunChatApiService
import com.example.TensorFlowObjectDetector.data.chat.CloudRunChatRepository
import com.example.TensorFlowObjectDetector.data.chat.CloudRunRetrofitClient
import com.example.TensorFlowObjectDetector.data.chat.DirectGeminiChatRepository
import com.example.TensorFlowObjectDetector.data.chat.GeminiApiService
import com.example.TensorFlowObjectDetector.data.chat.GeminiRetrofitClient
import com.example.TensorFlowObjectDetector.tensorchat.ChatCloudRepository
import com.example.TensorFlowObjectDetector.tensorchat.ChatRepository
import com.example.TensorFlowObjectDetector.tensorchat.ChatUseCaseFactory
import com.example.TensorFlowObjectDetector.tensorchat.SendPromptCloudUseCase
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
    fun provideCloudRunChatApiService(): CloudRunChatApiService = CloudRunRetrofitClient.api

    @Provides
    @Singleton
    fun provideChatCloudRepository(
        application: Application,
        api: CloudRunChatApiService
    ): ChatCloudRepository {
        return CloudRunChatRepository(
            context = application,
            api = api
        )
    }

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

    @Provides
    fun provideSendPromptCloudUseCase(chatCloudRepository: ChatCloudRepository): SendPromptCloudUseCase {
        return ChatUseCaseFactory.createSendPromptCloudUseCase(chatCloudRepository)
    }
}
