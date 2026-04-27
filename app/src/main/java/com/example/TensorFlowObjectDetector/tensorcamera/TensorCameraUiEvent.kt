package com.example.TensorFlowObjectDetector.tensorcamera

import android.net.Uri

sealed class TensorCameraUiEvent {
    data class OnImageCaptured(val uri: Uri) : TensorCameraUiEvent()
    data class OnImageSelected(val uri: Uri) : TensorCameraUiEvent()
    object OnCaptureError : TensorCameraUiEvent()
    object ResetCapturedImage : TensorCameraUiEvent()
}