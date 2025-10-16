package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.viewmodel.ProtocolViewModel
import com.example.diagnosticapp.viewmodel.PatientViewModel

class TaskListActivity : BaseActivity() {

    private lateinit var patientViewModel: PatientViewModel
    private lateinit var protocolViewModel : ProtocolViewModel
    private lateinit var gridLayout: GridLayout
    private lateinit var title: EditText
    private lateinit var editButton: ImageButton

    private var protocolData: ProtocolData? = null

    private var isEdited = false

    //private var protocolIndex: Int = 0 // index of protocol in patient's list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks_list)

        patientViewModel = ViewModelProvider(this).get(PatientViewModel::class.java)
        protocolViewModel = ViewModelProvider(this).get(ProtocolViewModel::class.java)

        gridLayout = findViewById(R.id.gridLayoutTasks)
        title = findViewById(R.id.etTitle)
        editButton = findViewById(R.id.btnEdit)

        val patient = patientViewModel.getCurrentPatient() ?: return
        protocolData = patientViewModel.getCurrentProtocol()

        if(!protocolViewModel.getProtocol(protocolData!!.protocolName)?.editable!!){
            editButton.visibility = ImageButton.GONE
        }else{
            editButton.setOnClickListener{
                if(isEdited){
                    isEdited = false
                    editButton.setImageResource(R.drawable.edit_button)
                    setEditable(title, false)
                }else{
                    isEdited = true
                    editButton.setImageResource(R.drawable.edit_off_button)
                    setEditable(title, true)
                }
            }
        }

        setupTasks()
    }

    override fun onResume() {
        super.onResume()
        updateButtonColors()
    }

    private fun setupTasks() {
        gridLayout.removeAllViews()

        if(protocolData != null) {
            if (!protocolViewModel.getProtocol(protocolData!!.protocolName)?.editable!!) {
                title.setText(protocolData!!.protocolName)
                addTaskButtons()
                updateButtonColors()
            }
        }else{
            setEditable(title, true)
            addPlusButton()
        }

    }

    private fun addTaskButtons(){
        val dp = resources.displayMetrics.density
        val margin = (8 * dp).toInt()

        for (taskData in protocolData?.taskDataList!!) {
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
                setOnClickListener { startTaskActivity(taskData.id, taskData.type) }
            }
            gridLayout.addView(button)
        }
    }

    private fun addPlusButton(){
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
            setBackgroundColor(getColor(R.color.gray))
//            setOnClickListener { startTaskActivity(taskData.id, taskData.type) }
        }
        gridLayout.addView(plusButton)
    }

    private fun updateButtonColors() {
        val patient = patientViewModel.getCurrentPatient() ?: return
        //val protocol = patient.protocols.getOrNull(protocolIndex) ?: return
        val protocol = patientViewModel.getCurrentProtocol() ?: return

        for (i in 0 until gridLayout.childCount) {
            val button = gridLayout.getChildAt(i) as Button
            val taskData = protocol.taskDataList.getOrNull(i) ?: continue

            val colorRes = when (taskData.status) {
                TaskStatus.UNCOMPLETED -> R.color.gray
                TaskStatus.COMPLETED -> R.color.orange
                TaskStatus.SAVED -> R.color.green
            }
            button.setBackgroundColor(ContextCompat.getColor(this, colorRes))
        }
    }

    private fun startTaskActivity(taskId: String, taskType: Int) {
        val intent = when (taskType) {
            1 -> Intent(this, VoiceTaskActivity::class.java)
            2 -> Intent(this, WritingTaskActivity::class.java)
            else -> {
                android.widget.Toast.makeText(this, "Unsupported task type: $taskType", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
        }

        intent.putExtra("TASK_ID", taskId)
        //intent.putExtra("PROTOCOL_INDEX", protocolIndex)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, PatientActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    fun setEditable(editText: EditText, editable: Boolean) {
        editText.isFocusable = editable
        editText.isFocusableInTouchMode = editable
        editText.isCursorVisible = editable
        editText.isLongClickable = editable
        editText.isEnabled = editable
    }
}

