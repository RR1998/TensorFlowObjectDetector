package com.example.TensorFlowObjectDetector.tensordetails

import android.net.Uri

data class TensorDetailUIState(
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
    val objectName: String? = null,
    val objectDescription: String? = null,
    val scientificName: String? = null,
    val confidence: Float? = null,
    val sourceAttribution: String? = null,
    val modelNotice: String? = null,
    val detectionResults: List<ObjectDetectionResult> = emptyList(),
    val errorMessage: String? = null
)
