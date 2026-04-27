package com.example.TensorFlowObjectDetector.navigator

sealed class Screen(val route: String) {
    object Camera : Screen("camera")
    object Details : Screen("details/{imageUri}") {
        fun createRoute(imageUri: String) = "details/$imageUri"
    }
    object Chat : Screen("chat")
}

