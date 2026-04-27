package com.example.TensorFlowObjectDetector.utils

import com.example.TensorFlowObjectDetector.R

fun navigationBarAssets(route: String) : Pair<Int, String>{
    return when(route){
        "camera" -> Pair(R.drawable.ic_camera, "Camera")
        "history" -> Pair(R.drawable.ic_history, "Detail")
        "chat" -> Pair(R.drawable.ic_chat, "Chat")
        else -> Pair(R.drawable.ic_camera, "Camera")
    }
}