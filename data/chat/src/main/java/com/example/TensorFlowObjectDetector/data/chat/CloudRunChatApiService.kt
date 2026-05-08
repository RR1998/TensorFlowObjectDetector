package com.example.TensorFlowObjectDetector.data.chat

import retrofit2.http.Body
import retrofit2.http.POST

interface CloudRunChatApiService {

    @POST("chat")
    suspend fun sendMessage(
        @Body request: ChatBackendRequest
    ): ChatBackendResponse
}
