package com.example.diagnosticapp.ui

import android.os.Bundle
import android.widget.Button
import com.example.diagnosticapp.R
import com.example.diagnosticapp.viewmodel.WritingView

class WritingTestActivity : BaseActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing_test)

        val writingViewModel = findViewById<WritingView>(R.id.drawingView)
        val btnErase = findViewById<Button>(R.id.btnErase)

        btnErase.setOnClickListener {
            writingViewModel.clear()
        }
    }
}