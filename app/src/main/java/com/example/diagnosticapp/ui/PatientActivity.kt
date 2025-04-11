package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.viewmodel.SharedPatientViewModel

class PatientActivity : AppCompatActivity(){

    private lateinit var tvId: TextView
    private lateinit var tvInfo: TextView
    private lateinit var btnWriting: Button
    private lateinit var btnVoice: Button
    private lateinit var viewModel : SharedPatientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient)

        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)
        val patient = viewModel.getCurrentPatient()
        val patientId = patient?.id
        val taskNumber = patient?.protocol1Tasks?.size
        val taskSaved = patient?.protocol1Tasks
            ?.count { it.status == TaskStatus.SAVED }

        tvId = findViewById(R.id.tvId)
        tvInfo = findViewById(R.id.tvInfo)
        btnWriting = findViewById(R.id.btnWritingProtocol)
        btnVoice = findViewById(R.id.btnVoiceProtocol)

        tvId.text = patientId.toString()
        tvInfo.text = "${patient?.sex}, ${patient?.age} rokov"

        btnVoice.text = "HLASOVÝ PROTOKOL $taskSaved/$taskNumber"

        btnVoice.setOnClickListener {
            val intent = Intent(this, VoiceTasksListActivity::class.java)
            startActivity(intent)
        }
    }
}