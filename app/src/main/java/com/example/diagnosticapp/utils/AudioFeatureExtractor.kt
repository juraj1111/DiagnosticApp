package com.example.diagnosticapp.utils

import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.util.fft.FFT
import kotlin.math.log10
import kotlin.math.sqrt
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioFeatureExtractor {

    fun extractFeatures(file: File): FloatArray {
        // window = 50ms, step = 25ms
        val sampleRate = 16000
        val windowSize = (0.05f * sampleRate).toInt() // 800 samples
        val stepSize = (0.025f * sampleRate).toInt()  // 400 samples

        val dispatcher = AudioDispatcherFactory.fromPipe(
            file.absolutePath,
            sampleRate,
            windowSize,
            stepSize
        )

        val energyValues = mutableListOf<Float>()
        val zcrValues = mutableListOf<Float>()

        dispatcher.addAudioProcessor(object : AudioProcessor {
            override fun process(audioEvent: AudioEvent): Boolean {
                val buffer = audioEvent.floatBuffer
                // Energy
                var sumEnergy = 0f
                for (v in buffer) sumEnergy += v * v
                val energy = sumEnergy / buffer.size
                energyValues.add(energy)

                // Zero Crossing Rate
                var crossings = 0
                for (i in 1 until buffer.size) {
                    if ((buffer[i - 1] >= 0 && buffer[i] < 0) ||
                        (buffer[i - 1] < 0 && buffer[i] >= 0)
                    ) crossings++
                }
                val zcr = crossings.toFloat() / buffer.size
                zcrValues.add(zcr)
                return true
            }

            override fun processingFinished() {}
        })

        dispatcher.run()

        // Compute mean & std for each feature
        val energyMean = energyValues.average().toFloat()
        val energyStd = sqrt(energyValues.map { (it - energyMean) * (it - energyMean) }.average().toFloat())
        val zcrMean = zcrValues.average().toFloat()
        val zcrStd = sqrt(zcrValues.map { (it - zcrMean) * (it - zcrMean) }.average().toFloat())

        // Combine (like your Python vector: [mean1, mean2, std1, std2])
        return floatArrayOf(energyMean, zcrMean, energyStd, zcrStd)
    }
}
