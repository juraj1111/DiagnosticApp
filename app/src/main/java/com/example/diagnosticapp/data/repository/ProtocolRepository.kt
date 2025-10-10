package com.example.diagnosticapp.data.repository

import android.content.Context

import com.example.diagnosticapp.data.model.ProtocolDefinition
import com.example.diagnosticapp.data.model.TaskData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ProtocolRepository {
    private val _protocols = mutableListOf<ProtocolDefinition>()
    val protocols: List<ProtocolDefinition> get() = _protocols

    fun init(context: Context) {
        val dir = File(context.filesDir, "protocols")
        if (!dir.exists()) dir.mkdirs()

        _protocols.clear()

        val voiceTasksDefinition = VoiceTasks.getAllTasks()

        val writingTasksDefinition = WritingTasks.getAllTasks()


        val protocol1 = ProtocolDefinition(
            name = "Hlasový protokol",
            type = 1,
            editable = false,
            tasks = voiceTasksDefinition
        )
        val protocol2 = ProtocolDefinition(
            name = "Písací protokol",
            type = 2,
            editable = false,
            tasks = writingTasksDefinition
        )

        _protocols.add(protocol1)
        _protocols.add(protocol2)
        _protocols.addAll(loadUserProtocols(dir))
    }

    private fun loadDefaultProtocols(context: Context): List<ProtocolDefinition> {
        return try {
            val inputStream = context.assets.open("default_protocols.json")
            val json = inputStream.bufferedReader().use { it.readText() }
            Json.decodeFromString(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun loadUserProtocols(dir: File): List<ProtocolDefinition> {
        val list = mutableListOf<ProtocolDefinition>()
        dir.listFiles()?.forEach { file ->
            try {
                val json = file.readText()
                val protocol = Json.decodeFromString<ProtocolDefinition>(json)
                list.add(protocol)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    fun saveUserProtocol(protocol: ProtocolDefinition, context: Context) {
        val dir = File(context.filesDir, "protocols")
        val file = File(dir, "${protocol.name}.json")
        file.writeText(Json.encodeToString(protocol))
        _protocols.add(protocol)
    }
}
