package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.ProtocolViewModel
import com.example.diagnosticapp.viewmodel.PatientViewModel

class WritingTaskActivity : BaseActivity() {

    private lateinit var taskId: String

    private val viewModelProtocol: ProtocolViewModel by viewModels()
    private val viewModelPatient: PatientViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing_task)

        // Get the task ID from the Intent extras
        taskId = intent.getStringExtra("TASK_ID") ?: ""

        Log.d("WritingTaskActivity", "Extra: $taskId.")

        // Set up the Start button
        val btnStart = findViewById<Button>(R.id.btn_start)
        val btnTest = findViewById<Button>(R.id.btn_test)


        btnStart.setOnClickListener {
            val intent = Intent(this, WritingTaskExecutionActivity::class.java)
            intent.putExtra("TASK_ID", taskId) // Pass the task ID
            startActivity(intent)
        }

        btnTest.setOnClickListener {
            val intent = Intent(this, WritingTestActivity::class.java)
            startActivity(intent)
        }

        val tvTaskName = findViewById<TextView>(R.id.tv_task_name)
        val tvTextDescription = findViewById<TextView>(R.id.tv_task_description)

        var protocol = viewModelProtocol.getProtocol(viewModelPatient.getCurrentProtocol()!!.protocolName)
        val task = protocol?.tasks?.find { it.id == taskId }

        tvTaskName.text = task?.let { getString(it.nameResId) }
        tvTextDescription.text = task?.let { getString(it.descriptionResId) }
        Log.d("WritingTaskInfoFragment", "Loading title and description for $taskId")
    }

}
