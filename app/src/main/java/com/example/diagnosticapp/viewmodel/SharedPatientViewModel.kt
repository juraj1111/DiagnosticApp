package com.example.diagnosticapp.viewmodel

import android.text.BoringLayout
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.diagnosticapp.data.model.Patient
import com.example.diagnosticapp.data.model.TaskData
import com.example.diagnosticapp.data.repository.PatientRepository
import com.example.diagnosticapp.data.repository.VoiceTasks
import com.example.diagnosticapp.data.repository.WritingTasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope

class SharedPatientViewModel : ViewModel() {

    private val _currentPatient = MutableLiveData<Patient>()
    val currentPatient: LiveData<Patient> = _currentPatient

    fun testDB(){
        PatientRepository.testWebDAVConnection()
    }


    fun createNewPatient(age: Int, sex: String) {
        val voiceTasksData = VoiceTasks
                .getAllTasks()
            .map { voiceTask -> TaskData(id = voiceTask.id) }  // default status is UNCOMPLETED

        val writingTasksData = WritingTasks
            .getAllTasks()
            .map { voiceTask -> TaskData(id = voiceTask.id) }  // default status is UNCOMPLETED

        val protocol1Tasks = voiceTasksData.toMutableList()
        val protocol2Tasks = writingTasksData.toMutableList()

        val newPatient = Patient(
            id = System.currentTimeMillis().toInt(), // or UUID.randomUUID().toString()
            age = age,
            sex = sex,
            disease = "encylopathy", //TODO doplanie od pouzivatela
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

    fun getIsUpdated(): Boolean {
        return PatientRepository.isUpdated
    }



    fun sendPatientData() {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                PatientRepository.sendPatientData()
//            } catch (e: Exception) {
//                Log.e("Update", "Error sending patient data: ${e.message}")
//            }
//        }
    }

}
