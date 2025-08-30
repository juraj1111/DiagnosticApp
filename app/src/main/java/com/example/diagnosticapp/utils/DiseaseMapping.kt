package com.example.diagnosticapp.utils

object DiseaseMapping {
    val map = mapOf(
        "encephalopathy" to "Encefalopatia"
    )

    // convenience functions
    fun getSlovakName(englishKey: String): String? = map[englishKey]

    fun getEnglishKey(slovakName: String): String? = map.entries.find { it.value == slovakName }?.key
}