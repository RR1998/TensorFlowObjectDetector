package com.example.TensorFlowObjectDetector.utils

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.TensorFlowObjectDetector.R
import com.example.TensorFlowObjectDetector.tensordetails.ObjectDetectionResult

fun navigationBarAssets(route: String): Pair<Int, String> {
    return when (route) {
        "camera" -> Pair(R.drawable.ic_camera, "Camera")
        "history" -> Pair(R.drawable.ic_history, "Detail")
        "chat" -> Pair(R.drawable.ic_chat, "Chat")
        else -> Pair(R.drawable.ic_camera, "Camera")
    }
}


fun loadImageSize(context: android.content.Context, uri: Uri): ImageSize? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val rawSize = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
        if (options.outWidth > 0 && options.outHeight > 0) {
            ImageSize(options.outWidth, options.outHeight)
        } else {
            null
        }
    } ?: return null

    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        runCatching {
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
            .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    val needsSwap = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE

    return if (needsSwap) {
        ImageSize(width = rawSize.height, height = rawSize.width)
    } else {
        rawSize
    }
}

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

sealed interface ObjectAnalysisResult {
    data class Success(
        val matches: List<ObjectDetectionResult>,
        val modelNotice: String?
    ) : ObjectAnalysisResult

    data class Error(val message: String) : ObjectAnalysisResult
}
