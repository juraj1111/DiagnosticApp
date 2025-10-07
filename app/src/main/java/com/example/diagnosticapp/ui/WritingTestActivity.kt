package com.example.diagnosticapp.ui

import android.os.Bundle
import android.widget.Button
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.WritingViewModel

class WritingTestActivity : BaseActivity(){

    private lateinit var btnErase : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing_test)

        val writingViewModel = findViewById<WritingViewModel>(R.id.drawingView)
        val btnErase = findViewById<Button>(R.id.btnErase)

        btnErase.setOnClickListener {
            writingViewModel.clear()
        }
//
//        saveButton.setOnClickListener {
//            drawingView.exportToFile(this)
//            Toast.makeText(this, "Saved to SVC file", Toast.LENGTH_SHORT).show()
//        }
    }
}