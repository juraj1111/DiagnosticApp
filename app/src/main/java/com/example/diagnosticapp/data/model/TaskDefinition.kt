package com.example.diagnosticapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProtocolDefinition(
    val name: String,
    val type: Int,
    val editable: Boolean,
    val tasks: List<TaskDefinition>
)

@Serializable
data class TaskDefinition(
    val id: String,         // Unique ID ("task1", "task2", etc.)
    val nameResId: Int,     // Resource ID for task name
    val descriptionResId: Int, // Resource ID for task description
    val imageId: Int? = null,           // Resource for the template image
    val type: Int,      //1 hlas, 2 pismo
    val order: Int,         // Task order (1, 2, 3...)
)