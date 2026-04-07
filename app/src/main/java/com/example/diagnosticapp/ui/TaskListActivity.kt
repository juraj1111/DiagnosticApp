package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskData
import com.example.diagnosticapp.data.model.TaskDefinition
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.viewmodel.EvaluationVoiceViewModel
import com.example.diagnosticapp.viewmodel.EvaluationWritingViewModel
import com.example.diagnosticapp.viewmodel.PatientViewModel
import com.example.diagnosticapp.viewmodel.ProtocolViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskListActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var reevaluateButton: android.widget.Button
    private lateinit var progressText: TextView
    private lateinit var patientProtocolInfoText: TextView
    private lateinit var evaluationContainer: LinearLayout
    private lateinit var evaluationStatusBadge: TextView
    private lateinit var tasksRecyclerView: RecyclerView

    private lateinit var patientViewModel: PatientViewModel
    private lateinit var protocolViewModel: ProtocolViewModel
    private lateinit var taskAdapter: TaskAdapter

    private var protocolData: ProtocolData? = null
    private var protocolDef: ProtocolDefinition? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        patientViewModel = ViewModelProvider(this)[PatientViewModel::class.java]
        protocolViewModel = ViewModelProvider(this)[ProtocolViewModel::class.java]

        initViews()
        setupRecyclerView()
        setupButtons()
        loadTasksData()

        // Observe patient changes to update task statuses
        patientViewModel.patientLive.observe(this) { updatedPatient ->

            if (updatedPatient?.id != patientViewModel.getCurrentPatient()?.id) return@observe

            protocolData = updatedPatient?.protocols?.find {
                it.protocolName == protocolData?.protocolName
            }
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        runEvaluationIfNeeded()

        // Retry uploading tasks stuck at COMPLETED (failed because of no internet)
        patientViewModel.syncPendingUploads()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        reevaluateButton = findViewById(R.id.reevaluateButton)
        progressText = findViewById(R.id.progressText)
        patientProtocolInfoText = findViewById(R.id.patientProtocolInfoText)
        evaluationContainer = findViewById(R.id.evaluationContainer)
        evaluationStatusBadge = findViewById(R.id.evaluationStatusBadge)
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter { taskDef, taskData ->
            onTaskSelected(taskDef, taskData)
        }

        tasksRecyclerView.apply {
            layoutManager = GridLayoutManager(this@TaskListActivity, 2)
            adapter = taskAdapter
        }
    }

    private fun setupButtons() {
        backButton.setOnClickListener {
            // Clear the back stack and return to PatientDetailActivity
            val intent = Intent(this, PatientDetailActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        reevaluateButton.setOnClickListener {
            onReevaluateClick()
        }
    }

    private fun loadTasksData() {
        protocolData = patientViewModel.getCurrentProtocol()

        if (protocolData == null) {
            Toast.makeText(this, "Chyba: Protokol nenájdený", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        protocolDef = protocolViewModel.getProtocol(protocolData!!.protocolName)

        if (protocolDef == null) {
            Toast.makeText(this, "Chyba: Definícia protokolu nenájdená", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        updateUI()
    }

    private fun updateUI() {
        val patient = patientViewModel.getCurrentPatient()
        val protocol = protocolData
        val definition = protocolDef

        if (patient == null || protocol == null || definition == null) return

        // Update patient and protocol info
        val patientId = String.format("%04d", patient.id)
        patientProtocolInfoText.text = "Pacient #$patientId • ${protocol.protocolName}"

        // Update progress
        val completedCount = protocol.taskDataList.count {
            it.status == TaskStatus.COMPLETED || it.status == TaskStatus.SAVED
        }
        val totalCount = definition.tasks.size
        progressText.text = "$completedCount / $totalCount úloh dokončených"

        // Enable/disable reevaluate button based on completed tasks
        reevaluateButton.isEnabled = completedCount > 0

        // Update evaluation status display
        val evaluationStatus = protocol.evaluationStatus
        val evaluationProbability = protocol.evaluationProbability

        if (evaluationStatus != null && evaluationProbability != null) {
            evaluationContainer.visibility = View.VISIBLE

            if (evaluationStatus == "Healthy") {
                evaluationStatusBadge.text = "Zdravý (${String.format("%.1f", evaluationProbability * 100)}%)"
                evaluationStatusBadge.setTextColor(0xFF22C55E.toInt())
                evaluationStatusBadge.setBackgroundColor(0xFFDCFCE7.toInt())
            } else {
                evaluationStatusBadge.text = "Choroba (${String.format("%.1f", evaluationProbability * 100)}%)"
                evaluationStatusBadge.setTextColor(0xFFEF4444.toInt())
                evaluationStatusBadge.setBackgroundColor(0xFFFEE2E2.toInt())
            }
        } else {
            evaluationContainer.visibility = View.GONE
        }

        // Update tasks list
        taskAdapter.updateTasks(definition.tasks, protocol.taskDataList)
    }

    private fun onTaskSelected(taskDef: TaskDefinition, taskData: TaskData) {
        val intent = when (taskDef.type) {
            1 -> Intent(this, VoiceTaskActivity::class.java)
            2 -> Intent(this, WritingTaskActivity::class.java)
            else -> {
                Toast.makeText(
                    this,
                    "Nepodporovaný typ úlohy: ${taskDef.type}",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        intent.putExtra("TASK_ID", taskDef.id)
        startActivity(intent)
    }

    private fun runEvaluationIfNeeded() {
        val protocol = protocolData ?: return
        val definition = protocolDef ?: return

        // Check if there are any completed tasks
        val hasCompletedTasks = protocol.taskDataList.any {
            it.status == TaskStatus.COMPLETED || it.status == TaskStatus.SAVED
        }

        if (!hasCompletedTasks) {
            Log.d("TaskListActivity", "No completed tasks, skipping evaluation")
            return
        }

        Log.d("TaskListActivity", "Running automatic evaluation...")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (definition.type) {
                    1 -> runVoiceEvaluation(protocol)
                    2 -> runWritingEvaluation(protocol)
                    else -> Log.w("TaskListActivity", "Unknown protocol type: ${definition.type}")
                }
            } catch (e: Exception) {
                Log.e("TaskListActivity", "Error running evaluation: ${e.message}", e)
            }
        }
    }

    private fun runVoiceEvaluation(protocol: ProtocolData) {
        val viewModel = ViewModelProvider(this)[EvaluationVoiceViewModel::class.java]

        viewModel.evaluatePatient(this) { result ->
            Log.d("TaskListActivity", "Voice evaluation result: $result")
            parseAndSaveEvaluationResult(protocol, result)
        }
    }

    private fun runWritingEvaluation(protocol: ProtocolData) {
        val viewModel = ViewModelProvider(this)[EvaluationWritingViewModel::class.java]

        viewModel.evaluatePatient(this) { result ->
            Log.d("TaskListActivity", "Writing evaluation result: $result")
            parseAndSaveEvaluationResult(protocol, result)
        }
    }

    private fun parseAndSaveEvaluationResult(protocol: ProtocolData, result: String) {
        // Parse result to extract status and probability
        // Expected format: "Pacient má príznaky choroby. (pravdepodobnosť: 85.5%)"
        // or: "Pacient je v norme. (pravdepodobnosť: 92.3%)"

        val isHealthy = result.contains("v norme", ignoreCase = true)
        val isDisease = result.contains("príznaky choroby", ignoreCase = true)

        // Extract probability percentage
        val probabilityRegex = """(\d+[.,]?\d*)%""".toRegex()
        val probabilityMatch = probabilityRegex.find(result)
        val probability = probabilityMatch?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toFloatOrNull()
            ?.div(100f)

        if (isHealthy) {
            protocol.evaluationStatus = "Healthy"
            protocol.evaluationProbability = probability
        } else if (isDisease) {
            protocol.evaluationStatus = "Disease"
            protocol.evaluationProbability = probability
        }

        Log.d("TaskListActivity", "Evaluation parsed: status=${protocol.evaluationStatus}, probability=${protocol.evaluationProbability}")

        // Save updated patient data — with network error handling
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val patient = patientViewModel.getCurrentPatient()
                if (patient != null) {
                    val success = com.example.diagnosticapp.data.repository.PatientRepository.updatePatient(patient)

                    withContext(Dispatchers.Main) {
                        updateUI()
                        if (!success) {
                            Toast.makeText(
                                this@TaskListActivity,
                                "Vyhodnotenie uložené lokálne. Synchronizácia zlyhala.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskListActivity", "Network error saving evaluation: ${e.javaClass.simpleName} — ${e.message}")
                withContext(Dispatchers.Main) {
                    updateUI()
                    Toast.makeText(
                        this@TaskListActivity,
                        "Vyhodnotenie uložené lokálne. Synchronizácia zlyhala.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun onReevaluateClick() {
        val protocol = protocolData ?: return
        val definition = protocolDef ?: return

        // Check if there are any completed tasks
        val hasCompletedTasks = protocol.taskDataList.any {
            it.status == TaskStatus.COMPLETED || it.status == TaskStatus.SAVED
        }

        if (!hasCompletedTasks) {
            Toast.makeText(this, "Žiadne dokončené úlohy na prehodnotenie", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Prehodnocujem s aktívnym modelom...", Toast.LENGTH_SHORT).show()

        Log.d("TaskListActivity", "Manual reevaluation triggered...")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (definition.type) {
                    1 -> runVoiceEvaluation(protocol)
                    2 -> runWritingEvaluation(protocol)
                    else -> {
                        Toast.makeText(
                            this@TaskListActivity,
                            "Neznámy typ protokolu",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("TaskListActivity", "Error during reevaluation: ${e.message}", e)
                Toast.makeText(
                    this@TaskListActivity,
                    "Chyba pri prehodnotení: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

// Adapter for Tasks
class TaskAdapter(
    private val onTaskClick: (TaskDefinition, TaskData) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var taskDefinitions = listOf<TaskDefinition>()
    private var taskDataList = listOf<TaskData>()

    fun updateTasks(definitions: List<TaskDefinition>, dataList: List<TaskData>) {
        this.taskDefinitions = definitions
        this.taskDataList = dataList
        notifyDataSetChanged()
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskIconContainer: LinearLayout = itemView.findViewById(R.id.taskIconContainer)
        val taskNumberText: TextView = itemView.findViewById(R.id.taskNumberText)
        val taskCheckIcon: ImageView = itemView.findViewById(R.id.taskCheckIcon)
        val taskNameText: TextView = itemView.findViewById(R.id.taskNameText)

        fun bind(taskDef: TaskDefinition, position: Int) {
            // Find corresponding task data
            val taskData = taskDataList.find { it.id == taskDef.id }
            val isCompleted = taskData?.status == TaskStatus.COMPLETED ||
                    taskData?.status == TaskStatus.SAVED

            taskNameText.text = taskDef.name

            if (isCompleted) {
                // Show checkmark
                taskNumberText.visibility = View.GONE
                taskCheckIcon.visibility = View.VISIBLE
                taskIconContainer.setBackgroundColor(0xFF22C55E.toInt()) // Green

                // Change card background to green tint
                (itemView as? androidx.cardview.widget.CardView)?.setCardBackgroundColor(
                    0xFFDCFCE7.toInt()
                )
            } else {
                // Show task number
                taskNumberText.visibility = View.VISIBLE
                taskNumberText.text = (position + 1).toString()
                taskCheckIcon.visibility = View.GONE
                taskIconContainer.setBackgroundColor(0xFF2196F3.toInt()) // Blue

                // Default white background
                (itemView as? androidx.cardview.widget.CardView)?.setCardBackgroundColor(
                    0xFFFFFFFF.toInt()
                )
            }

            itemView.setOnClickListener {
                if (taskData != null) {
                    onTaskClick(taskDef, taskData)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(taskDefinitions[position], position)
    }

    override fun getItemCount() = taskDefinitions.size
}