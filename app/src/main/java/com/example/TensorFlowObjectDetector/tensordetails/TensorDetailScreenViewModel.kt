package com.example.TensorFlowObjectDetector.tensordetails

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.IMAGE_URI_KEY
import com.example.TensorFlowObjectDetector.tensorflow.processing.classifiers.ObjectDetectionAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TensorDetailScreenViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private var analyzer: ObjectDetectionAnalyzer? = null
    private val _uiState = MutableStateFlow(TensorDetailUIState())
    val uiState: StateFlow<TensorDetailUIState> = _uiState.asStateFlow()

    init {
        val rawImageUri = savedStateHandle.get<String>(IMAGE_URI_KEY)
        val normalizedImageUri = rawImageUri
            ?.takeUnless { it.isBlank() }
            ?.takeUnless { it == "{$IMAGE_URI_KEY}" || it.equals("null", ignoreCase = true) }

        if (normalizedImageUri == null) {
            _uiState.update { it.copy(imageUri = null, errorMessage = null) }
        } else {
            val imageUri = normalizedImageUri.toUri()
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
                            objectName = topMatch.plantName,
                            objectDescription = topMatch.description,
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
