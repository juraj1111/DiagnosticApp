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
import androidx.lifecycle.viewModelScope
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.data.repository.PatientRepository.isUpdated
import kotlinx.coroutines.withContext

class SharedPatientViewModel : ViewModel() {

    private val _currentPatient = MutableLiveData<Patient>()
    val currentPatient: LiveData<Patient> = _currentPatient

    fun testDB(){
        PatientRepository.testWebDAVConnection()
    }


    fun createNewPatient(age: Int, sex: String, onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            val voiceTasksData = VoiceTasks
                .getAllTasks()
                .map { voiceTask -> TaskData(id = voiceTask.id, type = 1) }  // default status is UNCOMPLETED

            val writingTasksData = WritingTasks
                .getAllTasks()
                .map { voiceTask -> TaskData(id = voiceTask.id, type = 2) }  // default status is UNCOMPLETED

            val protocol1Tasks = voiceTasksData.toMutableList()
            val protocol2Tasks = writingTasksData.toMutableList()

            val disease = "encylopathy" //TODO doplanie od pouzivatela
            val path = disease

            val newPatient = Patient(
                id = PatientRepository.getNextPatientId(path).toInt(),
                age = age,
                sex = sex,
                disease = disease,
                protocol1Tasks = protocol1Tasks,
                protocol2Tasks = protocol2Tasks
            )

            PatientRepository.currentPatient = newPatient
            _currentPatient.value = newPatient  // Update LiveData to observe the new patient

            val success =  PatientRepository.updatePatient(newPatient)
            onResult(success)
        }
    }


    fun getCurrentPatient(): Patient? {
        return PatientRepository.currentPatient
    }

    fun createExistingPatient(id: Int, disease: String, onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            val patient: Patient? = PatientRepository.fetchPatient(id, disease)
            PatientRepository.isUpdated = true

            if (patient == null) {
                onResult(false)
                return@launch
            }

            PatientRepository.currentPatient = patient
            //_currentPatient.value = patient
            onResult(true)
        }
    }

    fun getIsUpdated(): Boolean {
        return PatientRepository.isUpdated
    }


//    private val _sendResult = MutableLiveData<Result<Unit>>()
//    val sendResult: LiveData<Result<Unit>> = _sendResult
//
//    fun sendPatientData() {
//        viewModelScope.launch {
//            val result = runCatching { sendPatientDataInternal() }
//            _sendResult.value = result
//        }
//    }
//
//    private suspend fun sendPatientDataInternal(): Unit = withContext(Dispatchers.IO) {
//        val patient = PatientRepository.currentPatient
//            ?: throw IllegalStateException("No patient exists for sending data.")
//
//        var success = true
//
//        val remoteVoicePath = "${patient.disease}/${patient.id}/voice/"
//        for (voiceTask in patient.protocol1Tasks.filter { it.status == TaskStatus.COMPLETED }) {
//            if (!PatientRepository.uploadFileToNextcloud(remoteVoicePath, voiceTask)) {
//                success = false
//            }
//        }
//
//        val remoteWritingPath = "${patient.disease}/${patient.id}/writing/"
//        for (writingTask in patient.protocol2Tasks.filter { it.status == TaskStatus.COMPLETED }) {
//            if (!PatientRepository.uploadFileToNextcloud(remoteWritingPath, writingTask)) {
//                success = false
//            }
//        }
//
//        // update patient.json regardless of partial success
//        PatientRepository.updatePatient(patient)
//
//        if (!success) throw Exception("Partial failure")
//    }

//    fun sendPatientData(): Boolean {
//        val patient = PatientRepository.currentPatient ?: run {
//            Log.e("PatientRepository", "No patient exists for sending data.")
//            return false
//        }
//
//        var success: Boolean = true
//        val remoteVoicePath = "${patient.disease}/${patient.id}/voice/"
//        for (voiceTask in patient.protocol1Tasks) {
//            if(!PatientRepository.uploadFileToNextcloud(remoteVoicePath, voiceTask))
//                success = false
//        }
//
//        val remoteWritingPath = "${patient.disease}/${patient.id}/writing/"
//        for (writingTask in patient.protocol2Tasks) {
//            if(!PatientRepository.uploadFileToNextcloud(remoteWritingPath, writingTask))
//                success = false
//        }
//
//        viewModelScope.launch {
//            PatientRepository.updatePatient(PatientRepository.currentPatient!!)
//        }
//
//        if(success) {
//            isUpdated = true
//            return true
//        }else{
//            return false
//        }
//
//    }

    suspend fun sendPatientData(): Boolean = withContext(Dispatchers.IO) {
        val patient = PatientRepository.currentPatient ?: return@withContext false

        var success = true
        val idFormatted: String = String.format("%04d", patient.id)

        val remoteVoicePath = "${patient.disease}/${idFormatted}/voice/"
        for (voiceTask in patient.protocol1Tasks.filter { it.status == TaskStatus.COMPLETED }) {
            if (!PatientRepository.uploadFileToNextcloud(remoteVoicePath, voiceTask)) {
                success = false
            }
        }

        val remoteWritingPath = "${patient.disease}/${idFormatted}/writing/"
        for (writingTask in patient.protocol2Tasks.filter { it.status == TaskStatus.COMPLETED }) {
            if (!PatientRepository.uploadFileToNextcloud(remoteWritingPath, writingTask)) {
                success = false
            }
        }

        if (success) {
            PatientRepository.updatePatient(patient)
            PatientRepository.isUpdated = true
        }

        success
    }

}
