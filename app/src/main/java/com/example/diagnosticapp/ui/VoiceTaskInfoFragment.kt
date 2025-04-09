package com.example.diagnosticapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.diagnosticapp.R

class VoiceTaskInfoFragment : Fragment() {

    private var taskId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getInt(ARG_TASK_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voice_task_info, container, false)

        val tvTitle = view.findViewById<TextView>(R.id.tv_task_name)
        val tvDescription = view.findViewById<TextView>(R.id.tv_task_description)

        tvTitle.text = getString(R.string.task_1_name)
        tvDescription.text = getString(R.string.task_1_description)

        return view
    }

    companion object {
        private const val ARG_TASK_ID = "task_id"

        fun newInstance(taskId: Int): VoiceTaskInfoFragment {
            return VoiceTaskInfoFragment().apply {
                arguments = Bundle().apply { putInt(ARG_TASK_ID, taskId) }
            }
        }
    }
}
