package com.example.TensorFlowObjectDetector.tensordetails

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.TensorFlowObjectDetector.utils.ObjectAnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TensorDetailScreenViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        const val IMAGE_URI_KEY = "imageUri"
    }

    private var analyzer: ObjectDetectionAnalyzer? = null
    private val _uiState = MutableStateFlow(TensorDetailUIState())
    val uiState: StateFlow<TensorDetailUIState> = _uiState.asStateFlow()

    init {
        val rawImageUri = savedStateHandle.get<String>(IMAGE_URI_KEY)
        if (rawImageUri.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "No image URI was provided for the detail screen.") }
        } else {
            val imageUri = rawImageUri.toUri()
            _uiState.update { it.copy(imageUri = imageUri) }
            analyzeImage(imageUri)
        }
    }

    private fun analyzeImage(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val analyzerInstance = analyzer ?: runCatching {
                ObjectDetectionAnalyzer(getApplication())
            }.getOrElse { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message
                            ?: "The TensorFlow model could not be initialized for image analysis."
                    )
                }
                return@launch
            }.also { analyzer = it }

            when (val result = analyzerInstance.analyzeImage(imageUri)) {
                is ObjectAnalysisResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            detectionResults = emptyList(),
                            errorMessage = result.message
                        )
                    }
                }

                is ObjectAnalysisResult.Success -> {
                    val topMatch = result.matches.first()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            detectionResults = result.matches,
                            plantName = topMatch.plantName,
                            plantDescription = topMatch.description,
                            scientificName = topMatch.scientificName,
                            confidence = topMatch.confidence,
                            sourceAttribution = topMatch.sourceAttribution,
                            modelNotice = result.modelNotice,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        analyzer?.release()
        super.onCleared()
    }
}
