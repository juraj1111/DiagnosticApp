package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.diagnosticapp.R

class VoiceTasksActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_tasks)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // This will close the current activity and return to the previous one
        }

        val btnTask1 = findViewById<Button>(R.id.btnTask1)
        btnTask1.setOnClickListener {
            val intent = Intent(this, VoiceTaskActivity::class.java)
            intent.putExtra("TASK_ID", "task1") // Start task 1
            startActivity(intent)
        }
    }

}