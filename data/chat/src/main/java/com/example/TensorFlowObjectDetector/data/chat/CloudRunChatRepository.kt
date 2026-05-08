package com.example.TensorFlowObjectDetector.data.chat

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.TensorFlowObjectDetector.tensorchat.ChatCloudRepository
import com.example.TensorFlowObjectDetector.tensorchat.ChatRepository

class CloudRunChatRepository(
    private val context: Context,
    private val api: CloudRunChatApiService
) : ChatCloudRepository {

    override suspend fun sendPrompt(
        prompt: String,
        imageUri: String?
    ): String {
        val request = ChatBackendRequest(
            message = prompt,
            image = imageUri?.let { buildBackendImage(it) }
        )

        val response = api.sendMessage(request)

        return response.answer
    }

    private fun buildBackendImage(imageUri: String): ChatBackendImage? {
        return runCatching {
            val uri = Uri.parse(imageUri)

            val mimeType = context.contentResolver.getType(uri)
                ?: "image/jpeg"

            val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            } ?: return null

            ChatBackendImage(
                mimeType = mimeType,
                base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            )
        }.getOrNull()
    }
}
