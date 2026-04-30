package com.example.TensorFlowObjectDetector.tensordetails

fun String.normalizePlantLabel(): String? {
    return trim()
        .lowercase()
        .replace('_', ' ')
        .let { label ->
            when (label) {
                "oxeye daisy" -> "daisy"
                "common sunflower" -> "sunflower"
                "lady's slipper", "yellow lady's slipper", "slipper orchid" -> "orchid"
                "daisy", "daisies" -> "daisy"
                "dandelion", "dandelions" -> "dandelion"
                "sunflower", "sunflowers" -> "sunflower"
                "rose", "roses" -> "rose"
                "tulip", "tulips" -> "tulip"
                "lily" -> "lily"
                "orchid" -> "orchid"
                "lavender" -> "lavender"
                else -> null
            }
        }
}
