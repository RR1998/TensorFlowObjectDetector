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
import com.example.TensorFlowObjectDetector.tensorcamera.TensorCameraScreen
import com.example.TensorFlowObjectDetector.tensorchat.TensorChatScreen
import com.example.TensorFlowObjectDetector.tensordetails.TensorDetailScreen
import com.example.TensorFlowObjectDetector.tensordetails.TensorDetailScreenViewModel
import com.example.TensorFlowObjectDetector.utils.TensorDetailScreenViewModelFactory

@Composable
fun AppNavHost(navController: NavHostController, startDestination: String) {
    val application = LocalContext.current.applicationContext as android.app.Application

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Camera.route) {
            TensorCameraScreen(
                onImageAction = { uri ->
                    navController.navigate(Screen.Details.createRoute(Uri.encode(uri.toString())))
                }
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
                )
            )
        }
        composable(Screen.Chat.route) {
            TensorChatScreen()
        }
    }
}
