package com.example.TensorFlowObjectDetector.navigator

import android.net.Uri
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.IMAGE_URI_KEY
import com.example.TensorFlowObjectDetector.tensorchat.ChatContext
import com.example.TensorFlowObjectDetector.utils.ROUTE_CAMERA
import com.example.TensorFlowObjectDetector.utils.ROUTE_CHAT
import com.example.TensorFlowObjectDetector.utils.ROUTE_CHAT_WITH_IMAGE_ARG
import com.example.TensorFlowObjectDetector.utils.ROUTE_DETAILS
import com.example.TensorFlowObjectDetector.utils.ROUTE_DETAILS_WITH_IMAGE_ARG

sealed class Screen(val route: String) {
    object Camera : Screen(ROUTE_CAMERA)
    object Details : Screen(ROUTE_DETAILS_WITH_IMAGE_ARG) {
        fun createRoute(imageUri: String) = "$ROUTE_DETAILS?$IMAGE_URI_KEY=$imageUri"
    }
    object Chat : Screen(ROUTE_CHAT) {
        const val navRoute = ROUTE_CHAT_WITH_IMAGE_ARG
        fun createRoute(context: ChatContext): String {
            val label = Uri.encode(context.currentResult.label)
            val confidence = context.currentResult.confidence
            val category = context.currentResult.category.name
            val imageUri = Uri.encode(context.extraMetadata[IMAGE_URI_KEY].orEmpty())
            return "$ROUTE_CHAT?label=$label&confidence=$confidence&category=$category&$IMAGE_URI_KEY=$imageUri"
        }
    }
}
