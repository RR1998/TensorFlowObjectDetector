package com.example.TensorFlowObjectDetector.tensordetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun TensorDetailScreen(viewModel: TensorDetailScreenViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Plant Detection Analysis",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            uiState.imageUri?.let { uri ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
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
                    title = "Model Notice",
                    body = modelNotice,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }

        uiState.errorMessage?.let { errorMessage ->
            item {
                InfoCard(
                    title = "Analysis Error",
                    body = errorMessage,
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            }
        }

        if (!uiState.isLoading && uiState.plantName != null) {
            item {
                val plantName = uiState.plantName.orEmpty()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = plantName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.confidence?.let { confidence ->
                            Text(
                                text = "Confidence: ${String.format("%.1f", confidence * 100)}%",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        uiState.scientificName?.let { scientificName ->
                            Text(
                                text = "Scientific name: $scientificName",
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
                    text = "Other Verified Matches",
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
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = result.plantName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Confidence: ${String.format("%.1f", result.confidence * 100)}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        result.scientificName?.let { scientificName ->
                            Text(
                                text = "Scientific name: $scientificName",
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

@Composable
private fun InfoCard(
    title: String,
    body: String,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
