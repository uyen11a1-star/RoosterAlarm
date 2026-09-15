package com.example.roosteralarm

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavUtil {
    fun write(samples: ShortArray, sampleRate: Int, path: String) {
        val dataSize = samples.size * 2
        val totalSize = 36 + dataSize
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray(Charsets.US_ASCII)); h.putInt(totalSize)
        h.put("WAVE".toByteArray(Charsets.US_ASCII))
        h.put("fmt ".toByteArray(Charsets.US_ASCII)); h.putInt(16)
        h.putShort(1); h.putShort(1); h.putInt(sampleRate)
        h.putInt(sampleRate * 2); h.putShort(2); h.putShort(16)
        h.put("data".toByteArray(Charsets.US_ASCII)); h.putInt(dataSize)

        FileOutputStream(File(path)).use { fos ->
            fos.write(h.array())
            val body = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) body.putShort(s)
            fos.write(body.array())
        }
    }
}
