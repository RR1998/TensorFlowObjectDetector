package com.example.TensorFlowObjectDetector.tensorflow.processing.classifiers

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.example.TensorFlowObjectDetector.constants.AppConstants
import com.example.TensorFlowObjectDetector.tensordetails.ObjectDetectionResult
import com.example.TensorFlowObjectDetector.tensordetails.ResultCategory
import com.example.TensorFlowObjectDetector.tensordetails.VerifiedPlantInfoRepository
import com.example.TensorFlowObjectDetector.tensordetails.normalizePlantLabel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PlantClassifier(
    private val interpreter: Interpreter,
    private val labels: List<String>,
    private val plantInfoRepository: VerifiedPlantInfoRepository = VerifiedPlantInfoRepository()
) {

    fun classifyTopK(bitmap: Bitmap, maxResults: Int): List<ObjectDetectionResult> {
        val input = preprocess(bitmap)
        val probabilities = runInference(input)
        if (probabilities.isEmpty()) return emptyList()

        return probabilities.indices
            .sortedByDescending { probabilities[it] }
            .take(maxResults.coerceAtLeast(AppConstants.General.CONST_ONE_VALUE))
            .map { index ->
                val rawLabel = labels.getOrElse(index) { AppConstants.Ml.UNKNOWN_LABEL }
                val normalizedLabel = rawLabel.normalizePlantLabel()
                val metadata = normalizedLabel?.let(plantInfoRepository::metadataForLabel)
                    ?: mapOf("sourceAttribution" to "Local TensorFlow Lite model")

                ObjectDetectionResult(
                    plantName = metadata["commonName"] ?: rawLabel,
                    confidence = probabilities[index],
                    category = ResultCategory.PLANT,
                    metadata = metadata
                )
            }
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
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

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                when (inputDataType) {
                    DataType.FLOAT32 -> {
                        buffer.putFloat(r / AppConstants.Ml.PIXEL_MAX_VALUE)
                        buffer.putFloat(g / AppConstants.Ml.PIXEL_MAX_VALUE)
                        buffer.putFloat(b / AppConstants.Ml.PIXEL_MAX_VALUE)
                    }

                    DataType.UINT8, DataType.INT8 -> {
                        buffer.put(
                            quantize(
                                r / AppConstants.Ml.PIXEL_MAX_VALUE,
                                scale,
                                zeroPoint,
                                inputDataType
                            ).toByte()
                        )
                        buffer.put(
                            quantize(
                                g / AppConstants.Ml.PIXEL_MAX_VALUE,
                                scale,
                                zeroPoint,
                                inputDataType
                            ).toByte()
                        )
                        buffer.put(
                            quantize(
                                b / AppConstants.Ml.PIXEL_MAX_VALUE,
                                scale,
                                zeroPoint,
                                inputDataType
                            ).toByte()
                        )
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
        val outputTensor = interpreter.getOutputTensor(AppConstants.General.CONST_ZERO_VALUE)
        val outputDataType = outputTensor.dataType()
        val outputClassCount = outputTensor.shape().lastOrNull() ?: labels.size

        return when (outputDataType) {
            DataType.FLOAT32 -> {
                val output = Array(AppConstants.General.CONST_ONE_VALUE) { FloatArray(outputClassCount) }
                interpreter.run(input, output)
                output[AppConstants.General.CONST_ZERO_VALUE]
            }

            DataType.UINT8, DataType.INT8 -> {
                val qParams = outputTensor.quantizationParams()
                val scale = qParams.scale
                val zeroPoint = qParams.zeroPoint
                val output = Array(AppConstants.General.CONST_ONE_VALUE) { ByteArray(outputClassCount) }
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
