package com.example.diagnosticapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.diagnosticapp.data.repository.PatientRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ai.onnxruntime.*
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.utils.VoiceFeatureExtractorV2
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

class EvaluationVoiceViewModel : ViewModel() {

    fun evaluatePatient(context: Context, onResult: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var env: OrtEnvironment? = null
            var session: OrtSession? = null

            try {
                env = OrtEnvironment.getEnvironment()
                val modelFile = File.createTempFile("model", ".onnx", context.cacheDir)
                context.assets.open("rf_model.onnx").use { input ->
                    FileOutputStream(modelFile).use { output -> input.copyTo(output) }
                }

                session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())

                // Log model info for debugging
                Log.d("ONNX", "=== Model Input Info ===")
                for (info in session.inputInfo.entries) {
                    val tensorInfo = info.value.info as? TensorInfo
                    Log.d("ONNX", "Input '${info.key}': shape=${tensorInfo?.shape?.joinToString()}, type=${tensorInfo?.type}")
                }

                Log.d("ONNX", "=== Model Output Info ===")
                for (info in session.outputInfo.entries) {
                    Log.d("ONNX", "Output '${info.key}': type=${info.value.info}")
                }

                // Extract features
                val protocol = PatientRepository.currentProtocol
                val features = extractFeaturesFromTasks(protocol)

                Log.d("ONNX", "Extracted features (${features.size}): ${features.joinToString()}")

                // Validate features
                if (features.any { it.isNaN() || it.isInfinite() }) {
                    throw IllegalStateException("Invalid features detected (NaN or Infinite values)")
                }

                // Create input tensor
                val inputName = session.inputNames.iterator().next()
                val shape = longArrayOf(1, features.size.toLong())

                val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), shape)

                // Run inference
                val results = session.run(mapOf(inputName to inputTensor))

                try {
                    Log.d("ONNX", "Number of outputs: ${results.size()}")

                    // Parse outputs based on actual structure
                    val diagnosis = parseModelOutput(results)

                    CoroutineScope(Dispatchers.Main).launch {
                        onResult(diagnosis)
                    }
                } finally {
                    results.close()
                }

            } catch (e: Exception) {
                Log.e("ONNX", "Error during evaluation", e)
                Log.e("ONNX", "Error type: ${e.javaClass.simpleName}")
                Log.e("ONNX", "Error message: ${e.message}")
                e.printStackTrace()

                CoroutineScope(Dispatchers.Main).launch {
                    onResult("Chyba pri vyhodnocovaní: ${e.message}")
                }
            } finally {
                session?.close()
                env?.close()
            }
        }
    }

    private fun parseModelOutput(results: OrtSession.Result): String {

        // ===== OUTPUT 0: predicted label as int64 =====
        val labelTensor = results[0] as OnnxTensor
        val labelArray = labelTensor.value as LongArray
        val predictedIndex = labelArray[0].toInt()
        val predictedLabel = if (predictedIndex == 1) "pd" else "healthy"

        // ===== OUTPUT 1: probability map inside OnnxSequence =====
        val seq = results[1] as OnnxSequence      // <-- FIXED
        val onnxMap = seq.value[0] as OnnxMap     // <-- FIXED

        // Convert probability OnnxMap → Kotlin Map<String, Float>
        @Suppress("UNCHECKED_CAST")
        val rawMap = onnxMap.value as Map<Any, Any>

        val probMap = rawMap.mapNotNull { (key, value) ->
            val label = when (key) {
                0L, 0 -> "healthy"
                1L, 1 -> "pd"
                is String -> key
                else -> key.toString()
            }

            val prob = when (value) {
                is Float -> value
                is Double -> value.toFloat()
                is Number -> value.toFloat()
                else -> return@mapNotNull null
            }

            label to prob
        }.toMap()

        val healthyProb = probMap["healthy"] ?: 0f
        val pdProb = probMap["pd"] ?: 0f

        Log.d("ONNX", "Probabilities = healthy=$healthyProb, pd=$pdProb")

        // ===== DIAGNOSIS TEXT =====
        return formatDiagnosis(pdProb, healthyProb)
    }

    private fun formatDiagnosis(pdProb: Float, healthyProb: Float): String {
        val (diagnosisText, confidence) = if (pdProb >= healthyProb) {
            "Pacient má príznaky choroby." to pdProb
        } else {
            "Pacient je v norme." to healthyProb
        }

        return "$diagnosisText (pravdepodobnosť: ${"%.1f".format(confidence * 100)}%)"
    }

    private fun extractFeaturesFromTasks(protocol: ProtocolData?): FloatArray {
        val audioPaths: List<String> =
            protocol?.taskDataList?.mapNotNull { it.resultFilePath } ?: emptyList()

        if (audioPaths.isEmpty()) {
            Log.w("ONNX", "No audio paths found in protocol")
            return FloatArray(12) { 0f }
        }

        Log.d("ONNX", "Processing ${audioPaths.size} audio files")

        val allFeatures = mutableListOf<FloatArray>()
        for (path in audioPaths) {
            val features = VoiceFeatureExtractorV2(path)

            // Validate extracted features
            if (features.any { it.isNaN() || it.isInfinite() }) {
                Log.w("ONNX", "Invalid features from file: $path")
                continue
            }

            Log.d("ONNX", "Features from $path: ${features.joinToString()}")
            allFeatures.add(features)
        }

        if (allFeatures.isEmpty()) {
            Log.e("ONNX", "No valid features extracted from any audio file")
            return FloatArray(12) { 0f }
        }

        // Average features across all recordings
        val averaged = FloatArray(12) { 0f }
        for (features in allFeatures) {
            for (i in features.indices) {
                averaged[i] += features[i]
            }
        }

        for (i in averaged.indices) {
            averaged[i] /= allFeatures.size.toFloat()
        }

        Log.d("ONNX", "Averaged features: ${averaged.joinToString()}")
        return averaged
    }
}
