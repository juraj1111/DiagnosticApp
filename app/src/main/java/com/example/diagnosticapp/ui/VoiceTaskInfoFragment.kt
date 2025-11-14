package com.example.diagnosticapp.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.ProtocolViewModel
import com.example.diagnosticapp.viewmodel.PatientViewModel

class VoiceTaskInfoFragment : Fragment() {

    private var taskId: String = ""

    private lateinit var viewModelPatient : PatientViewModel
    private lateinit var viewModelProtocol : ProtocolViewModel

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

        viewModelPatient = ViewModelProvider(this).get(PatientViewModel::class.java)
        viewModelProtocol = ViewModelProvider(this).get(ProtocolViewModel::class.java)

        var protocol = viewModelProtocol.getProtocol(viewModelPatient.getCurrentProtocol()!!.protocolName)
        val task = protocol?.tasks?.find { it.id == taskId }

        tvTitle.text = task?.name
        tvDescription.text = task?.description
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
