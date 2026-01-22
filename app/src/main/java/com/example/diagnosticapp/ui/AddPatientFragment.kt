package com.example.diagnosticapp.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.diagnosticapp.R

class AddPatientFragment : DialogFragment() {

    private lateinit var ageEditText: EditText
    private lateinit var sexSpinner: Spinner
    private lateinit var diseaseSpinner: Spinner
    private lateinit var cancelButton: Button
    private lateinit var addButton: Button

    private var onPatientAddedListener: ((age: Int, sex: String, disease: String) -> Unit)? = null

    companion object {
        fun newInstance(onPatientAdded: (age: Int, sex: String, disease: String) -> Unit): AddPatientFragment {
            return AddPatientFragment().apply {
                this.onPatientAddedListener = onPatientAdded
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_patient, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSpinners()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog wider
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun initViews(view: View) {
        ageEditText = view.findViewById(R.id.ageEditText)
        sexSpinner = view.findViewById(R.id.sexSpinner)
//        diseaseSpinner = view.findViewById(R.id.diseaseSpinner)
        cancelButton = view.findViewById(R.id.cancelButton)
        addButton = view.findViewById(R.id.addButton)
    }

    private fun setupSpinners() {
        // Sex spinner
        val sexOptions = arrayOf("Muž", "Žena")
        val sexAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sexOptions
        )
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sexSpinner.adapter = sexAdapter

        // Disease spinner
//        val diseaseOptions = arrayOf("PD", "AD", "MS", "ALS")
//        val diseaseAdapter = ArrayAdapter(
//            requireContext(),
//            android.R.layout.simple_spinner_item,
//            diseaseOptions
//        )
//        diseaseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        diseaseSpinner.adapter = diseaseAdapter
    }

    private fun setupButtons() {
        cancelButton.setOnClickListener {
            dismiss()
        }

        addButton.setOnClickListener {
            handleAddPatient()
        }
    }

    private fun handleAddPatient() {
        val ageText = ageEditText.text.toString()
        val sex = sexSpinner.selectedItem.toString()
//        val disease = diseaseSpinner.selectedItem.toString()
        val disease = "PD"

        // Validation
        if (ageText.isEmpty()) {
            ageEditText.error = "Vek je povinný"
            Toast.makeText(requireContext(), "Prosím zadajte vek", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageText.toIntOrNull()
        if (age == null || age <= 0 || age > 120) {
            ageEditText.error = "Prosím zadajte platný vek"
            Toast.makeText(requireContext(), "Prosím zadajte platný vek", Toast.LENGTH_SHORT).show()
            return
        }

        // Call the listener
        onPatientAddedListener?.invoke(age, sex, disease)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onPatientAddedListener = null
    }
}