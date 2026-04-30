package com.example.TensorFlowObjectDetector.tensordetails

data class VerifiedPlantInfo(
    val commonName: String,
    val supportedLabels: Set<String>,
    val scientificName: String,
    val description: String,
    val sourceAttribution: String
)
