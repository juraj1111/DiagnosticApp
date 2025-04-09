package com.example.diagnosticapp.ui

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.diagnosticapp.R
import android.content.Intent

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

        btnExist.setOnClickListener {
//            val intent = Intent(this, PatientSelectionActivity::class.java)
//            startActivity(intent)
        }
    }

}