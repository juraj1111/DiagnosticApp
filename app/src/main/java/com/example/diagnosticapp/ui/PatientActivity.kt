package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.data.repository.PatientRepository
import com.example.diagnosticapp.viewmodel.SharedPatientViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PatientActivity : AppCompatActivity(){

    private lateinit var tvId: TextView
    private lateinit var tvInfo: TextView
    private lateinit var btnWriting: Button
    private lateinit var btnVoice: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDrawTest: Button
    private lateinit var viewModel : SharedPatientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient)

        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)
        val patient = viewModel.getCurrentPatient()
        val patientId = patient?.id
        val voiceTaskNumber = patient?.protocol1Tasks?.size
        val voiceTaskSaved = patient?.protocol1Tasks
            ?.count { it.status == TaskStatus.SAVED }
        val writingTaskNumber = patient?.protocol2Tasks?.size
        val writingTaskSaved = patient?.protocol2Tasks
            ?.count { it.status == TaskStatus.SAVED }

        tvId = findViewById(R.id.tvId)
        tvInfo = findViewById(R.id.tvInfo)
        btnWriting = findViewById(R.id.btnWritingProtocol)
        btnVoice = findViewById(R.id.btnVoiceProtocol)
        btnUpdate = findViewById(R.id.btnUpdateData)
        btnDrawTest = findViewById(R.id.btnDrawTest)

        tvId.text = patientId.toString()
        tvInfo.text = "${patient?.sex}, ${patient?.age} rokov"

        btnVoice.text = "HLASOVÝ PROTOKOL $voiceTaskSaved/$voiceTaskNumber"
        btnWriting.text = "PÍSACÍ PROTOKOL $writingTaskSaved/$writingTaskNumber"

        btnVoice.setOnClickListener {
            val intent = Intent(this, VoiceTasksListActivity::class.java)
            startActivity(intent)
        }

        btnWriting.setOnClickListener {
            val intent = Intent(this, WritingTasksListActivity::class.java)
            startActivity(intent)
        }

        if(viewModel.getIsUpdated() == true){
            btnUpdate.text = "Aktuálne"
            btnUpdate.isEnabled = false
            btnUpdate.setBackgroundColor(getColor(R.color.green))
        }else{
            btnUpdate.text = "Odoslať"
            btnUpdate.isEnabled = true
            btnUpdate.setBackgroundColor(getColor(R.color.yellow))
        }

//        btnUpdate.setOnClickListener {
//            lifecycleScope.launch(Dispatchers.IO) {
//                try {
//                    if(!viewModel.sendPatientData())
//                        Toast.makeText(this, "Bohužiaľ sa nepodalrilio odoslať všetky údaje, skúste to neskôr.", Toast.LENGTH_LONG).show()
//                } catch (e: Exception) {
//                    Log.e("Update", "Error sending patient data: ${e.message}")
//                    Toast.makeText(this, "Bohužiaľ sa nepodalrilio odoslať všetky údaje, skúste to neskôr.", Toast.LENGTH_LONG).show()
//                }
//                withContext(Dispatchers.Main) {
//                    recreate()
//                }
//            }
//        }

//        btnUpdate.setOnClickListener {
//            viewModel.sendPatientData()
//        }

        btnUpdate.setOnClickListener {
            lifecycleScope.launch {
                val success = viewModel.sendPatientData()

                if (success) {
                    Toast.makeText(this@PatientActivity, "Údaje boli úspešne odoslané.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@PatientActivity, "Bohužiaľ sa nepodarilo odoslať všetky údaje, skúste to neskôr.", Toast.LENGTH_LONG).show()
                }

                recreate()
            }
        }

//        viewModel.sendResult.observe(this) { result ->
//            result.fold(
//                onSuccess = {
//                    Toast.makeText(
//                        this,
//                        "Údaje boli úspešne odoslané.",
//                        Toast.LENGTH_LONG
//                    ).show()
//                    recreate()
//                },
//                onFailure = {
//                    Toast.makeText(
//                        this,
//                        "Bohužiaľ sa nepodarilo odoslať všetky údaje, skúste to neskôr.",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            )
//        }

        btnDrawTest.setOnClickListener {
            val intent = Intent(this, WritingTestActivity::class.java)
            startActivity(intent)
        }
    }

}