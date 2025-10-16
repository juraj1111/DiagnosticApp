package com.example.diagnosticapp.data.model

import kotlinx.serialization.Serializable

enum class TaskStatus { UNCOMPLETED, COMPLETED, SAVED }

@Serializable
data class Patient(
    val id: Int,   // Unique patient ID
    var age: Int,
    var sex: String,
    var disease: String,
    var protocols: MutableList<ProtocolData>
)

@Serializable
data class ProtocolData(
    val protocolName: String,
    val taskDataList: MutableList<TaskData>
)

@Serializable
data class TaskData(
    val id: String,         // Unique identifier ("task1", "task2", etc.)
    var status: TaskStatus = TaskStatus.UNCOMPLETED,  // Pending, Completed, etc.
    var resultFilePath: String? = null,  // Path to saved result
    var type: Int //typ 1 pre hlas, typ 2 pre pismo
)