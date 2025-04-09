package com.example.diagnosticapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.diagnosticapp.data.model.Patient
import com.example.diagnosticapp.data.model.TaskData
import com.example.diagnosticapp.data.repository.PatientRepository
import com.example.diagnosticapp.data.repository.VoiceTasks

class SharedPatientViewModel : ViewModel() {

    private val _currentPatient = MutableLiveData<Patient>()
    val currentPatient: LiveData<Patient> = _currentPatient

    fun createNewPatient(age: Int, sex: String) {
        val allTaskData = VoiceTasks
                .getAllTasks()
            .map { voiceTask -> TaskData(id = voiceTask.id) }  // default status is UNCOMPLETED

        val protocol1Tasks = allTaskData.toMutableList()
        val protocol2Tasks = allTaskData.toMutableList()

        val newPatient = Patient(
            id = System.currentTimeMillis().toInt(), // or UUID.randomUUID().toString()
            age = age,
            sex = sex,
            protocol1Tasks = protocol1Tasks,
            protocol2Tasks = protocol2Tasks
        )

        PatientRepository.currentPatient = newPatient
        _currentPatient.value = newPatient  // Update LiveData to observe the new patient
    }

    // Optionally, if you want to expose the current patient, you can create a getter method
    fun getCurrentPatient(): Patient? {
        return PatientRepository.currentPatient
    }

}
