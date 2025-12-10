package com.example.diagnosticapp.ui

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
import com.example.diagnosticapp.viewmodel.EvaluationVoiceViewModel
import com.example.diagnosticapp.viewmodel.EvaluationWritingViewModel

class EvaluationFragment : DialogFragment() {

    private lateinit var viewModel: EvaluationVoiceViewModel
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

        val mode = arguments?.getString("mode", "voice") ?: "voice"
        progressBar.visibility = View.VISIBLE
        tvResult.text = "Prebieha vyhodnocovanie..."

        when (mode) {

            "voice" -> {
                val viewModel = ViewModelProvider(this)
                    .get(EvaluationVoiceViewModel::class.java)

                viewModel.evaluatePatient(requireContext()) { result ->
                    progressBar.visibility = View.GONE
                    tvResult.text = result
                }
            }

            "writing" -> {
                val viewModel = ViewModelProvider(this)
                    .get(EvaluationWritingViewModel::class.java)

                viewModel.evaluatePatient(requireContext()) { result ->
                    progressBar.visibility = View.GONE
                    tvResult.text = result
                }
            }

            else -> {
                progressBar.visibility = View.GONE
                tvResult.text = "Neznámy typ hodnotenia."
            }
        }

        btnClose.setOnClickListener { dismiss() }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            resources.getDimensionPixelSize(R.dimen.dialog_width),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
