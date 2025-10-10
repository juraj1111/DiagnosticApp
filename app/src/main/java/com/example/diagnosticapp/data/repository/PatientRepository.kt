package com.example.diagnosticapp.data.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.collection.emptyLongSet
import com.example.diagnosticapp.data.model.Patient
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.BuildConfig
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.TaskData

import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.impl.SardineException;
import com.thegrizzlylabs.sardineandroid.util.SardineUtil;
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.File
import java.io.IOException
import kotlin.math.log

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

object PatientRepository {

    var currentPatient: Patient? = null
    var currentProtocol: ProtocolData? = null

    var isUpdated: Boolean = true

    fun testWebDAVConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            val sardine = OkHttpSardine()
            val username = BuildConfig.USERNAME
            val password = BuildConfig.PASSWORD
            val baseUrl = BuildConfig.URL
            val testUrl = "${baseUrl}remote.php/dav/files/$username/"

            Log.d("WebDAV", "Testing WebDAV connection...")
            Log.d("WebDAV", "Using URL: $testUrl")
            Log.d("WebDAV", "Username: $username")
            Log.d("WebDAV", "Password length: ${password.length}")

            try {
                sardine.setCredentials(username, password)

                val exists = sardine.exists(testUrl)

                Log.i("WebDAV", "✅ Connection successful. Path exists: $exists")
            } catch (e: SardineException) {
                Log.e("WebDAV", "❌ SardineException")
                Log.e("WebDAV", "Status code: ${e.statusCode}")
                Log.e("WebDAV", "Message: ${e.message}")
            } catch (e: Exception) {
                Log.e("WebDAV", "❌ Unexpected Exception: ${e.message}")
                e.printStackTrace()
            }
        }
    }



    fun updateVoiceTask(taskId: String, filePath: String?) {
        val task = currentProtocol?.taskDataList?.find { it.id == taskId }
        if (task != null) {
            task.resultFilePath = filePath
            task.status = TaskStatus.COMPLETED
            isUpdated = false
            Log.d("PatientRepository", "Updated task $taskId with file path: $filePath")

            CoroutineScope(Dispatchers.IO).launch {
                val idFormatted = String.format("%04d", currentPatient!!.id)
                val remotePath = "${currentPatient!!.disease}/$idFormatted/voice/"
                if (uploadFileToNextcloud(remotePath, task)) {
                    updatePatient(currentPatient!!)
                    isUpdated = true
                    Log.d("PatientRepository", "Auto-sync success for task $taskId")
                } else {
                    Log.e("PatientRepository", "Auto-sync failed for task $taskId")
                }
            }
        } else {
            Log.e("PatientRepository", "Voice task $taskId not found.")
        }
    }

    fun updateWritingTask(taskId: String, filePath: String?) {
        val task = currentProtocol?.taskDataList?.find { it.id == taskId }
        if (task != null) {
            task.resultFilePath = filePath
            task.status = TaskStatus.COMPLETED
            isUpdated = false
            Log.d("PatientRepository", "Updated task $taskId with file path: $filePath")

            CoroutineScope(Dispatchers.IO).launch {
                val idFormatted = String.format("%04d", currentPatient!!.id)
                val remotePath = "${currentPatient!!.disease}/$idFormatted/writing/"
                if (uploadFileToNextcloud(remotePath, task)) {
                    updatePatient(currentPatient!!)
                    isUpdated = true
                    Log.d("PatientRepository", "Auto-sync success for task $taskId")
                } else {
                    Log.e("PatientRepository", "Auto-sync failed for task $taskId")
                }
            }

        } else {
            Log.e("PatientRepository", "Voice task $taskId not found.")
        }
    }


    fun uploadFileToNextcloud(remotePath: String, task: TaskData): Boolean {
        val sardine = OkHttpSardine()
        sardine.setCredentials(BuildConfig.USERNAME, BuildConfig.PASSWORD)
        val baseUrl = BuildConfig.URL

        val filePath: String? = task.resultFilePath
        val  fileName: String
        fileName = if(task.type == 1)
            task.id + ".wav"
        else
            task.id + ".svc"

        if (filePath == null) {
            Log.e("PatientRepository", "File path is null. Skipping upload.")
            return false
        }

        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            Log.e("PatientRepository", "File does not exist or is unreadable: $filePath")
            return false
        }

        val fileBytes = file.readBytes()

        // 1. Separate path into directory and filename
        val fullPath = baseUrl + remotePath + fileName

        // 2. Recursively ensure directories exist
        ensureDirectoriesExist(sardine, baseUrl, remotePath.substringBeforeLast("/"))

        // 3. Upload file
        return try {
            sardine.put(fullPath, fileBytes)
            Log.i("PatientRepository", "Successfully uploaded: $fullPath")

            task.status = TaskStatus.SAVED
            true

        } catch (e: SardineException) {
            Log.e("PatientRepository", "Sardine error while uploading $fileName: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("PatientRepository", "Unexpected error during upload of $fileName: ${e.message}")
            false
        }
    }

    fun ensureDirectoriesExist(sardine: OkHttpSardine, baseUrl: String, relativePath: String) {
        val parts = relativePath.split("/").filter { it.isNotEmpty() }
        var currentPath = baseUrl
        for (part in parts) {
            currentPath += "$part/"
            if (!sardine.exists(currentPath)) {
                sardine.createDirectory(currentPath)
            }
        }
    }

    suspend fun getNextPatientId(remotePath: String): Int = withContext(Dispatchers.IO){
        val sardine = OkHttpSardine()
        sardine.setCredentials(BuildConfig.USERNAME, BuildConfig.PASSWORD)

        val baseUrl = BuildConfig.URL
        val fullUrl = baseUrl + remotePath

        try {
            val resources = sardine.list(fullUrl)

            val ids = resources
                .filter { it.isDirectory && it.href != null }
                .mapNotNull { resource ->
                    val dirName = resource.href.toString()
                        .trimEnd('/')
                        .substringAfterLast("/")
                    dirName.toIntOrNull()  // only keep directories that are numbers
                }

            val maxId = ids.maxOrNull() ?: 0
            val nextId = maxId + 1
            nextId
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error getting next patient ID: ${e.message}", e)
            -1 // default if none exist or error occurs
        }
    }

    suspend fun updatePatient(patient: Patient): Boolean = withContext(Dispatchers.IO){
        val sardine = OkHttpSardine()
        sardine.setCredentials(BuildConfig.USERNAME, BuildConfig.PASSWORD)

        val baseUrl = BuildConfig.URL
        val remotePath = patient.disease + "/" + String.format("%04d", patient.id)

        return@withContext try {
            ensureDirectoriesExist(sardine, baseUrl, remotePath)
            val json = Json.encodeToString(patient)
            Log.d("PatientRepository", "Uploading to: $baseUrl$remotePath/patient.json")
            sardine.put("$baseUrl$remotePath/patient.json", json.toByteArray())
            true
        }catch (e: Exception){
            Log.e("PatientRepository", "Error updating patient: ${e.message}", e)
            false
        }
    }

    suspend fun fetchPatient(id: Int, disease: String) : Patient? = withContext(Dispatchers.IO){
        try {
            val sardine = OkHttpSardine().apply {
                setCredentials(BuildConfig.USERNAME, BuildConfig.PASSWORD)
            }

            val remotePath = "${BuildConfig.URL}$disease/${String.format("%04d", id)}"
            val patientFile = "$remotePath/patient.json"

            // check if patient.json exists
            if (!sardine.exists(patientFile)) return@withContext null

            val jsonBytes = sardine.get(patientFile).readBytes()
            Json.decodeFromString<Patient>(String(jsonBytes))
        } catch (e: Exception) {
            Log.e("WebDAV", "Error fetching patient: ${e.message}", e)
            null
        }
    }
}