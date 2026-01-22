package com.example.diagnosticapp.data.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class ProtocolDefinition(
    var name: String,
    val type: Int,
    val editable: Boolean,
    val tasks: MutableList<TaskDefinition>
) : JavaSerializable

@Serializable
data class TaskDefinition(
    val id: String,         // Unique ID ("task1", "task2", etc.)
    var name: String,
    var description: String,
    val imageId: Int? = null,           // Resource for the template image
    val type: Int,      //1 hlas, 2 pismo
    val order: Int,         // Task order (1, 2, 3...)
) : JavaSerializable