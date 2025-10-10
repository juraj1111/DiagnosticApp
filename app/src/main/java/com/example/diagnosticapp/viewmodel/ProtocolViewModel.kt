package com.example.diagnosticapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.repository.ProtocolRepository

class ProtocolViewModel : ViewModel(){
    fun getProtocol(name: String) : ProtocolDefinition{
        val protocol = ProtocolRepository.protocols.find { it.name == name }
        return requireNotNull(protocol) { "Protocol '$name' not found" }
    }


}