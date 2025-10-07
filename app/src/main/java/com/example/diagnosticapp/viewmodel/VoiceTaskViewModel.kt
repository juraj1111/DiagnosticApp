package com.example.diagnosticapp.viewmodel

import android.Manifest
import android.app.Application
import android.media.*
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.diagnosticapp.data.repository.PatientRepository
import java.io.*

enum class RecordingState { IDLE, RECORDING, PAUSED, STOPPED }
enum class PlaybackState { IDLE, PLAYING, PAUSED, STOPPED }

class VoiceTaskViewModel(application: Application) : AndroidViewModel(application) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var isPaused = false
    private var outputFilePath: String? = null

    private var mediaPlayer: MediaPlayer? = null

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

    // Audio parameters
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var totalBytesWritten = 0

    // ---------------- RECORDING ----------------
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        val fileName = "voice_task_${System.currentTimeMillis()}.wav"
        val context = getApplication<Application>().applicationContext
        val file = File(context.filesDir, fileName)
        outputFilePath = file.absolutePath

        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording = true
        isPaused = false
        totalBytesWritten = 0

        _recordingState.value = RecordingState.RECORDING
        _recordingTime.value = 0
        startTimer(_recordingTime)

        // Write PCM to file in background
        recordingThread = Thread {
            writeAudioDataToFile(file, bufferSize)
        }
        recordingThread?.start()
    }

    private fun writeAudioDataToFile(file: File, bufferSize: Int) {
        val data = ByteArray(bufferSize)
        val fos = FileOutputStream(file)
        val bos = BufferedOutputStream(fos)
        val dos = DataOutputStream(bos)

        // Write dummy WAV header first (placeholder, updated later)
        writeWavHeader(dos, sampleRate, channelConfig, audioFormat, 0)

        while (isRecording) {
            val read = audioRecord?.read(data, 0, data.size) ?: 0
            if (read > 0 && !isPaused) {
                dos.write(data, 0, read)
                totalBytesWritten += read
            }
        }

        dos.flush()
        dos.close()

        // Update header with real data size
        updateWavHeader(file, totalBytesWritten, sampleRate, channelConfig, audioFormat)
    }

    fun pauseRecording() {
        if (isRecording && !isPaused) {
            isPaused = true
            _recordingState.postValue(RecordingState.PAUSED)
            stopTimer()
        }
    }

    fun resumeRecording() {
        if (isRecording && isPaused) {
            isPaused = false
            _recordingState.postValue(RecordingState.RECORDING)
            startTimer(_recordingTime)
        }
    }

    fun stopRecording() {
        isRecording = false
        isPaused = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread = null

        _recordingState.value = RecordingState.STOPPED
        stopTimer()
    }

    fun resetRecording() {
        stopRecording()
        outputFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete() // delete the incomplete/old recording
            }
        }
        outputFilePath = null
        totalBytesWritten = 0
        _recordingState.value = RecordingState.IDLE
        _recordingTime.value = 0
    }

    // ---------------- PLAYBACK ----------------
    fun startPlayback() {
        if (outputFilePath.isNullOrEmpty()) return
        val file = File(outputFilePath!!)
        if (!file.exists()) return

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
            _playbackState.value = PlaybackState.PLAYING
            _playbackTime.value = 0
            startTimer(_playbackTime)

            setOnCompletionListener { stopPlayback() }
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
        stopPlayback()
        _playbackState.value = PlaybackState.IDLE
        _playbackTime.value = 0
    }

    // ---------------- WAV HEADER HELPERS ----------------
    private fun writeWavHeader(
        out: DataOutputStream,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        dataLength: Int
    ) {
        val channels = if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        val bitsPerSample = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 16 else 8
        val byteRate = sampleRate * channels * bitsPerSample / 8

        out.writeBytes("RIFF")
        out.writeInt(Integer.reverseBytes(36 + dataLength))
        out.writeBytes("WAVE")
        out.writeBytes("fmt ")
        out.writeInt(Integer.reverseBytes(16)) // Subchunk size
        out.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // PCM
        out.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt())
        out.writeInt(Integer.reverseBytes(sampleRate))
        out.writeInt(Integer.reverseBytes(byteRate))
        out.writeShort(java.lang.Short.reverseBytes((channels * bitsPerSample / 8).toShort()).toInt())
        out.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
        out.writeBytes("data")
        out.writeInt(Integer.reverseBytes(dataLength))
    }

    private fun updateWavHeader(
        file: File,
        dataLength: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int
    ) {
        val raf = RandomAccessFile(file, "rw")
        val channels = if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        val bitsPerSample = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 16 else 8
        val byteRate = sampleRate * channels * bitsPerSample / 8

        raf.seek(4)
        raf.writeInt(Integer.reverseBytes(36 + dataLength))
        raf.seek(40)
        raf.writeInt(Integer.reverseBytes(dataLength))
        raf.close()
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
        audioRecord?.release()
        mediaPlayer?.release()
        stopTimer()
    }
}




//package com.example.diagnosticapp.viewmodel
//
//import android.app.Application
//import android.media.MediaPlayer
//import android.media.MediaRecorder
//import android.os.Build
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MutableLiveData
//import com.example.diagnosticapp.data.repository.PatientRepository
//import java.io.File
//import java.io.IOException
//
//enum class RecordingState {
//    IDLE,       // not recording
//    RECORDING,  // actively recording
//    PAUSED,     // paused recording
//    STOPPED     // finished recording
//}
//
//enum class PlaybackState {
//    IDLE,       // not playing
//    PLAYING,    // actively playing
//    PAUSED,     // paused playback
//    STOPPED     // playback finished/stopped
//}
//
//class VoiceTaskViewModel(application: Application) : AndroidViewModel(application) {
//
//    private var mediaRecorder: MediaRecorder? = null
//    private var mediaPlayer: MediaPlayer? = null
//    private var outputFilePath: String? = null
//
//    private val handler = Handler(Looper.getMainLooper())
//    private var timerRunnable: Runnable? = null
//    private var currentTime = 0
//
//    private val _recordingState = MutableLiveData(RecordingState.IDLE)
//    val recordingState: LiveData<RecordingState> get() = _recordingState
//
//    private val _playbackState = MutableLiveData(PlaybackState.IDLE)
//    val playbackState: LiveData<PlaybackState> get() = _playbackState
//
//    private val _recordingTime = MutableLiveData(0)
//    val recordingTime: LiveData<Int> get() = _recordingTime
//
//    private val _playbackTime = MutableLiveData(0)
//    val playbackTime: LiveData<Int> get() = _playbackTime
//
//    // ---------------- RECORDING ----------------
//    fun startRecording() {
//        val fileName = "voice_task_${System.currentTimeMillis()}.3gp"
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
//            _recordingState.value = RecordingState.RECORDING
//            _recordingTime.value = 0
//            startTimer(_recordingTime)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    fun pauseRecording() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            mediaRecorder?.pause()
//            _recordingState.value = RecordingState.PAUSED
//            stopTimer()
//        }
//    }
//
//    fun resumeRecording() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            mediaRecorder?.resume()
//            _recordingState.value = RecordingState.RECORDING
//            startTimer(_recordingTime)
//        }
//    }
//
//    fun stopRecording() {
//        mediaRecorder?.apply {
//            stop()
//            release()
//        }
//        mediaRecorder = null
//        _recordingState.value = RecordingState.STOPPED
//        stopTimer()
//    }
//
//    fun resetRecording() {
//        try {
//            mediaRecorder?.apply {
//                stop()
//                release()
//            }
//        } catch (e: Exception) {
//            // ignore if already stopped
//        }
//        mediaRecorder = null
//        _recordingState.value = RecordingState.IDLE
//        _recordingTime.value = 0
//        stopTimer()
//    }
//
//    // ---------------- PLAYBACK ----------------
//    fun startPlayback() {
//        if (outputFilePath.isNullOrEmpty()) return
//        val file = File(outputFilePath!!)
//        if (!file.exists()) return
//
//        mediaPlayer = MediaPlayer().apply {
//            try {
//                setDataSource(file.absolutePath)
//                prepare()
//                start()
//                _playbackState.value = PlaybackState.PLAYING
//                _playbackTime.value = 0
//                startTimer(_playbackTime)
//
//                setOnCompletionListener {
//                    stopPlayback()
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    fun pausePlayback() {
//        mediaPlayer?.pause()
//        _playbackState.value = PlaybackState.PAUSED
//        stopTimer()
//    }
//
//    fun resumePlayback() {
//        mediaPlayer?.start()
//        _playbackState.value = PlaybackState.PLAYING
//        startTimer(_playbackTime)
//    }
//
//    fun stopPlayback() {
//        mediaPlayer?.apply {
//            stop()
//            release()
//        }
//        mediaPlayer = null
//        _playbackState.value = PlaybackState.STOPPED
//        stopTimer()
//    }
//
//    fun resetPlayback() {
//        try {
//            mediaPlayer?.apply {
//                stop()
//                release()
//            }
//        } catch (e: Exception) {
//            // ignore if already stopped
//        }
//        mediaPlayer = null
//        _playbackState.value = PlaybackState.IDLE
//        _playbackTime.value = 0
//        stopTimer()
//    }
//
//    // ---------------- TIMER ----------------
//    private fun startTimer(liveData: MutableLiveData<Int>) {
//        stopTimer()
//        currentTime = liveData.value ?: 0
//        timerRunnable = object : Runnable {
//            override fun run() {
//                currentTime++
//                liveData.value = currentTime
//                handler.postDelayed(this, 1000L)
//            }
//        }
//        handler.postDelayed(timerRunnable!!, 1000L)
//    }
//
//    private fun stopTimer() {
//        timerRunnable?.let { handler.removeCallbacks(it) }
//        timerRunnable = null
//    }
//
//    fun saveRecording(taskId: String?) {
//        if (!outputFilePath.isNullOrEmpty() && taskId != null) {
//            PatientRepository.updateVoiceTask(taskId, outputFilePath)
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        mediaRecorder?.release()
//        mediaPlayer?.release()
//        stopTimer()
//    }
//}
//
