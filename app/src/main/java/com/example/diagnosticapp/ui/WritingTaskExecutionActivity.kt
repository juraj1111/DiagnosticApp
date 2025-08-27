package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.repository.WritingTasks
import com.example.diagnosticapp.viewmodel.DrawingView

class WritingTaskExecutionActivity : BaseActivity() {

    private lateinit var taskId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing_task_execution)

        taskId = intent.getStringExtra("TASK_ID") ?: ""

        if (taskId != null) {
            Log.d("WritingTaskActivity", "Received taskId: $taskId")
        } else {
            Log.e("WritingTaskActivity", "No taskId found in Intent extras!")
        }


        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        val btnErase = findViewById<Button>(R.id.btnErase)
        val btnSave = findViewById<Button>(R.id.btnSave)

        val task = WritingTasks.getTaskById(taskId)

        val imageView = findViewById<ImageView>(R.id.backgroundImage)
        if (task != null) {
            imageView.setImageResource(task.imageId)
        } else {
            Log.e("TaskError", "Task with id=$taskId not found!")
            imageView.setImageResource(R.drawable.default_image) // fallback
        }

        btnErase.setOnClickListener {
            drawingView.clear()
        }

        btnSave.setOnClickListener(){
            drawingView.saveWriting(taskId)
            val intent = Intent(this, WritingTasksListActivity::class.java)
            startActivity(intent)
        }


    }


}
