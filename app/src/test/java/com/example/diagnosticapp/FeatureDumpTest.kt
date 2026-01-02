package com.example.diagnosticapp

import com.example.diagnosticapp.utils.extractFeaturesFromWav
import org.junit.Test
import java.io.File

class FeatureDumpTest {
    @Test
    fun main() {
        val root = File("C:\\Users\\Juraj\\Desktop\\DiagnosticApp_model_voice\\parkinson_env312\\parkinson_ml\\test")
        val out = File("android_extracted_features.csv")
        out.printWriter().use { pw ->
            pw.println("subject,label," + (0 until 16).joinToString(",") { "f$it" })

            root.walk().filter { it.extension == "wav" }.forEach { wav ->
                val subject = wav.parentFile.name
                val label = wav.parentFile.parentFile.name   // pd / young_hc / elderly_hc

                val feats = extractFeaturesFromWav(wav.absolutePath)

                pw.println("$subject,$label,${feats.joinToString(",")}")
                println("Processed $wav")
            }
        }

        println("Done! Saved to android_extracted_features.csv")
    }
}