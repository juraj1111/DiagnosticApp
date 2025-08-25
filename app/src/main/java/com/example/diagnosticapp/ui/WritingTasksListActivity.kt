package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider

import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.TaskData
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.viewmodel.SharedPatientViewModel

class WritingTasksListActivity : AppCompatActivity() {

    private lateinit var viewModel : SharedPatientViewModel
    private lateinit var taskButtons: Map<String, Button>
    private lateinit var taskMap: Map<String, TaskData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing_tasks_list)

        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)
        val patient = viewModel.getCurrentPatient()
        taskMap = patient?.protocol2Tasks?.associateBy { it.id }!!

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // This will close the current activity and return to the previous one
        }

        setupTaskButtons()

    }

    override fun onResume() {
        super.onResume()
        updateButtonColors()
    }

    private fun setupTaskButtons() {
        taskButtons = mapOf(
            "task1" to findViewById(R.id.btnTask1),
            "task2" to findViewById(R.id.btnTask2),
            "task3" to findViewById(R.id.btnTask3),
            "task4" to findViewById(R.id.btnTask4),
            "task5" to findViewById(R.id.btnTask5),
            "task6" to findViewById(R.id.btnTask6),
            "task7" to findViewById(R.id.btnTask7),
            "task8" to findViewById(R.id.btnTask8),
            "task9" to findViewById(R.id.btnTask9)
        )

        taskButtons.forEach { (taskId, button) ->
            val task = taskMap[taskId]
            if (task == null) {
                Log.e("DrawTasksListActivity", "No task found for id: $taskId")
            } else {
                Log.d("DrawTasksListActivity", "Found task $taskId with status: ${task.status}")
            }
            button.setOnClickListener {
                startTaskActivity(taskId)
            }
        }
    }

    private fun startTaskActivity(taskId: String) {
        Log.d("WritingTasksListActivity", "Starting activity for $taskId")
        val intent = Intent(this, WritingTaskActivity::class.java)
        intent.putExtra("TASK_ID", taskId)
        startActivity(intent)
    }

    private fun updateButtonColors() {
        val patient = viewModel.getCurrentPatient()
        val taskMap = patient?.protocol2Tasks?.associateBy { it.id } ?: return

        taskButtons.forEach { (taskId, button) ->
            val status = taskMap[taskId]?.status ?: TaskStatus.UNCOMPLETED
            val colorRes = when (status) {
                TaskStatus.UNCOMPLETED -> R.color.gray
                TaskStatus.COMPLETED -> R.color.orange
                TaskStatus.SAVED -> R.color.green
            }
            Log.d("WritingTasksListActivity", "Task $taskId status: $status")
            button.setBackgroundColor(getColor(colorRes))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, PatientActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}