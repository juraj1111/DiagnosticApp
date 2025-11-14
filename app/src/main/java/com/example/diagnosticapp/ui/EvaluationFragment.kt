package com.example.diagnosticapp.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.EvaluationViewModel

class EvaluationFragment : DialogFragment() {

    private lateinit var viewModel: EvaluationViewModel
    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnRun: Button
    private lateinit var btnClose: Button


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_evaluation, container, false)

        tvResult = view.findViewById(R.id.tvResult)
        progressBar = view.findViewById(R.id.progressBarEval)
        btnClose = view.findViewById(R.id.btnCloseEval)

        viewModel = ViewModelProvider(this).get(EvaluationViewModel::class.java)

        progressBar.visibility = View.VISIBLE
        tvResult.text = "Prebieha vyhodnocovanie..."
        viewModel.evaluatePatient(requireContext()) { result ->
            progressBar.visibility = View.GONE
            tvResult.text = result
        }

        btnClose.setOnClickListener { dismiss() }

        return view
    }
}
