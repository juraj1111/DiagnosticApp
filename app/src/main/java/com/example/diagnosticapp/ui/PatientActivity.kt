package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.example.diagnosticapp.utils.DiseaseMapping

class PatientActivity : BaseActivity(){

    private lateinit var tvId: TextView
    private lateinit var tvInfo: TextView
    private lateinit var btnWriting: Button
    private lateinit var btnVoice: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnDrawTest: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var viewModel : SharedPatientViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient)

        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)
        val patient = viewModel.getCurrentPatient()
        val patientId = patient?.id
        val voiceTaskNumber = patient?.protocols?.get(0)?.taskDataList?.size
        val voiceTaskSaved = patient?.protocols?.get(0)?.taskDataList?.count({ it.status == TaskStatus.SAVED })
        val writingTaskNumber = patient?.protocols?.get(1)?.taskDataList?.size
        val writingTaskSaved = patient?.protocols?.get(1)?.taskDataList?.count { it.status == TaskStatus.SAVED }

        progressBar = findViewById(R.id.progressBar)
        tvId = findViewById(R.id.tvId)
        tvInfo = findViewById(R.id.tvInfo)
        btnWriting = findViewById(R.id.btnWritingProtocol)
        btnVoice = findViewById(R.id.btnVoiceProtocol)
        btnUpdate = findViewById(R.id.btnUpdateData)
        btnDrawTest = findViewById(R.id.btnDrawTest)

        tvId.text = String.format("%04d", patientId)

        if (patient != null) {
            tvInfo.text = "${patient.sex}, ${patient.age} rokov, ${DiseaseMapping.getSlovakName(patient.disease)}"
        }

        btnVoice.text = "HLASOVÝ PROTOKOL $voiceTaskSaved/$voiceTaskNumber"
        btnWriting.text = "PÍSACÍ PROTOKOL $writingTaskSaved/$writingTaskNumber"

        btnVoice.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            if (patient != null) {
                viewModel.setCurrentProtocol(patient.protocols[0])
            }
            startActivity(intent)
        }

        btnWriting.setOnClickListener {
            val intent = Intent(this, TaskListActivity::class.java)
            if (patient != null) {
                viewModel.setCurrentProtocol(patient.protocols[1])
            }
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
                progressBar.visibility = View.VISIBLE
                val success = viewModel.sendPatientData()
                progressBar.visibility = View.GONE

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


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                if (viewModel.getIsUpdated()) {
//                    val intent = Intent(this@PatientActivity, MainActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                    startActivity(intent)
//                    finish()
//                } else {
//                    Toast.makeText(
//                        this@PatientActivity,
//                        "Prosím uložte dáta pred odchodom.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
            override fun handleOnBackPressed() {
                if (viewModel.getIsUpdated()) {
                    // Safe to leave directly
                    val intent = Intent(this@PatientActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // Show a confirmation dialog
                    AlertDialog.Builder(this@PatientActivity)
                        .setTitle("Neuložené dáta")
                        .setMessage("Naozaj chcete odísť bez uloženia?")
                        .setPositiveButton("Áno") { _, _ ->
                            // User confirmed → leave anyway
                            val intent = Intent(this@PatientActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Nie") { dialog, _ ->
                            // Just dismiss and stay on screen
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        })


//        findViewById<ImageButton?>(R.id.btnBack)?.setOnClickListener {
//            if (viewModel.isPatientSaved()) {
//                finish()
//            } else {
//                Toast.makeText(
//                    this@PatientInfoActivity,
//                    "Please save first.",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }

    }

}

