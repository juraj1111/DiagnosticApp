package com.example.diagnosticapp.data.repository

import com.example.diagnosticapp.data.model.WritingTask
import com.example.diagnosticapp.R

object WritingTasks {
    private val tasks = listOf(
        WritingTask(
            id = "task1",
            nameResId = R.string.draw_task_1_name,
            descriptionResId = R.string.draw_task_1_description,
            imageId = R.drawable.task1_writing,
            order = 1,
        ),
        WritingTask(
            id = "task2",
            nameResId = R.string.draw_task_2_name,
            descriptionResId = R.string.draw_task_2_description,
            imageId = R.drawable.task1_writing,
            order = 2,
        ),
        WritingTask(
            id = "task3",
            nameResId = R.string.draw_task_3_name,
            descriptionResId = R.string.draw_task_3_description,
            imageId = R.drawable.task3_writing,
            order = 3,
        ),
        WritingTask(
            id = "task4",
            nameResId = R.string.draw_task_4_name,
            descriptionResId = R.string.draw_task_4_description,
            imageId = R.drawable.task3_writing,
            order = 4,
        ),
        WritingTask(
            id = "task5",
            nameResId = R.string.draw_task_5_name,
            descriptionResId = R.string.draw_task_5_description,
            imageId = R.drawable.task5_writing,
            order = 5,
        ),
        WritingTask(
            id = "task6",
            nameResId = R.string.draw_task_6_name,
            descriptionResId = R.string.draw_task_6_description,
            imageId = R.drawable.task6_writing,
            order = 6,
        ),
        WritingTask(
            id = "task7",
            nameResId = R.string.draw_task_7_name,
            descriptionResId = R.string.draw_task_7_description,
            imageId = R.drawable.task7_writing,
            order = 7,
        ),
        WritingTask(
            id = "task8",
            nameResId = R.string.draw_task_8_name,
            descriptionResId = R.string.draw_task_8_description,
            imageId = R.drawable.task8_writing,
            order = 8,
        ),
        WritingTask(
            id = "task9",
            nameResId = R.string.draw_task_9_name,
            descriptionResId = R.string.draw_task_9_description,
            imageId = R.drawable.task9_writing,
            order = 9,
        )
    )

    fun getTaskById(id: String): WritingTask? {
        return tasks.find { it.id == id }
    }

    fun getAllTasks(): List<WritingTask> = tasks.toList()
}
