package com.example.diagnosticapp.data.repository

import com.example.diagnosticapp.data.model.TaskDefinition

object VoiceTasks {

    private val tasks = mutableListOf(
        TaskDefinition(
            id = "task1",
            name = "Dlhá samohláska A",
            description = "Dlhá samohláska A, cca 10 sekúnd na normálnej úrovni.",
            type = 1,
            order = 1
        ),
        TaskDefinition(
            id = "task2",
            name = "Dlhá samohláska A",
            description = "Dlhá samohláska A, cca 10 sekúnd na hlasitej úrovni.",
            type = 1,
            order = 2
        ),
        TaskDefinition(
            id = "task3",
            name = "Dlhá samohláska E",
            description = "Dlhá samohláska E, cca 10 sekúnd na normálnej úrovni.",
            type = 1,
            order = 3
        ),
        TaskDefinition(
            id = "task4",
            name = "Dlhá samohláska E",
            description = "Dlhá samohláska E, cca 10 sekúnd na hlasitej úrovni.",
            type = 1,
            order = 4
        ),
        TaskDefinition(
            id = "task5",
            name = "Opakovať PA-TA-KA",
            description = "Opakovať PA-TA-KA prirodzenou rýchlosťou (5x – 8x).",
            type = 1,
            order = 5
        ),
        TaskDefinition(
            id = "task6",
            name = "Rýchlo opakovať PA-TA-KA",
            description = "Opakovať PA-TA-KA najrýchlejšou možnou rýchlosťou (5x – 8x).",
            type = 1,
            order = 6
        ),
        TaskDefinition(
            id = "task7",
            name = "Opakovať PA-PA-PA",
            description = "Opakovať PA-PA-PA prirodzenou rýchlosťou (5x – 8x).",
            type = 1,
            order = 7
        ),
        TaskDefinition(
            id = "task8",
            name = "Rýchlo opakovať PA-PA-PA",
            description = "Opakovať PA-PA-PA najrýchlejšou možnou rýchlosťou (5x – 8x).",
            type = 1,
            order = 8
        ),
        TaskDefinition(
            id = "task9",
            name = "Čítanie viet",
            description = "Čítať doleuvedené vety. Slovo podčiarknuté a zvýraznené veľkými písmenami zdôrazniť.\n\nDnes sme to už nestihli, ale možno ZAJTRA navštívime všetkých známych.\nAj keď sme spolu telefonovali možno zajtra NAVŠTÍVIME všetkých známych.\nPríbuzných sme už navštívili, možno zajtra navštívime všetkých ZNÁMYCH.",
            type = 1,
            order = 9
        ),
        TaskDefinition(
            id = "task10",
            name = "Čítanie textu",
            description = "Ešte aj koncom 19. storočia domáci aj pocestní prechádzali cez radové typy dedín, aj cez rozptýlené osídlenie...",
            type = 1,
            order = 10
        ),
        TaskDefinition(
            id = "task11",
            name = "Voľná reč",
            description = "Porozprávať niečo o koníčkoch, čomu sa venuje vo voľnom čase, kde pracoval/pracuje.",
            type = 1,
            order = 11
        )
    )

    fun getAllTasks(): MutableList<TaskDefinition> = tasks
    fun getTaskById(id: String): TaskDefinition? = tasks.find { it.id == id }
}
