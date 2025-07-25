package com.example.diagnosticapp.data.repository

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.diagnosticapp.data.model.Patient
import com.example.diagnosticapp.data.model.TaskStatus
import com.example.diagnosticapp.BuildConfig
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

object PatientRepository {

    var currentPatient: Patient? = null

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
        val task = currentPatient?.protocol1Tasks?.find { it.id == taskId }
        if (task != null) {
            task.resultFilePath = filePath
            task.status = TaskStatus.COMPLETED
            isUpdated = false
            Log.d("PatientRepository", "Updated task $taskId with file path: $filePath")
        } else {
            Log.e("PatientRepository", "Voice task $taskId not found.")
        }
    }

//    val files = sardine.list("https://poseidon.fei.tuke.sk/remote.php/dav/files/jBlasko/")
//    files.forEach {
//        Log.d("WebDAV", "Found: ${it.name}")
//    }


    suspend fun sendPatientData() {
        val patient = currentPatient ?: run {
            Log.e("PatientRepository", "No patient exists for sending data.")
            return
        }

        val remoteVoicePath = "${patient.disease}/${patient.id}/voice/"
        for (voiceTask in patient.protocol1Tasks) {
            uploadFileToNextcloud(remoteVoicePath, voiceTask)
        }

        val remoteWritingPath = "${patient.disease}/${patient.id}/writing/"
        for (writingTask in patient.protocol2Tasks) {
            uploadFileToNextcloud(remoteWritingPath, writingTask)
        }

        isUpdated = true
    }


    fun uploadFileToNextcloud(
        remotePath: String,
        task: TaskData
    ) {
        val sardine = OkHttpSardine()
        sardine.setCredentials(BuildConfig.USERNAME, BuildConfig.PASSWORD)
        val baseUrl = BuildConfig.URL

        val filePath: String? = task.resultFilePath
        val fileName: String = task.id

        if (filePath == null) {
            Log.e("PatientRepository", "File path is null. Skipping upload.")
            return
        }

        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            Log.e("PatientRepository", "File does not exist or is unreadable: $filePath")
            return
        }

        val fileBytes = file.readBytes()

        // 1. Separate path into directory and filename
        val fullPath = baseUrl + remotePath + fileName

        // 2. Recursively ensure directories exist
        ensureDirectoriesExist(sardine, baseUrl, remotePath.substringBeforeLast("/"))

        // 3. Upload file
        try {
            sardine.put(fullPath, fileBytes)
            Log.i("PatientRepository", "Successfully uploaded: $fullPath")

            task.status = TaskStatus.SAVED

        } catch (e: SardineException) {
            Log.e("PatientRepository", "Sardine error while uploading $fileName: ${e.message}")
        } catch (e: Exception) {
            Log.e("PatientRepository", "Unexpected error during upload of $fileName: ${e.message}")
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

}