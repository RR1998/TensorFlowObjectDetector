package com.example.TensorFlowObjectDetector.tensorcamera

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@HiltViewModel
class TensorCameraViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TensorCameraUiState())
    val uiState: StateFlow<TensorCameraUiState> = _uiState.asStateFlow()

    fun onEvent(event: TensorCameraUiEvent) {
        when (event) {
            is TensorCameraUiEvent.OnImageCaptured -> {
                _uiState.update { it.copy(capturedImageUri = event.uri, isLoading = false) }
            }

            is TensorCameraUiEvent.OnImageSelected -> {
                _uiState.update { it.copy(capturedImageUri = event.uri, isLoading = false) }
            }

            TensorCameraUiEvent.OnCaptureError -> {
                _uiState.update {
                    it.copy(
                        errorMessage = "Failed to capture image",
                        isLoading = false
                    )
                }
            }

            TensorCameraUiEvent.ResetCapturedImage -> {
                _uiState.update { it.copy(capturedImageUri = null, isLoading = false) }
            }
        }
    }

    fun takePhoto(context: Context, cameraController: LifecycleCameraController) {
        _uiState.update { it.copy(isLoading = true) }

        val outputFile = createOutputFile(context)

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        cameraController.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        outputFile
                    )
                    onEvent(TensorCameraUiEvent.OnImageCaptured(savedUri))
                }

                override fun onError(exception: ImageCaptureException) {
                    onEvent(TensorCameraUiEvent.OnCaptureError)
                }
            }
        )
    }

    private fun createOutputFile(context: Context): File {
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(System.currentTimeMillis())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.filesDir
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
}
