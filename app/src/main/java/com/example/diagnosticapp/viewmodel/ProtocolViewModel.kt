package com.example.diagnosticapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskData
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
}