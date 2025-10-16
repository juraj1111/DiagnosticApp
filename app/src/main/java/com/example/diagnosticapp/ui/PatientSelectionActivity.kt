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
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.PatientViewModel
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.example.diagnosticapp.utils.DiseaseMapping

class PatientSelectionActivity : BaseActivity() {

    private lateinit var viewModel: PatientViewModel

    private lateinit var etId: EditText
    private lateinit var btnFindPatient: Button
    private lateinit var spinner: Spinner
    private lateinit var progressBar: ProgressBar

    private var selected_disease: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_selection)

        progressBar = findViewById(R.id.progressBar)

        etId = findViewById(R.id.et_id)
        btnFindPatient = findViewById(R.id.btn_find_patient)
        spinner= findViewById(R.id.spinner_options)

        viewModel = ViewModelProvider(this).get(PatientViewModel::class.java)

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
            progressBar.visibility = View.VISIBLE
            viewModel.createExistingPatient(id, disease) { success ->
                if (success) {
                    progressBar.visibility = View.GONE
                    val intent = Intent(this, PatientActivity::class.java)
                    startActivity(intent)
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "Bohužiaľ zadaný pacient nebol nájdený.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val slovakName = diseaseLabels[position]
                selected_disease = DiseaseMapping.getEnglishKey(slovakName)  // always English key
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

}