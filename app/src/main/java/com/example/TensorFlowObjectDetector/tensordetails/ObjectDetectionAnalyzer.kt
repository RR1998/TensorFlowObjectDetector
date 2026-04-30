package com.example.TensorFlowObjectDetector.tensordetails

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.TensorFlowObjectDetector.constants.AppConstants
import com.example.TensorFlowObjectDetector.utils.ObjectAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileNotFoundException
import java.nio.channels.FileChannel

class ObjectDetectionAnalyzer(
    context: Context,
    private val plantInfoRepository: VerifiedPlantInfoRepository = VerifiedPlantInfoRepository()
) {

    companion object {
        private const val PLANT_MODEL_ASSET = "models/plants_model.tflite"
        private const val OBJECT_MODEL_ASSET = "models/object_detector.tflite"
        private const val GENERAL_MODEL_ASSET = "models/general_model.tflite"
        private const val OBJECT_LABELS_ASSET = "models/object_labels.txt"
        private const val GENERAL_LABELS_ASSET = "models/general_labels.txt"
        private const val PLANT_LABELS_ASSET = "models/plants_labels.txt"

        // TODO: Change this back to 1 after validating multi-match behavior in UI.
        private const val MAX_MATCHES_TO_UI = AppConstants.Ml.MAX_MATCHES_TO_UI
    }

    private val appContext = context.applicationContext

    private var plantSession: ClassifierSession<PlantClassifier>? = null

    // TODO Clean up redundant sessions
    private var objectSession: ClassifierSession<ObjectClassifier>? = null
    private var generalSession: ClassifierSession<ObjectClassifier>? = null

    suspend fun analyzeImage(imageUri: Uri): ObjectAnalysisResult =
        withContext(Dispatchers.Default) {
            val bitmap = loadBitmapRespectOrientation(imageUri)
                ?: return@withContext ObjectAnalysisResult.Error("Unable to open the selected image.")

            try {
                val objectConfig = runCatching {
                    resolveModelConfig(
                        model = OBJECT_MODEL_ASSET,
                        labels = OBJECT_LABELS_ASSET
                    )
                }.getOrElse { throwable ->
                    return@withContext ObjectAnalysisResult.Error(
                        throwable.message ?: "The object detector model could not be resolved."
                    )
                }
                val generalConfig = runCatching {
                    resolveModelConfig(
                        model = GENERAL_MODEL_ASSET,
                        labels = GENERAL_LABELS_ASSET
                    )
                }.getOrElse { throwable ->
                    return@withContext ObjectAnalysisResult.Error(
                        throwable.message ?: "The general model could not be resolved."
                    )
                }
                val objectClassifier =
                    getOrCreateSharedObjectClassifier(
                        modelConfig = objectConfig,
                        normalization = InputNormalization.ZERO_TO_ONE,
                        currentSession = objectSession
                    ) { updated -> objectSession = updated }
                val generalClassifier =
                    getOrCreateSharedObjectClassifier(
                        modelConfig = generalConfig,
                        normalization = InputNormalization.ZERO_TO_255,
                        currentSession = generalSession
                    ) { updated -> generalSession = updated }
                val objectPrediction = objectClassifier.classify(bitmap)

                val cropBitmap =
                    objectPrediction.detectionBox?.let { cropToDetection(bitmap, it) } ?: bitmap
                val cropCreated = cropBitmap !== bitmap

                val generalPredictions = generalClassifier.classifyTopK(
                    bitmap = cropBitmap,
                    maxResults = MAX_MATCHES_TO_UI
                )
                val topGeneralPrediction = generalPredictions.firstOrNull()
                    ?: ObjectClassificationResult(
                        label = AppConstants.Ml.UNKNOWN_LABEL,
                        confidence = AppConstants.Ml.ZERO_CONFIDENCE,
                        detectionBox = null
                    )

                if (!isPlantLikeLabel(topGeneralPrediction.label)) {
                    if (cropCreated) cropBitmap.recycle()
                    return@withContext ObjectAnalysisResult.Success(
                        matches = generalPredictions.ifEmpty { listOf(topGeneralPrediction) }.map { prediction ->
                            ObjectDetectionResult(
                                plantName = prediction.label,
                                confidence = prediction.confidence,
                                metadata = mapOf(
                                    "sourceAttribution" to "General model"
                                ),
                                detectionBox = objectPrediction.detectionBox
                            )
                        },
                        modelNotice = if (objectPrediction.detectionBox == null) {
                            "Object detector did not return a box; general model ran on full image."
                        } else {
                            null
                        }
                    )
                }

                val plantConfig = runCatching {
                    resolveModelConfig(
                        model = PLANT_MODEL_ASSET,
                        labels = PLANT_LABELS_ASSET
                    )
                }.getOrElse { throwable ->
                    if (cropCreated) cropBitmap.recycle()
                    return@withContext ObjectAnalysisResult.Error(
                        throwable.message ?: "The plant model could not be resolved."
                    )
                }
                val plantClassifier = getOrCreatePlantClassifier(plantConfig)
                val results = plantClassifier.classifyTopK(
                    bitmap = cropBitmap,
                    maxResults = MAX_MATCHES_TO_UI
                ).map { it.copy(detectionBox = objectPrediction.detectionBox) }
                if (cropCreated) cropBitmap.recycle()
                if (results.isEmpty()) {
                    return@withContext ObjectAnalysisResult.Error(
                        "No prediction options were generated by the plant model."
                    )
                }

                val notice = if (objectPrediction.detectionBox == null) {
                    "Object detector did not return a box; models ran on full image."
                } else {
                    null
                }

                ObjectAnalysisResult.Success(
                    matches = results,
                    modelNotice = notice
                )
            } catch (t: Throwable) {
                ObjectAnalysisResult.Error(
                    t.message ?: "The TensorFlow classifier could not be initialized."
                )
            } finally {
                bitmap.recycle()
            }
        }

    fun release() {
        plantSession?.interpreter?.close()
        objectSession?.interpreter?.close()
        generalSession?.interpreter?.close()
        plantSession = null
        objectSession = null
        generalSession = null
    }

    private fun getOrCreatePlantClassifier(modelConfig: ModelConfig): PlantClassifier {
        val current = plantSession
        if (current != null &&
            current.modelAsset == modelConfig.modelAsset &&
            current.labels == modelConfig.labels
        ) {
            return current.classifier
        }

        current?.interpreter?.close()
        val interpreter = createInterpreter(modelConfig.modelAsset)
        val classifier = PlantClassifier(
            interpreter = interpreter,
            labels = modelConfig.labels,
            plantInfoRepository = plantInfoRepository
        )

        plantSession = ClassifierSession(
            modelAsset = modelConfig.modelAsset,
            labels = modelConfig.labels,
            interpreter = interpreter,
            classifier = classifier
        )
        return classifier
    }

    private fun getOrCreateSharedObjectClassifier(
        modelConfig: ModelConfig,
        normalization: InputNormalization,
        currentSession: ClassifierSession<ObjectClassifier>?,
        setSession: (ClassifierSession<ObjectClassifier>) -> Unit
    ): ObjectClassifier {
        if (currentSession != null &&
            currentSession.modelAsset == modelConfig.modelAsset &&
            currentSession.labels == modelConfig.labels
        ) {
            return currentSession.classifier
        }

        currentSession?.interpreter?.close()
        val interpreter = createInterpreter(modelConfig.modelAsset)
        val classifier = ObjectClassifier(
            interpreter = interpreter,
            labels = modelConfig.labels,
            normalization
        )

        setSession(
            ClassifierSession(
                modelAsset = modelConfig.modelAsset,
                labels = modelConfig.labels,
                interpreter = interpreter,
                classifier = classifier
            )
        )
        return classifier
    }

    private fun createInterpreter(modelAsset: String): Interpreter {
        val mappedModel = appContext.assets.openFd(modelAsset).use { descriptor ->
            descriptor.createInputStream().channel.map(
                FileChannel.MapMode.READ_ONLY,
                descriptor.startOffset,
                descriptor.declaredLength
            )
        }
        return Interpreter(mappedModel)
    }

    private fun resolveModelConfig(model: String, labels: String): ModelConfig {
        if (!assetExists(model)) {
            throw FileNotFoundException(
                "Expected $model in app/src/main/assets/models."
            )
        }
        if (!assetExists(labels)) {
            throw FileNotFoundException(
                "Expected $labels in app/src/main/assets/models."
            )
        }

        return ModelConfig(
            modelAsset = model,
            labels = readLabels(labels)
        )
    }

    private fun readLabels(assetPath: String): List<String> {
        return appContext.assets.open(assetPath).bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
        }
    }

    private fun loadBitmapRespectOrientation(imageUri: Uri): Bitmap? {
        val bitmap =
            appContext.contentResolver.openInputStream(imageUri)?.use(BitmapFactory::decodeStream)
                ?: return null

        val orientation = appContext.contentResolver.openInputStream(imageUri)?.use { stream ->
            runCatching {
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> AppConstants.Image.ROTATION_90
            ExifInterface.ORIENTATION_ROTATE_180 -> AppConstants.Image.ROTATION_180
            ExifInterface.ORIENTATION_ROTATE_270 -> AppConstants.Image.ROTATION_270
            else -> AppConstants.Image.NO_ROTATION
        }
        if (rotationDegrees == AppConstants.Image.NO_ROTATION) return bitmap

        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply { postRotate(rotationDegrees) },
            true
        )
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private fun assetExists(assetPath: String): Boolean {
        return runCatching { appContext.assets.open(assetPath).close() }.isSuccess
    }

    private fun isPlantLikeLabel(label: String): Boolean {
        val normalized = label.trim().lowercase()
        val plantKeywords = listOf(
            "plant", "flower", "flora", "leaf", "tree", "grass",
            "daisy", "rose", "tulip", "sunflower", "orchid", "lily", "dandelion"
        )
        return plantKeywords.any { normalized.contains(it) }
    }

    private fun cropToDetection(bitmap: Bitmap, box: DetectionBox): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val left = (box.left * width).toInt()
            .coerceIn(AppConstants.General.CONST_ZERO_VALUE, width - AppConstants.General.CONST_ONE_VALUE)
        val top = (box.top * height).toInt()
            .coerceIn(AppConstants.General.CONST_ZERO_VALUE, height - AppConstants.General.CONST_ONE_VALUE)
        val right = (box.right * width).toInt()
            .coerceIn(left + AppConstants.General.CONST_ONE_VALUE, width)
        val bottom = (box.bottom * height).toInt()
            .coerceIn(top + AppConstants.General.CONST_ONE_VALUE, height)

        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }
}
