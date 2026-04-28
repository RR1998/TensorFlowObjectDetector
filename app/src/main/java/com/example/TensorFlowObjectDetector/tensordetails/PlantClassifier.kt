package com.example.TensorFlowObjectDetector.tensordetails

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.example.TensorFlowObjectDetector.utils.normalizePlantLabel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PlantClassifier(
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val plantInfoRepository: VerifiedPlantInfoRepository = VerifiedPlantInfoRepository()
) {

    fun classify(bitmap: Bitmap): ObjectDetectionResult {
        return classifyTopK(bitmap, maxResults = 1).first()
    }

    fun classifyTopK(bitmap: Bitmap, maxResults: Int): List<ObjectDetectionResult> {
        val input = preprocess(bitmap)
        val probabilities = runInference(input)
        if (probabilities.isEmpty()) return emptyList()

        return probabilities.indices
            .sortedByDescending { probabilities[it] }
            .take(maxResults.coerceAtLeast(1))
            .map { index ->
                val rawLabel = labels.getOrElse(index) { "unknown" }
                val normalizedLabel = rawLabel.normalizePlantLabel()
                val metadata = normalizedLabel?.let(plantInfoRepository::metadataForLabel)
                    ?: mapOf("sourceAttribution" to "Local TensorFlow Lite model")

                ObjectDetectionResult(
                    plantName = metadata["commonName"] ?: rawLabel,
                    confidence = probabilities[index],
                    metadata = metadata
                )
            }
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
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

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                when (inputDataType) {
                    DataType.FLOAT32 -> {
                        buffer.putFloat(r / 255f)
                        buffer.putFloat(g / 255f)
                        buffer.putFloat(b / 255f)
                    }

                    DataType.UINT8, DataType.INT8 -> {
                        buffer.put(quantize(r / 255f, scale, zeroPoint, inputDataType).toByte())
                        buffer.put(quantize(g / 255f, scale, zeroPoint, inputDataType).toByte())
                        buffer.put(quantize(b / 255f, scale, zeroPoint, inputDataType).toByte())
                    }

                    else -> Unit
                }
            }
        }

        if (resized !== bitmap) {
            resized.recycle()
        }
        buffer.rewind()
        return buffer
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
