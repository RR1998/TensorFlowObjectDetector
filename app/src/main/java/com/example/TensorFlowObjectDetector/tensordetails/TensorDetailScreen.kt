package com.example.TensorFlowObjectDetector.tensordetails

import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.TensorFlowObjectDetector.R
import com.example.TensorFlowObjectDetector.ui.theme.CustomDimens
import com.example.TensorFlowObjectDetector.utils.ImageSize
import com.example.TensorFlowObjectDetector.utils.loadImageSize

@Composable
fun TensorDetailScreen(viewModel: TensorDetailScreenViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TensorDetailContent(uiState = uiState)
}

@Composable
private fun TensorDetailContent(uiState: TensorDetailUIState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(CustomDimens.dimen16Dp),
        verticalArrangement = Arrangement.spacedBy(CustomDimens.dimen16Dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.screen_detail_object_detection_analysis),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            uiState.imageUri?.let { uri ->
                val box = uiState.detectionResults.firstOrNull()?.detectionBox
                val context = LocalContext.current
                val imageSize = remember(uri) { loadImageSize(context, uri) }
                val configuration = LocalConfiguration.current
                val maxCardHeight = CustomDimens.dimen280Dp
                val availableWidth = (configuration.screenWidthDp.dp - CustomDimens.dimen32Dp)
                    .coerceAtLeast(CustomDimens.dimen0Dp)
                val aspectRatio = imageSize
                    ?.takeIf { it.width > 0 && it.height > 0 }
                    ?.let { it.width.toFloat() / it.height.toFloat() }
                    ?: 1f
                val widthAtMaxHeight = maxCardHeight * aspectRatio
                val cardWidth =
                    if (widthAtMaxHeight <= availableWidth) widthAtMaxHeight else availableWidth
                val cardHeight = if (widthAtMaxHeight <= availableWidth) {
                    maxCardHeight
                } else {
                    availableWidth / aspectRatio
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                    ) {
                        DetectionImageWithOverlay(
                            uri = uri,
                            box = box,
                            imageSizeOverride = imageSize
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            item {
                CircularProgressIndicator()
            }
        }

        uiState.modelNotice?.let { modelNotice ->
            item {
                InfoCard(
                    title = stringResource(id = R.string.screen_detail_model_notice),
                    body = modelNotice,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }

        uiState.errorMessage?.let { errorMessage ->
            item {
                InfoCard(
                    title = stringResource(id = R.string.screen_detail_analysis_error),
                    body = errorMessage,
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            }
        }

        if (!uiState.isLoading && uiState.plantName != null) {
            item {
                val plantName = uiState.plantName
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(CustomDimens.dimen16Dp),
                        verticalArrangement = Arrangement.spacedBy(CustomDimens.dimen8Dp)
                    ) {
                        Text(
                            text = plantName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.confidence?.let { confidence ->
                            Text(
                                text = stringResource(
                                    id = R.string.screen_detail_confidence,
                                    confidence * 100
                                ),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        uiState.scientificName?.let { scientificName ->
                            Text(
                                text = stringResource(
                                    id = R.string.screen_detail_scientific_name,
                                    scientificName
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        uiState.plantDescription?.let { description ->
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        uiState.sourceAttribution?.let { attribution ->
                            Text(
                                text = attribution,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (uiState.detectionResults.size > 1) {
            item {
                Text(
                    text = stringResource(id = R.string.screen_detail_other_verified_matches),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(uiState.detectionResults.drop(1)) { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(CustomDimens.dimen16Dp),
                        verticalArrangement = Arrangement.spacedBy(CustomDimens.dimen6Dp)
                    ) {
                        Text(
                            text = result.plantName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                id = R.string.screen_detail_confidence,
                                result.confidence * 100
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        result.scientificName?.let { scientificName ->
                            Text(
                                text = stringResource(
                                    id = R.string.screen_detail_scientific_name,
                                    scientificName
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = result.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                        result.sourceAttribution?.let { attribution ->
                            Text(
                                text = attribution,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun TensorDetailScreenPreview() {
    TensorDetailContent(
        uiState = TensorDetailUIState(
            isLoading = false,
            plantName = stringResource(id = R.string.screen_detail_preview_plant_name_rose),
            plantDescription = stringResource(
                id = R.string.screen_detail_preview_plant_description_rose
            ),
            scientificName = stringResource(id = R.string.screen_detail_preview_scientific_name_rose),
            confidence = 0.87f,
            sourceAttribution = stringResource(
                id = R.string.screen_detail_preview_source_attribution_rose
            ),
            modelNotice = stringResource(id = R.string.screen_detail_preview_model_notice),
            detectionResults = listOf(
                ObjectDetectionResult(
                    plantName = stringResource(id = R.string.screen_detail_preview_plant_name_rose),
                    confidence = 0.87f,
                    metadata = mapOf(
                        "description" to stringResource(
                            id = R.string.screen_detail_preview_plant_description_rose
                        ),
                        "scientificName" to stringResource(
                            id = R.string.screen_detail_preview_scientific_name_rose
                        ),
                        "sourceAttribution" to stringResource(
                            id = R.string.screen_detail_preview_source_attribution_rose
                        )
                    ),
                    detectionBox = DetectionBox(0.15f, 0.2f, 0.75f, 0.8f)
                ),
                ObjectDetectionResult(
                    plantName = stringResource(id = R.string.screen_detail_preview_plant_name_tulip),
                    confidence = 0.08f,
                    metadata = mapOf(
                        "description" to stringResource(
                            id = R.string.screen_detail_preview_plant_description_tulip
                        ),
                        "scientificName" to stringResource(
                            id = R.string.screen_detail_preview_scientific_name_tulip
                        ),
                        "sourceAttribution" to stringResource(
                            id = R.string.screen_detail_preview_source_attribution_tulip
                        )
                    )
                ),
                ObjectDetectionResult(
                    plantName = stringResource(id = R.string.screen_detail_preview_plant_name_daisy),
                    confidence = 0.05f,
                    metadata = mapOf(
                        "description" to stringResource(
                            id = R.string.screen_detail_preview_plant_description_daisy
                        ),
                        "scientificName" to stringResource(
                            id = R.string.screen_detail_preview_scientific_name_daisy
                        ),
                        "sourceAttribution" to stringResource(
                            id = R.string.screen_detail_preview_source_attribution_daisy
                        )
                    )
                )
            )
        )
    )
}

@Composable
private fun DetectionImageWithOverlay(
    uri: Uri,
    box: DetectionBox?,
    imageSizeOverride: ImageSize? = null,
    imageModelOverride: Any? = null
) {
    val context = LocalContext.current
    val imageSize = imageSizeOverride ?: remember(uri) { loadImageSize(context, uri) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                if (imageWidth <= 0f || imageHeight <= 0f) return@Canvas

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
                        width = (right - left).coerceAtLeast(0f),
                        height = (bottom - top).coerceAtLeast(0f)
                    ),
                    style = Stroke(width = CustomDimens.dimen4Dp.toPx())
                )
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
                .height(CustomDimens.dimen280Dp)
        ) {
            DetectionImageWithOverlay(
                uri = Uri.EMPTY,
                box = DetectionBox(0.2f, 0.18f, 0.78f, 0.82f),
                imageSizeOverride = ImageSize(width = 720, height = 1280),
                imageModelOverride = android.R.drawable.ic_menu_gallery
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    containerColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(CustomDimens.dimen16Dp),
            verticalArrangement = Arrangement.spacedBy(CustomDimens.dimen8Dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
