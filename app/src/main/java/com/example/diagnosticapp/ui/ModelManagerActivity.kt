package com.example.diagnosticapp.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diagnosticapp.R
import com.example.diagnosticapp.data.model.ModelData
import com.example.diagnosticapp.data.repository.ModelRepository
import java.io.File

class ModelManagerActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var uploadVoiceModelButton: android.widget.Button
    private lateinit var uploadWritingModelButton: android.widget.Button
    private lateinit var voiceModelsRecyclerView: RecyclerView
    private lateinit var writingModelsRecyclerView: RecyclerView

    private lateinit var voiceAdapter: ModelAdapter
    private lateinit var writingAdapter: ModelAdapter

    private var uploadingType = 1 // 1 = voice, 2 = writing

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleFileSelected(uri.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_manager)

        initViews()
        setupRecyclerViews()
        setupButtons()
        loadModels()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        uploadVoiceModelButton = findViewById(R.id.uploadVoiceModelButton)
        uploadWritingModelButton = findViewById(R.id.uploadWritingModelButton)
        voiceModelsRecyclerView = findViewById(R.id.voiceModelsRecyclerView)
        writingModelsRecyclerView = findViewById(R.id.writingModelsRecyclerView)
    }

    private fun setupRecyclerViews() {
        voiceAdapter = ModelAdapter(
            onModelClick = { model -> setActiveModel(model) },
            onDeleteClick = { model -> deleteModel(model) }
        )
        voiceModelsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ModelManagerActivity)
            adapter = voiceAdapter
        }

        writingAdapter = ModelAdapter(
            onModelClick = { model -> setActiveModel(model) },
            onDeleteClick = { model -> deleteModel(model) }
        )
        writingModelsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ModelManagerActivity)
            adapter = writingAdapter
        }
    }

    private fun setupButtons() {
        backButton.setOnClickListener {
            finish()
        }

        uploadVoiceModelButton.setOnClickListener {
            uploadingType = 1
            openFilePicker()
        }

        uploadWritingModelButton.setOnClickListener {
            uploadingType = 2
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream"))
        }
        filePickerLauncher.launch(intent)
    }

    private fun handleFileSelected(fileUri: String) {
        try {
            val uri = android.net.Uri.parse(fileUri)

            // Get the file name
            val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "model.onnx"

            // Validate .onnx extension
            if (!fileName.endsWith(".onnx")) {
                Toast.makeText(this, "Prosím vyberte .onnx súbor", Toast.LENGTH_SHORT).show()
                return
            }

            // Copy file to internal storage
            val modelsDir = File(filesDir, "custom_models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            val destFile = File(modelsDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!destFile.exists()) {
                Toast.makeText(this, "Súbor nebol nájdený", Toast.LENGTH_SHORT).show()
                return
            }

            val modelName = fileName.removeSuffix(".onnx")
            val success = ModelRepository.addModel(this, modelName, destFile.absolutePath, uploadingType)

            if (success) {
                Toast.makeText(this, "Model úspešne pridaný", Toast.LENGTH_SHORT).show()
                loadModels()
            } else {
                Toast.makeText(this, "Pridanie modelu zlyhalo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ModelManager", "Error handling file: ${e.message}", e)
            Toast.makeText(this, "Chyba pri spracovaní súboru: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadModels() {
        val allModels = ModelRepository.getAllModels()

        val voiceModels = allModels.filter { it.type == 1 }
        val writingModels = allModels.filter { it.type == 2 }

        voiceAdapter.updateModels(voiceModels)
        writingAdapter.updateModels(writingModels)
    }

    private fun setActiveModel(model: ModelData) {
        val success = ModelRepository.setActiveModel(this, model.id)

        if (success) {
            Toast.makeText(this, "Model aktivovaný: ${model.name}", Toast.LENGTH_SHORT).show()
            loadModels()
        } else {
            Toast.makeText(this, "Aktivácia modelu zlyhala", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteModel(model: ModelData) {
        if (model.isDefault) {
            Toast.makeText(this, "Predvolené modely nemožno vymazať", Toast.LENGTH_SHORT).show()
            return
        }

        if (model.isActive) {
            Toast.makeText(this, "Aktívne modely nemožno vymazať", Toast.LENGTH_SHORT).show()
            return
        }

        val success = ModelRepository.deleteModel(this, model.id)

        if (success) {
            Toast.makeText(this, "Model vymazaný", Toast.LENGTH_SHORT).show()
            loadModels()
        } else {
            Toast.makeText(this, "Vymazanie modelu zlyhalo", Toast.LENGTH_SHORT).show()
        }
    }
}

class ModelAdapter(
    private val onModelClick: (ModelData) -> Unit,
    private val onDeleteClick: (ModelData) -> Unit
) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

    private var models = listOf<ModelData>()

    fun updateModels(newModels: List<ModelData>) {
        models = newModels
        notifyDataSetChanged()
    }

    inner class ModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val modelNameText: TextView = itemView.findViewById(R.id.modelNameText)
        val modelInfoText: TextView = itemView.findViewById(R.id.modelInfoText)
        val activeCheckIcon: ImageView = itemView.findViewById(R.id.activeCheckIcon)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteModelButton)

        fun bind(model: ModelData) {
            modelNameText.text = model.name

            modelInfoText.text = if (model.isDefault) {
                "Vstavený predvolený model"
            } else {
                File(model.filePath).name
            }

            // Show check icon if active
            activeCheckIcon.visibility = if (model.isActive) View.VISIBLE else View.GONE

            // Show/hide delete button
            deleteButton.visibility = if (model.isDefault || model.isActive) {
                View.GONE
            } else {
                View.VISIBLE
            }

            deleteButton.setOnClickListener {
                onDeleteClick(model)
            }

            itemView.setOnClickListener {
                onModelClick(model)
            }

            // Highlight active model
            if (model.isActive) {
                itemView.setBackgroundColor(0xFFDCFCE7.toInt()) // Light green
            } else {
                itemView.setBackgroundColor(0xFFFFFFFF.toInt()) // White
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model, parent, false)
        return ModelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        holder.bind(models[position])
    }

    override fun getItemCount() = models.size
}