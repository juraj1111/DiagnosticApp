package com.example.diagnosticapp.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.diagnosticapp.data.repository.PatientRepository
import java.io.File
import java.io.IOException

class VoiceTaskViewModel(application: Application) : AndroidViewModel(application) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var outputFilePath: String? = null

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> get() = _isRecording

    private val _isPlaying = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    fun startRecording() {
        val fileName = "voice_task_${System.currentTimeMillis()}.3gp"  // Change file extension to .3gp
        val context = getApplication<Application>().applicationContext
        val file = File(context.filesDir, fileName)
        outputFilePath = file.absolutePath

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFilePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _isRecording.value = true
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        _isRecording.value = false
        if (outputFilePath.isNullOrEmpty()) {
            Log.e("VoiceTask", "No recording found to play.")
            return
        }
        val file = File(outputFilePath!!)
        if (!file.exists()) {
            Log.e("VoiceTask", "File does not exist: $outputFilePath")
            return
        }
    }

    fun startPlayback() {
        if (outputFilePath.isNullOrEmpty()) {
            Log.e("VoiceTask", "No recording found to play.")
            return
        }

        val file = File(outputFilePath!!)
        if (!file.exists()) {
            Log.e("VoiceTask", "File does not exist: $outputFilePath")
            return
        }

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()
                _isPlaying.value = true
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e("VoiceTask", "Error during playback: ${e.message}")
            }
        }
    }

    fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
    }

    fun saveRecording(taskId: String?) {
        Log.e("VoiceTaskViewModel", "Trying to save recording for $taskId")
        if (!outputFilePath.isNullOrEmpty() && taskId != null) {
            Log.e("VoiceTaskViewModel", "Save recording")
            PatientRepository.updateVoiceTask(taskId, outputFilePath)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}





//package com.example.diagnosticapp.viewmodel
//
//import android.app.Application
//import android.media.MediaPlayer
//import android.media.MediaRecorder
//import android.util.Log
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import com.example.diagnosticapp.data.repository.PatientRepository
//import java.io.File
//import java.io.IOException
//
//class VoiceTaskViewModel(application: Application) : AndroidViewModel(application) {
//
//    private var mediaRecorder: MediaRecorder? = null
//    private var mediaPlayer: MediaPlayer? = null
//    private var outputFilePath: String? = null
//
//    private val _isRecording = MutableLiveData(false)
//    val isRecording: LiveData<Boolean> get() = _isRecording
//
//    private val _isPlaying = MutableLiveData(false)
//    val isPlaying: LiveData<Boolean> get() = _isPlaying
//
//    fun startRecording() {
//        val fileName = "voice_task_${System.currentTimeMillis()}.mp3"
//        val context = getApplication<Application>().applicationContext
//        val file = File(context.filesDir, fileName)
//        outputFilePath = file.absolutePath
//
//        try {
//            mediaRecorder = MediaRecorder().apply {
//                setAudioSource(MediaRecorder.AudioSource.MIC)
//                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
//                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
//                setOutputFile(outputFilePath)
//                prepare()
//                start()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()  // Log the exception to understand what's going wrong
//        }
//
//        _isRecording.value = true
//    }
//
//    fun stopRecording() {
//        mediaRecorder?.apply {
//            stop()
//            release()
//        }
//        mediaRecorder = null
//        _isRecording.value = false
//    }
//
//
//    fun startPlayback() {
//        val mediaPlayer = MediaPlayer()
//        val file = File(outputFilePath)
//
//        if (file.exists()) {
//            try {
//                mediaPlayer.setDataSource(file.absolutePath)
//                mediaPlayer.prepare()
//                mediaPlayer.start()
//            } catch (e: IOException) {
//                e.printStackTrace()
//                // Handle the error (e.g., file format issue, etc.)
//            }
//        } else {
//            Log.e("VoiceTask", "File does not exist: $outputFilePath")
//        }
//    }
//
//    fun stopPlayback() {
//        mediaPlayer?.apply {
//            stop()
//            release()
//        }
//        mediaPlayer = null
//        _isPlaying.value = false
//    }
//
//
//    fun saveRecording(taskId: String?){
//        if(outputFilePath != null && taskId != null) {
//            PatientRepository.updateVoiceTask(taskId, outputFilePath)
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        mediaRecorder?.release()
//    }
//}
