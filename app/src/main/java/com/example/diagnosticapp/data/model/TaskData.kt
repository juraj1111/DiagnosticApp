package com.example.diagnosticapp.data.model

enum class TaskStatus { UNCOMPLETED, COMPLETED, SAVED }

data class TaskData(
    val id: String,         // Unique identifier ("task1", "task2", etc.)
    var status: TaskStatus = TaskStatus.UNCOMPLETED,  // Pending, Completed, etc.
    var resultFilePath: String? = null  // Path to saved result
)