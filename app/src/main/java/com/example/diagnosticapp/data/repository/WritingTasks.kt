package com.example.diagnosticapp.data.repository

import com.example.diagnosticapp.data.model.WritingTask
import com.example.diagnosticapp.R

object WritingTasks {
    private val tasks = listOf(
        WritingTask(
            id = "task1",
            nameResId = R.string.draw_task_1_name,
            descriptionResId = R.string.draw_task_1_description,
            imageId = R.drawable.img,
            order = 1,
        )
    )

    fun getTaskById(id: String): WritingTask? {
        return tasks.find { it.id == id }
    }

    fun getAllTasks(): List<WritingTask> = tasks.toList()
}
