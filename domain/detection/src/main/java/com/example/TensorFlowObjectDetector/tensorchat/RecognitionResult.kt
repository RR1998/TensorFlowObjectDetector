package com.example.TensorFlowObjectDetector.tensorchat

import com.example.TensorFlowObjectDetector.tensordetails.ObjectDetectionResult
import com.example.TensorFlowObjectDetector.tensordetails.ResultCategory

data class RecognitionResult(
    val label: String,
    val confidence: Float,
    val category: ResultCategory
)

fun ObjectDetectionResult.toRecognitionResult(): RecognitionResult {
    return RecognitionResult(
        label = plantName,
        confidence = confidence,
        category = category
    )
}
