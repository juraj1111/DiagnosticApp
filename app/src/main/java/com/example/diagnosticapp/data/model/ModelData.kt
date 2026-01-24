package com.example.diagnosticapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ModelData(
    val id: String,
    val name: String,
    val filePath: String,
    val type: Int,  // 1 = voice, 2 = writing
    val isDefault: Boolean = false,
    val isActive: Boolean = false
)