package com.example.TensorFlowObjectDetector.tensordetails

import android.net.Uri

data class TensorDetailUIState(
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
    val plantName: String? = null,
    val plantDescription: String? = null,
    val scientificName: String? = null,
    val confidence: Float? = null,
    val sourceAttribution: String? = null,
    val modelNotice: String? = null,
    val detectionResults: List<PlantDetectionResult> = emptyList(),
    val errorMessage: String? = null
)

data class PlantDetectionResult(
    val plantName: String,
    val confidence: Float,
    val metadata: Map<String, String> = emptyMap()
) {
    val description: String
        get() = metadata["description"] ?: "No description available for this plant."

    val scientificName: String?
        get() = metadata["scientificName"]

    val sourceAttribution: String?
        get() = metadata["sourceAttribution"]
}
