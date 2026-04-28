package com.example.TensorFlowObjectDetector.tensordetails

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.get
import androidx.core.graphics.scale
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
        val input = preprocess(bitmap, normalization)
        if (interpreter.outputTensorCount > 1) {
            return classifyDetectionModel(input)
        }

        val probabilities = runInference(input)
        val bestIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val label = labels.getOrElse(bestIndex) { "unknown" }
        return ObjectClassificationResult(
            label = label,
            confidence = probabilities[bestIndex],
            detectionBox = null
        )
    }

    private fun preprocess(
        bitmap: Bitmap,
        normalization: InputNormalization = InputNormalization.ZERO_TO_ONE
    ): ByteBuffer {
        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()
        val inputDataType = inputTensor.dataType()
        val qParams = inputTensor.quantizationParams()
        val scale = qParams.scale
        val zeroPoint = qParams.zeroPoint

        val inputHeight = inputShape.getOrNull(1) ?: 224
        val inputWidth = inputShape.getOrNull(2) ?: 224
        val resized = bitmap.scale(inputWidth, inputHeight)

        val bytesPerChannel = when (inputDataType) {
            DataType.FLOAT32 -> 4
            DataType.UINT8, DataType.INT8 -> 1
            else -> throw IllegalStateException("Unsupported input tensor type: $inputDataType")
        }

        val buffer = ByteBuffer.allocateDirect(1 * inputWidth * inputHeight * 3 * bytesPerChannel)
        buffer.order(ByteOrder.nativeOrder())

        for (y in 0 until inputHeight) {
            for (x in 0 until inputWidth) {
                val pixel = resized[x, y]
                val r = Color.red(pixel).toFloat()
                val g = Color.green(pixel).toFloat()
                val b = Color.blue(pixel).toFloat()

                val rf = if (normalization == InputNormalization.ZERO_TO_ONE) r / 255f else r
                val gf = if (normalization == InputNormalization.ZERO_TO_ONE) g / 255f else g
                val bf = if (normalization == InputNormalization.ZERO_TO_ONE) b / 255f else b

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

    private fun classifyDetectionModel(input: ByteBuffer): ObjectClassificationResult {
        val outputs = mutableMapOf<Int, Any>()
        for (index in 0 until interpreter.outputTensorCount) {
            val tensor = interpreter.getOutputTensor(index)
            outputs[index] = createOutputContainer(tensor.shape(), tensor.dataType())
        }

        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        val boxTensorIndex = (0 until interpreter.outputTensorCount).firstOrNull { index ->
            val shape = interpreter.getOutputTensor(index).shape()
            shape.size == 3 && shape[2] == 4
        } ?: return ObjectClassificationResult("unknown", 0f, null)

        val numBoxes = interpreter.getOutputTensor(boxTensorIndex).shape()[1]

        val rank2Candidates = (0 until interpreter.outputTensorCount)
            .filter { index ->
                index != boxTensorIndex &&
                    interpreter.getOutputTensor(index).shape().size == 2 &&
                    interpreter.getOutputTensor(index).shape()[1] == numBoxes
            }
            .associateWith { index -> extract2DFloat(outputs[index]) ?: floatArrayOf() }
            .filterValues { it.isNotEmpty() }

        if (rank2Candidates.isEmpty()) {
            return ObjectClassificationResult("unknown", 0f, null)
        }

        val scoreCandidate = rank2Candidates.maxByOrNull { (_, values) -> scoreLikelihood(values) }
            ?: return ObjectClassificationResult("unknown", 0f, null)
        val scoreIndex = scoreCandidate.key
        val scores = scoreCandidate.value

        val classCandidate = rank2Candidates
            .filterKeys { it != scoreIndex }
            .maxByOrNull { (_, values) -> classLikelihood(values) }
        val classes = classCandidate?.value ?: floatArrayOf()

        val bestDetection = scores.indices.maxByOrNull { scores[it] } ?: return ObjectClassificationResult("unknown", 0f, null)
        val confidence = normalizeConfidence(scores[bestDetection])
        val rawIndex = if (bestDetection in classes.indices) classes[bestDetection].roundToInt() else -1
        val detectionBox = extractDetectionBox(outputs[boxTensorIndex], bestDetection)
        val mappedIndex = when {
            rawIndex in labels.indices -> rawIndex
            rawIndex - 1 in labels.indices -> rawIndex - 1
            else -> -1
        }
        val label = if (mappedIndex >= 0) labels[mappedIndex] else "unknown"
        return ObjectClassificationResult(
            label = label,
            confidence = confidence,
            detectionBox = detectionBox
        )
    }

    private fun scoreLikelihood(values: FloatArray): Float {
        if (values.isEmpty()) return Float.NEGATIVE_INFINITY
        val inProbRange = values.count { it in -0.05f..1.05f }.toFloat() / values.size
        val hasFraction = values.count { kotlin.math.abs(it - it.roundToInt()) > 0.001f }.toFloat() / values.size
        return inProbRange * 2f + hasFraction
    }

    private fun classLikelihood(values: FloatArray): Float {
        if (values.isEmpty()) return Float.NEGATIVE_INFINITY
        val integerLike = values.count { kotlin.math.abs(it - it.roundToInt()) < 0.001f }.toFloat() / values.size
        val nonNegative = values.count { it >= -0.001f }.toFloat() / values.size
        return integerLike + nonNegative
    }

    private fun normalizeConfidence(raw: Float): Float {
        return when {
            raw.isNaN() || raw.isInfinite() -> 0f
            raw < 0f -> 0f
            raw <= 1f -> raw
            raw <= 100f -> raw / 100f
            else -> 1f
        }
    }

    private fun createOutputContainer(shape: IntArray, dataType: DataType): Any {
        return when (dataType) {
            DataType.FLOAT32 -> {
                when (shape.size) {
                    1 -> FloatArray(shape[0])
                    2 -> Array(shape[0]) { FloatArray(shape[1]) }
                    3 -> Array(shape[0]) { Array(shape[1]) { FloatArray(shape[2]) } }
                    else -> throw IllegalStateException("Unsupported output tensor rank: ${shape.size}")
                }
            }

            DataType.UINT8, DataType.INT8 -> {
                when (shape.size) {
                    1 -> ByteArray(shape[0])
                    2 -> Array(shape[0]) { ByteArray(shape[1]) }
                    3 -> Array(shape[0]) { Array(shape[1]) { ByteArray(shape[2]) } }
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
                    is ByteArray -> first.map { (it.toInt() and 0xFF).toFloat() }.toFloatArray()
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
                            is ByteArray -> candidate.map { (it.toInt() and 0xFF).toFloat() }.toFloatArray()
                            else -> null
                        }
                    }
                    else -> null
                }
            }

            else -> null
        } ?: return null

        if (values.size < 4) return null

        val ymin = values[0].coerceIn(0f, 1f)
        val xmin = values[1].coerceIn(0f, 1f)
        val ymax = values[2].coerceIn(0f, 1f)
        val xmax = values[3].coerceIn(0f, 1f)
        if (xmax <= xmin || ymax <= ymin) return null
        return DetectionBox(
            left = xmin,
            top = ymin,
            right = xmax,
            bottom = ymax
        )
    }

    private fun runInference(input: ByteBuffer): FloatArray {
        val outputTensor = interpreter.getOutputTensor(0)
        val outputDataType = outputTensor.dataType()
        val outputClassCount = outputTensor.shape().lastOrNull() ?: labels.size

        return when (outputDataType) {
            DataType.FLOAT32 -> {
                val output = Array(1) { FloatArray(outputClassCount) }
                interpreter.run(input, output)
                output[0]
            }

            DataType.UINT8, DataType.INT8 -> {
                val qParams = outputTensor.quantizationParams()
                val scale = qParams.scale
                val zeroPoint = qParams.zeroPoint
                val output = Array(1) { ByteArray(outputClassCount) }
                interpreter.run(input, output)
                output[0].map { byteValue ->
                    val quantized = if (outputDataType == DataType.UINT8) {
                        byteValue.toInt() and 0xFF
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
            DataType.UINT8 -> quantized.coerceIn(0, 255)
            DataType.INT8 -> quantized.coerceIn(-128, 127)
            else -> quantized
        }
    }
}

data class ObjectClassificationResult(
    val label: String,
    val confidence: Float,
    val detectionBox: DetectionBox?
)

enum class InputNormalization {
    ZERO_TO_ONE,
    ZERO_TO_255
}
