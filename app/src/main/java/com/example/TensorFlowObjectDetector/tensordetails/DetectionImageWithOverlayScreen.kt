package com.example.TensorFlowObjectDetector.tensordetails

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.TensorFlowObjectDetector.R
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.CONST_ONE_VALUE_FLOAT
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.CONST_ZERO_VALUE
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.CONST_ZERO_VALUE_FLOAT
import com.example.TensorFlowObjectDetector.ui.theme.CustomDimens
import com.example.TensorFlowObjectDetector.utils.loadImageSize

@Composable
fun DetectionImageWithOverlay(
    uiState: TensorDetailUIState,
    imageModelOverride: Any? = null
) {
    uiState.imageUri?.let { uri ->
        val box = uiState.detectionResults.firstOrNull()?.detectionBox
        val context = LocalContext.current
        val imageSize = remember(uri) { loadImageSize(context, uri) }
        val configuration = LocalConfiguration.current
        val maxCardHeight = CustomDimens.dimen280Dp
        val availableWidth = (configuration.screenWidthDp.dp - CustomDimens.dimen32Dp)
            .coerceAtLeast(CustomDimens.dimen0Dp)
        val aspectRatio = imageSize
            ?.takeIf { it.width > CONST_ZERO_VALUE && it.height > CONST_ZERO_VALUE }
            ?.let { it.width.toFloat() / it.height.toFloat() }
            ?: 1f
        val widthAtMaxHeight = maxCardHeight * aspectRatio
        val cardHeight = if (widthAtMaxHeight <= availableWidth) {
            maxCardHeight
        } else {
            availableWidth / aspectRatio
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight),
            elevation = CardDefaults.cardElevation(
                defaultElevation = CustomDimens.dimen8Dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(CONST_ONE_VALUE_FLOAT)) {
                    AsyncImage(
                        model = imageModelOverride ?: uri,
                        contentDescription = stringResource(id = R.string.screen_detail_selected_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (box != null && imageSize != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val imageWidth = imageSize.width.toFloat()
                            val imageHeight = imageSize.height.toFloat()
                            if (imageWidth <= 0f || imageHeight <= CONST_ZERO_VALUE_FLOAT) return@Canvas

                            val scale = minOf(size.width / imageWidth, size.height / imageHeight)
                            val displayedWidth = imageWidth * scale
                            val displayedHeight = imageHeight * scale
                            val offsetX = (size.width - displayedWidth) / 2f
                            val offsetY = (size.height - displayedHeight) / 2f

                            val left = offsetX + box.left * displayedWidth
                            val top = offsetY + box.top * displayedHeight
                            val right = offsetX + box.right * displayedWidth
                            val bottom = offsetY + box.bottom * displayedHeight

                            drawRect(
                                color = Color.Red,
                                topLeft = Offset(left, top),
                                size = Size(
                                    width = (right - left).coerceAtLeast(CONST_ZERO_VALUE_FLOAT),
                                    height = (bottom - top).coerceAtLeast(CONST_ZERO_VALUE_FLOAT)
                                ),
                                style = Stroke(width = CustomDimens.dimen4Dp.toPx())
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(CustomDimens.dimen16Dp)
                        .weight(CONST_ONE_VALUE_FLOAT),
                    verticalArrangement = Arrangement.spacedBy(CustomDimens.dimen8Dp)
                ) {
                    Text(
                        text = uiState.objectName.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    uiState.confidence?.let { confidence ->
                        Column {
                            Text(
                                text = stringResource(id = R.string.screen_detail_confidence),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(
                                    id = R.string.screen_detail_percentage,
                                    confidence * 100
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                            LinearProgressIndicator(
                                progress = { confidence.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                gapSize = CustomDimens.dimen0Dp,
                                drawStopIndicator = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 300)
@Composable
private fun DetectionImageWithOverlayPreview() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .width(CustomDimens.dimen157_5Dp)
                .height(CustomDimens.dimen280Dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = CustomDimens.dimen8Dp
            )
        ) {
            DetectionImageWithOverlay(
                uiState = TensorDetailUIState(),
                imageModelOverride = android.R.drawable.ic_menu_gallery
            )
        }
    }
}
