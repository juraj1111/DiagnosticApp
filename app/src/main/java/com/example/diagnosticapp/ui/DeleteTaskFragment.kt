package com.example.diagnosticapp.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class DeleteTaskFragment : DialogFragment() {

    interface TaskDeletedListener {
        fun onTaskDeleted(taskId: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val taskId = requireArguments().getString("TASK_ID") ?: return super.onCreateDialog(savedInstanceState)

        return AlertDialog.Builder(requireContext())
            .setTitle("Odstrániť úlohu")
            .setMessage("Naozaj chcete odstrániť túto úlohu?")
            .setPositiveButton("Odstrániť") { _, _ ->
                (activity as? TaskDeletedListener)?.onTaskDeleted(taskId)
                dismiss()
            }
            .setNegativeButton("Zrušiť") { _, _ ->
                dismiss()
            }
            .create()
    }

    companion object {
        fun newInstance(taskId: String): DeleteTaskFragment {
            val fragment = DeleteTaskFragment()
            val args = Bundle().apply {
                putString("TASK_ID", taskId)
            }
            fragment.arguments = args
            return fragment
        }
    }

}