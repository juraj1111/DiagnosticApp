package com.example.diagnosticapp.data.repository

import com.example.diagnosticapp.data.model.VoiceTask
import com.example.diagnosticapp.R

object VoiceTasks {
    private val tasks = listOf(
        VoiceTask(
            id = "task1",
            nameResId = R.string.task_1_name,
            descriptionResId = R.string.task_1_description,
            order = 1,
        ),
        VoiceTask(
            id = "task2",
            nameResId = R.string.task_2_name,
            descriptionResId = R.string.task_2_description,
            order = 2,
        ),
        VoiceTask(
            id = "task3",
            nameResId = R.string.task_3_name,
            descriptionResId = R.string.task_3_description,
            order = 3,
        ),
        VoiceTask(
            id = "task4",
            nameResId = R.string.task_4_name,
            descriptionResId = R.string.task_4_description,
            order = 4,
        ),
        VoiceTask(
            id = "task5",
            nameResId = R.string.task_5_name,
            descriptionResId = R.string.task_5_description,
            order = 5,
        ),
        VoiceTask(
            id = "task6",
            nameResId = R.string.task_6_name,
            descriptionResId = R.string.task_6_description,
            order = 6,
        ),
        VoiceTask(
            id = "task7",
            nameResId = R.string.task_7_name,
            descriptionResId = R.string.task_7_description,
            order = 7,
        ),
        VoiceTask(
            id = "task8",
            nameResId = R.string.task_8_name,
            descriptionResId = R.string.task_8_description,
            order = 8,
        ),
        VoiceTask(
            id = "task9",
            nameResId = R.string.task_9_name,
            descriptionResId = R.string.task_9_description,
            order = 9,
        ),
        VoiceTask(
            id = "task10",
            nameResId = R.string.task_10_name,
            descriptionResId = R.string.task_10_description,
            order = 10,
        ),
        VoiceTask(
            id = "task11",
            nameResId = R.string.task_11_name,
            descriptionResId = R.string.task_11_description,
            order = 11,
        )
    )

    fun getTaskById(id: String): VoiceTask? {
        return tasks.find { it.id == id }
    }

    fun getAllTasks(): List<VoiceTask> = tasks.toList()
}
