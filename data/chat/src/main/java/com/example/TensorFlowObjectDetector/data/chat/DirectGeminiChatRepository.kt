package com.example.TensorFlowObjectDetector.data.chat

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.TensorFlowObjectDetector.tensorchat.ChatRepository
import retrofit2.HttpException

class DirectGeminiChatRepository(
    private val context: Context,
    private val api: GeminiApiService,
    private val apiKey: String
) : ChatRepository {
    companion object {
        private const val MODEL_FLASH_2_5 = "gemini-2.5-flash"
        private const val MODEL_FLASH_2_0 = "gemini-2.0-flash"
        private const val MODEL_FLASH_2_5_LITE = "gemini-2.5-flash-lite"
    }

    suspend fun sendMessage(message: String, imageUri: String? = null): String {
        val parts = mutableListOf(
            GeminiPart(text = message)
        )
        imageUri?.let { uriString ->
            buildImagePart(uriString)?.let(parts::add)
        }

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = parts
                )
            )
        )

        val response = executeWithModelFallback(request)

        return extractResponseText(response)
    }

    private suspend fun executeWithModelFallback(request: GeminiRequest): GeminiResponse {
        return try {
            requestWithModel(MODEL_FLASH_2_5, request)
        } catch (firstException: HttpException) {
            if (firstException.code() != 429) throw firstException
            try {
                requestWithModel(MODEL_FLASH_2_0, request)
            } catch (secondException: HttpException) {
                if (secondException.code() != 429) throw secondException
                requestWithModel(MODEL_FLASH_2_5_LITE, request)
            }
        }
    }

    private suspend fun requestWithModel(model: String, request: GeminiRequest): GeminiResponse {
        return api.generateContent(
            model = model,
            apiKey = apiKey,
            request = request
        )
    }

    private fun extractResponseText(response: GeminiResponse): String {
        return response
            .candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: "No response received."
    }

    override suspend fun sendPrompt(prompt: String, imageUri: String?): String {
        return sendMessage(prompt, imageUri)
    }

    private fun buildImagePart(imageUri: String): GeminiPart? {
        return runCatching {
            val uri = Uri.parse(imageUri)
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            GeminiPart(
                inlineData = GeminiInlineData(
                    mimeType = mimeType,
                    data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                )
            )
        }.getOrNull()
    }
}
