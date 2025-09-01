package com.example.diagnosticapp.viewmodel

import android.app.Application
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.diagnosticapp.data.repository.PatientRepository
import java.io.File
import java.io.IOException

enum class RecordingState {
    IDLE,       // not recording
    RECORDING,  // actively recording
    PAUSED,     // paused recording
    STOPPED     // finished recording
}

enum class PlaybackState {
    IDLE,       // not playing
    PLAYING,    // actively playing
    PAUSED,     // paused playback
    STOPPED     // playback finished/stopped
}

class VoiceTaskViewModel(application: Application) : AndroidViewModel(application) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var outputFilePath: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var currentTime = 0

    private val _recordingState = MutableLiveData(RecordingState.IDLE)
    val recordingState: LiveData<RecordingState> get() = _recordingState

    private val _playbackState = MutableLiveData(PlaybackState.IDLE)
    val playbackState: LiveData<PlaybackState> get() = _playbackState

    private val _recordingTime = MutableLiveData(0)
    val recordingTime: LiveData<Int> get() = _recordingTime

    private val _playbackTime = MutableLiveData(0)
    val playbackTime: LiveData<Int> get() = _playbackTime

    // ---------------- RECORDING ----------------
    fun startRecording() {
        val fileName = "voice_task_${System.currentTimeMillis()}.3gp"
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
            _recordingState.value = RecordingState.RECORDING
            _recordingTime.value = 0
            startTimer(_recordingTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
            _recordingState.value = RecordingState.PAUSED
            stopTimer()
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.resume()
            _recordingState.value = RecordingState.RECORDING
            startTimer(_recordingTime)
        }
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        _recordingState.value = RecordingState.STOPPED
        stopTimer()
    }

    fun resetRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // ignore if already stopped
        }
        mediaRecorder = null
        _recordingState.value = RecordingState.IDLE
        _recordingTime.value = 0
        stopTimer()
    }

    // ---------------- PLAYBACK ----------------
    fun startPlayback() {
        if (outputFilePath.isNullOrEmpty()) return
        val file = File(outputFilePath!!)
        if (!file.exists()) return

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()
                _playbackState.value = PlaybackState.PLAYING
                _playbackTime.value = 0
                startTimer(_playbackTime)

                setOnCompletionListener {
                    stopPlayback()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun pausePlayback() {
        mediaPlayer?.pause()
        _playbackState.value = PlaybackState.PAUSED
        stopTimer()
    }

    fun resumePlayback() {
        mediaPlayer?.start()
        _playbackState.value = PlaybackState.PLAYING
        startTimer(_playbackTime)
    }

    fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState.STOPPED
        stopTimer()
    }

    fun resetPlayback() {
        try {
            mediaPlayer?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // ignore if already stopped
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState.IDLE
        _playbackTime.value = 0
        stopTimer()
    }

    // ---------------- TIMER ----------------
    private fun startTimer(liveData: MutableLiveData<Int>) {
        stopTimer()
        currentTime = liveData.value ?: 0
        timerRunnable = object : Runnable {
            override fun run() {
                currentTime++
                liveData.value = currentTime
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(timerRunnable!!, 1000L)
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    fun saveRecording(taskId: String?) {
        if (!outputFilePath.isNullOrEmpty() && taskId != null) {
            PatientRepository.updateVoiceTask(taskId, outputFilePath)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
        stopTimer()
    }
}


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
//    private val _recordingTime = MutableLiveData<Int>() // seconds
//    val recordingTime: LiveData<Int> get() = _recordingTime
//
//    private val _playbackTime = MutableLiveData<Int>() // seconds
//    val playbackTime: LiveData<Int> get() = _playbackTime
//
//    //    private var timerHandler: Handler? = null
//    private var timerRunnable: Runnable? = null
//    private var currentTime = 0
//
//    fun startRecording() {
//        val fileName =
//            "voice_task_${System.currentTimeMillis()}.3gp"  // Change file extension to .3gp
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
//            e.printStackTrace()
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
//        if (outputFilePath.isNullOrEmpty()) {
//            Log.e("VoiceTask", "No recording found to play.")
//            return
//        }
//        val file = File(outputFilePath!!)
//        if (!file.exists()) {
//            Log.e("VoiceTask", "File does not exist: $outputFilePath")
//            return
//        }
//    }
//
//    fun startPlayback() {
//        if (outputFilePath.isNullOrEmpty()) {
//            Log.e("VoiceTask", "No recording found to play.")
//            return
//        }
//
//        val file = File(outputFilePath!!)
//        if (!file.exists()) {
//            Log.e("VoiceTask", "File does not exist: $outputFilePath")
//            return
//        }
//
//        mediaPlayer = MediaPlayer().apply {
//            try {
//                setDataSource(file.absolutePath)
//                prepare()
//                start()
//                _isPlaying.value = true
//            } catch (e: IOException) {
//                e.printStackTrace()
//                Log.e("VoiceTask", "Error during playback: ${e.message}")
//            }
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
//    fun saveRecording(taskId: String?) {
//        Log.e("VoiceTaskViewModel", "Trying to save recording for $taskId")
//        if (!outputFilePath.isNullOrEmpty() && taskId != null) {
//            Log.e("VoiceTaskViewModel", "Save recording")
//            PatientRepository.updateVoiceTask(taskId, outputFilePath)
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        mediaRecorder?.release()
//        mediaPlayer?.release()
//    }
//}

