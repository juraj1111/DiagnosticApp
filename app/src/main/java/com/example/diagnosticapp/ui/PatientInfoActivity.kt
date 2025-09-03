package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.utils.DiseaseMapping
import com.example.diagnosticapp.viewmodel.SharedPatientViewModel

class PatientInfoActivity : BaseActivity() {

    private lateinit var viewModel: SharedPatientViewModel

    private lateinit var etAge: EditText
    private lateinit var radioSexGroup: RadioGroup
    private lateinit var btnSave: Button
    private lateinit var spinner: Spinner
    private lateinit var progressBar: ProgressBar

    private var selected_disease: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_info)

        progressBar = findViewById(R.id.progressBar)
        etAge = findViewById(R.id.et_age)
        radioSexGroup = findViewById(R.id.radioSexGroup)
        btnSave = findViewById(R.id.btn_save)
        spinner= findViewById(R.id.spinner_options)

        btnSave.setOnClickListener {
            saveDataAndClose()
        }

        val diseaseLabels = DiseaseMapping.map.values.toList()

        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            diseaseLabels
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.textSize = 30f   // selected item text size
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.textSize = 25f   // dropdown item text size
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val slovakName = diseaseLabels[position]
                selected_disease = DiseaseMapping.getEnglishKey(slovakName)  // always English key
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    private fun saveDataAndClose() {
        val age = etAge.text.toString().toIntOrNull()
        val selectedSexId = radioSexGroup.checkedRadioButtonId
        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)

        if (age != null && selectedSexId != -1 && selected_disease != null) {
            val selectedSex = findViewById<RadioButton>(selectedSexId).text.toString()

            progressBar.visibility = View.VISIBLE
            viewModel.createNewPatient(age, selectedSex, selected_disease!!) { success ->
                if (success) {
                    progressBar.visibility = View.GONE
                    val intent = Intent(this, PatientActivity::class.java)
                    startActivity(intent)
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Bohužiaľ nepodarilo sa vytvoriť pacienta.", Toast.LENGTH_SHORT).show()
                }
            }
        }else{
            Toast.makeText(this, "Prosím vyplňte všetky údaje o pacientovi.", Toast.LENGTH_SHORT).show()
        }
    }
}
