package com.example.diagnosticapp.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.diagnosticapp.R
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import com.example.diagnosticapp.data.repository.PatientRepository.uploadFileToNextcloud
import com.example.diagnosticapp.viewmodel.SharedPatientViewModel
//import com.example.diagnosticapp.data.repository.PatientRepository.listFilesFromNextcloud
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var btnNew : Button
    private lateinit var btnExist : Button

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnNew = findViewById(R.id.btn_new_patient);
        btnExist = findViewById(R.id.btn_existing_patient);

        btnNew.setOnClickListener {
            val intent = Intent(this, PatientInfoActivity::class.java)
            startActivity(intent)
        }

        val viewModel : SharedPatientViewModel
        viewModel = ViewModelProvider(this).get(SharedPatientViewModel::class.java)

        viewModel.testDB()

        btnExist.setOnClickListener {
//            val intent = Intent(this, PatientSelectionActivity::class.java)
//            startActivity(intent)
        }


//        val file = File(filesDir, "dummy_upload.txt")
//        file.writeText("Temporary test content")
//        val remotePath = ""
//        Thread {
//            try {
//                val baseUrl = "https://poseidon.fei.tuke.sk/remote.php/dav/files/jBlasko/"
//                val file = File(filesDir, "dummy_upload.txt")
//                file.writeText("Temporary test content")
//                val remotePath = "Doctor1/DiseaseX/Patient001/voice/task1.3gp"
//
//                uploadFileToNextcloud(baseUrl, remotePath, file)
//                Log.d("WebDav", "Upload OK")
//            } catch (e: Exception) {
//                Log.e("WebDav", "Upload error: ${e.message}")
//            }
//        }.start()
    }

}