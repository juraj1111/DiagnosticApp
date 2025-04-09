package com.example.diagnosticapp.data.model

data class VoiceTask(
    val id: String,         // Unique ID ("task1", "task2", etc.)
    val nameResId: Int,     // Resource ID for task name
    val descriptionResId: Int, // Resource ID for task description
    val order: Int,         // Task order (1, 2, 3...)
)