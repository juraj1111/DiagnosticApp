package com.example.diagnosticapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.viewmodel.SharedPatientViewModel

class TaskListActivity : BaseActivity() {

    private lateinit var viewModel: SharedPatientViewModel
    private lateinit var gridLayout: GridLayout
    private lateinit var titleView: TextView

    //private var protocolIndex: Int = 0 // index of protocol in patient's list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks_list)

        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)

        gridLayout = findViewById(R.id.gridLayoutTasks)
        titleView = findViewById(R.id.tvTitle)

        //protocolIndex = intent.getIntExtra("PROTOCOL_INDEX", 0)

        setupTasks()
    }

    override fun onResume() {
        super.onResume()
        updateButtonColors()
    }

    private fun setupTasks() {
        val patient = viewModel.getCurrentPatient() ?: return
        //val protocol = patient.protocols.getOrNull(protocolIndex) ?: return
        val protocol = viewModel.getCurrentProtocol() ?: return

        titleView.text = protocol.protocolName

        gridLayout.removeAllViews()

        val dp = resources.displayMetrics.density
        val margin = (8 * dp).toInt()

        // Dynamically create buttons for each task
        for (taskData in protocol.taskDataList) {
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

        updateButtonColors()
    }

    private fun updateButtonColors() {
        val patient = viewModel.getCurrentPatient() ?: return
        //val protocol = patient.protocols.getOrNull(protocolIndex) ?: return
        val protocol = viewModel.getCurrentProtocol() ?: return

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
}

