package com.example.TensorFlowObjectDetector.utils

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.IMAGE_URI_KEY
import com.example.TensorFlowObjectDetector.R

const val ROUTE_CAMERA = "camera"
const val ROUTE_DETAILS = "details"
const val ROUTE_DETAILS_WITH_IMAGE_ARG = "$ROUTE_DETAILS?$IMAGE_URI_KEY={$IMAGE_URI_KEY}"
const val ROUTE_CHAT = "chat"
const val ARG_LABEL = "label"
const val ARG_CONFIDENCE = "confidence"
const val ARG_CATEGORY = "category"
const val ROUTE_CHAT_WITH_CONTEXT_ARGS =
    "$ROUTE_CHAT?$ARG_LABEL={$ARG_LABEL}&$ARG_CONFIDENCE={$ARG_CONFIDENCE}&$ARG_CATEGORY={$ARG_CATEGORY}"
const val ROUTE_CHAT_WITH_IMAGE_ARG =
    "$ROUTE_CHAT_WITH_CONTEXT_ARGS&$IMAGE_URI_KEY={$IMAGE_URI_KEY}"
const val NULL_STRING_VALUE = "null"

const val NAV_LABEL_CAMERA = "Camera"
const val NAV_LABEL_DETAIL = "Detail"
const val NAV_LABEL_CHAT = "Chat"
const val DEFAULT_LABEL_UNKNOWN = "Unknown"
const val CONTENT_DESCRIPTION_BACK = "Back"

fun navigationBarAssets(route: String): Pair<Int, String> {
    return when (route) {
        ROUTE_CAMERA -> Pair(R.drawable.ic_camera, NAV_LABEL_CAMERA)
        ROUTE_DETAILS, ROUTE_DETAILS_WITH_IMAGE_ARG -> Pair(R.drawable.ic_history, NAV_LABEL_DETAIL)
        ROUTE_CHAT, ROUTE_CHAT_WITH_CONTEXT_ARGS, ROUTE_CHAT_WITH_IMAGE_ARG -> Pair(
            R.drawable.ic_chat,
            NAV_LABEL_CHAT
        )

        else -> Pair(R.drawable.ic_camera, NAV_LABEL_CAMERA)
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

