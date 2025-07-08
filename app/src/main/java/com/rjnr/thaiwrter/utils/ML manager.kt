package com.rjnr.thaiwrter.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.rjnr.thaiwrter.data.models.Point
import com.rjnr.thaiwrter.data.models.THAI_CHARACTERS
import com.rjnr.thaiwrter.data.models.ThaiCharacter
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.locks.ReentrantLock
import androidx.core.graphics.createBitmap

data class CharacterPrediction(
    val characterIndex: Int,
    val confidence: Float,
    val alternativePredictions: List<Pair<Int, Float>>
) {
    val character: String
        get() = MLStrokeValidator.getCharacterFromIndex(characterIndex)

    val alternativeCharacters: List<Pair<String, Float>>
        get() = alternativePredictions.map {
            MLStrokeValidator.getCharacterFromIndex(it.first) to it.second
        }

    val pronunciation: String
        get() = MLStrokeValidator.getPronunciation(characterIndex)
}

// MLStrokeValidator.kt
class MLStrokeValidator(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val imageSize = 256
    private val lock = ReentrantLock()

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelFile = "thai_character_model.tflite"
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                useNNAPI = true // Use Neural Network API if available
            }
            interpreter = Interpreter(loadModelFile(context, modelFile), options)
            Log.d("MLStrokeValidator", "Model loaded successfully")
        } catch (e: Exception) {
            Log.e("MLStrokeValidator", "Error loading model", e)
        }
    }

    private fun loadModelFile(context: Context, filename: String): ByteBuffer {
        val modelPath = context.assets.openFd(filename)
        val inputStream = FileInputStream(modelPath.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = modelPath.startOffset
        val declaredLength = modelPath.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predictCharacter(points: List<Point>, width: Float, height: Float): CharacterPrediction? {
        return try {
            val bitmap = convertStrokeToBitmap(points, width, height)

            // Debug output for stroke conversion
            Log.d("MLStrokeValidator", "Original bitmap size: ${bitmap.width}x${bitmap.height}")

            val inputArray = preprocessImage(bitmap)
            val outputArray = Array(1) { FloatArray(55) }

            interpreter?.run(inputArray, outputArray)

            // Debug output for prediction
            val predictions = outputArray[0].mapIndexed { index, value ->
                index to value
            }.sortedByDescending { it.second }

            Log.d("MLStrokeValidator", "Top 3 predictions:")
            predictions.take(3).forEach { (index, conf) ->
                Log.d("MLStrokeValidator", "${getCharacterFromIndex(index)}: ${conf * 100}%")
            }

            processResults(outputArray[0])
        } catch (e: Exception) {
            Log.e("MLStrokeValidator", "Error during prediction", e)
            null
        }
    }

    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayscaleBitmap = createBitmap(width, height)
        val canvas = Canvas(grayscaleBitmap)

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscaleBitmap
    }

    private fun convertStrokeToBitmap(points: List<Point>, width: Float, height: Float): Bitmap {
        val bitmap = createBitmap(imageSize, imageSize)
        val canvas = Canvas(bitmap)

        // Set white background
        canvas.drawColor(Color.WHITE)

        // Scale points to fit image size
        val scaleX = imageSize / width
        val scaleY = imageSize / height

        // Draw strokes in black
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 8f * scaleX // Scale stroke width
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }

        val path = android.graphics.Path()
        points.forEachIndexed { index, point ->
            val scaledX = point.x * scaleX
            val scaledY = point.y * scaleY
            if (index == 0) {
                path.moveTo(scaledX, scaledY)
            } else {
                path.lineTo(scaledX, scaledY)
            }
        }

        canvas.drawPath(path, paint)
        return bitmap
    }

    // 2. Fix preprocessing to match Python code
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize to 256x256
        val resizedBitmap = bitmap.scale(imageSize, imageSize)

        // Convert to grayscale if needed
        val grayscaleBitmap = convertToGrayscale(resizedBitmap)

        // Create input buffer
        val inputBuffer = ByteBuffer.allocateDirect(1 * imageSize * imageSize * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        // Normalize pixels exactly like Python: simply divide by 255
        for (y in 0 until imageSize) {
            for (x in 0 until imageSize) {
                val pixel = grayscaleBitmap[x, y]
                val grayValue = Color.red(pixel) / 255.0f
                inputBuffer.putFloat(grayValue)
            }
        }

        inputBuffer.rewind()
        return inputBuffer
    }


    private fun calculateBounds(bitmap: Bitmap): Rect {
        val bounds = Rect()
        var minX = bitmap.width
        var minY = bitmap.height
        var maxX = 0
        var maxY = 0

        // Find the bounds of the character (non-white pixels)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap[x, y]
                if (pixel != Color.WHITE) {
                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    maxX = maxOf(maxX, x)
                    maxY = maxOf(maxY, y)
                }
            }
        }

        bounds.set(minX, minY, maxX, maxY)
        return bounds
    }

    private fun processResults(outputs: FloatArray): CharacterPrediction {
        val predictions = outputs.mapIndexed { index, confidence ->
            Pair(index, confidence)
        }.sortedByDescending { it.second }

        return CharacterPrediction(
            characterIndex = predictions[0].first,
            confidence = predictions[0].second,
            alternativePredictions = predictions.subList(1, minOf(5, predictions.size))
        )
    }

    companion object {
        fun getCharacterFromIndex(index: Int): String =
            THAI_CHARACTERS.getOrNull(index)?.character ?: "?"

        fun getPronunciation(index: Int): String =
            THAI_CHARACTERS.getOrNull(index)?.pronunciation ?: "?"

        val ALL_CHARS: List<Pair<String, String>> =
            THAI_CHARACTERS.map { it.character to it.pronunciation }

        fun randomCharacter(): ThaiCharacter = THAI_CHARACTERS.random()
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val mlPrediction: CharacterPrediction?,
    val confidence: Float
)
//<svg width="83" height="128" viewBox="0 0 83 128" fill="none" xmlns="http://www.w3.org/2000/svg">
//<path d="M14 127C11.6 38.2 7 55 28 33L1 15C1 15 26.9941 0.0325775 45 1C60.4269 1.82886 82 5.00001 82 15C82 25 82 127 82 127" stroke="black"/>
//</svg>
