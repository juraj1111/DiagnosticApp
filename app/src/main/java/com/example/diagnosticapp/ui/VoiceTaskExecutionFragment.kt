package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
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

        val btnPlay = view.findViewById<ImageView>(R.id.play_button)
        val btnRestart = view.findViewById<ImageView>(R.id.restart_button)
        val btnSave = view.findViewById<ImageView>(R.id.save_button)

        val viewModel = ViewModelProvider(this)[VoiceTaskViewModel::class.java]

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
                viewModel.startPlayback()
            }else if(executionState === ExecutionState.PLAYING){
                executionState = ExecutionState.RECORDED
                btnPlay.setImageResource(R.drawable.play_circle)
                viewModel.stopPlayback()
            }
        }

        btnRestart.setOnClickListener {
            if(executionState === ExecutionState.RECORDED || executionState === ExecutionState.RECORDING || executionState === ExecutionState.PLAYING){
                if(executionState === ExecutionState.PLAYING) viewModel.stopPlayback()
                executionState = ExecutionState.START
                btnPlay.setImageResource(R.drawable.mic_circle)
            }
        }

        btnSave.setOnClickListener {
            if(executionState === ExecutionState.RECORDED || executionState === ExecutionState.PLAYING){
                if(executionState === ExecutionState.PLAYING) viewModel.stopPlayback()
                viewModel.saveRecording(taskId)
                val intent = Intent(requireContext(), VoiceTasksActivity::class.java)
                startActivity(intent)
            }
        }

        return view
    }

    companion object {
        private const val ARG_TASK_ID = "task_id"

        fun newInstance(taskId: Int): VoiceTaskExecutionFragment {
            val fragment = VoiceTaskExecutionFragment()
            val args = Bundle()
            args.putInt(ARG_TASK_ID, taskId)
            fragment.arguments = args
            return fragment
        }
    }
}
