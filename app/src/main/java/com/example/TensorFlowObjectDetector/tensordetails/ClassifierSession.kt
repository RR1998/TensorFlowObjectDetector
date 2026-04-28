package com.example.TensorFlowObjectDetector.tensordetails

import org.tensorflow.lite.Interpreter

data class ClassifierSession<T>(
    val modelAsset: String,
    val labels: List<String>,
    val interpreter: Interpreter,
    val classifier: T
)