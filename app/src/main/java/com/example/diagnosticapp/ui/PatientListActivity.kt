package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.PatientIndexEntry
import com.example.diagnosticapp.data.repository.PatientRepository
import com.example.diagnosticapp.viewmodel.PatientViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class PatientListActivity : AppCompatActivity() {

    private lateinit var searchEditText: TextInputEditText
    private lateinit var patientRecyclerView: RecyclerView
    private lateinit var manageProtocolsButton: Button
    private lateinit var addPatientButton: Button
    private lateinit var emptyStateText: TextView
    private lateinit var manageModelsButton: Button

    private lateinit var patientAdapter: PatientAdapter
    private lateinit var viewModel: PatientViewModel

    private var allPatients = listOf<PatientIndexEntry>()
    private var filteredPatients = listOf<PatientIndexEntry>()

    // Current disease type - you might want to pass this as an intent extra
    private var currentDisease: String = "PD" // default, can be changed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_list)

        // Get disease from intent if provided
        currentDisease = intent.getStringExtra("DISEASE") ?: "PD" //TODO: zmenit chorobu

        viewModel = ViewModelProvider(this)[PatientViewModel::class.java]

        initViews()
        setupRecyclerView()
        setupSearchBar()
        setupButtons()
        loadPatients()
    }

    private fun initViews() {
        searchEditText = findViewById(R.id.searchEditText)
        patientRecyclerView = findViewById(R.id.patientRecyclerView)
        manageProtocolsButton = findViewById(R.id.manageProtocolsButton)
        addPatientButton = findViewById(R.id.addPatientButton)
        emptyStateText = findViewById(R.id.emptyStateText)
        manageModelsButton = findViewById(R.id.manageModelsButton)
    }

    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter(
            patients = emptyList(),
            onPatientClick = { patient ->
                onPatientItemClick(patient)
            }
        )

        patientRecyclerView.apply {
            layoutManager = androidx.recyclerview.widget.GridLayoutManager(
                this@PatientListActivity,
                3 // Number of columns
            )
            adapter = patientAdapter
        }
    }

    private fun setupSearchBar() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPatients(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtons() {
        manageProtocolsButton.setOnClickListener {
            onManageProtocolsClick()
        }

        addPatientButton.setOnClickListener {
            onAddPatientClick()
        }

        manageModelsButton.setOnClickListener {
            val intent = Intent(this, ModelManagerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadPatients() {
        lifecycleScope.launch {
            try {
                showEmptyState("Načitávam pacientov...", false)

                val patients = PatientRepository.fetchPatientIndex(currentDisease)
                allPatients = patients
                filteredPatients = patients
                updateRecyclerView()

                if (patients.isEmpty()) {
                    showEmptyState("Nenašiel sa žiaden pacient.\nklikni 'Pridať pacienta' pre vytvorenie nového pacienta.", true)
                } else {
                    hideEmptyState()
                }

                Log.d("PatientListActivity", "Loaded ${patients.size} patients")
            } catch (e: Exception) {
                Log.e("PatientListActivity", "Error loading patients: ${e.message}", e)
                showEmptyState("Pripojenie k serveru neúspešné.\nProsím skontrolujte internetové pripojenie a skúste znova.", true)
                Toast.makeText(
                    this@PatientListActivity,
                    "Načítanie pacientov zlyhalo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun filterPatients(query: String) {
        filteredPatients = if (query.isEmpty()) {
            allPatients
        } else {
            allPatients.filter { patient ->
                patient.id.toString().contains(query, ignoreCase = true)
            }
        }
        updateRecyclerView()

        // Show empty state if no results after filtering
        if (filteredPatients.isEmpty() && query.isNotEmpty()) {
            showEmptyState("Žiaden pacient nezodpovedá '$query'", true)
        } else if (filteredPatients.isEmpty() && allPatients.isEmpty()) {
            showEmptyState("Nenašiel sa žiaden pacient.\nKlikni 'Pridať pacienta' pre vytvorenie nového pacienta.", true)
        } else {
            hideEmptyState()
        }
    }

    private fun updateRecyclerView() {
        patientAdapter.updatePatients(filteredPatients)
    }

    private fun showEmptyState(message: String, showRecyclerView: Boolean) {
        emptyStateText.text = message
        emptyStateText.visibility = View.VISIBLE
        patientRecyclerView.visibility = if (showRecyclerView) View.GONE else View.VISIBLE
    }

    private fun hideEmptyState() {
        emptyStateText.visibility = View.GONE
        patientRecyclerView.visibility = View.VISIBLE
    }

    // Click handlers - to be implemented later
    private fun onPatientItemClick(patient: PatientIndexEntry) {
        // Load the full patient data and navigate to patient details
        lifecycleScope.launch {
            try {
                showEmptyState("Načítavanie údajov pacienta...", false)

                viewModel.createExistingPatient(patient.id, currentDisease) { success ->
                    hideEmptyState()
                    if (success) {
                        Log.d("PatientListActivity", "Patient loaded: ID=${patient.id}")

                        // Navigate to patient details
                        val intent = Intent(this@PatientListActivity, PatientDetailActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@PatientListActivity,
                            "Nepodarilo sa načítať údaje pacienta",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                hideEmptyState()
                Log.e("PatientListActivity", "Error loading patient: ${e.message}", e)
                Toast.makeText(
                    this@PatientListActivity,
                    "Chyba pri načítavaní pacienta",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun onManageProtocolsClick() {
        val intent = Intent(this, ProtocolManagerActivity::class.java)
        startActivity(intent)
    }

    private fun onAddPatientClick() {
        val dialog = AddPatientFragment.newInstance { age, sex, disease ->
            createNewPatient(age, sex, disease)
        }
        dialog.show(supportFragmentManager, "AddPatientDialog")
    }

    private fun createNewPatient(age: Int, sex: String, disease: String) {
        lifecycleScope.launch {
            try {
                showEmptyState("Vytváram pacienta...", false)

                viewModel.createNewPatient(age, sex, disease) { success ->
                    if (success) {
                        Toast.makeText(
                            this@PatientListActivity,
                            "Pacient vytvorený úspešne",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Reload the patient list
                        loadPatients()
                    } else {
                        hideEmptyState()
                        Toast.makeText(
                            this@PatientListActivity,
                            "Vytvorenie pacienta zlyhalo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                hideEmptyState()
                Log.e("PatientListActivity", "Error creating patient: ${e.message}", e)
                Toast.makeText(
                    this@PatientListActivity,
                    "Error creating patient: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

// RecyclerView Adapter
class PatientAdapter(
    private var patients: List<PatientIndexEntry>,
    private val onPatientClick: (PatientIndexEntry) -> Unit
) : RecyclerView.Adapter<PatientAdapter.PatientViewHolder>() {

    inner class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImage: ImageView = itemView.findViewById(R.id.avatarImage)
        val patientIdText: TextView = itemView.findViewById(R.id.patientIdText)
        val patientInfoText: TextView = itemView.findViewById(R.id.patientInfoText)
        val protocolCountText: TextView = itemView.findViewById(R.id.protocolCountText)

        fun bind(patient: PatientIndexEntry) {
            patientIdText.text = "Pacient #${String.format("%04d", patient.id)}"
            patientInfoText.text = "${patient.age} • ${patient.sex}"

            // TODO: Protocol count - can be enhanced later if needed
            protocolCountText.text = ""

            itemView.setOnClickListener {
                onPatientClick(patient)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(patients[position])
    }

    override fun getItemCount(): Int = patients.size

    fun updatePatients(newPatients: List<PatientIndexEntry>) {
        patients = newPatients
        notifyDataSetChanged()
    }
}