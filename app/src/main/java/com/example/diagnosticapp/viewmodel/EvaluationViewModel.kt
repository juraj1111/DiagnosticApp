package com.example.diagnosticapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.diagnosticapp.data.repository.PatientRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import ai.onnxruntime.*
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.ZeroCrossingRateProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import be.tarsos.dsp.mfcc.MFCC
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.utils.AudioFeatureExtractor
import com.example.diagnosticapp.utils.extractFeaturesFromWav
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class EvaluationViewModel : ViewModel() {

    fun evaluatePatient(context: Context, onResult: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val env = OrtEnvironment.getEnvironment()
                val modelFile = File.createTempFile("model", ".onnx", context.cacheDir)
                context.assets.open("parkinson_voice_android.onnx").use { input ->
                    FileOutputStream(modelFile).use { output -> input.copyTo(output) }
                }

                val session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())


                // Example: Construct input from patient data
                val patient = PatientRepository.currentPatient
                val protocol = PatientRepository.currentProtocol

                val features = extractFeaturesFromTasks(protocol)
                //for (i in 0 until 13) features[i] *= 25f
                val inputName = session.inputNames.iterator().next()

                val shape = longArrayOf(1, features.size.toLong())
                val bb = ByteBuffer.allocateDirect(features.size * 4).order(ByteOrder.nativeOrder())
                val fb = bb.asFloatBuffer()
                fb.put(features)
                fb.rewind()

                Log.d("Evaluation", "Input tensor shape = ${shape.joinToString()} | type = Float | values = ${features.joinToString()}")
                val inputTensor = OnnxTensor.createTensor(env, fb, shape)
                for (info in session.inputInfo.entries) {
                    Log.d("ONNX", "Input ${info.key} shape = ${(info.value.info as TensorInfo).shape.joinToString()}")
                }


                val results = session.run(mapOf(inputName to inputTensor))
                try {
                    // Output 0: label
                    val labelsTensor = results[0] as OnnxTensor
                    val labelArray = labelsTensor.value as Array<String>
                    val predictedLabel = labelArray.firstOrNull() ?: "unknown"

// Output 1: sequence of maps
                    val probsSeq = results[1] as OnnxSequence

// Output 1: sequence of maps
                    val seq = results[1] as OnnxSequence

// The sequence contains one OnnxMap
                    val onnxMap = seq.value[0] as OnnxMap

// Convert to normal HashMap<String, Float>
                    @Suppress("UNCHECKED_CAST")
                    val rawMap = onnxMap.value as Map<Any, Any>

                    val probMap = rawMap.mapNotNull { (k, v) ->
                        val key = k as? String ?: return@mapNotNull null
                        val value = when (v) {
                            is Float -> v
                            is Double -> v.toFloat()
                            is Number -> v.toFloat()
                            else -> null
                        } ?: return@mapNotNull null
                        key to value
                    }.toMap()

                    Log.d("ONNX", "Probability map: $probMap")


                    val pdProb = probMap["pd"] ?: 0f
                    val healthyProb = probMap["healthy"] ?: 0f

                    val (diagnosisText, p) =
                        if (pdProb >= healthyProb)
                            "Pacient má príznaky choroby." to pdProb
                        else
                            "Pacient je v norme." to healthyProb

                    val diagnosis = "$diagnosisText (p = ${"%.2f".format(p)})"


                    CoroutineScope(Dispatchers.Main).launch {
                        onResult(diagnosis)
                    }
                } finally {
                    results.close()  // VERY IMPORTANT: close Result
                }


                session.close()
                env.close()

            } catch (e: Exception) {
                Log.e("Evaluation", "Error during evaluation", e)
                CoroutineScope(Dispatchers.Main).launch {
                    onResult("Chyba pri vyhodnocovaní: ${e.message}")
                }
            }
        }
    }

//    fun evaluatePatient(context: Context, onResult: (String) -> Unit) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                // ... (ONNX setup and input preparation is here) ...
//                val env = OrtEnvironment.getEnvironment()
//                val modelFile = File.createTempFile("model", ".onnx", context.cacheDir)
//                context.assets.open("parkinson_voice_android.onnx").use { input ->
//                    FileOutputStream(modelFile).use { output -> input.copyTo(output) }
//                }
//                val session = env.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
//
//                val patient = PatientRepository.currentPatient
//                val protocol = PatientRepository.currentProtocol
//                var features = extractFeaturesFromTasks(protocol) // This returns FloatArray(16)
//
//                // IMPORTANT: Apply necessary scaling here if your model requires it!
//                // Example (replace with your actual scaling if needed):
//                // features = applyFeatureScaling(features)
//
//                val inputName = session.inputNames.iterator().next()
//                val shape = longArrayOf(1, features.size.toLong())
//                val bb = ByteBuffer.allocateDirect(features.size * 4).order(ByteOrder.nativeOrder())
//                val fb = bb.asFloatBuffer()
//                fb.put(features)
//                fb.rewind()
//
//                Log.d("Evaluation", "Input tensor shape = ${shape.joinToString()} | type = Float | values = ${features.joinToString()}")
//                val inputTensor = OnnxTensor.createTensor(env, fb, shape)
//
//                for (info in session.inputInfo.entries) {
//                    Log.d("ONNX", "Input ${info.key} shape = ${(info.value.info as TensorInfo).shape.joinToString()}")
//                }
//
//                // --- Running the session and handling results ---
//                // ... (Input preparation and session setup is fine) ...
//
//                val results = session.run(mapOf(inputName to inputTensor))
//                try {
//                    // 1. Get Output 0: Predicted Label Tensor (Tensor<String>)
//                    val labelsValue = results.get(0)
//                    val labelsTensor = labelsValue as OnnxTensor
//                    val labelArray = labelsTensor.value as Array<String>
//                    val predictedLabel = labelArray.firstOrNull() ?: "unknown"
//                    Log.d("ONNX", "Predicted Label: $predictedLabel")
//
//                    // ... (Previous logic for getting results.get(0) for label is fine) ...
//
//// 2. Get Output 1: Probabilities Sequence (Sequence<Map>)
//                    val probsValue = results.get(1)
//                    val probsSeq = probsValue as OnnxSequence
//
//// Sequence should contain exactly 1 map, which is an OnnxMap object
//                    val onnxMapValue = probsSeq.value.firstOrNull()
//                        ?: throw IllegalStateException("ONNX probability sequence is empty.")
//
//// Explicitly cast the map element to OnnxMap
//                    val onnxMap = onnxMapValue as OnnxMap
//
//// --- THE FIX: Use methods instead of properties ---
//
//// 2a. Get the Keys (class names)
//                    val keyTensorValue = onnxMap.getMapKeys()
//                    val keyTensor = keyTensorValue as OnnxTensor
//                    val keys = keyTensor.value as Array<String> // Array<String> contains "healthy", "pd"
//
//// 2b. Get the Values (probabilities)
//                    val valueTensorValue = onnxMap.getMapValues()
//                    val valueTensor = valueTensorValue as OnnxTensor
//                    val values = valueTensor.value as FloatArray // FloatArray contains the scores
//
//// Build the final Kotlin map
//// Use toTypedArray() for FloatArray to satisfy the zip function signature
//                    val probMap = keys.zip(values.toTypedArray()).toMap()
//
//                    Log.d("ONNX", "Decoded probability map: $probMap")
//
//// ... (Rest of the diagnosis logic is fine) ...
//
//// ... (Rest of the evaluation logic) ...
//
//                    // Keys should be exactly "healthy" and "pd" (based on model training)
//                    val pdProb = probMap["pd"] ?: 0f
//                    val healthyProb = probMap["healthy"] ?: 0f
//
//                    val (diagnosisText, p) =
//                        if (pdProb >= healthyProb)
//                            "Pacient má príznaky choroby." to pdProb
//                        else
//                            "Pacient je v norme." to healthyProb
//
//                    val diagnosis = "$diagnosisText (p = ${"%.2f".format(p)})"
//
//                    CoroutineScope(Dispatchers.Main).launch {
//                        onResult(diagnosis)
//                    }
//                } finally {
//                    results.close()
//                }
//
//                session.close()
//                env.close()
//
//            } catch (e: Exception) {
//                Log.e("Evaluation", "Error during evaluation", e)
//                CoroutineScope(Dispatchers.Main).launch {
//                    onResult("Chyba pri vyhodnocovaní: ${e.message}")
//                }
//            }
//        }
//    }

    private fun extractFeaturesFromTasks(protocol: ProtocolData?): FloatArray {
        // collect all recording paths from your protocol object
        val audioPaths: List<String> =
            protocol?.taskDataList?.mapNotNull { it.resultFilePath } ?: emptyList()

        if (audioPaths.isEmpty()) {
            return FloatArray(16) { 0f }
        }

        val all = mutableListOf<FloatArray>()
        for (p in audioPaths) {
            val f = extractFeaturesFromWav(p)
            all.add(f)
        }

        // average across files
        val out = FloatArray(16) { 0f }
        for (vec in all) {
            for (i in vec.indices) {
                out[i] += vec[i]
            }
        }
        for (i in out.indices) {
            out[i] /= all.size.toFloat()
        }

        // IMPORTANT: ONNX expects shape [1, 16], so we’ll add that in evaluatePatient
        return out
    }

}
