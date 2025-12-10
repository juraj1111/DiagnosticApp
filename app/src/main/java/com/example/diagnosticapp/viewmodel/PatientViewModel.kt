package com.example.diagnosticapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.diagnosticapp.data.model.Patient
import com.example.diagnosticapp.data.model.TaskData
import com.example.diagnosticapp.data.repository.PatientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.data.repository.ProtocolRepository
import kotlinx.coroutines.withContext
import java.text.Normalizer

class PatientViewModel : ViewModel() {

    private val _currentPatient = MutableLiveData<Patient>()
    val currentPatient: LiveData<Patient> = _currentPatient

    val patientLive: LiveData<Patient?> = PatientRepository.patientLive

    fun testDB(){
        PatientRepository.testWebDAVConnection()
    }

    fun getCurrentProtocol() : ProtocolData? {
        return PatientRepository.currentProtocol
    }

    fun setCurrentProtocol(protocol: ProtocolData) {
        PatientRepository.currentProtocol = protocol
    }

    fun notifyPatientChanged() {
        _currentPatient.value = PatientRepository.currentPatient
    }

    fun createNewPatient(age: Int, sex: String, disease: String, onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            val path = disease

            val nextId: Int = PatientRepository.getNextPatientId(path)
            if(nextId == -1){
                onResult(false)
                return@launch
            }

            val newPatient = Patient(
                id = nextId,
                age = age,
                sex = sex,
                disease = disease,
                protocols = createPatientProtocols()
            )

            PatientRepository.currentPatient = newPatient
            _currentPatient.value = newPatient  // Update LiveData to observe the new patient

            val success =  PatientRepository.updatePatient(newPatient)
            onResult(success)
        }
    }

    fun createPatientProtocols(): MutableList<ProtocolData> {
        return ProtocolRepository.protocols.map { definition ->
            val taskDataList = definition.tasks.map { taskDef ->
                TaskData(
                    id = taskDef.id,
                    status = TaskStatus.UNCOMPLETED,
                    resultFilePath = null,
                    type = taskDef.type
                )
            }.toMutableList()

            ProtocolData(
                protocolName = definition.name,
                taskDataList = taskDataList
            )
        }.toMutableList()
    }


    fun getCurrentPatient(): Patient? {
        return PatientRepository.currentPatient
    }

    fun createExistingPatient(id: Int, disease: String, onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            val patient: Patient? = PatientRepository.fetchPatient(id, disease)

            val protocols = createPatientProtocols()
            for(protocol in protocols){
                if(patient?.protocols?.find { it.protocolName == protocol.protocolName } == null){
                    patient?.protocols?.add(protocol)
                }
            }


            PatientRepository.isUpdated = true

            if (patient == null) {
                onResult(false)
                return@launch
            }

            PatientRepository.currentPatient = patient

            onResult(true)
        }
    }

    fun getIsUpdated(): Boolean {
        return PatientRepository.isUpdated
    }

    suspend fun sendPatientData(): Boolean = withContext(Dispatchers.IO) {
        val patient = PatientRepository.currentPatient ?: return@withContext false

        var success = true
        val idFormatted: String = String.format("%04d", patient.id)

        for (protocol in patient.protocols){
            val sanitizedFolderName = sanitizeFolderName(protocol.protocolName)
            val remoteVoicePath = "${patient.disease}/${idFormatted}/${sanitizedFolderName}/"

            for (task in protocol.taskDataList.filter { it.status == TaskStatus.COMPLETED }) {
                if (!PatientRepository.uploadFileToNextcloud(remoteVoicePath, task)) {
                    success = false
                }
            }
        }

        if (success) {
            PatientRepository.updatePatient(patient)
            PatientRepository.isUpdated = true
        }

        success
    }

    fun sanitizeFolderName(name: String): String {
        // 1. Remove accents (e.g. "Hlasový" → "Hlasovy")
        val normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        // 2. Replace spaces with underscores
        val noSpaces = normalized.replace(" ", "_")

        // 3. Remove unsafe symbols (keep letters, numbers, underscores, and hyphens)
        val safe = noSpaces.replace("[^A-Za-z0-9_\\-]".toRegex(), "")

        return safe
    }

}
