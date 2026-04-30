package com.example.TensorFlowObjectDetector.tensordetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.TensorFlowObjectDetector.R
import com.example.TensorFlowObjectDetector.ui.theme.CustomDimens

@Composable
fun TensorDetailScreen(viewModel: TensorDetailScreenViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TensorDetailContent(uiState = uiState)
}

@Composable
fun TensorDetailContent(
    uiState: TensorDetailUIState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = CustomDimens.dimen16Dp),
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
            DetectionImageWithOverlay(uiState = uiState)
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
                    body = modelNotice
                )
            }
        }

        uiState.objectDescription?.let { objectDescription ->
            item {
                InfoCard(
                    title = stringResource(R.string.screen_detail_information),
                    body = objectDescription
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

        if (uiState.detectionResults.size > 1) {
            item {
                Text(
                    text = stringResource(id = R.string.screen_detail_other_verified_matches),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(uiState.detectionResults.drop(0)) { result ->
                ExtraCoincidencesItem(result)
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
            objectName = stringResource(id = R.string.screen_detail_preview_plant_name_rose),
            objectDescription = stringResource(
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
