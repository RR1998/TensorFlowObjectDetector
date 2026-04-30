package com.example.TensorFlowObjectDetector.tensorflow.processing.classifiers

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.example.TensorFlowObjectDetector.constants.AppConstants
import com.example.TensorFlowObjectDetector.constants.AppConstants.General.CONST_ONE_VALUE
import com.example.TensorFlowObjectDetector.tensordetails.DetectionBox
import com.example.TensorFlowObjectDetector.tensordetails.ObjectClassificationResult
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

class ObjectClassifier(
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val normalization: InputNormalization
) {

    fun classify(bitmap: Bitmap): ObjectClassificationResult {
        return classifyTopK(
            bitmap = bitmap,
            maxResults = CONST_ONE_VALUE
        ).firstOrNull()
            ?: ObjectClassificationResult(
                AppConstants.Ml.UNKNOWN_LABEL,
                AppConstants.Ml.ZERO_CONFIDENCE,
                null
            )
    }

    fun classifyTopK(bitmap: Bitmap, maxResults: Int): List<ObjectClassificationResult> {
        val input = preprocess(bitmap, normalization)
        if (interpreter.outputTensorCount > CONST_ONE_VALUE) {
            return classifyDetectionModelTopK(input, maxResults)
        }

        val probabilities = runInference(input)
        if (probabilities.isEmpty()) return emptyList()

        return probabilities.indices
            .sortedByDescending { probabilities[it] }
            .take(maxResults.coerceAtLeast(CONST_ONE_VALUE))
            .map { index ->
                ObjectClassificationResult(
                    label = labels.getOrElse(index) { AppConstants.Ml.UNKNOWN_LABEL },
                    confidence = probabilities[index],
                    detectionBox = null
                )
            }
    }

    private fun preprocess(
        bitmap: Bitmap,
        normalization: InputNormalization = InputNormalization.ZERO_TO_ONE
    ): ByteBuffer {
        val inputTensor = interpreter.getInputTensor(AppConstants.General.CONST_ZERO_VALUE)
        val inputShape = inputTensor.shape()
        val inputDataType = inputTensor.dataType()
        val qParams = inputTensor.quantizationParams()
        val scale = qParams.scale
        val zeroPoint = qParams.zeroPoint

        val inputHeight = inputShape.getOrNull(AppConstants.General.CONST_ONE_VALUE)
            ?: AppConstants.Ml.DEFAULT_IMAGE_SIZE
        val inputWidth = inputShape.getOrNull(AppConstants.General.CONST_TWO_VALUE)
            ?: AppConstants.Ml.DEFAULT_IMAGE_SIZE
        val resized = bitmap.scale(inputWidth, inputHeight)

        val bytesPerChannel = when (inputDataType) {
            DataType.FLOAT32 -> 4
            DataType.UINT8, DataType.INT8 -> 1
            else -> throw IllegalStateException("Unsupported input tensor type: $inputDataType")
        }

        val buffer = ByteBuffer.allocateDirect(
            AppConstants.Ml.DEFAULT_BATCH_SIZE * inputWidth * inputHeight *
                AppConstants.Ml.RGB_CHANNEL_COUNT * bytesPerChannel
        )
        buffer.order(ByteOrder.nativeOrder())

        for (y in 0 until inputHeight) {
            for (x in 0 until inputWidth) {
                val pixel = resized[x, y]
                val r = Color.red(pixel).toFloat()
                val g = Color.green(pixel).toFloat()
                val b = Color.blue(pixel).toFloat()

                val rf = if (normalization == InputNormalization.ZERO_TO_ONE) {
                    r / AppConstants.Ml.PIXEL_MAX_VALUE
                } else r
                val gf = if (normalization == InputNormalization.ZERO_TO_ONE) {
                    g / AppConstants.Ml.PIXEL_MAX_VALUE
                } else g
                val bf = if (normalization == InputNormalization.ZERO_TO_ONE) {
                    b / AppConstants.Ml.PIXEL_MAX_VALUE
                } else b

                when (inputDataType) {
                    DataType.FLOAT32 -> {
                        buffer.putFloat(rf)
                        buffer.putFloat(gf)
                        buffer.putFloat(bf)
                    }

                    DataType.UINT8, DataType.INT8 -> {
                        buffer.put(quantize(rf, scale, zeroPoint, inputDataType).toByte())
                        buffer.put(quantize(gf, scale, zeroPoint, inputDataType).toByte())
                        buffer.put(quantize(bf, scale, zeroPoint, inputDataType).toByte())
                    }

                    else -> Unit
                }
            }
        }

        if (resized !== bitmap) resized.recycle()

        buffer.rewind()
        return buffer
    }

    private fun classifyDetectionModelTopK(
        input: ByteBuffer,
        maxResults: Int
    ): List<ObjectClassificationResult> {
        val outputs = mutableMapOf<Int, Any>()
        for (index in AppConstants.General.CONST_ZERO_VALUE until interpreter.outputTensorCount) {
            val tensor = interpreter.getOutputTensor(index)
            outputs[index] = createOutputContainer(tensor.shape(), tensor.dataType())
        }

        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        val boxTensorIndex =
            (AppConstants.General.CONST_ZERO_VALUE until interpreter.outputTensorCount).firstOrNull { index ->
                val shape = interpreter.getOutputTensor(index).shape()
                shape.size == AppConstants.General.CONST_THREE_VALUE &&
                    shape[AppConstants.General.CONST_TWO_VALUE] == AppConstants.General.CONST_FOUR_VALUE
            } ?: return listOf(
                ObjectClassificationResult(
                    AppConstants.Ml.UNKNOWN_LABEL,
                    AppConstants.Ml.ZERO_CONFIDENCE,
                    null
                )
            )

        val numBoxes =
            interpreter.getOutputTensor(boxTensorIndex).shape()[AppConstants.General.CONST_ONE_VALUE]

        val rank2Candidates = (AppConstants.General.CONST_ZERO_VALUE until interpreter.outputTensorCount)
            .filter { index ->
                index != boxTensorIndex &&
                    interpreter.getOutputTensor(index).shape().size == AppConstants.General.CONST_TWO_VALUE &&
                    interpreter.getOutputTensor(index).shape()[AppConstants.General.CONST_ONE_VALUE] == numBoxes
            }
            .associateWith { index -> extract2DFloat(outputs[index]) ?: floatArrayOf() }
            .filterValues { it.isNotEmpty() }

        if (rank2Candidates.isEmpty()) {
            return listOf(
                ObjectClassificationResult(
                    AppConstants.Ml.UNKNOWN_LABEL,
                    AppConstants.Ml.ZERO_CONFIDENCE,
                    null
                )
            )
        }

        val scoreCandidate = rank2Candidates.maxByOrNull { (_, values) -> scoreLikelihood(values) }
            ?: return listOf(
                ObjectClassificationResult(
                    AppConstants.Ml.UNKNOWN_LABEL,
                    AppConstants.Ml.ZERO_CONFIDENCE,
                    null
                )
            )
        val scoreIndex = scoreCandidate.key
        val scores = scoreCandidate.value

        val classCandidate = rank2Candidates
            .filterKeys { it != scoreIndex }
            .maxByOrNull { (_, values) -> classLikelihood(values) }
        val classes = classCandidate?.value ?: floatArrayOf()

        return scores.indices
            .sortedByDescending { scores[it] }
            .take(maxResults.coerceAtLeast(CONST_ONE_VALUE))
            .map { detectionIndex ->
                val confidence = normalizeConfidence(scores[detectionIndex])
                val rawIndex = if (detectionIndex in classes.indices) {
                    classes[detectionIndex].roundToInt()
                } else {
                    -1
                }
                val mappedIndex = when {
                    rawIndex in labels.indices -> rawIndex
                    rawIndex - AppConstants.General.CONST_ONE_VALUE in labels.indices ->
                        rawIndex - AppConstants.General.CONST_ONE_VALUE
                    else -> -1
                }
                ObjectClassificationResult(
                    label = if (mappedIndex >= AppConstants.General.CONST_ZERO_VALUE) {
                        labels[mappedIndex]
                    } else AppConstants.Ml.UNKNOWN_LABEL,
                    confidence = confidence,
                    detectionBox = extractDetectionBox(outputs[boxTensorIndex], detectionIndex)
                )
            }
    }

    private fun scoreLikelihood(values: FloatArray): Float {
        if (values.isEmpty()) return Float.NEGATIVE_INFINITY
        val inProbRange = values.count {
            it in AppConstants.Ml.SCORE_PROB_RANGE_MIN..AppConstants.Ml.SCORE_PROB_RANGE_MAX
        }.toFloat() / values.size
        val hasFraction = values.count {
            kotlin.math.abs(it - it.roundToInt()) > AppConstants.Ml.EPSILON
        }.toFloat() / values.size
        return inProbRange * 2f + hasFraction
    }

    private fun classLikelihood(values: FloatArray): Float {
        if (values.isEmpty()) return Float.NEGATIVE_INFINITY
        val integerLike = values.count {
            kotlin.math.abs(it - it.roundToInt()) < AppConstants.Ml.EPSILON
        }.toFloat() / values.size
        val nonNegative = values.count { it >= -AppConstants.Ml.EPSILON }.toFloat() / values.size
        return integerLike + nonNegative
    }

    private fun normalizeConfidence(raw: Float): Float {
        return when {
            raw.isNaN() || raw.isInfinite() -> AppConstants.Ml.ZERO_CONFIDENCE
            raw < AppConstants.Ml.ZERO_CONFIDENCE -> AppConstants.Ml.ZERO_CONFIDENCE
            raw <= AppConstants.Ml.FULL_SCALE_CONFIDENCE -> raw
            raw <= AppConstants.Ml.PERCENT_SCALE_MAX -> raw / AppConstants.Ml.PERCENT_SCALE_MAX
            else -> AppConstants.Ml.FULL_SCALE_CONFIDENCE
        }
    }

    private fun createOutputContainer(shape: IntArray, dataType: DataType): Any {
        return when (dataType) {
            DataType.FLOAT32 -> {
                when (shape.size) {
                    AppConstants.General.CONST_ONE_VALUE -> {
                        FloatArray(shape[AppConstants.General.CONST_ZERO_VALUE])
                    }

                    AppConstants.General.CONST_TWO_VALUE -> {
                        Array(shape[AppConstants.General.CONST_ZERO_VALUE]) {
                            FloatArray(shape[AppConstants.General.CONST_ONE_VALUE])
                        }
                    }

                    AppConstants.General.CONST_THREE_VALUE -> {
                        Array(shape[AppConstants.General.CONST_ZERO_VALUE]) {
                            Array(shape[AppConstants.General.CONST_ONE_VALUE]) {
                                FloatArray(shape[AppConstants.General.CONST_TWO_VALUE])
                            }
                        }
                    }

                    else -> throw IllegalStateException("Unsupported output tensor rank: ${shape.size}")
                }
            }

            DataType.UINT8, DataType.INT8 -> {
                when (shape.size) {
                    AppConstants.General.CONST_ONE_VALUE -> {
                        ByteArray(shape[AppConstants.General.CONST_ZERO_VALUE])
                    }

                    AppConstants.General.CONST_TWO_VALUE -> {
                        Array(shape[AppConstants.General.CONST_ZERO_VALUE]) {
                            ByteArray(shape[AppConstants.General.CONST_ONE_VALUE])
                        }
                    }

                    AppConstants.General.CONST_THREE_VALUE -> {
                        Array(shape[AppConstants.General.CONST_ZERO_VALUE]) {
                            Array(shape[AppConstants.General.CONST_ONE_VALUE]) {
                                ByteArray(shape[AppConstants.General.CONST_TWO_VALUE])
                            }
                        }
                    }

                    else -> throw IllegalStateException("Unsupported output tensor rank: ${shape.size}")
                }
            }

            else -> throw IllegalStateException("Unsupported output tensor type: $dataType")
        }
    }

    private fun extract2DFloat(output: Any?): FloatArray? {
        return when (output) {
            is Array<*> -> {
                val first = output.firstOrNull()
                when (first) {
                    is FloatArray -> first
                    is ByteArray -> first.map {
                        (it.toInt() and AppConstants.Ml.UINT8_MAX).toFloat()
                    }.toFloatArray()

                    else -> null
                }
            }

            else -> null
        }
    }

    private fun extractDetectionBox(output: Any?, index: Int): DetectionBox? {
        val values = when (output) {
            is Array<*> -> {
                val first = output.firstOrNull()
                when (first) {
                    is Array<*> -> {
                        val candidate = first.getOrNull(index)
                        when (candidate) {
                            is FloatArray -> candidate
                            is ByteArray -> candidate.map {
                                (it.toInt() and AppConstants.Ml.UINT8_MAX).toFloat()
                            }.toFloatArray()

                            else -> null
                        }
                    }

                    else -> null
                }
            }

            else -> null
        } ?: return null

        if (values.size < AppConstants.General.CONST_FOUR_VALUE) return null

        val ymin = values[AppConstants.General.CONST_ZERO_VALUE]
            .coerceIn(AppConstants.Ml.ZERO_CONFIDENCE, AppConstants.Ml.FULL_SCALE_CONFIDENCE)
        val xmin = values[AppConstants.General.CONST_ONE_VALUE]
            .coerceIn(AppConstants.Ml.ZERO_CONFIDENCE, AppConstants.Ml.FULL_SCALE_CONFIDENCE)
        val ymax = values[AppConstants.General.CONST_TWO_VALUE]
            .coerceIn(AppConstants.Ml.ZERO_CONFIDENCE, AppConstants.Ml.FULL_SCALE_CONFIDENCE)
        val xmax = values[AppConstants.General.CONST_THREE_VALUE]
            .coerceIn(AppConstants.Ml.ZERO_CONFIDENCE, AppConstants.Ml.FULL_SCALE_CONFIDENCE)
        if (xmax <= xmin || ymax <= ymin) return null
        return DetectionBox(
            left = xmin,
            top = ymin,
            right = xmax,
            bottom = ymax
        )
    }

    private fun runInference(input: ByteBuffer): FloatArray {
        val outputTensor = interpreter.getOutputTensor(AppConstants.General.CONST_ZERO_VALUE)
        val outputDataType = outputTensor.dataType()
        val outputClassCount = outputTensor.shape().lastOrNull() ?: labels.size

        return when (outputDataType) {
            DataType.FLOAT32 -> {
                val output =
                    Array(AppConstants.General.CONST_ONE_VALUE) { FloatArray(outputClassCount) }
                interpreter.run(input, output)
                output[AppConstants.General.CONST_ZERO_VALUE]
            }

            DataType.UINT8, DataType.INT8 -> {
                val qParams = outputTensor.quantizationParams()
                val scale = qParams.scale
                val zeroPoint = qParams.zeroPoint
                val output =
                    Array(AppConstants.General.CONST_ONE_VALUE) { ByteArray(outputClassCount) }
                interpreter.run(input, output)
                output[AppConstants.General.CONST_ZERO_VALUE].map { byteValue ->
                    val quantized = if (outputDataType == DataType.UINT8) {
                        byteValue.toInt() and AppConstants.Ml.UINT8_MAX
                    } else {
                        byteValue.toInt()
                    }
                    (quantized - zeroPoint) * scale
                }.toFloatArray()
            }

            else -> throw IllegalStateException("Unsupported output tensor type: $outputDataType")
        }
    }

    private fun quantize(value: Float, scale: Float, zeroPoint: Int, dataType: DataType): Int {
        if (scale == 0f) return zeroPoint
        val quantized = (value / scale + zeroPoint).toInt()
        return when (dataType) {
            DataType.UINT8 -> quantized.coerceIn(AppConstants.Ml.UINT8_MIN, AppConstants.Ml.UINT8_MAX)
            DataType.INT8 -> quantized.coerceIn(AppConstants.Ml.INT8_MIN, AppConstants.Ml.INT8_MAX)
            else -> quantized
        }
    }
}
