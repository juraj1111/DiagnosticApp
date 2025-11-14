package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.viewmodel.PatientViewModel
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskData
import com.example.diagnosticapp.utils.DiseaseMapping
import com.example.diagnosticapp.viewmodel.ProtocolViewModel

class PatientActivity : BaseActivity(), CreateProtocolDialogFragment.OnProtocolCreatedListener, DeleteProtocolFragment.DeleteProtocolListener{

    private lateinit var tvId: TextView
    private lateinit var tvInfo: TextView
    private lateinit var btnWriting: Button
    private lateinit var btnVoice: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnNewProtocol: Button
    private lateinit var btnDrawTest: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var patientViewModel : PatientViewModel
    private lateinit var protocolViewModel : ProtocolViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient)

        patientViewModel = ViewModelProvider(this).get(PatientViewModel::class.java)
        protocolViewModel = ViewModelProvider(this).get(ProtocolViewModel::class.java)
        val patient = patientViewModel.getCurrentPatient()
        val patientId = patient?.id

        progressBar = findViewById(R.id.progressBar)
        tvId = findViewById(R.id.tvId)
        tvInfo = findViewById(R.id.tvInfo)
        btnNewProtocol = findViewById(R.id.btnNewProtocol)
        btnUpdate = findViewById(R.id.btnUpdateData)
        btnDrawTest = findViewById(R.id.btnDrawTest)

        tvId.text = String.format("%04d", patientId)

        if (patient != null) {
            tvInfo.text = "${patient.sex}, ${patient.age} rokov, ${DiseaseMapping.getSlovakName(patient.disease)}"
        }

        setupProtocolButtons()

        btnNewProtocol.setOnClickListener {
            CreateProtocolDialogFragment().show(supportFragmentManager, "CreateProtocolDialog")
        }

        if(patientViewModel.getIsUpdated() == true){
            btnUpdate.text = "Aktuálne"
            btnUpdate.isEnabled = false
            btnUpdate.setBackgroundColor(getColor(R.color.green))
        }else{
            btnUpdate.text = "Odoslať"
            btnUpdate.isEnabled = true
            btnUpdate.setBackgroundColor(getColor(R.color.yellow))
        }

        btnUpdate.setOnClickListener {
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                val success = patientViewModel.sendPatientData()
                progressBar.visibility = View.GONE

                if (success) {
                    Toast.makeText(this@PatientActivity, "Údaje boli úspešne odoslané.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@PatientActivity, "Bohužiaľ sa nepodarilo odoslať všetky údaje, skúste to neskôr.", Toast.LENGTH_LONG).show()
                }

                recreate()
            }
        }

        btnDrawTest.setOnClickListener {
            val intent = Intent(this, WritingTestActivity::class.java)
            startActivity(intent)
        }


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (patientViewModel.getIsUpdated()) {
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
    }

    private fun setupProtocolButtons() {
        val gridLayout = findViewById<GridLayout>(R.id.gridLayoutProtocols)
        gridLayout.removeAllViews()

        val patient = patientViewModel.getCurrentPatient()
        val protocols = protocolViewModel.getAllProtocols()

        for (protocol in protocols) {
            // Count total & saved tasks
            val totalTasks = protocol.tasks.size
            val patientProtocol = patient!!.protocols.find { it.protocolName == protocol.name }
            val savedTasks =  patientProtocol!!.taskDataList.count { it.status == TaskStatus.SAVED }

            // Button label text with progress info
            val label = "${protocol.name.uppercase()}  $savedTasks/$totalTasks"

            // Create button
            val button = Button(this).apply {
                text = label
                textSize = 18f
                isAllCaps = false
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = resources.getDimensionPixelSize(R.dimen.protocol_button_height)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }

                // Optional color differentiation: user-created protocols = blue, predefined = gray
//                val colorRes = if (protocol.editable) R.color.light_blue else R.color.gray
//                setBackgroundColor(getColor(colorRes))

                setOnClickListener {
                    openProtocol(patientProtocol)
                }
            }
            gridLayout.addView(button)

            if(protocol.editable) {
                button.setOnLongClickListener {
                    val fragment = DeleteProtocolFragment.newInstance(protocol.name)
                    fragment.show(supportFragmentManager, "DeleteProtocolDialog")
                    true
                }
            }
        }
    }

    private fun openProtocol(protocol: ProtocolData) {
        patientViewModel.setCurrentProtocol(protocol)
        val intent = Intent(this, TaskListActivity::class.java)
        startActivity(intent)
    }

    override fun onProtocolCreated(protocol: ProtocolDefinition) {
        // Save and add to repository
        protocolViewModel.createNewProtocol(protocol)
        val protocolData = ProtocolData(
            protocolName = protocol.name,
            taskDataList = emptyList<TaskData>().toMutableList()
        )

        patientViewModel.getCurrentPatient()?.protocols?.add(protocolData)
        setupProtocolButtons()
    }

    override fun onProtocolDeleteConfirmed(protocolName: String) {
        protocolViewModel.deleteProtocol(protocolViewModel.getProtocol(protocolName)!!)
        patientViewModel.getCurrentPatient()?.protocols?.removeIf { it.protocolName == protocolName }
        setupProtocolButtons()
    }

}

