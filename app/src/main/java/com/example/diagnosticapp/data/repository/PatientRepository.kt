package com.example.diagnosticapp.data.repository

import com.example.diagnosticapp.data.model.Patient
import com.example.diagnosticapp.data.model.TaskStatus

object PatientRepository {

    var currentPatient: Patient? = null

    fun updateVoiceTask(taskId: String, filePath: String?) {
        currentPatient?.protocol1Tasks?.find { it.id == taskId }?.apply {
            resultFilePath = filePath
            status = TaskStatus.COMPLETED}
    }

}