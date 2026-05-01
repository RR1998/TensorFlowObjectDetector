package com.example.TensorFlowObjectDetector.tensordetails

data class ObjectDetectionResult(
    val plantName: String,
    val confidence: Float,
    val category: ResultCategory = ResultCategory.GENERAL,
    val metadata: Map<String, String> = emptyMap(),
    val detectionBox: DetectionBox? = null
) {
    val description: String
        get() = metadata["description"] ?: "No description available."

    val scientificName: String?
        get() = metadata["scientificName"]

    val sourceAttribution: String?
        get() = metadata["sourceAttribution"]
}
