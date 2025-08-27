package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.SharedPatientViewModel

class PatientInfoActivity : BaseActivity() {

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

//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.title = "Pacient – Info"
    }

//    override fun onSupportNavigateUp(): Boolean {
//        onBackPressed() // behaves same as system back
//        return true
//    }


    private fun saveDataAndClose() {
        val age = etAge.text.toString().toIntOrNull()
        val selectedSexId = radioSexGroup.checkedRadioButtonId
        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)

        if (age != null && selectedSexId != -1) {
            val selectedSex = findViewById<RadioButton>(selectedSexId).text.toString()

            viewModel.createNewPatient(age, selectedSex) { success ->
                if (success) {
                    val intent = Intent(this, PatientActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Bohužiaľ nepodarilo sa vytvoriť pacienta.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
