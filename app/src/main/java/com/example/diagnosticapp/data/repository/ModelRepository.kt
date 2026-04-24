package com.example.diagnosticapp.data.repository

import android.content.Context
import android.util.Log
import com.example.diagnosticapp.data.model.ModelData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ModelRepository {
    
    private const val MODELS_FILE = "models.json"
    private var models = mutableListOf<ModelData>()
    
    fun init(context: Context) {
        // Load saved models
        val file = File(context.filesDir, MODELS_FILE)
        if (file.exists()) {
            try {
                val json = file.readText()
                models = Json.decodeFromString<MutableList<ModelData>>(json)
            } catch (e: Exception) {
                Log.e("ModelRepository", "Error loading models: ${e.message}")
                models = mutableListOf()
            }
        }
        
        // Add default models if not present
        addDefaultModelsIfNeeded()
        saveModels(context)
    }
    
    private fun addDefaultModelsIfNeeded() {
        // Voice default model
        if (models.none { it.type == 1 && it.isDefault }) {
            models.add(
                ModelData(
                    id = "default_voice",
                    name = "Predvolený hlasový model",
                    filePath = "rf_model-old.onnx", // From assets
                    type = 1,
                    isDefault = true,
                    isActive = true
                )
            )
        }
        
        // Writing default model
        if (models.none { it.type == 2 && it.isDefault }) {
            models.add(
                ModelData(
                    id = "default_writing",
                    name = "Predvolený písací model",
                    filePath = "handwriting_rf.onnx", // From assets
                    type = 2,
                    isDefault = true,
                    isActive = true
                )
            )
        }
    }
    
    fun getAllModels(): List<ModelData> = models.toList()
    
    fun getActiveModel(type: Int): ModelData? {
        return models.find { it.type == type && it.isActive }
    }
    
    fun addModel(context: Context, name: String, filePath: String, type: Int): Boolean {
        return try {
            val newModel = ModelData(
                id = "model_${System.currentTimeMillis()}",
                name = name,
                filePath = filePath,
                type = type,
                isDefault = false,
                isActive = false
            )
            models.add(newModel)
            saveModels(context)
            true
        } catch (e: Exception) {
            Log.e("ModelRepository", "Error adding model: ${e.message}")
            false
        }
    }
    
    fun setActiveModel(context: Context, modelId: String): Boolean {
        return try {
            val model = models.find { it.id == modelId } ?: return false
            
            // Deactivate all models of the same type
            models.forEach { 
                if (it.type == model.type && it.id != modelId) {
                    val index = models.indexOf(it)
                    models[index] = it.copy(isActive = false)
                }
            }
            
            // Activate selected model
            val index = models.indexOf(model)
            models[index] = model.copy(isActive = true)
            
            saveModels(context)
            true
        } catch (e: Exception) {
            Log.e("ModelRepository", "Error setting active model: ${e.message}")
            false
        }
    }
    
    fun deleteModel(context: Context, modelId: String): Boolean {
        return try {
            val model = models.find { it.id == modelId } ?: return false
            
            // Cannot delete default or active models
            if (model.isDefault || model.isActive) {
                return false
            }
            
            // Delete the file if it exists
            val file = File(model.filePath)
            if (file.exists() && !model.isDefault) {
                file.delete()
            }
            
            models.removeIf { it.id == modelId }
            saveModels(context)
            true
        } catch (e: Exception) {
            Log.e("ModelRepository", "Error deleting model: ${e.message}")
            false
        }
    }
    
    private fun saveModels(context: Context) {
        try {
            val json = Json.encodeToString(models)
            val file = File(context.filesDir, MODELS_FILE)
            file.writeText(json)
        } catch (e: Exception) {
            Log.e("ModelRepository", "Error saving models: ${e.message}")
        }
    }
}