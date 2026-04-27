package com.example.TensorFlowObjectDetector.tensorcamera

import android.net.Uri

data class TensorCameraUiState(
    val capturedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)