package com.example.diagnosticapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskDefinition
import com.example.diagnosticapp.viewmodel.ProtocolViewModel

class ProtocolManagerActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var createProtocolButton: Button
    private lateinit var protocolsRecyclerView: RecyclerView

    private lateinit var viewModel: ProtocolViewModel
    private lateinit var protocolsAdapter: AllProtocolsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_protocol_manager)

        viewModel = ViewModelProvider(this)[ProtocolViewModel::class.java]

        initViews()
        setupRecyclerView()
        setupButtons()
        loadProtocols()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        createProtocolButton = findViewById(R.id.createProtocolButton)
        protocolsRecyclerView = findViewById(R.id.protocolsRecyclerView)
    }

    private fun setupRecyclerView() {
        protocolsAdapter = AllProtocolsAdapter(
            onEditClick = { protocol -> onEditProtocol(protocol) },
            onDeleteClick = { protocol -> onDeleteProtocol(protocol) }
        )

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS

        protocolsRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = protocolsAdapter
        }
    }

    private fun setupButtons() {
        backButton.setOnClickListener {
            finish()
        }

        createProtocolButton.setOnClickListener {
            onCreateProtocolClick()
        }
    }

    private fun loadProtocols() {
        val allProtocols = viewModel.getAllProtocols()
        val predefinedProtocols = allProtocols.filter { !it.editable }
        val customProtocols = allProtocols.filter { it.editable }

        protocolsAdapter.updateProtocols(predefinedProtocols, customProtocols)
    }

    private fun onCreateProtocolClick() {
        val dialog = CreateProtocolFragment.newInstance {
            loadProtocols()
        }
        dialog.show(supportFragmentManager, "CreateProtocolDialog")
    }

    private fun onEditProtocol(protocol: ProtocolDefinition) {
        val dialog = CreateProtocolFragment.newInstance(protocol.name) {
            loadProtocols()
        }
        dialog.show(supportFragmentManager, "EditProtocolDialog")
    }

    private fun onDeleteProtocol(protocol: ProtocolDefinition) {
        viewModel.deleteProtocol(protocol)
        loadProtocols()
        Toast.makeText(this, "Protokol odstránený: ${protocol.name}", Toast.LENGTH_SHORT).show()
    }
}

// Adapter with multiple view types
class AllProtocolsAdapter(
    private val onEditClick: (ProtocolDefinition) -> Unit,
    private val onDeleteClick: (ProtocolDefinition) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PROTOCOL = 1
        private const val VIEW_TYPE_EMPTY = 2
    }

    private data class ListItem(
        val type: Int,
        val header: String? = null,
        val protocol: ProtocolDefinition? = null
    )

    private var items = listOf<ListItem>()

    fun updateProtocols(predefined: List<ProtocolDefinition>, custom: List<ProtocolDefinition>) {
        val newItems = mutableListOf<ListItem>()

        // Predefined section
        newItems.add(ListItem(VIEW_TYPE_HEADER, "Preddefinované protokoly"))
        predefined.forEach {
            newItems.add(ListItem(VIEW_TYPE_PROTOCOL, protocol = it))
        }

        // Custom section
        newItems.add(ListItem(VIEW_TYPE_HEADER, "Vlastné protokoly"))
        if (custom.isEmpty()) {
            newItems.add(ListItem(VIEW_TYPE_EMPTY))
        } else {
            custom.forEach {
                newItems.add(ListItem(VIEW_TYPE_PROTOCOL, protocol = it))
            }
        }

        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_protocol_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_EMPTY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_protocol_empty, parent, false)
                EmptyViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_protocol, parent, false)
                ProtocolViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is HeaderViewHolder -> holder.bind(item.header ?: "")
            is ProtocolViewHolder -> item.protocol?.let { holder.bind(it, onEditClick, onDeleteClick) }
            is EmptyViewHolder -> {} // No binding needed
        }

        // Make headers span full width
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = (item.type == VIEW_TYPE_HEADER || item.type == VIEW_TYPE_EMPTY)
        }
    }

    override fun getItemCount() = items.size

    // Header ViewHolder
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.headerText)

        fun bind(text: String) {
            headerText.text = text
        }
    }

    // Empty ViewHolder
    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    // Protocol ViewHolder
    class ProtocolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val protocolNameText: TextView = itemView.findViewById(R.id.protocolNameText)
        private val protocolTaskCountText: TextView = itemView.findViewById(R.id.protocolTaskCountText)
        private val protocolTypeBadge: TextView = itemView.findViewById(R.id.protocolTypeBadge)
        private val editButton: ImageButton = itemView.findViewById(R.id.editProtocolButton)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteProtocolButton)
        private val tasksContainer: LinearLayout = itemView.findViewById(R.id.tasksContainer)

        fun bind(
            protocol: ProtocolDefinition,
            onEditClick: (ProtocolDefinition) -> Unit,
            onDeleteClick: (ProtocolDefinition) -> Unit
        ) {
            protocolNameText.text = protocol.name

            val taskCount = protocol.tasks.size
            protocolTaskCountText.text = if (taskCount == 1) "1 úloha" else "$taskCount úloh"

            if (protocol.editable) {
                protocolTypeBadge.visibility = View.GONE
                editButton.visibility = View.VISIBLE
                deleteButton.visibility = View.VISIBLE

                editButton.setOnClickListener { onEditClick(protocol) }
                deleteButton.setOnClickListener { onDeleteClick(protocol) }
            } else {
                protocolTypeBadge.visibility = View.VISIBLE
                protocolTypeBadge.text = "preddefinovaný"
                editButton.visibility = View.GONE
                deleteButton.visibility = View.GONE
            }

            tasksContainer.removeAllViews()
            protocol.tasks.forEach { task ->
                val taskView = createTaskView(task)
                tasksContainer.addView(taskView)
            }
        }

        private fun createTaskView(task: TaskDefinition): View {
            val taskView = LinearLayout(itemView.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 8
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val nameText = TextView(itemView.context).apply {
                text = task.name
                textSize = 14f
                setTextColor(0xFF666666.toInt())
            }

            taskView.addView(nameText)
            return taskView
        }
    }
}