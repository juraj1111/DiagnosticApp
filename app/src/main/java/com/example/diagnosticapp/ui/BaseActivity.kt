package com.example.diagnosticapp.ui

import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.diagnosticapp.R

open class BaseActivity : AppCompatActivity() {

    override fun setContentView(layoutResID: Int) {
        // Inflate the base layout (with back button)
        val baseLayout = layoutInflater.inflate(R.layout.activity_base, null)
        setContentView(baseLayout)

        // Inflate the activity's own layout into the placeholder
        val container: FrameLayout = baseLayout.findViewById(R.id.contentContainer)
        layoutInflater.inflate(layoutResID, container, true)

        // Handle the fixed back button
        val btnBack: ImageButton = baseLayout.findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            onBackPressed() // default behavior
        }
    }
}
