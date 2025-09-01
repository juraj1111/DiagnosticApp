package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.PlaybackState
import com.example.diagnosticapp.viewmodel.RecordingState
import com.example.diagnosticapp.viewmodel.VoiceTaskViewModel

enum class ExecutionState {
    START,
    RECORDING,
    RECORDED,
    PLAYING,
}




class VoiceTaskExecutionFragment : Fragment() {

    private var taskId: String? = null

    private var executionState:ExecutionState = ExecutionState.START

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getString(ARG_TASK_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voice_task_execution, container, false)

        val btnPlay = view.findViewById<ImageView>(R.id.record_button)
        val btnRestart = view.findViewById<ImageView>(R.id.restart_button)
        val btnSave = view.findViewById<ImageView>(R.id.save_button)
        val btnPause = view.findViewById<ImageView>(R.id.pause_button)
        val tvTime = view.findViewById<TextView>(R.id.time_textview)
        val viewModel = ViewModelProvider(this)[VoiceTaskViewModel::class.java]

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

        btnPlay.setOnClickListener {
            if(executionState === ExecutionState.START){
                executionState = ExecutionState.RECORDING
                btnPlay.setImageResource(R.drawable.stop_circle)
                viewModel.startRecording()
            }else if(executionState === ExecutionState.RECORDING){
                executionState = ExecutionState.RECORDED
                btnPlay.setImageResource(R.drawable.play_circle)
                viewModel.stopRecording()
            }else if(executionState === ExecutionState.RECORDED){
                executionState = ExecutionState.PLAYING
                btnPlay.setImageResource(R.drawable.stop_circle)
                btnSave.setImageResource(R.drawable.tick_green)
                viewModel.startPlayback()
            }else if(executionState === ExecutionState.PLAYING){
                executionState = ExecutionState.RECORDED
                btnPlay.setImageResource(R.drawable.play_circle)
                viewModel.stopPlayback()
            }
        }

        btnRestart.setOnClickListener {
            when (executionState) {
                ExecutionState.RECORDING -> {
                    viewModel.resetRecording()
                }
                ExecutionState.RECORDED -> {
                    viewModel.resetRecording() // in case we want fresh start
                }
                ExecutionState.PLAYING -> {
                    viewModel.resetPlayback()
                }
                else -> {}
            }

            // Reset overall execution flow
            executionState = ExecutionState.START
            btnPlay.setImageResource(R.drawable.mic_circle)
            btnSave.setImageResource(R.drawable.tick)
            btnPause.setImageResource(R.drawable.pause) // back to pause icon
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
            if(executionState === ExecutionState.RECORDED || executionState === ExecutionState.PLAYING){
                if(executionState === ExecutionState.PLAYING) viewModel.stopPlayback()
                viewModel.saveRecording(taskId)
                val intent = Intent(requireContext(), VoiceTasksListActivity::class.java)
                startActivity(intent)
            }
        }

        return view
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
