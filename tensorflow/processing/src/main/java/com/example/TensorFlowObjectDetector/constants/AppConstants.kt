package com.example.TensorFlowObjectDetector.constants

object AppConstants {
    object General {
        const val IMAGE_URI_KEY = "imageUri"
        const val CONST_ZERO_VALUE = 0
        const val CONST_ONE_VALUE = 1
        const val CONST_TWO_VALUE = 2
        const val CONST_THREE_VALUE = 3
        const val CONST_FOUR_VALUE = 4
        const val CONST_ZERO_VALUE_FLOAT = 0f
        const val CONST_ONE_VALUE_FLOAT = 1f
    }

    object Ml {
        const val DEFAULT_IMAGE_SIZE = 224
        const val RGB_CHANNEL_COUNT = 3
        const val DEFAULT_BATCH_SIZE = General.CONST_ONE_VALUE
        const val PIXEL_MAX_VALUE = 255f
        const val UINT8_MIN = General.CONST_ZERO_VALUE
        const val UINT8_MAX = 255
        const val INT8_MIN = -128
        const val INT8_MAX = 127
        const val MAX_MATCHES_TO_UI = 5
        const val UNKNOWN_LABEL = "unknown"
        const val ZERO_CONFIDENCE = General.CONST_ZERO_VALUE_FLOAT
        const val FULL_SCALE_CONFIDENCE = General.CONST_ONE_VALUE_FLOAT
        const val PERCENT_SCALE_MAX = 100f
        const val SCORE_PROB_RANGE_MIN = -0.05f
        const val SCORE_PROB_RANGE_MAX = 1.05f
        const val EPSILON = 0.001f
    }

    object Image {
        const val NO_ROTATION = 0f
        const val ROTATION_90 = 90f
        const val ROTATION_180 = 180f
        const val ROTATION_270 = 270f
    }
}
