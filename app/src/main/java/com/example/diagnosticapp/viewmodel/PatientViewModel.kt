package com.example.diagnosticapp.viewmodel

import android.util.Log
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

    // Replace the existing createExistingPatient function in PatientViewModel

    fun createExistingPatient(id: Int, disease: String, onResult: (Boolean) -> Unit){
        viewModelScope.launch {
            val patient: Patient? = PatientRepository.fetchPatient(id, disease)

            if (patient == null) {
                onResult(false)
                return@launch
            }

            // Get all current protocol definitions
            val currentProtocols = ProtocolRepository.protocols
            val currentProtocolNames = currentProtocols.map { it.name }.toSet()

            // Remove protocols that no longer exist (deleted protocols)
            patient.protocols.removeAll { protocolData ->
                !currentProtocolNames.contains(protocolData.protocolName)
            }

            // Add any new protocols that were created after this patient
            val existingProtocolNames = patient.protocols.map { it.protocolName }.toSet()
            val newProtocols = currentProtocols.filter {
                !existingProtocolNames.contains(it.name)
            }

            newProtocols.forEach { protocolDef ->
                val newProtocolData = ProtocolData(
                    protocolName = protocolDef.name,
                    taskDataList = protocolDef.tasks.map { taskDef ->
                        TaskData(
                            id = taskDef.id,
                            status = TaskStatus.UNCOMPLETED,
                            resultFilePath = null,
                            type = taskDef.type
                        )
                    }.toMutableList()
                )
                patient.protocols.add(newProtocolData)
            }

            PatientRepository.currentPatient = patient
            for (protocolDef in currentProtocols) {
                PatientRepository.syncCurrentPatientWithProtocol(protocolDef)
            }

            PatientRepository.isUpdated = true

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
            val sanitizedFolderName = PatientRepository.sanitizeFolderName(protocol.protocolName)
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

    /**
     * Silently retries uploading any tasks stuck at COMPLETED (not yet SAVED).
     * Call this from onResume() of any activity — it runs in the background
     * and does nothing if there's no pending work or no internet.
     */
    fun syncPendingUploads() {
        val patient = PatientRepository.currentPatient ?: return

        // Quick check: are there any COMPLETED (not SAVED) tasks?
        val hasPending = patient.protocols.any { protocol ->
            protocol.taskDataList.any { it.status == TaskStatus.COMPLETED }
        }
        if (!hasPending) return

        Log.d("PatientViewModel", "Found pending uploads — retrying in background...")

        viewModelScope.launch {
            try {
                val success = sendPatientData()
                if (success) {
                    Log.d("PatientViewModel", "Pending uploads synced successfully")
                } else {
                    Log.w("PatientViewModel", "Some uploads still pending — will retry later")
                }
            } catch (e: Exception) {
                Log.e("PatientViewModel", "Sync retry failed: ${e.javaClass.simpleName} — ${e.message}")
                // Silent failure — will retry on next onResume()
            }
        }
    }

    fun syncPatientProtocols() {
        val patient = PatientRepository.currentPatient ?: return

        // Get all current protocol definitions
        val currentProtocols = ProtocolRepository.protocols
        val currentProtocolNames = currentProtocols.map { it.name }.toSet()

        // Remove protocols that no longer exist
        patient.protocols.removeAll { protocolData ->
            !currentProtocolNames.contains(protocolData.protocolName)
        }

        // Add any new protocols that were created after this patient
        val existingProtocolNames = patient.protocols.map { it.protocolName }.toSet()
        val newProtocols = currentProtocols.filter {
            !existingProtocolNames.contains(it.name)
        }

        newProtocols.forEach { protocolDef ->
            val newProtocolData = ProtocolData(
                protocolName = protocolDef.name,
                taskDataList = protocolDef.tasks.map { taskDef ->
                    TaskData(
                        id = taskDef.id,
                        status = TaskStatus.UNCOMPLETED,
                        resultFilePath = null,
                        type = taskDef.type
                    )
                }.toMutableList()
            )
            patient.protocols.add(newProtocolData)
        }

        _currentPatient.value = patient
    }
}