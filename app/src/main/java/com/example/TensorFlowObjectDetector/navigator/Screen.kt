package com.example.TensorFlowObjectDetector.navigator

import android.net.Uri
import com.example.TensorFlowObjectDetector.tensorchat.ChatContext

sealed class Screen(val route: String) {
    object Camera : Screen("camera")
    object Details : Screen("details/{imageUri}") {
        fun createRoute(imageUri: String) = "details/$imageUri"
    }
    object Chat : Screen("chat") {
        const val navRoute = "chat?label={label}&confidence={confidence}&category={category}&imageUri={imageUri}"
        fun createRoute(context: ChatContext): String {
            val label = Uri.encode(context.currentResult.label)
            val confidence = context.currentResult.confidence
            val category = context.currentResult.category.name
            val imageUri = Uri.encode(context.extraMetadata["imageUri"].orEmpty())
            return "chat?label=$label&confidence=$confidence&category=$category&imageUri=$imageUri"
        }
    }
}

