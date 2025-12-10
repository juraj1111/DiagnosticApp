package com.example.diagnosticapp.utils

import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.mfcc.MFCC
import be.tarsos.dsp.util.fft.FFT
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

fun VoiceFeatureExtractorV1(path: String): FloatArray {
    val file = File(path)
    if (!file.exists()) return FloatArray(16) { 0f }

    val sampleRate = 16000f
    val bufferSize = 1024
    val hopSize = 512

    val signal = readWavAsFloatArray(file)

    val mfccProcessor = MFCC(bufferSize, sampleRate, 13, 40, 300f, 8000f)
    val fft = FFT(bufferSize)
    val magnitude = FloatArray(bufferSize / 2)
    val frameFeatures = mutableListOf<FloatArray>()

    // Define fake audio format
    val format = TarsosDSPAudioFormat(sampleRate, 16, 1, true, false)

    var pos = 0
    while (pos + bufferSize <= signal.size) {
        val frame = signal.copyOfRange(pos, pos + bufferSize)

        // Create dummy AudioEvent and assign samples manually
        val event = AudioEvent(format)
        event.setFloatBuffer(frame)

        // 1) MFCCs
        mfccProcessor.process(event)
        val mfccs = mfccProcessor.mfcc

        // 2) RMS
        var rms = 0f
        for (s in frame) rms += s * s
        rms = sqrt(rms / frame.size)

        // 3) ZCR
        var zcrCount = 0
        for (i in 1 until frame.size) {
            if ((frame[i - 1] >= 0 && frame[i] < 0) || (frame[i - 1] < 0 && frame[i] >= 0)) zcrCount++
        }
        val zcr = zcrCount.toFloat() / frame.size.toFloat()

        // 4) Spectral centroid
        val fftBuffer = frame.copyOf(bufferSize)
        fft.forwardTransform(fftBuffer)
        fft.modulus(fftBuffer, magnitude)

        var num = 0f
        var den = 0f
        for (i in magnitude.indices) {
            val f = i * sampleRate / bufferSize
            num += f * magnitude[i]
            den += magnitude[i]
        }
        val centroid = if (den > 0f) num / den else 0f

        // Combine into one vector (13 MFCCs + RMS + ZCR + centroid)
        val featureVec = FloatArray(16)
        for (i in 0 until 13) featureVec[i] = mfccs.getOrNull(i) ?: 0f
        featureVec[13] = rms
        featureVec[14] = zcr
        featureVec[15] = centroid

        frameFeatures.add(featureVec)
        pos += hopSize
    }

    if (frameFeatures.isEmpty()) return FloatArray(16) { 0f }

    // Average all frames
    val avg = FloatArray(16)
    for (f in frameFeatures) for (i in f.indices) avg[i] += f[i]
    for (i in avg.indices) avg[i] /= frameFeatures.size.toFloat()



    return avg
}

fun readWavAsFloatArray(file: File): FloatArray {
    val input = file.inputStream().buffered()
    val header = ByteArray(44)
    input.read(header)

    val pcmData = input.readBytes()
    val shorts = ShortArray(pcmData.size / 2)
    ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

    val floats = FloatArray(shorts.size)
    for (i in shorts.indices) {
        floats[i] = shorts[i] / 32768.0f
    }
    return floats
}

