package com.example.TensorFlowObjectDetector.navigator

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.CONST_ZERO_VALUE_FLOAT
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.IMAGE_URI_KEY
import com.example.TensorFlowObjectDetector.tensorcamera.TensorCameraScreen
import com.example.TensorFlowObjectDetector.tensorchat.ChatContext
import com.example.TensorFlowObjectDetector.tensorchat.RecognitionResult
import com.example.TensorFlowObjectDetector.tensorchat.TensorChatScreen
import com.example.TensorFlowObjectDetector.tensordetails.ResultCategory
import com.example.TensorFlowObjectDetector.tensordetails.TensorDetailScreen
import com.example.TensorFlowObjectDetector.utils.ARG_CATEGORY
import com.example.TensorFlowObjectDetector.utils.ARG_CONFIDENCE
import com.example.TensorFlowObjectDetector.utils.ARG_LABEL
import com.example.TensorFlowObjectDetector.utils.DEFAULT_LABEL_UNKNOWN
import com.example.TensorFlowObjectDetector.utils.NULL_STRING_VALUE

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    onChatContextReady: (ChatContext) -> Unit = {}
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Camera.route) {
            TensorCameraScreen(
                onImageAction = { uri ->
                    navController.navigate(
                        route = Screen.Details.createRoute(
                            imageUri = Uri.encode(
                                uri.toString()
                            )
                        )
                    )
                },
                onChatContextReady = onChatContextReady
            )
        }
        composable(
            route = Screen.Chat.navRoute,
            arguments = listOf(
                navArgument(ARG_LABEL) {
                    type = NavType.StringType; defaultValue = DEFAULT_LABEL_UNKNOWN
                },
                navArgument(ARG_CONFIDENCE) {
                    type = NavType.FloatType; defaultValue = CONST_ZERO_VALUE_FLOAT
                },
                navArgument(ARG_CATEGORY) {
                    type = NavType.StringType; defaultValue = ResultCategory.GENERAL.name
                },
                navArgument(IMAGE_URI_KEY) {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val label = backStackEntry.arguments?.getString(ARG_LABEL).orEmpty()
            val confidence = backStackEntry.arguments?.getFloat(ARG_CONFIDENCE)
                ?: CONST_ZERO_VALUE_FLOAT
            val categoryRaw = backStackEntry.arguments?.getString(ARG_CATEGORY)
            val imageUriRaw = backStackEntry.arguments?.getString(IMAGE_URI_KEY)
            val imageUri = imageUriRaw
                ?.takeUnless { it.isBlank() }
                ?.takeUnless {
                    it.equals(NULL_STRING_VALUE, ignoreCase = true) || it == "{$IMAGE_URI_KEY}"
                }
            val category = runCatching { ResultCategory.valueOf(categoryRaw.orEmpty()) }
                .getOrDefault(ResultCategory.GENERAL)
            val chatContext = ChatContext(
                currentResult = RecognitionResult(
                    label = label,
                    confidence = confidence,
                    category = category
                ),
                extraMetadata = buildMap {
                    imageUri?.let { put(IMAGE_URI_KEY, it) }
                }
            )
            TensorChatScreen(
                chatContext = chatContext,
                chatViewModel = hiltViewModel()
            )
        }
        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument(IMAGE_URI_KEY) {
                type = NavType.StringType
                defaultValue = ""
                nullable = true
            })
        ) { backStackEntry ->
            TensorDetailScreen(
                viewModel = hiltViewModel(backStackEntry),
                onSeeAdvancedAnalysis = { chatContext ->
                    navController.navigate(Screen.Chat.createRoute(chatContext))
                }
            )
        }
    }
}
