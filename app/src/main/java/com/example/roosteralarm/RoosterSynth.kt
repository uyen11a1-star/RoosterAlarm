package com.example.roosteralarm

import kotlin.math.PI
import kotlin.math.sin

/**
 * Synth tieng ga gay bang code, khong dung file am thanh.
 *
 * Cau truc tieng ga trong "O o o o o":
 *   Am tiet 1: 750Hz, 0.35s  (bat dau cao)
 *   Am tiet 2: 680Hz, 0.20s
 *   Am tiet 3: 600Hz, 0.15s
 *   Am tiet 4: 540Hz, 0.15s
 *   Am tiet 5: 480Hz, 0.55s  (cuoi keo dai)
 *
 * Moi am tiet co:
 *   - Envelope attack 30ms -> sustain -> release 80ms
 *   - Vibrato ±20Hz o 7Hz (tao cam giac "run" nhu ga that)
 *   - Them hai bac 2 (x2 freq) bien do 15% cho am am hon sine thuan
 */
object RoosterSynth {

    const val SAMPLE_RATE = 44100

    const val KIND_ROOSTER = 0
    const val KIND_HEN = 1
    const val KIND_CHICK = 2

    private data class Syllable(val freq: Double, val duration: Double, val vibratoAmp: Double)

    private val ROOSTER = listOf(
        Syllable(750.0, 0.35, 18.0),
        Syllable(680.0, 0.20, 22.0),
        Syllable(600.0, 0.15, 25.0),
        Syllable(540.0, 0.15, 22.0),
        Syllable(480.0, 0.55, 15.0)
    )

    private val HEN = listOf(
        Syllable(520.0, 0.12, 30.0),
        Syllable(560.0, 0.10, 35.0),
        Syllable(500.0, 0.14, 28.0)
    )

    private val CHICK = (0 until 6).map { i ->
        Syllable(1100.0 + (i % 2) * 200.0, 0.07, 50.0)
    }

    fun synthesize(kind: Int): ShortArray {
        val syllables = when (kind) {
            KIND_HEN -> HEN
            KIND_CHICK -> CHICK
            else -> ROOSTER
        }

        val totalSamples = syllables.sumOf { (it.duration * SAMPLE_RATE).toInt() } +
            (syllables.size * (0.04 * SAMPLE_RATE).toInt())   // khoang nghi 40ms giua cac am tiet

        val out = ShortArray(totalSamples)
        var pos = 0

        for (syl in syllables) {
            val n = (syl.duration * SAMPLE_RATE).toInt()
            val attackN = (0.03 * SAMPLE_RATE).toInt()
            val releaseN = (0.08 * SAMPLE_RATE).toInt()

            var phase = 0.0
            val twoPi = 2.0 * PI

            for (i in 0 until n) {
                // Envelope
                val env = when {
                    i < attackN -> i.toDouble() / attackN
                    i > n - releaseN -> (n - i).toDouble() / releaseN
                    else -> 1.0
                }
                // Vibrato: dieu tan cham
                val t = i.toDouble() / SAMPLE_RATE
                val vib = syl.vibratoAmp * sin(twoPi * 7.0 * t)
                val freq = syl.freq + vib

                // Song co ban + hai bac 2
                val s1 = sin(phase)
                val s2 = sin(phase * 2.0) * 0.15
                val sample = (s1 + s2) * env * 0.6

                out[pos + i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                phase += twoPi * freq / SAMPLE_RATE
                if (phase > twoPi * 100) phase -= twoPi * 100
            }
            pos += n

            // Khoang nghi 40ms giua cac am tiet
            pos += (0.04 * SAMPLE_RATE).toInt().coerceAtMost(totalSamples - pos - 1)
        }

        return out.copyOf(pos)
    }

    /** Tao 1 file WAV tam de preview/ring dung MediaPlayer neu can */
    fun makeWavFile(kind: Int, path: String) {
        val pcm = synthesize(kind)
        WavUtil.write(pcm, SAMPLE_RATE, path)
    }
}
