package com.example.diagnosticapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskDefinition
import com.example.diagnosticapp.viewmodel.ProtocolViewModel

class CreateProtocolFragment : DialogFragment() {

    private lateinit var dialogTitle: TextView
    private lateinit var dialogDescription: TextView
    private lateinit var protocolNameEditText: EditText
    private lateinit var protocolTypeSpinner: Spinner
    private lateinit var taskCountText: TextView
    private lateinit var taskNameEditText: EditText
    private lateinit var taskDescriptionEditText: EditText
    private lateinit var addTaskButton: Button
    private lateinit var tasksListLabel: TextView
    private lateinit var protocolTasksContainer: LinearLayout
    private lateinit var cancelButton: Button
    private lateinit var saveProtocolButton: Button

    private lateinit var viewModel: ProtocolViewModel
    private var editingProtocol: ProtocolDefinition? = null
    private val tasksList = mutableListOf<TaskDefinition>()
    private var taskCounter = 1

    private var onProtocolSavedListener: (() -> Unit)? = null

    companion object {
        private const val ARG_PROTOCOL_NAME = "protocol_name"

        fun newInstance(
            protocolName: String? = null,
            onProtocolSaved: () -> Unit
        ): CreateProtocolFragment {
            return CreateProtocolFragment().apply {
                arguments = Bundle().apply {
                    protocolName?.let { putString(ARG_PROTOCOL_NAME, it) }
                }
                this.onProtocolSavedListener = onProtocolSaved
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
        viewModel = ViewModelProvider(requireActivity())[ProtocolViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_protocol, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupProtocolTypeSpinner()
        setupButtons()
        loadProtocolData()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun initViews(view: View) {
        dialogTitle = view.findViewById(R.id.dialogTitle)
        dialogDescription = view.findViewById(R.id.dialogDescription)
        protocolNameEditText = view.findViewById(R.id.protocolNameEditText)
        protocolTypeSpinner = view.findViewById(R.id.protocolTypeSpinner)
        taskCountText = view.findViewById(R.id.taskCountText)
        taskNameEditText = view.findViewById(R.id.taskNameEditText)
        taskDescriptionEditText = view.findViewById(R.id.taskDescriptionEditText)
        addTaskButton = view.findViewById(R.id.addTaskButton)
        tasksListLabel = view.findViewById(R.id.tasksListLabel)
        protocolTasksContainer = view.findViewById(R.id.protocolTasksContainer)
        cancelButton = view.findViewById(R.id.cancelButton)
        saveProtocolButton = view.findViewById(R.id.saveProtocolButton)
    }

    private fun setupProtocolTypeSpinner() {
        val typeOptions = arrayOf("Hlasový protokol (1)", "Písací protokol (2)")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            typeOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        protocolTypeSpinner.adapter = adapter
    }

    private fun setupButtons() {
        addTaskButton.setOnClickListener {
            handleAddTask()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        saveProtocolButton.setOnClickListener {
            handleSaveProtocol()
        }
    }

    private fun loadProtocolData() {
        val protocolName = arguments?.getString(ARG_PROTOCOL_NAME)

        protocolName?.let { name ->
            editingProtocol = viewModel.getProtocol(name)
        }

        editingProtocol?.let { protocol ->
            dialogTitle.text = "Upraviť protokol"
            dialogDescription.text = "Upravte detaily protokolu a úlohy."
            saveProtocolButton.text = "Aktualizovať protokol"

            protocolNameEditText.setText(protocol.name)
            protocolTypeSpinner.setSelection(protocol.type - 1) // type 1 or 2 -> index 0 or 1

            tasksList.addAll(protocol.tasks)
            taskCounter = protocol.tasks.size + 1
            updateTasksList()
        }
    }

    private fun handleAddTask() {
        val taskName = taskNameEditText.text.toString().trim()
        val taskDescription = taskDescriptionEditText.text.toString().trim()

        if (taskName.isEmpty()) {
            taskNameEditText.error = "Zadajte názov úlohy"
            Toast.makeText(requireContext(), "Zadajte názov úlohy", Toast.LENGTH_SHORT).show()
            return
        }

        if (taskDescription.isEmpty()) {
            taskDescriptionEditText.error = "Zadajte inštrukcie"
            Toast.makeText(requireContext(), "Zadajte inštrukcie", Toast.LENGTH_SHORT).show()
            return
        }

        // Get protocol type from spinner (1 or 2)
        val protocolType = protocolTypeSpinner.selectedItemPosition + 1

        val newTask = TaskDefinition(
            id = "task_${System.currentTimeMillis()}",
            name = taskName,
            description = taskDescription,
            imageId = null,
            type = protocolType, // Task type matches protocol type
            order = taskCounter++
        )

        tasksList.add(newTask)
        updateTasksList()

        // Clear inputs
        taskNameEditText.text.clear()
        taskDescriptionEditText.text.clear()

        Toast.makeText(requireContext(), "Úloha pridaná", Toast.LENGTH_SHORT).show()
    }

    private fun handleRemoveTask(task: TaskDefinition) {
        tasksList.remove(task)
        updateTasksList()
        Toast.makeText(requireContext(), "Úloha odstránená", Toast.LENGTH_SHORT).show()
    }

    private fun updateTasksList() {
        taskCountText.text = "(${tasksList.size})"

        if (tasksList.isEmpty()) {
            tasksListLabel.visibility = View.GONE
            protocolTasksContainer.visibility = View.GONE
        } else {
            tasksListLabel.visibility = View.VISIBLE
            protocolTasksContainer.visibility = View.VISIBLE
        }

        protocolTasksContainer.removeAllViews()

        tasksList.forEachIndexed { index, task ->
            val taskCard = createTaskCard(task, index + 1)
            protocolTasksContainer.addView(taskCard)
        }
    }

    private fun createTaskCard(task: TaskDefinition, position: Int): View {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            radius = 8f
            cardElevation = 2f
        }

        val contentLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 16, 16, 16)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Task number badge
        val badgeText = TextView(requireContext()).apply {
            text = position.toString()
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            background = resources.getDrawable(android.R.drawable.btn_default, null)
            setBackgroundColor(0xFF2196F3.toInt())
            setPadding(16, 8, 16, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 24
            }
        }

        // Task info layout
        val infoLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = LinearLayout.VERTICAL
        }

        val taskNameLayout = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val taskNameText = TextView(requireContext()).apply {
            text = task.name
            textSize = 16f
            setTextColor(0xFF000000.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
        }

        taskNameLayout.addView(taskNameText)

        val taskDescText = TextView(requireContext()).apply {
            text = task.description
            textSize = 12f
            setTextColor(0xFF999999.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
        }

        infoLayout.addView(taskNameLayout)
        infoLayout.addView(taskDescText)

        // Delete button
        val deleteButton = ImageButton(requireContext()).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            background = null
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { handleRemoveTask(task) }
        }

        contentLayout.addView(badgeText)
        contentLayout.addView(infoLayout)
        contentLayout.addView(deleteButton)
        cardView.addView(contentLayout)

        return cardView
    }

    private fun handleSaveProtocol() {
        val protocolName = protocolNameEditText.text.toString().trim()
        val protocolType = protocolTypeSpinner.selectedItemPosition + 1 // 1 or 2

        if (protocolName.isEmpty()) {
            protocolNameEditText.error = "Zadajte názov protokolu"
            Toast.makeText(requireContext(), "Zadajte názov protokolu", Toast.LENGTH_SHORT).show()
            return
        }

        if (tasksList.isEmpty()) {
            Toast.makeText(requireContext(), "Pridajte aspoň jednu úlohu", Toast.LENGTH_SHORT).show()
            return
        }

        val protocol = ProtocolDefinition(
            name = protocolName,
            type = protocolType,
            editable = true,
            tasks = tasksList.toMutableList()
        )

        // Save using ViewModel
        editingProtocol?.let {
            viewModel.deleteProtocol(editingProtocol!!)
        }
        viewModel.createNewProtocol(protocol)

        Toast.makeText(requireContext(), "Protokol uložený: ${protocol.name}", Toast.LENGTH_SHORT).show()

        // Notify listener
        onProtocolSavedListener?.invoke()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onProtocolSavedListener = null
    }
}