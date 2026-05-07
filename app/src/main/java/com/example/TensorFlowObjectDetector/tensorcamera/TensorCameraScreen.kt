package com.example.TensorFlowObjectDetector.tensorcamera

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.TensorFlowObjectDetector.R
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.IMAGE_URI_KEY
import com.example.TensorFlowObjectDetector.tensorchat.ChatContext
import com.example.TensorFlowObjectDetector.tensorchat.RecognitionResult
import com.example.TensorFlowObjectDetector.tensordetails.ResultCategory
import com.example.TensorFlowObjectDetector.ui.theme.CustomDimens

@Composable
fun TensorCameraScreen(
    viewModel: TensorCameraViewModel = hiltViewModel(),
    onImageAction: (Uri) -> Unit,
    onChatContextReady: (ChatContext) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraController = remember { LifecycleCameraController(context) }


    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                viewModel.onEvent(TensorCameraUiEvent.OnImageSelected(it))
            }
        }
    )


    // Handle captured image navigation
    LaunchedEffect(uiState.capturedImageUri) {
        uiState.capturedImageUri?.let { uri ->
            onChatContextReady(
                ChatContext(
                    currentResult = RecognitionResult(
                        label = "Captured image",
                        confidence = 0f,
                        category = ResultCategory.GENERAL
                    ),
                    extraMetadata = mapOf(IMAGE_URI_KEY to uri.toString())
                )
            )
            onImageAction(uri)
            // Reset the state after navigation
            viewModel.onEvent(TensorCameraUiEvent.ResetCapturedImage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = CustomDimens.dimen32Dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CustomDimens.dimen16Dp)
        ) {
            // Camera and Gallery buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(CustomDimens.dimen32Dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery button (FAB-like)
                IconButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(CustomDimens.dimen64Dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = stringResource(id = R.string.screen_camera_open_gallery),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(CustomDimens.dimen32Dp)
                    )
                }

                // Take Photo button
                IconButton(
                    onClick = {
                        viewModel.takePhoto(context, cameraController)
                    },
                    modifier = Modifier
                        .size(CustomDimens.dimen80Dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = stringResource(id = R.string.screen_camera_take_photo),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(CustomDimens.dimen36Dp)
                    )
                }
            }
        }

        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                // You could add a CircularProgressIndicator here
            }
        }
    }
}
