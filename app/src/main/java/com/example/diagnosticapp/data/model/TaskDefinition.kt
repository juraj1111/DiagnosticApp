package com.example.diagnosticapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProtocolDefinition(
    var name: String,
    val type: Int,
    val editable: Boolean,
    val tasks: MutableList<TaskDefinition>
)

@Serializable
data class TaskDefinition(
    val id: String,         // Unique ID ("task1", "task2", etc.)
    var name: String,     // Resource ID for task name
    var description: String, // Resource ID for task description
    val imageId: Int? = null,           // Resource for the template image
    val type: Int,      //1 hlas, 2 pismo
    val order: Int,         // Task order (1, 2, 3...)
)