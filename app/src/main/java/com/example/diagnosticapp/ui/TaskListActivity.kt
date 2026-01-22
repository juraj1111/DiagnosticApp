package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
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
import com.example.diagnosticapp.viewmodel.PatientViewModel
import com.example.diagnosticapp.viewmodel.ProtocolViewModel

class TaskListActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var progressText: TextView
    private lateinit var patientProtocolInfoText: TextView
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
            protocolData = updatedPatient?.protocols?.find {
                it.protocolName == protocolData?.protocolName
            }
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        progressText = findViewById(R.id.progressText)
        patientProtocolInfoText = findViewById(R.id.patientProtocolInfoText)
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
            finish()
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

//package com.example.diagnosticapp.ui
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Button
//import android.widget.EditText
//import android.widget.GridLayout
//import android.widget.ImageButton
//import androidx.core.content.ContextCompat
//import androidx.core.os.bundleOf
//import androidx.lifecycle.ViewModelProvider
//import com.example.diagnosticapp.R
//import com.example.diagnosticapp.data.model.ProtocolData
//import com.example.diagnosticapp.data.model.ProtocolDefinition
//import com.example.diagnosticapp.data.model.TaskData
//import com.example.diagnosticapp.data.model.TaskDefinition
//import com.example.diagnosticapp.data.model.TaskStatus
//import com.example.diagnosticapp.viewmodel.ProtocolViewModel
//import com.example.diagnosticapp.viewmodel.PatientViewModel
//
//class TaskListActivity : BaseActivity(),
//    CreateTaskFragment.TaskCreatedListener,
//    DeleteTaskFragment.TaskDeletedListener {
//
//    private lateinit var patientViewModel: PatientViewModel
//    private lateinit var protocolViewModel: ProtocolViewModel
//    private lateinit var gridLayout: GridLayout
//    private lateinit var title: EditText
//    private lateinit var editButton: ImageButton
//    private lateinit var btnEvaluate: Button
//
//    private var protocolData: ProtocolData? = null
//    private var protocolDef: ProtocolDefinition? = null
//    private var isEdited = false // edit mode flag
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_tasks_list)
//
//        patientViewModel = ViewModelProvider(this).get(PatientViewModel::class.java)
//        protocolViewModel = ViewModelProvider(this).get(ProtocolViewModel::class.java)
//
//        gridLayout = findViewById(R.id.gridLayoutTasks)
//        title = findViewById(R.id.etTitle)
//        editButton = findViewById(R.id.btnEdit)
//        btnEvaluate = findViewById<Button>(R.id.btnEvaluate)
//
//        protocolData = patientViewModel.getCurrentProtocol()
//        protocolDef = protocolViewModel.getProtocol(protocolData!!.protocolName)
//
//        if (protocolDef?.editable == false) {
//            editButton.visibility = ImageButton.GONE
//        } else {
//            editButton.setOnClickListener {
//                if (isEdited) saveTitleChange()
//                toggleEditMode()
//            }
//        }
//
//        setupTasks()
//
//        btnEvaluate.setOnClickListener {
//            val fragment = EvaluationFragment()
//            if(protocolViewModel.getProtocol(patientViewModel.getCurrentProtocol()!!.protocolName)!!.type == 1){
//                fragment.arguments = bundleOf("mode" to "voice")
//            }else{
//                fragment.arguments = bundleOf("mode" to "writing")
//            }
//            fragment.show(supportFragmentManager, "EvaluationFragment")
//        }
//        updateEvaluateButtonState(btnEvaluate)
//
//        patientViewModel.patientLive.observe(this) { updatedPatient ->
//            protocolData = updatedPatient?.protocols?.find { it.protocolName == protocolData?.protocolName }
//            updateButtonColors()
//            updateEvaluateButtonState(btnEvaluate)
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        updateButtonColors()
//    }
//
//    private fun setupTasks() {
//        gridLayout.removeAllViews()
//        title.setText(protocolDef?.name ?: "")
//        addTaskButtons()
//        updateButtonColors()
//    }
//
//    private fun addTaskButtons() {
//        val dp = resources.displayMetrics.density
//        val margin = (8 * dp).toInt()
//
//        protocolDef?.tasks?.forEach { taskData ->
//            val button = Button(this).apply {
//                text = "Úloha ${taskData.id.removePrefix("task")}"
//                textSize = 18f
//                setPadding(8, 8, 8, 8)
//
//                val params = GridLayout.LayoutParams().apply {
//                    width = 0
//                    height = GridLayout.LayoutParams.WRAP_CONTENT
//                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
//                    setMargins(margin, margin, margin, margin)
//                }
//                layoutParams = params
//                setBackgroundColor(getColor(R.color.gray))
//            }
//
//            if (isEdited) {
//                button.setOnClickListener {
//                    // open edit dialog
//                    val editFragment =
//                        CreateTaskFragment.newInstance(taskData.id, editable = true)
//                    editFragment.show(supportFragmentManager, "EditTaskDialog")
//                }
//                button.setOnLongClickListener {
//                    // open delete dialog
//                    val deleteFragment = DeleteTaskFragment.newInstance(taskData.id)
//                    deleteFragment.show(supportFragmentManager, "DeleteTaskDialog")
//                    true
//                }
//            } else {
//                button.setOnClickListener {
//                    startTaskActivity(taskData.id, taskData.type)
//                }
//            }
//
//            gridLayout.addView(button)
//        }
//
//        // Add plus button only in edit mode
//        if (isEdited) addPlusButton()
//    }
//
//    private fun addPlusButton() {
//        val dp = resources.displayMetrics.density
//        val margin = (8 * dp).toInt()
//
//        val plusButton = Button(this).apply {
//            text = "Pridať úlohu"
//            textSize = 18f
//            setPadding(8, 8, 8, 8)
//
//            val params = GridLayout.LayoutParams().apply {
//                width = 0
//                height = GridLayout.LayoutParams.WRAP_CONTENT
//                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
//                setMargins(margin, margin, margin, margin)
//            }
//
//            layoutParams = params
//            setBackgroundColor(getColor(R.color.orange))
//            setOnClickListener {
//                val fragment = CreateTaskFragment()
//                fragment.show(supportFragmentManager, "CreateTaskDialog")
//            }
//        }
//        gridLayout.addView(plusButton)
//    }
//
//    private fun updateButtonColors() {
//        for (i in 0 until gridLayout.childCount) {
//            val view = gridLayout.getChildAt(i)
//            if (view !is Button) continue
//            val taskData = protocolData!!.taskDataList.getOrNull(i) ?: continue
//
//            val colorRes = when (taskData.status) {
//                TaskStatus.UNCOMPLETED -> R.color.gray
//                TaskStatus.COMPLETED -> R.color.orange
//                TaskStatus.SAVED -> R.color.green
//            }
//            view.setBackgroundColor(ContextCompat.getColor(this, colorRes))
//        }
//        updateEvaluateButtonState(btnEvaluate)
//    }
//
//    private fun startTaskActivity(taskId: String, taskType: Int) {
//        if (isEdited) return // prevent in edit mode
//        val intent = when (taskType) {
//            1 -> Intent(this, VoiceTaskActivity::class.java)
//            2 -> Intent(this, WritingTaskActivity::class.java)
//            else -> {
//                android.widget.Toast.makeText(this, "Unsupported task type: $taskType", android.widget.Toast.LENGTH_SHORT).show()
//                return
//            }
//        }
//        intent.putExtra("TASK_ID", taskId)
//        startActivity(intent)
//    }
//
//    override fun onTaskCreated(title: String, description: String, taskId: String?) {
//        val currentProtocol = patientViewModel.getCurrentProtocol() ?: return
//        val currentDef = protocolViewModel.getProtocol(currentProtocol.protocolName) ?: return
//
//        val type = currentDef.type
//
//        if (taskId == null) { //TODO: Ordering doesnt really work after deleting middle task
//            // 🟢 create new
//            val newOrder = (currentDef.tasks.maxOfOrNull { it.order } ?: 0) + 1
//            val newTaskDef = TaskDefinition(
//                id = "task$newOrder",
//                name = title,
//                description = description,
//                type = type,
//                order = newOrder
//            )
//            val newTaskData = TaskData(
//                id = "task$newOrder",
//                status = TaskStatus.UNCOMPLETED,
//                resultFilePath = null,
//                type = type
//            )
//            protocolViewModel.addTaskToCurrentProtocol(newTaskDef)
//        } else {
//            // 🟢 edit existing
//            val taskDef = currentDef.tasks.find { it.id == taskId }
//            if (taskDef != null) {
//                taskDef.name = title
//                taskDef.description = description
//            }
//        }
//        setupTasks()
//    }
//
//    override fun onTaskDeleted(taskId: String) {
//        val currentProtocol = patientViewModel.getCurrentProtocol() ?: return
//        currentProtocol.taskDataList.removeAll { it.id == taskId }
//        protocolViewModel.removeTaskFromCurrentProtocol(taskId)
//        currentProtocol.taskDataList.forEachIndexed { index, task ->
//            task.id = "task${index + 1}"
//        }
//        setupTasks()
//    }
//
//    private fun toggleEditMode() {
//        isEdited = !isEdited
//        editButton.setImageResource(if (isEdited) R.drawable.edit_off_button else R.drawable.edit_button)
//        setEditable(title, isEdited)
//        setupTasks()
//    }
//
//    private fun saveTitleChange() {
//        val newTitle = title.text.toString().trim()
//        val currentProtocol = patientViewModel.getCurrentProtocol() ?: return
//        if (newTitle.isNotEmpty() && newTitle != currentProtocol.protocolName) {
//            val oldName = currentProtocol.protocolName
//            currentProtocol.protocolName = newTitle
//            protocolViewModel.renameProtocol(oldName, newTitle)
//        }
//    }
//
//    fun setEditable(editText: EditText, editable: Boolean) {
//        editText.isFocusable = editable
//        editText.isFocusableInTouchMode = editable
//        editText.isCursorVisible = editable
//        editText.isEnabled = editable
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        val intent = Intent(this, PatientActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
//        finish()
//    }
//
//    private fun updateEvaluateButtonState(btn: Button) {
//        val protocol = patientViewModel.getCurrentProtocol() ?: return
//        if (protocol.taskDataList.size == 0) return
//        val allCompleted = protocol.taskDataList.all { it.status == TaskStatus.SAVED }
//
//        btn.isEnabled = allCompleted
//        btn.setBackgroundColor(
//            ContextCompat.getColor(this, if (allCompleted) R.color.green else R.color.gray)
//        )
//    }
//
//
//
//}
