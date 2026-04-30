package com.example.TensorFlowObjectDetector.tensordetails

sealed interface ObjectAnalysisResult {
    data class Success(
        val matches: List<ObjectDetectionResult>,
        val modelNotice: String?
    ) : ObjectAnalysisResult

    data class Error(val message: String) : ObjectAnalysisResult
}
