package com.example.TensorFlowObjectDetector.tensordetails

data class ObjectClassificationResult(
    val label: String,
    val confidence: Float,
    val detectionBox: DetectionBox?
)
