package com.example.diagnosticapp.data.repository

import com.example.diagnosticapp.data.model.TaskDefinition

object VoiceTasks {

    private val tasks = mutableListOf(
        // Samohláska A (2 fonácie)
        TaskDefinition(
            id = "task1",
            name = "Samohláska A – 1. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku A na jedinom výdychu tak dlho, kým sa vám nevyprázdnia pľúca.",
            type = 1,
            order = 1
        ),
        TaskDefinition(
            id = "task2",
            name = "Samohláska A – 2. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku A po dobu 5 sekúnd.",
            type = 1,
            order = 2
        ),
        // Samohláska E (2 fonácie)
        TaskDefinition(
            id = "task3",
            name = "Samohláska E – 1. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku E na jedinom výdychu tak dlho, kým sa vám nevyprázdnia pľúca.",
            type = 1,
            order = 3
        ),
        TaskDefinition(
            id = "task4",
            name = "Samohláska E – 2. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku E po dobu 5 sekúnd.",
            type = 1,
            order = 4
        ),
        // Samohláska I (2 fonácie)
        TaskDefinition(
            id = "task5",
            name = "Samohláska I – 1. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku I na jedinom výdychu tak dlho, kým sa vám nevyprázdnia pľúca.",
            type = 1,
            order = 5
        ),
        TaskDefinition(
            id = "task6",
            name = "Samohláska I – 2. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku I po dobu 5 sekúnd.",
            type = 1,
            order = 6
        ),
        // Samohláska O (2 fonácie)
        TaskDefinition(
            id = "task7",
            name = "Samohláska O – 1. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku O na jedinom výdychu tak dlho, kým sa vám nevyprázdnia pľúca.",
            type = 1,
            order = 7
        ),
        TaskDefinition(
            id = "task8",
            name = "Samohláska O – 2. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku O po dobu 5 sekúnd.",
            type = 1,
            order = 8
        ),
        // Samohláska U (2 fonácie)
        TaskDefinition(
            id = "task9",
            name = "Samohláska U – 1. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku U na jedinom výdychu tak dlho, kým sa vám nevyprázdnia pľúca.",
            type = 1,
            order = 9
        ),
        TaskDefinition(
            id = "task10",
            name = "Samohláska U – 2. fonácia",
            description = "Zhlboka sa nadýchnite a fonujte samohlásku U po dobu 5 sekúnd.",
            type = 1,
            order = 10
        )
    )

    fun getAllTasks(): MutableList<TaskDefinition> = tasks
    fun getTaskById(id: String): TaskDefinition? = tasks.find { it.id == id }
}