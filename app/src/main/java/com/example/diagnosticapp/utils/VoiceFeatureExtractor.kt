package com.example.diagnosticapp.utils

import android.util.Log
import be.tarsos.dsp.pitch.Yin
import kotlin.math.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun VoiceFeatureExtractor(path: String): FloatArray {
    val file = File(path)
    if (!file.exists()) {
        Log.e("FeatureExtractor", "File not found: $path")
        return FloatArray(12) { 0f }
    }

    val sampleRate = 16000f
    val frameSize = 1024
    val hopSize = 256

    val samples = try {
        readWavAsFloatArrayV2(file)
    } catch (e: Exception) {
        Log.e("FeatureExtractor", "Error reading WAV file: ${e.message}")
        return FloatArray(12) { 0f }
    }

    if (samples.isEmpty()) {
        Log.e("FeatureExtractor", "No samples read from file")
        return FloatArray(12) { 0f }
    }

    Log.d("FeatureExtractor", "Processing ${samples.size} samples from $path")

    val yin = Yin(sampleRate, frameSize)
    val f0List = mutableListOf<Float>()
    val amplitudePeaks = mutableListOf<Float>()
    val frameEnergyDb = mutableListOf<Float>()

    var pos = 0
    var validFrames = 0

    while (pos + frameSize <= samples.size) {
        val frame = samples.copyOfRange(pos, pos + frameSize)

        // PITCH (F0)
        try {
            val pitch = yin.getPitch(frame)
            // More lenient F0 range: 60-500 Hz covers most adult voices
            if (pitch.pitch in 60f..500f && !pitch.pitch.isNaN()) {
                f0List.add(pitch.pitch)
            }
        } catch (e: Exception) {
            Log.w("FeatureExtractor", "Error computing pitch at frame $validFrames: ${e.message}")
        }

        // AMPLITUDE PEAK (for shimmer)
        val peak = frame.maxOfOrNull { abs(it) } ?: 0f
        if (peak.isFinite()) {
            amplitudePeaks.add(peak)
        }

        // Frame energy for HNR/NHR
        val energy = frame.sumOf { (it * it).toDouble() }.toFloat()
        if (energy.isFinite() && energy > 0) {
            val db = 10f * log10(energy + 1e-8f)
            if (db.isFinite()) {
                frameEnergyDb.add(db)
            }
        }

        pos += hopSize
        validFrames++
    }

    Log.d("FeatureExtractor", "Extracted ${f0List.size} valid F0 values from $validFrames frames")

    // Need minimum frames for reliable features
    if (f0List.size < 5) {
        Log.w("FeatureExtractor", "Insufficient F0 values (${f0List.size}), returning zeros")
        return FloatArray(12) { 0f }
    }

    // F0 statistics
    val meanF0 = f0List.average().toFloat().coerceFinite(0f)
    val stdF0 = f0List.standardDeviation().coerceFinite(0f)
    val minF0 = f0List.minOrNull()?.coerceFinite(0f) ?: 0f
    val maxF0 = f0List.maxOrNull()?.coerceFinite(0f) ?: 0f

    // Jitter (local, RAP, PPQ)
    val jitterLocal = computeJitterLocal(f0List).coerceFinite(0f)
    val jitterRap = computeJitterRAP(f0List).coerceFinite(0f)
    val jitterPpq = computeJitterPPQ(f0List).coerceFinite(0f)

    // Shimmer (local, APQ3, APQ5)
    val shimmerLocal = computeShimmerLocal(amplitudePeaks).coerceFinite(0f)
    val shimmerAPQ3 = computeShimmerAPQ3(amplitudePeaks).coerceFinite(0f)
    val shimmerAPQ5 = computeShimmerAPQ5(amplitudePeaks).coerceFinite(0f)

    // HNR & NHR
    val hnr = computeHNR(frameEnergyDb).coerceFinite(0f)
    val nhr = computeNHR(frameEnergyDb).coerceFinite(0f)

    val features = floatArrayOf(
        meanF0, stdF0, minF0, maxF0,
        jitterLocal, jitterRap, jitterPpq,
        shimmerLocal, shimmerAPQ3, shimmerAPQ5,
        hnr, nhr
    )

    Log.d("FeatureExtractor", "Features: ${features.joinToString()}")
    return features
}

// Extension to ensure finite values
private fun Float.coerceFinite(default: Float): Float {
    return if (this.isFinite()) this else default
}

fun List<Float>.standardDeviation(): Float {
    if (this.size < 2) return 0f
    val mean = this.average().toFloat()
    var sum = 0f
    for (v in this) {
        val d = v - mean
        sum += d * d
    }
    val result = sqrt(sum / (this.size - 1).toFloat())
    return if (result.isFinite()) result else 0f
}

fun computeJitterLocal(f0: List<Float>): Float {
    if (f0.size < 2) return 0f
    var sum = 0f
    for (i in 1 until f0.size) {
        sum += abs(f0[i] - f0[i - 1])
    }
    val mean = f0.average().toFloat()
    return if (mean > 0f) (sum / (f0.size - 1)) / mean else 0f
}

fun computeJitterRAP(f0: List<Float>): Float {
    if (f0.size < 3) return 0f
    var sum = 0f
    for (i in 1 until f0.size - 1) {
        val avg = (f0[i - 1] + f0[i] + f0[i + 1]) / 3f
        sum += abs(f0[i] - avg)
    }
    val mean = f0.average().toFloat()
    return if (mean > 0f) (sum / (f0.size - 2)) / mean else 0f
}

fun computeJitterPPQ(f0: List<Float>): Float {
    if (f0.size < 5) return 0f
    var sum = 0f
    for (i in 2 until f0.size - 2) {
        val avg = (f0[i - 2] + f0[i - 1] + f0[i] + f0[i + 1] + f0[i + 2]) / 5f
        sum += abs(f0[i] - avg)
    }
    val mean = f0.average().toFloat()
    return if (mean > 0f) (sum / (f0.size - 4)) / mean else 0f
}

fun computeShimmerLocal(amps: List<Float>): Float {
    if (amps.size < 2) return 0f
    var sum = 0f
    for (i in 1 until amps.size) {
        sum += abs(amps[i] - amps[i - 1])
    }
    val mean = amps.average().toFloat()
    return if (mean > 0f) (sum / (amps.size - 1)) / mean else 0f
}

fun computeShimmerAPQ3(amps: List<Float>): Float {
    if (amps.size < 3) return 0f
    var sum = 0f
    for (i in 1 until amps.size - 1) {
        val avg = (amps[i - 1] + amps[i] + amps[i + 1]) / 3f
        sum += abs(amps[i] - avg)
    }
    val mean = amps.average().toFloat()
    return if (mean > 0f) (sum / (amps.size - 2)) / mean else 0f
}

fun computeShimmerAPQ5(amps: List<Float>): Float {
    if (amps.size < 5) return 0f
    var sum = 0f
    for (i in 2 until amps.size - 2) {
        val avg = (amps[i - 2] + amps[i - 1] + amps[i] + amps[i + 1] + amps[i + 2]) / 5f
        sum += abs(amps[i] - avg)
    }
    val mean = amps.average().toFloat()
    return if (mean > 0f) (sum / (amps.size - 4)) / mean else 0f
}

fun computeHNR(energyDb: List<Float>): Float {
    if (energyDb.isEmpty()) return 0f

    var mean = 0f
    for (v in energyDb) mean += v
    mean /= energyDb.size.toFloat()

    var variance = 0f
    for (v in energyDb) {
        val d = v - mean
        variance += d * d
    }
    variance /= energyDb.size.toFloat()

    // Avoid division by zero
    if (variance < 1e-6f) return 0f

    val ratio = (mean * mean) / variance
    return 10f * log10(ratio.coerceAtLeast(1e-6f))
}

fun computeNHR(energyDb: List<Float>): Float {
    if (energyDb.isEmpty()) return 0f
    val min = energyDb.minOrNull() ?: return 0f
    val max = energyDb.maxOrNull() ?: return 0f
    return if (max > 0f) (max - min) / max else 0f
}

fun readWavAsFloatArrayV2(file: File): FloatArray {
    val input = file.inputStream().buffered()
    val header = ByteArray(44)
    val headerRead = input.read(header)

    if (headerRead != 44) {
        throw IllegalStateException("Invalid WAV file: header too short")
    }

    val pcm = input.readBytes()
    input.close()

    if (pcm.size < 2) {
        throw IllegalStateException("Invalid WAV file: no audio data")
    }

    val shorts = ShortArray(pcm.size / 2)
    ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

    return FloatArray(shorts.size) { i -> shorts[i] / 32768f }
}