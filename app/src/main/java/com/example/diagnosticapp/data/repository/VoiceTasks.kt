package com.example.diagnosticapp.data.repository

import com.example.diagnosticapp.data.model.VoiceTask
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.TaskDefinition

object VoiceTasks {
    private val tasks = listOf(
        TaskDefinition(
            id = "task1",
            nameResId = R.string.task_1_name,
            descriptionResId = R.string.task_1_description,
            type = 1,
            order = 1,
        ),
        TaskDefinition(
            id = "task2",
            nameResId = R.string.task_2_name,
            descriptionResId = R.string.task_2_description,
            type = 1,
            order = 2,
        ),
        TaskDefinition(
            id = "task3",
            nameResId = R.string.task_3_name,
            descriptionResId = R.string.task_3_description,
            type = 1,
            order = 3,
        ),
        TaskDefinition(
            id = "task4",
            nameResId = R.string.task_4_name,
            descriptionResId = R.string.task_4_description,
            type = 1,
            order = 4,
        ),
        TaskDefinition(
            id = "task5",
            nameResId = R.string.task_5_name,
            descriptionResId = R.string.task_5_description,
            type = 1,
            order = 5,
        ),
        TaskDefinition(
            id = "task6",
            nameResId = R.string.task_6_name,
            descriptionResId = R.string.task_6_description,
            type = 1,
            order = 6,
        ),
        TaskDefinition(
            id = "task7",
            nameResId = R.string.task_7_name,
            descriptionResId = R.string.task_7_description,
            type = 1,
            order = 7,
        ),
        TaskDefinition(
            id = "task8",
            nameResId = R.string.task_8_name,
            descriptionResId = R.string.task_8_description,
            type = 1,
            order = 8,
        ),
        TaskDefinition(
            id = "task9",
            nameResId = R.string.task_9_name,
            descriptionResId = R.string.task_9_description,
            type = 1,
            order = 9,
        ),
        TaskDefinition(
            id = "task10",
            nameResId = R.string.task_10_name,
            descriptionResId = R.string.task_10_description,
            type = 1,
            order = 10,
        ),
        TaskDefinition(
            id = "task11",
            nameResId = R.string.task_11_name,
            descriptionResId = R.string.task_11_description,
            type = 1,
            order = 11,
        )
    )

    fun getTaskById(id: String): TaskDefinition? {
        return tasks.find { it.id == id }
    }

    fun getAllTasks(): List<TaskDefinition> = tasks.toList()
}
