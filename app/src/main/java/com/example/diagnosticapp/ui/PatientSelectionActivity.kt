package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.SharedPatientViewModel

class PatientSelectionActivity : BaseActivity() {

    private lateinit var viewModel: SharedPatientViewModel

    private lateinit var etId: EditText
    private lateinit var btnFindPatient: Button
    private lateinit var spinner: Spinner

    private var selected_disease: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_selection)

        etId = findViewById(R.id.et_id)
        btnFindPatient = findViewById(R.id.btn_find_patient)
        spinner= findViewById(R.id.spinner_options)

        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)

        btnFindPatient.setOnClickListener{
            val id = etId.text.toString().toIntOrNull()
            val disease = selected_disease

            if (id == null) {
                Toast.makeText(this, "Prosím, zadajte platné ID pacienta.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (disease.isNullOrEmpty()) {
                Toast.makeText(this, "Prosím, vyberte chorobu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ This calls ViewModel, which handles coroutine inside
            viewModel.createExistingPatient(id, disease) { success ->
                if (success) {
                    val intent = Intent(this, PatientActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Bohužiaľ zadaný pacient nebol nájdený.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val diseases = resources.getStringArray(R.array.Diseases)
        val options = listOf("encylopathy") //TODO pridavat choroby
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, diseases)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selected_disease = options[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

}