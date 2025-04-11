package com.example.diagnosticapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.TaskData
import com.example.diagnosticapp.data.model.VoiceTask
import com.example.diagnosticapp.data.repository.VoiceTasks

class VoiceTaskInfoFragment : Fragment() {

    private var taskId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getString(ARG_TASK_ID) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_voice_task_info, container, false)

        val tvTitle = view.findViewById<TextView>(R.id.tv_task_name)
        val tvDescription = view.findViewById<TextView>(R.id.tv_task_description)

        val task = VoiceTasks.getTaskById(taskId)

        tvTitle.text = task?.let { getString(it.nameResId) }
        tvDescription.text = task?.let { getString(it.descriptionResId) }
        Log.d("VoiceTaskInfoFragment", "Loading title and description for $taskId")

        return view
    }

    companion object {
        private const val ARG_TASK_ID = "TASK_ID"

        fun newInstance(taskId: String): VoiceTaskInfoFragment {
            return VoiceTaskInfoFragment().apply {
                arguments = Bundle().apply { putString(ARG_TASK_ID, taskId) }
            }
        }
    }
}
