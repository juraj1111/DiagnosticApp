package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.repository.PatientRepository
import com.example.diagnosticapp.viewmodel.SharedPatientViewModel

class PatientInfoActivity : AppCompatActivity() {

    private lateinit var viewModel: SharedPatientViewModel

    private lateinit var etAge: EditText
    private lateinit var radioSexGroup: RadioGroup
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_info)

        etAge = findViewById(R.id.et_age)
        radioSexGroup = findViewById(R.id.radioSexGroup)
        btnSave = findViewById(R.id.btn_save)

        btnSave.setOnClickListener {
            saveDataAndClose()
        }
    }

    private fun saveDataAndClose() {
        val age = etAge.text.toString().toIntOrNull()
        val selectedSexId = radioSexGroup.checkedRadioButtonId
        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)

        if (age != null && selectedSexId != -1) {
            val selectedSex = findViewById<RadioButton>(selectedSexId).text.toString()

            viewModel.createNewPatient(age, selectedSex)

            val intent = Intent(this, ActivityPatient::class.java)
            startActivity(intent)
        }
    }
}
