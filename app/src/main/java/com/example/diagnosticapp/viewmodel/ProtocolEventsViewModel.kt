package com.example.diagnosticapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProtocolEventsViewModel : ViewModel() {
    private val _protocolChanged = MutableLiveData<Unit>()
    val protocolChanged: LiveData<Unit> = _protocolChanged

    fun notifyProtocolChanged() {
        _protocolChanged.value = Unit
    }
}