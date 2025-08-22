package com.example.diagnosticapp.data.model


import kotlinx.serialization.Serializable

@Serializable
data class Patient(
    val id: Int,   // Unique patient ID
    var age: Int,
    var sex: String,
    var disease: String,
    var protocol1Tasks: MutableList<TaskData>,  // Stores tasks for protocol 1
    var protocol2Tasks: MutableList<TaskData>   // Stores tasks for protocol 2
)