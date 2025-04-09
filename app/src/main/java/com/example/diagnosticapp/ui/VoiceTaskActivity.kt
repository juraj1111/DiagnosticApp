package com.example.diagnosticapp.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.diagnosticapp.R

class VoiceTaskActivity : AppCompatActivity() {

    private var taskId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_task)

        taskId = intent.getIntExtra("TASK_ID", -1)

        if (savedInstanceState == null) {
            loadTaskInfoFragment()
        }

        val btnStart = findViewById<Button>(R.id.btn_start)

        btnStart.setOnClickListener {
            val overlayContainer = findViewById<FrameLayout>(R.id.execution_container)
            btnStart.visibility = View.GONE
            overlayContainer.visibility = View.VISIBLE

            // Add Overlay Fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.execution_container, VoiceTaskExecutionFragment())
                .commit()
        }
    }

    private fun loadTaskInfoFragment() {
        val fragment = VoiceTaskInfoFragment.newInstance(taskId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.info_container, fragment)
            .commit()
    }

    fun loadTaskExecutionFragment() {
        val fragment = VoiceTaskExecutionFragment.newInstance(taskId)
        supportFragmentManager.beginTransaction()
            .add(R.id.execution_container, fragment)  // Use add() to stack fragments
            .addToBackStack(null) // Pressing "back" returns to info screen
            .commit()
    }
}
