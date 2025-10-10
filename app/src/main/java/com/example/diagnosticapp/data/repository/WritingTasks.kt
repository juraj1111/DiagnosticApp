package com.example.diagnosticapp.data.repository

import com.example.diagnosticapp.data.model.WritingTask
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.TaskDefinition

object WritingTasks {
    private val tasks = listOf(
        TaskDefinition(
            id = "task1",
            nameResId = R.string.draw_task_1_name,
            descriptionResId = R.string.draw_task_1_description,
            imageId = R.drawable.task1_writing,
            type = 2,
            order = 1,
        ),
        TaskDefinition(
            id = "task2",
            nameResId = R.string.draw_task_2_name,
            descriptionResId = R.string.draw_task_2_description,
            imageId = R.drawable.task1_writing,
            type = 2,
            order = 2,
        ),
        TaskDefinition(
            id = "task3",
            nameResId = R.string.draw_task_3_name,
            descriptionResId = R.string.draw_task_3_description,
            imageId = R.drawable.task3_writing,
            type = 2,
            order = 3,
        ),
        TaskDefinition(
            id = "task4",
            nameResId = R.string.draw_task_4_name,
            descriptionResId = R.string.draw_task_4_description,
            imageId = R.drawable.task3_writing,
            type = 2,
            order = 4,
        ),
        TaskDefinition(
            id = "task5",
            nameResId = R.string.draw_task_5_name,
            descriptionResId = R.string.draw_task_5_description,
            imageId = R.drawable.task5_writing,
            type = 2,
            order = 5,
        ),
        TaskDefinition(
            id = "task6",
            nameResId = R.string.draw_task_6_name,
            descriptionResId = R.string.draw_task_6_description,
            imageId = R.drawable.task6_writing,
            type = 2,
            order = 6,
        ),
        TaskDefinition(
            id = "task7",
            nameResId = R.string.draw_task_7_name,
            descriptionResId = R.string.draw_task_7_description,
            imageId = R.drawable.task7_writing,
            type = 2,
            order = 7,
        ),
        TaskDefinition(
            id = "task8",
            nameResId = R.string.draw_task_8_name,
            descriptionResId = R.string.draw_task_8_description,
            imageId = R.drawable.task8_writing,
            type = 2,
            order = 8,
        ),
        TaskDefinition(
            id = "task9",
            nameResId = R.string.draw_task_9_name,
            descriptionResId = R.string.draw_task_9_description,
            imageId = R.drawable.task9_writing,
            type = 2,
            order = 9,
        )
    )

    fun getTaskById(id: String): TaskDefinition? {
        return tasks.find { it.id == id }
    }

    fun getAllTasks(): List<TaskDefinition> = tasks.toList()
}
