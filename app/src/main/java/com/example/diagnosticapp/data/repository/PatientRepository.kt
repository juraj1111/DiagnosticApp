package com.example.diagnosticapp.data.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.collection.emptyLongSet
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.diagnosticapp.data.model.Patient
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.BuildConfig
import com.example.diagnosticapp.data.model.PatientIndexEntry
import com.example.diagnosticapp.data.model.ProtocolData
import com.example.diagnosticapp.data.model.ProtocolDefinition
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

    private val _patientLive = MutableLiveData<Patient?>()
    val patientLive: LiveData<Patient?> = _patientLive

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



    fun updateTask(taskId: String, filePath: String?) {
        val task = currentProtocol?.taskDataList?.find { it.id == taskId }
        if (task == null) {
            Log.e("PatientRepository", "Task $taskId not found.")
            return
        }

        task.resultFilePath = filePath
        task.status = TaskStatus.COMPLETED
        _patientLive.postValue(currentPatient)
        isUpdated = false

        Log.d("PatientRepository", "Updated task $taskId with file path: $filePath")

        CoroutineScope(Dispatchers.IO).launch {
            val patient = currentPatient ?: return@launch
            val idFormatted = String.format("%04d", patient.id)
            val protocolName = currentProtocol?.protocolName ?: "UnknownProtocol"
            val remotePath = "${patient.disease}/$idFormatted/$protocolName/"

            if (uploadFileToNextcloud(remotePath, task)) {
                updatePatient(patient)
                isUpdated = true
                Log.d("PatientRepository", "Auto-sync success for task $taskId")
            } else {
                Log.e("PatientRepository", "Auto-sync failed for task $taskId")
            }
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
            _patientLive.postValue(currentPatient)
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

            // Update the index after successfully saving the patient
            updatePatientIndex(patient)

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

    fun syncCurrentPatientWithProtocol(updatedProtocol: ProtocolDefinition) {
        val patient = currentPatient ?: return
        val protocolData = patient.protocols.find { it.protocolName == updatedProtocol.name } ?: return

        val updatedTaskDataList = mutableListOf<TaskData>()

        // For each task definition in the protocol
        for (taskDef in updatedProtocol.tasks) {
            val existing = protocolData.taskDataList.find { it.id == taskDef.id }
            if (existing != null) {
                updatedTaskDataList.add(existing) // keep old progress
            } else {
                // new task added
                updatedTaskDataList.add(
                    TaskData(
                        id = taskDef.id,
                        status = TaskStatus.UNCOMPLETED,
                        resultFilePath = null,
                        type = taskDef.type
                    )
                )
            }
        }

        // Remove any old tasks no longer present
        protocolData.taskDataList = updatedTaskDataList
    }

    suspend fun fetchPatientIndex(disease: String): List<PatientIndexEntry> =
        withContext(Dispatchers.IO) {
            val sardine = OkHttpSardine().apply {
                setCredentials(BuildConfig.USERNAME, BuildConfig.PASSWORD)
            }

            val indexPath = "${BuildConfig.URL}$disease/patients_index.json"
            Log.d("PatientRepository", "Index path: ${indexPath}")
            if (!sardine.exists(indexPath)) return@withContext emptyList()

            val json = sardine.get(indexPath).readBytes().toString(Charsets.UTF_8)
            Json.decodeFromString(json)
        }


    /**
     * Updates the patient index file by adding or updating a patient entry
     */
    suspend fun updatePatientIndex(patient: Patient): Boolean = withContext(Dispatchers.IO) {
        val sardine = OkHttpSardine()
        sardine.setCredentials(BuildConfig.USERNAME, BuildConfig.PASSWORD)

        val baseUrl = BuildConfig.URL
        val indexPath = "${baseUrl}${patient.disease}/patients_index.json"

        return@withContext try {
            // Fetch existing index or create empty list
            val currentIndex = try {
                if (sardine.exists(indexPath)) {
                    val jsonBytes = sardine.get(indexPath).readBytes()
                    Json.decodeFromString<List<PatientIndexEntry>>(String(jsonBytes)).toMutableList()
                } else {
                    mutableListOf()
                }
            } catch (e: Exception) {
                Log.e("PatientRepository", "Error reading index, creating new: ${e.message}")
                mutableListOf()
            }

            // Create new entry from patient
            val newEntry = PatientIndexEntry(
                id = patient.id,
                age = patient.age,
                sex = patient.sex
            )

            // Remove old entry if exists (update case)
            currentIndex.removeAll { it.id == patient.id }

            // Add new entry
            currentIndex.add(newEntry)

            // Sort by ID for consistency
            currentIndex.sortBy { it.id }

            // Serialize and upload
            val json = Json { prettyPrint = true }.encodeToString(currentIndex)

            // Ensure disease directory exists
            ensureDirectoriesExist(sardine, baseUrl, patient.disease)

            sardine.put(indexPath, json.toByteArray())
            Log.i("PatientRepository", "Successfully updated patient index for ${patient.disease}")
            true
        } catch (e: Exception) {
            Log.e("PatientRepository", "Error updating patient index: ${e.message}", e)
            false
        }
    }
}