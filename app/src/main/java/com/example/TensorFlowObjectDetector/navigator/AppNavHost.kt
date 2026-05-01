package com.example.TensorFlowObjectDetector.navigator

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.TensorFlowObjectDetector.di.ChatEntryPoint
import com.example.TensorFlowObjectDetector.tensorcamera.TensorCameraScreen
import com.example.TensorFlowObjectDetector.tensorchat.ChatContext
import com.example.TensorFlowObjectDetector.tensorchat.RecognitionResult
import com.example.TensorFlowObjectDetector.tensorchat.TensorChatScreen
import com.example.TensorFlowObjectDetector.tensorchat.TensorChatViewModel
import com.example.TensorFlowObjectDetector.tensorchat.TensorChatViewModelFactory
import com.example.TensorFlowObjectDetector.tensordetails.ResultCategory
import com.example.TensorFlowObjectDetector.tensordetails.TensorDetailScreen
import com.example.TensorFlowObjectDetector.tensordetails.TensorDetailScreenViewModel
import com.example.TensorFlowObjectDetector.utils.TensorDetailScreenViewModelFactory
import dagger.hilt.android.EntryPointAccessors

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    onChatContextReady: (ChatContext) -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as android.app.Application

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Camera.route) {
            TensorCameraScreen(
                onImageAction = { uri ->
                    navController.navigate(Screen.Details.createRoute(Uri.encode(uri.toString())))
                },
                onChatContextReady = onChatContextReady
            )
        }
        composable(
            route = Screen.Chat.navRoute,
            arguments = listOf(
                navArgument("label") { type = NavType.StringType; defaultValue = "Unknown" },
                navArgument("confidence") { type = NavType.FloatType; defaultValue = 0f },
                navArgument("category") { type = NavType.StringType; defaultValue = ResultCategory.GENERAL.name },
                navArgument("imageUri") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val label = backStackEntry.arguments?.getString("label").orEmpty()
            val confidence = backStackEntry.arguments?.getFloat("confidence") ?: 0f
            val categoryRaw = backStackEntry.arguments?.getString("category")
            val imageUri = backStackEntry.arguments?.getString("imageUri").orEmpty()
            val category = runCatching { ResultCategory.valueOf(categoryRaw.orEmpty()) }
                .getOrDefault(ResultCategory.GENERAL)
            val chatContext = ChatContext(
                currentResult = RecognitionResult(
                    label = label,
                    confidence = confidence,
                    category = category
                ),
                extraMetadata = buildMap {
                    if (imageUri.isNotBlank()) put("imageUri", imageUri)
                }
            )
            val chatEntryPoint = EntryPointAccessors.fromApplication(
                application,
                ChatEntryPoint::class.java
            )
            val chatViewModel: TensorChatViewModel = viewModel(
                factory = TensorChatViewModelFactory(chatEntryPoint.sendPromptUseCase())
            )
            TensorChatScreen(
                chatContext = chatContext,
                chatViewModel = chatViewModel
            )
        }
        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument(TensorDetailScreenViewModel.IMAGE_URI_KEY) { type = NavType.StringType })
        ) { backStackEntry ->
            TensorDetailScreen(
                viewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = TensorDetailScreenViewModelFactory(
                        application = application,
                        owner = backStackEntry,
                        defaultArgs = backStackEntry.arguments
                    )
                ),
                onSeeAdvancedAnalysis = { chatContext ->
                    navController.navigate(Screen.Chat.createRoute(chatContext))
                }
            )
        }
    }
}
