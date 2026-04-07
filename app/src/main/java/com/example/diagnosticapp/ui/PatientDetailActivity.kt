package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.viewmodel.PatientViewModel
import com.example.diagnosticapp.viewmodel.ProtocolViewModel

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var patientIdText: TextView
    private lateinit var patientInfoText: TextView
    private lateinit var classificationBadge: TextView
    private lateinit var protocolsRecyclerView: RecyclerView

    private lateinit var patientViewModel: PatientViewModel
    private lateinit var protocolViewModel: ProtocolViewModel
    private lateinit var protocolAdapter: ProtocolSelectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_detail)

        patientViewModel = ViewModelProvider(this)[PatientViewModel::class.java]
        protocolViewModel = ViewModelProvider(this)[ProtocolViewModel::class.java]

        initViews()
        setupRecyclerView()
        setupButtons()
        loadPatientData()
    }

    override fun onResume() {
        super.onResume()
        // Reload patient data to get latest evaluation results
        val patient = patientViewModel.getCurrentPatient()
        if (patient != null) {
            // Reload protocols to show updated evaluation status
            val allProtocolDefinitions = protocolViewModel.getAllProtocols()
            val existingProtocolNames = allProtocolDefinitions.map { it.name }.toSet()

            // Filter patient protocols to only include those that still exist
            val validProtocols = patient.protocols.filter {
                existingProtocolNames.contains(it.protocolName)
            }

            protocolAdapter.updateProtocols(validProtocols, allProtocolDefinitions)
        }

        patientViewModel.syncPendingUploads()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        patientIdText = findViewById(R.id.patientIdText)
        patientInfoText = findViewById(R.id.patientInfoText)
        classificationBadge = findViewById(R.id.classificationBadge)
        protocolsRecyclerView = findViewById(R.id.protocolsRecyclerView)
    }

    private fun setupRecyclerView() {
        protocolAdapter = ProtocolSelectionAdapter { protocolData ->
            onProtocolSelected(protocolData)
        }

        protocolsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PatientDetailActivity)
            adapter = protocolAdapter
        }
    }

    private fun setupButtons() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadPatientData() {
        val patient = patientViewModel.getCurrentPatient()
        
        if (patient == null) {
            Toast.makeText(this, "Chyba: Pacient nenájdený", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Display patient info
        patientIdText.text = "Pacient #${String.format("%04d", patient.id)}"
        patientInfoText.text = "${patient.age} rokov • ${patient.sex}"

        // TODO: Show classification badge if available
        // classificationBadge.visibility = View.VISIBLE
        // classificationBadge.text = "Zdravý"

        // Load protocols
        val allProtocolDefinitions = protocolViewModel.getAllProtocols()
        val existingProtocolNames = allProtocolDefinitions.map { it.name }.toSet()

        // Filter patient protocols to only include those that still exist
        val validProtocols = patient.protocols.filter {
            existingProtocolNames.contains(it.protocolName)
        }

        protocolAdapter.updateProtocols(validProtocols, allProtocolDefinitions)
    }

    private fun onProtocolSelected(protocolData: ProtocolData) {
        patientViewModel.setCurrentProtocol(protocolData)

        // Navigate to task list activity
        val intent = Intent(this, TaskListActivity::class.java)
        startActivity(intent)
    }
}

// Adapter for Protocol Selection
class ProtocolSelectionAdapter(
    private val onProtocolClick: (ProtocolData) -> Unit
) : RecyclerView.Adapter<ProtocolSelectionAdapter.ProtocolViewHolder>() {

    private var protocolsData = listOf<ProtocolData>()
    private var protocolsDefinitions = listOf<ProtocolDefinition>()

    fun updateProtocols(
        protocols: List<ProtocolData>,
        definitions: List<ProtocolDefinition>
    ) {
        this.protocolsData = protocols
        this.protocolsDefinitions = definitions
        notifyDataSetChanged()
    }

    inner class ProtocolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val protocolNameText: TextView = itemView.findViewById(R.id.protocolNameText)
        val protocolTypeBadge1: TextView = itemView.findViewById(R.id.protocolTypeBadge1)
        val protocolTypeBadge2: TextView = itemView.findViewById(R.id.protocolTypeBadge2)
        val protocolProgressText: TextView = itemView.findViewById(R.id.protocolProgressText)
        val evaluationStatusBadge: TextView = itemView.findViewById(R.id.evaluationStatusBadge)
        val evaluationPercentageText: TextView = itemView.findViewById(R.id.evaluationPercentageText)

        fun bind(protocolData: ProtocolData) {
            // Find protocol definition
            val definition = protocolsDefinitions.find { it.name == protocolData.protocolName }

            protocolNameText.text = protocolData.protocolName
            
            // Set type badge
            val typeText1 = when (definition?.editable) {
                false -> "preddefinovaný"
                true -> "vlastný"
                else -> "vlastný"
            }
            val typeText2 = when (definition?.type) {
                1 -> "hlsový"
                2 -> "písací"
                else -> ""
            }
            protocolTypeBadge1.text = typeText1
            protocolTypeBadge2.text = typeText2

            // Calculate progress
            val completedTasks = protocolData.taskDataList.count { 
                it.status == TaskStatus.COMPLETED || it.status == TaskStatus.SAVED 
            }
            val totalTasks = protocolData.taskDataList.size
            protocolProgressText.text = "$completedTasks / $totalTasks úloh dokončených"

            val evaluationStatus = protocolData.evaluationStatus
            val evaluationProbability = protocolData.evaluationProbability

            if (evaluationStatus != null && evaluationProbability != null) {
                evaluationStatusBadge.visibility = View.VISIBLE

                if (evaluationStatus == "Healthy") {
                    evaluationStatusBadge.text = "Zdravý"
                    evaluationStatusBadge.setTextColor(0xFF22C55E.toInt())
                    evaluationStatusBadge.setBackgroundColor(0xFFDCFCE7.toInt())
                } else {
                    evaluationStatusBadge.text = "Choroba"
                    evaluationStatusBadge.setTextColor(0xFFEF4444.toInt())
                    evaluationStatusBadge.setBackgroundColor(0xFFFEE2E2.toInt())
                }
            } else {
                evaluationStatusBadge.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onProtocolClick(protocolData)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProtocolViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_protocol_selection, parent, false)
        return ProtocolViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProtocolViewHolder, position: Int) {
        holder.bind(protocolsData[position])
    }

    override fun getItemCount() = protocolsData.size
}