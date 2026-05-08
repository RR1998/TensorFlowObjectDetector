package com.example.TensorFlowObjectDetector.tensorchat

import com.example.TensorFlowObjectDetector.tensordetails.ResultCategory

data class RecognitionResult(
    val label: String,
    val confidence: Float,
    val category: ResultCategory
)
