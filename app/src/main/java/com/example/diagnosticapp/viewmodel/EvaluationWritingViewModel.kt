package com.example.diagnosticapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import ai.onnxruntime.*
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.repository.PatientRepository
import com.example.diagnosticapp.utils.WritingFeatureExtractor
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

class EvaluationWritingViewModel : ViewModel() {

    private val TAG = "EvalWriting"

    fun evaluatePatient(context: Context, onResult: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var env: OrtEnvironment? = null
            var session: OrtSession? = null

            try {
                Log.d(TAG, "=== Starting writing evaluation ===")

                env = OrtEnvironment.getEnvironment()

                // Get active writing model
                val activeModel = com.example.diagnosticapp.data.repository.ModelRepository.getActiveModel(2)
                if (activeModel == null) {
                    withContext(Dispatchers.Main) {
                        onResult("Chyba: Žiadny aktívny písací model")
                    }
                    return@launch
                }

                // Load model from file or assets
                val modelFile = if (activeModel.isDefault) {
                    // Load from assets
                    File.createTempFile("handwriting_model", ".onnx", context.cacheDir).apply {
                        context.assets.open(activeModel.filePath).use { input ->
                            FileOutputStream(this).use { output -> input.copyTo(output) }
                        }
                    }
                } else {
                    // Use custom model file
                    File(activeModel.filePath)
                }

                Log.d(TAG, "Model loaded: ${modelFile.absolutePath}")

                session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())

                // Log model inputs/outputs
                Log.d(TAG, "--- ONNX Model Inputs ---")
                session.inputInfo.forEach {
                    Log.d(TAG, "Input name=${it.key}, info=${it.value.info}")
                }

                Log.d(TAG, "--- ONNX Model Outputs ---")
                session.outputInfo.forEach {
                    Log.d(TAG, "Output name=${it.key}, info=${it.value.info}")
                }

                // Extract features
                val protocol = PatientRepository.currentProtocol
                val features = extractFeatures(protocol)

                Log.d(TAG, "FEATURE VECTOR (${features.size}):")
                Log.d(TAG, features.joinToString())

                // Create input tensor
                val inputName = session.inputNames.first()
                val shape = longArrayOf(1, features.size.toLong())
                val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), shape)

                Log.d(TAG, "Tensor created with shape=${shape.joinToString()}")

                // Run inference
                val results = session.run(mapOf(inputName to inputTensor))

                Log.d(TAG, "*** Inference Complete ***")
                val diagnosis = parseOutput(results)

                withContext(Dispatchers.Main) { onResult(diagnosis) }

                results.close()

            } catch (e: Exception) {
                Log.e(TAG, "ERROR evaluating writing", e)
                withContext(Dispatchers.Main) {
                    onResult("Chyba vyhodnocovania písania: ${e.message}")
                }
            } finally {
                session?.close()
                env?.close()
            }
        }
    }

    private fun extractFeatures(protocol: ProtocolData?): FloatArray {
        if (protocol == null) {
            Log.e(TAG, "Protocol is NULL")
            return FloatArray(58) { 0f }
        }

        val paths = protocol.taskDataList
            ?.mapNotNull { it.resultFilePath }
            ?.filter { it.endsWith(".svc") }
            ?: emptyList()

        Log.d(TAG, "Found ${paths.size} SVC files")

        val all = mutableListOf<FloatArray>()

        for (path in paths) {
            Log.d(TAG, "Processing SVC: $path")
            val f = WritingFeatureExtractor.extractFromSVC(path)
            all += f
        }

        if (all.isEmpty()) {
            Log.e(TAG, "No valid features extracted!")
            return FloatArray(58) { 0f }
        }

        Log.d(TAG, "Averaging ${all.size} feature vectors")

        // Average features
        val n = all[0].size
        val out = FloatArray(n)

        for (feat in all) {
            for (i in 0 until n) {
                out[i] += feat[i]
            }
        }
        for (i in 0 until n) out[i] /= all.size.toFloat()

        Log.d(TAG, "FINAL AVERAGED FEATURES:")
        Log.d(TAG, out.joinToString())

        return out
    }

    private fun parseOutput(results: OrtSession.Result): String {

        Log.d(TAG, "Parsing ONNX output...")

        // LABEL
        val labelTensor = results[0] as OnnxTensor
        val labelArr = labelTensor.longBuffer.array()
        val predictedLabel = labelArr[0].toInt()

        Log.d(TAG, "Predicted class index = $predictedLabel")

        // PROBABILITIES
        val seq = results[1] as OnnxSequence
        val map = seq.value[0] as OnnxMap

        @Suppress("UNCHECKED_CAST")
        val raw = map.value as Map<Any, Any>

        var healthyProb = 0f
        var pdProb = 0f

        for ((key, value) in raw) {
            val prob = (value as Number).toFloat()
            Log.d(TAG, "Probability[$key] = $prob")

            when (key.toString()) {
                "0", "healthy" -> healthyProb = prob
                "1", "pd" -> pdProb = prob
            }
        }

        Log.d(TAG, "Final probability: healthy=$healthyProb pd=$pdProb")

        return if (pdProb >= healthyProb)
            "Pacient má príznaky choroby. (pravdepodobnosť: ${"%.1f".format(pdProb * 100)}%)"
        else
            "Pacient je v norme. (pravdepodobnosť: ${"%.1f".format(healthyProb * 100)}%)"
    }
}
