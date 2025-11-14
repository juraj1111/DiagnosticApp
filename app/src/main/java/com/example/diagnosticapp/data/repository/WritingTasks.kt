package com.example.diagnosticapp.data.repository

import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.TaskDefinition

object WritingTasks {

    private val tasks = mutableListOf(
        TaskDefinition(
            id = "task1",
            name = "Písanie \"l\"",
            description = "5 opakovaní podľa predlohy.",
            imageId = R.drawable.task1_writing,
            type = 2,
            order = 1
        ),
        TaskDefinition(
            id = "task2",
            name = "Písanie \"l\"",
            description = "5 opakovaní podľa predlohy. Úloha má byť písaná čo najvyššou rýchlosťou, ale tak aby bol text zrozumiteľný.",
            imageId = R.drawable.task1_writing,
            type = 2,
            order = 2
        ),
        TaskDefinition(
            id = "task3",
            name = "Písanie \"le\"",
            description = "5 opakovaní podľa predlohy.",
            imageId = R.drawable.task3_writing,
            type = 2,
            order = 3
        ),
        TaskDefinition(
            id = "task4",
            name = "Písanie \"le\"",
            description = "5 opakovaní podľa predlohy. Úloha má byť písaná čo najvyššou rýchlosťou, ale tak aby bol text zrozumiteľný.",
            imageId = R.drawable.task3_writing,
            type = 2,
            order = 4
        ),
        TaskDefinition(
            id = "task5",
            name = "Písanie \"leto\"",
            description = "5 opakovaní podľa predlohy.",
            imageId = R.drawable.task5_writing,
            type = 2,
            order = 5
        ),
        TaskDefinition(
            id = "task6",
            name = "Písanie \"lamoken\"",
            description = "3 opakovania podľa predlohy.",
            imageId = R.drawable.task6_writing,
            type = 2,
            order = 6
        ),
        TaskDefinition(
            id = "task7",
            name = "Písanie \"interpunkcia\"",
            description = "2–3 opakovania podľa predlohy.",
            imageId = R.drawable.task7_writing,
            type = 2,
            order = 7
        ),
        TaskDefinition(
            id = "task8",
            name = "Písanie \"V lete bude teplo a sucho\"",
            description = "1 opakovanie podľa predlohy.",
            imageId = R.drawable.task8_writing,
            type = 2,
            order = 8
        ),
        TaskDefinition(
            id = "task9",
            name = "Kreslenie čiary",
            description = "Nakresliť vodorovnú čiaru na šírku papiera.",
            imageId = R.drawable.task9_writing,
            type = 2,
            order = 9
        )
    )

    fun getAllTasks(): MutableList<TaskDefinition> = tasks
    fun getTaskById(id: String): TaskDefinition? = tasks.find { it.id == id }
}
