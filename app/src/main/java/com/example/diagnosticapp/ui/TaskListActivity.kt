package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskData
import com.example.diagnosticapp.data.model.TaskDefinition
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.viewmodel.ProtocolViewModel
import com.example.diagnosticapp.viewmodel.PatientViewModel

class TaskListActivity : BaseActivity(),
    CreateTaskFragment.TaskCreatedListener,
    DeleteTaskFragment.TaskDeletedListener {

    private lateinit var patientViewModel: PatientViewModel
    private lateinit var protocolViewModel: ProtocolViewModel
    private lateinit var gridLayout: GridLayout
    private lateinit var title: EditText
    private lateinit var editButton: ImageButton
    private lateinit var btnEvaluate: Button

    private var protocolData: ProtocolData? = null
    private var protocolDef: ProtocolDefinition? = null
    private var isEdited = false // edit mode flag

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks_list)

        patientViewModel = ViewModelProvider(this).get(PatientViewModel::class.java)
        protocolViewModel = ViewModelProvider(this).get(ProtocolViewModel::class.java)

        gridLayout = findViewById(R.id.gridLayoutTasks)
        title = findViewById(R.id.etTitle)
        editButton = findViewById(R.id.btnEdit)
        btnEvaluate = findViewById<Button>(R.id.btnEvaluate)

        protocolData = patientViewModel.getCurrentProtocol()
        protocolDef = protocolViewModel.getProtocol(protocolData!!.protocolName)

        if (protocolDef?.editable == false) {
            editButton.visibility = ImageButton.GONE
        } else {
            editButton.setOnClickListener {
                if (isEdited) saveTitleChange()
                toggleEditMode()
            }
        }

        setupTasks()

        btnEvaluate.setOnClickListener {
            val fragment = EvaluationFragment()
            if(protocolViewModel.getProtocol(patientViewModel.getCurrentProtocol()!!.protocolName)!!.type == 1){
                fragment.arguments = bundleOf("mode" to "voice")
            }else{
                fragment.arguments = bundleOf("mode" to "writing")
            }
            fragment.show(supportFragmentManager, "EvaluationFragment")
        }
        updateEvaluateButtonState(btnEvaluate)

        patientViewModel.patientLive.observe(this) { updatedPatient ->
            protocolData = updatedPatient?.protocols?.find { it.protocolName == protocolData?.protocolName }
            updateButtonColors()
            updateEvaluateButtonState(btnEvaluate)
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonColors()
    }

    private fun setupTasks() {
        gridLayout.removeAllViews()
        title.setText(protocolDef?.name ?: "")
        addTaskButtons()
        updateButtonColors()
    }

    private fun addTaskButtons() {
        val dp = resources.displayMetrics.density
        val margin = (8 * dp).toInt()

        protocolDef?.tasks?.forEach { taskData ->
            val button = Button(this).apply {
                text = "Úloha ${taskData.id.removePrefix("task")}"
                textSize = 18f
                setPadding(8, 8, 8, 8)

                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(margin, margin, margin, margin)
                }
                layoutParams = params
                setBackgroundColor(getColor(R.color.gray))
            }

            if (isEdited) {
                button.setOnClickListener {
                    // open edit dialog
                    val editFragment =
                        CreateTaskFragment.newInstance(taskData.id, editable = true)
                    editFragment.show(supportFragmentManager, "EditTaskDialog")
                }
                button.setOnLongClickListener {
                    // open delete dialog
                    val deleteFragment = DeleteTaskFragment.newInstance(taskData.id)
                    deleteFragment.show(supportFragmentManager, "DeleteTaskDialog")
                    true
                }
            } else {
                button.setOnClickListener {
                    startTaskActivity(taskData.id, taskData.type)
                }
            }

            gridLayout.addView(button)
        }

        // Add plus button only in edit mode
        if (isEdited) addPlusButton()
    }

    private fun addPlusButton() {
        val dp = resources.displayMetrics.density
        val margin = (8 * dp).toInt()

        val plusButton = Button(this).apply {
            text = "Pridať úlohu"
            textSize = 18f
            setPadding(8, 8, 8, 8)

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(margin, margin, margin, margin)
            }

            layoutParams = params
            setBackgroundColor(getColor(R.color.orange))
            setOnClickListener {
                val fragment = CreateTaskFragment()
                fragment.show(supportFragmentManager, "CreateTaskDialog")
            }
        }
        gridLayout.addView(plusButton)
    }

    private fun updateButtonColors() {
        for (i in 0 until gridLayout.childCount) {
            val view = gridLayout.getChildAt(i)
            if (view !is Button) continue
            val taskData = protocolData!!.taskDataList.getOrNull(i) ?: continue

            val colorRes = when (taskData.status) {
                TaskStatus.UNCOMPLETED -> R.color.gray
                TaskStatus.COMPLETED -> R.color.orange
                TaskStatus.SAVED -> R.color.green
            }
            view.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        }
        updateEvaluateButtonState(btnEvaluate)
    }

    private fun startTaskActivity(taskId: String, taskType: Int) {
        if (isEdited) return // prevent in edit mode
        val intent = when (taskType) {
            1 -> Intent(this, VoiceTaskActivity::class.java)
            2 -> Intent(this, WritingTaskActivity::class.java)
            else -> {
                android.widget.Toast.makeText(this, "Unsupported task type: $taskType", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
        }
        intent.putExtra("TASK_ID", taskId)
        startActivity(intent)
    }

    override fun onTaskCreated(title: String, description: String, taskId: String?) {
        val currentProtocol = patientViewModel.getCurrentProtocol() ?: return
        val currentDef = protocolViewModel.getProtocol(currentProtocol.protocolName) ?: return

        val type = currentDef.type

        if (taskId == null) { //TODO: Ordering doesnt really work after deleting middle task
            // 🟢 create new
            val newOrder = (currentDef.tasks.maxOfOrNull { it.order } ?: 0) + 1
            val newTaskDef = TaskDefinition(
                id = "task$newOrder",
                name = title,
                description = description,
                type = type,
                order = newOrder
            )
            val newTaskData = TaskData(
                id = "task$newOrder",
                status = TaskStatus.UNCOMPLETED,
                resultFilePath = null,
                type = type
            )
            protocolViewModel.addTaskToCurrentProtocol(newTaskDef)
        } else {
            // 🟢 edit existing
            val taskDef = currentDef.tasks.find { it.id == taskId }
            if (taskDef != null) {
                taskDef.name = title
                taskDef.description = description
            }
        }
        setupTasks()
    }

    override fun onTaskDeleted(taskId: String) {
        val currentProtocol = patientViewModel.getCurrentProtocol() ?: return
        currentProtocol.taskDataList.removeAll { it.id == taskId }
        protocolViewModel.removeTaskFromCurrentProtocol(taskId)
        currentProtocol.taskDataList.forEachIndexed { index, task ->
            task.id = "task${index + 1}"
        }
        setupTasks()
    }

    private fun toggleEditMode() {
        isEdited = !isEdited
        editButton.setImageResource(if (isEdited) R.drawable.edit_off_button else R.drawable.edit_button)
        setEditable(title, isEdited)
        setupTasks()
    }

    private fun saveTitleChange() {
        val newTitle = title.text.toString().trim()
        val currentProtocol = patientViewModel.getCurrentProtocol() ?: return
        if (newTitle.isNotEmpty() && newTitle != currentProtocol.protocolName) {
            val oldName = currentProtocol.protocolName
            currentProtocol.protocolName = newTitle
            protocolViewModel.renameProtocol(oldName, newTitle)
        }
    }

    fun setEditable(editText: EditText, editable: Boolean) {
        editText.isFocusable = editable
        editText.isFocusableInTouchMode = editable
        editText.isCursorVisible = editable
        editText.isEnabled = editable
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, PatientActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun updateEvaluateButtonState(btn: Button) {
        val protocol = patientViewModel.getCurrentProtocol() ?: return
        if (protocol.taskDataList.size == 0) return
        val allCompleted = protocol.taskDataList.all { it.status == TaskStatus.SAVED }

        btn.isEnabled = allCompleted
        btn.setBackgroundColor(
            ContextCompat.getColor(this, if (allCompleted) R.color.green else R.color.gray)
        )
    }



}
