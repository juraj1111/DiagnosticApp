package com.example.diagnosticapp

import android.app.Application
import com.example.diagnosticapp.data.repository.ModelRepository
import com.example.diagnosticapp.data.repository.ProtocolRepository

class DiagnosticApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ModelRepository.init(this)
        ProtocolRepository.init(this)
    }
}
