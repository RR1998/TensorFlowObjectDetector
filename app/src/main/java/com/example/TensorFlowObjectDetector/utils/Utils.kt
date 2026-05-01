package com.example.TensorFlowObjectDetector.utils

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.TensorFlowObjectDetector.R

fun navigationBarAssets(route: String): Pair<Int, String> {
    return when (route) {
        "camera" -> Pair(R.drawable.ic_camera, "Camera")
        "details", "details/{imageUri}" -> Pair(R.drawable.ic_history, "Detail")
        "chat", "chat?label={label}&confidence={confidence}&category={category}" -> Pair(R.drawable.ic_chat, "Chat")
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

