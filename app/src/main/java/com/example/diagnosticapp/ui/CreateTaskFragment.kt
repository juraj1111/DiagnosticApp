package com.example.diagnosticapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.repository.VoiceTasks
import com.example.diagnosticapp.data.repository.WritingTasks

class CreateTaskFragment : DialogFragment() {

    interface TaskCreatedListener {
        fun onTaskCreated(title: String, description: String, taskId: String? = null)
    }

    private var taskId: String? = null
    private var editable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        arguments?.let {
            taskId = it.getString("TASK_ID")
            editable = it.getBoolean("EDITABLE", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_create_task, container, false)

        val editTitle = view.findViewById<EditText>(R.id.editTaskTitle)
        val editDescription = view.findViewById<EditText>(R.id.editTaskDescription)
        val btnCreate = view.findViewById<Button>(R.id.btnCreateTask)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelTask)

        if (editable && taskId != null) {
            // Prefill title and description for editing
            val task =
                VoiceTasks.getTaskById(taskId!!) ?: WritingTasks.getTaskById(taskId!!)
            task?.let {
                editTitle.setText(it.name)
                editDescription.setText(it.description)
            }
            btnCreate.text = "Uložiť"
        }

        btnCreate.setOnClickListener {
            val title = editTitle.text.toString().trim()
            val desc = editDescription.text.toString().trim()

            if (title.isNotEmpty()) {
                (activity as? TaskCreatedListener)?.onTaskCreated(title, desc, taskId)
                dismiss()
            } else {
                editTitle.error = "Title required"
            }
        }

        btnCancel.setOnClickListener { dismiss() }

        return view
    }

    companion object {
        fun newInstance(taskId: String? = null, editable: Boolean = false): CreateTaskFragment {
            val fragment = CreateTaskFragment()
            val args = Bundle().apply {
                putString("TASK_ID", taskId)
                putBoolean("EDITABLE", editable)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
