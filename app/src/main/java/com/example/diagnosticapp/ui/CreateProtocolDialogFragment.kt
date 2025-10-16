package com.example.diagnosticapp.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskDefinition
import com.example.diagnosticapp.viewmodel.ProtocolViewModel

class CreateProtocolDialogFragment : DialogFragment() {

    interface OnProtocolCreatedListener {
        fun onProtocolCreated(protocol: ProtocolDefinition)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var viewModel = ViewModelProvider(this).get(ProtocolViewModel::class.java)


        val view = LayoutInflater.from(context).inflate(R.layout.fragment_create_protocol, null)
        val etName = view.findViewById<EditText>(R.id.etProtocolName)
        val rgType = view.findViewById<RadioGroup>(R.id.rgProtocolType)
        val rbVoice = view.findViewById<RadioButton>(R.id.rbVoice)

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(true)
            .create().apply {

                view.findViewById<android.widget.Button>(R.id.btnCreate).setOnClickListener {
                    val name = etName.text.toString().trim()
                    if (name.isEmpty()) {
                        etName.error = "Enter a name"
                        return@setOnClickListener
                    }
                    if (viewModel.getProtocol(name) != null) {
                        etName.error = "Protocol with this name already exists"
                        return@setOnClickListener
                    }

                    val type = if (rgType.checkedRadioButtonId == rbVoice.id) 1 else 2

                    val newProtocol = ProtocolDefinition(
                        name = name,
                        type = type,
                        editable = true,
                        tasks = mutableListOf<TaskDefinition>() // empty for now
                    )

                    (activity as? OnProtocolCreatedListener)?.onProtocolCreated(newProtocol)
                    dismiss()
                }

                view.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener {
                    dismiss()
                }
            }
    }
}
