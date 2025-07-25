package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.repository.WritingTasks

class WritingTaskActivity : AppCompatActivity() {

    private lateinit var taskId: String

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

        val task = WritingTasks.getTaskById(taskId)

        tvTaskName.text = task?.let { getString(it.nameResId) }
        tvTextDescription.text = task?.let { getString(it.descriptionResId) }
        Log.d("WritingTaskInfoFragment", "Loading title and description for $taskId")
    }

}
