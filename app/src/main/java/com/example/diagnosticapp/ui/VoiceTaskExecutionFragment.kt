package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.PlaybackState
import com.example.diagnosticapp.viewmodel.RecordingState
import com.example.diagnosticapp.viewmodel.VoiceTaskViewModel
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Button
import androidx.appcompat.app.AlertDialog

enum class ExecutionState {
    START,
    RECORDING,
    RECORDED,
    PLAYING,
}

class VoiceTaskExecutionFragment : Fragment() {

    private var taskId: String? = null
    private var executionState: ExecutionState = ExecutionState.START
    private lateinit var btnPlay : ImageView
    private lateinit var viewModel : VoiceTaskViewModel

    // Register the permission launcher as a class-level property
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startRecordingWithPermission()
            } else {
                // Show a rationale message to the user
                Toast.makeText(
                    requireContext(),
                    "Microphone permission is required to record audio.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getString(ARG_TASK_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voice_task_execution, container, false)

        btnPlay = view.findViewById<ImageView>(R.id.record_button)
        val btnRestart = view.findViewById<ImageView>(R.id.restart_button)
        val btnSave = view.findViewById<ImageView>(R.id.save_button)
        val btnPause = view.findViewById<ImageView>(R.id.pause_button)
        val tvTime = view.findViewById<TextView>(R.id.time_textview)
        viewModel = ViewModelProvider(this)[VoiceTaskViewModel::class.java]

        viewModel.recordingTime.observe(viewLifecycleOwner) {
            if (executionState == ExecutionState.RECORDING) {
                tvTime.text = formatTime(it)
            }
        }

        viewModel.playbackTime.observe(viewLifecycleOwner) {
            if (executionState == ExecutionState.PLAYING) {
                tvTime.text = formatTime(it)
            }
        }

        // Initial state: button enabled
        btnPlay.isEnabled = true
        btnPlay.imageAlpha = 255

        // Only handle the transition from RECORDING (stopped) → RECORDED
        viewModel.isFileReady.observe(viewLifecycleOwner) { ready ->
            if (ready && executionState == ExecutionState.RECORDING) {
                // File is now ready after stopping recording
                executionState = ExecutionState.RECORDED
                btnPlay.setImageResource(R.drawable.play_circle)
                btnPlay.isEnabled = true
                btnPlay.imageAlpha = 255
            }
        }

        btnPlay.setOnClickListener {
            when (executionState) {
                ExecutionState.START -> {
                    checkAudioPermissionAndRecord()
                }
                ExecutionState.RECORDING -> {
                    // Disable button briefly while WAV file is being finalized
                    btnPlay.isEnabled = false
                    btnPlay.imageAlpha = 128
                    viewModel.stopRecording()
                    // Button re-enabled by isFileReady observer
                }
                ExecutionState.RECORDED -> {
                    executionState = ExecutionState.PLAYING
                    btnPlay.setImageResource(R.drawable.stop_circle)
                    btnSave.setImageResource(R.drawable.tick_green)
                    viewModel.startPlayback()
                }
                ExecutionState.PLAYING -> {
                    executionState = ExecutionState.RECORDED
                    btnPlay.setImageResource(R.drawable.play_circle)
                    viewModel.stopPlayback()
                }
            }
        }

        btnRestart.setOnClickListener {
            when (executionState) {
                ExecutionState.RECORDING -> {
                    viewModel.resetRecording()
                }
                ExecutionState.RECORDED -> {
                    viewModel.resetRecording()
                }
                ExecutionState.PLAYING -> {
                    viewModel.resetPlayback()
                    viewModel.resetRecording() // Also clean up recording
                }
                else -> {}
            }

            // Reset overall execution flow
            executionState = ExecutionState.START
            btnPlay.setImageResource(R.drawable.mic_circle)
            btnPlay.isEnabled = true
            btnPlay.imageAlpha = 255
            btnSave.setImageResource(R.drawable.tick)
            btnPause.setImageResource(R.drawable.pause)
            tvTime.text = "00:00"
        }

        btnPause.setOnClickListener {
            when (executionState) {
                ExecutionState.RECORDING -> {
                    when (viewModel.recordingState.value) {
                        RecordingState.RECORDING -> {
                            viewModel.pauseRecording()
                            btnPause.setImageResource(R.drawable.play)
                        }
                        RecordingState.PAUSED -> {
                            viewModel.resumeRecording()
                            btnPause.setImageResource(R.drawable.pause)
                        }
                        else -> {}
                    }
                }
                ExecutionState.PLAYING -> {
                    when (viewModel.playbackState.value) {
                        PlaybackState.PLAYING -> {
                            viewModel.pausePlayback()
                            btnPause.setImageResource(R.drawable.play)
                        }
                        PlaybackState.PAUSED -> {
                            viewModel.resumePlayback()
                            btnPause.setImageResource(R.drawable.pause)
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }

        btnSave.setOnClickListener {
            if (executionState == ExecutionState.RECORDED || executionState == ExecutionState.PLAYING) {
                if (executionState == ExecutionState.PLAYING) viewModel.stopPlayback()
                viewModel.saveRecording(taskId)
                val intent = Intent(requireContext(), TaskListActivity::class.java)
                startActivity(intent)
            }
        }

        return view
    }

    private fun checkAudioPermissionAndRecord() {
        when {
            // 1. Permission already granted — start immediately
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecordingWithPermission()
            }

            // 2. Show rationale if the user previously denied (but didn't check "Don't ask again")
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Potrebné povolenie pre mikrofón")
                    .setMessage("Táto aplikácia potrebuje prístup k mikrofónu pre záznam hlasu.")
                    .setPositiveButton("Povoliť") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    .setNegativeButton("Zrušiť", null)
                    .show()
            }

            // 3. First time asking — request directly
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecordingWithPermission() {
        executionState = ExecutionState.RECORDING
        btnPlay.setImageResource(R.drawable.stop_circle)
        viewModel.startRecording()
    }

    private fun formatTime(seconds: Int): String {
        val min = seconds / 60
        val sec = seconds % 60
        return String.format("%02d:%02d", min, sec)
    }

    companion object {
        private const val ARG_TASK_ID = "TASK_ID"

        fun newInstance(taskId: String): VoiceTaskExecutionFragment {
            val fragment = VoiceTaskExecutionFragment()
            val args = Bundle()
            args.putString(ARG_TASK_ID, taskId)
            fragment.arguments = args
            return fragment
        }
    }
}