package com.example.diagnosticapp.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.diagnosticapp.R

class VoiceTaskActivity : BaseActivity() {

    private lateinit var taskId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_task)

        // Get the task ID from the Intent extras
        taskId = intent.getStringExtra("TASK_ID") ?: ""

        Log.d("VoiceTaskActivity", "Extra: $taskId.")

        // Load the info fragment on first creation only
        if (savedInstanceState == null) {
            loadTaskInfoFragment()
        }

        // Set up the Start button
        val btnStart = findViewById<Button>(R.id.btn_start)
        btnStart.setOnClickListener {
            val overlayContainer = findViewById<FrameLayout>(R.id.execution_container)
            btnStart.visibility = View.GONE
            overlayContainer.visibility = View.VISIBLE

            // ✅ Corrected: Use newInstance(taskId)
            val fragment = VoiceTaskExecutionFragment.newInstance(taskId)
            supportFragmentManager.beginTransaction()
                .replace(R.id.execution_container, fragment)
                .commit()
        }
    }

    private fun loadTaskInfoFragment() {
        val fragment = VoiceTaskInfoFragment.newInstance(taskId)
        supportFragmentManager.beginTransaction()
            .replace(R.id.info_container, fragment)
            .commit()
    }

}
