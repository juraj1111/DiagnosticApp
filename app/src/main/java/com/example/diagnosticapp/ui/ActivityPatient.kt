package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.diagnosticapp.R

class ActivityPatient : AppCompatActivity(){

    private lateinit var tvId: TextView
    private lateinit var btnWriting: Button
    private lateinit var btnVoice: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient)

        tvId = findViewById(R.id.tvId)
        btnWriting = findViewById(R.id.btnWritingProtocol)
        btnVoice = findViewById(R.id.btnVoiceProtocol)

        btnVoice.setOnClickListener {
            val intent = Intent(this, VoiceTasksActivity::class.java)
            startActivity(intent)
        }
    }
}