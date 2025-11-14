package com.example.diagnosticapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskData
import com.example.diagnosticapp.data.model.TaskDefinition
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.data.repository.PatientRepository
import com.example.diagnosticapp.data.repository.ProtocolRepository

class ProtocolViewModel(application: Application) : AndroidViewModel(application){
    fun getProtocol(name: String) : ProtocolDefinition?{
        return ProtocolRepository.protocols.find { it.name == name }
    }

    fun getAllProtocols() : List<ProtocolDefinition>{
        return ProtocolRepository.protocols

    }

    fun createNewProtocol(protocolDefinition: ProtocolDefinition){
        ProtocolRepository.saveUserProtocol(protocolDefinition, getApplication<Application>().applicationContext)
        Log.d("ProtocolViewModel", "Created new protocol: ${protocolDefinition.name}")

    }

    fun deleteProtocol(protocolDefinition: ProtocolDefinition){
        ProtocolRepository.deleteUserProtocol(protocolDefinition, getApplication<Application>().applicationContext)
    }

    fun addTaskToCurrentProtocol(task: TaskDefinition) {
        val protocolDef = getProtocol(PatientRepository.currentProtocol!!.protocolName)
        val patientProtocol = PatientRepository.currentProtocol

        protocolDef?.tasks?.add(task)

        patientProtocol?.taskDataList?.add(
            TaskData(
                id = task.id,
                status = TaskStatus.UNCOMPLETED,
                resultFilePath = null,
                type = task.type
            )
        )

        ProtocolRepository.saveUserProtocol(
            protocolDef!!,
            getApplication<Application>().applicationContext
        )
    }


    fun removeTaskFromCurrentProtocol(taskId: String) {
        val currentProtocolData = PatientRepository.currentProtocol ?: return
        val currentProtocolDef = ProtocolRepository.protocols.find { it.name == currentProtocolData.protocolName } ?: return

        currentProtocolDef.tasks.removeIf { it.id == taskId }
        currentProtocolData.taskDataList.removeIf { it.id == taskId }

        ProtocolRepository.saveUserProtocol(currentProtocolDef, getApplication<Application>().applicationContext)
    }

    fun renameProtocol(oldName: String, newName: String) {
        ProtocolRepository.renameProtocol(
            oldName,
            newName,
            getApplication<Application>().applicationContext
        )
    }
}