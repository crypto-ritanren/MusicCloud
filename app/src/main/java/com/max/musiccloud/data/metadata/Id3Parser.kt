package com.max.musiccloud.data.metadata

data class ParsedId3(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkBytes: ByteArray? = null
)

object Id3Parser {

    fun parse(bytes: ByteArray): ParsedId3? {
        if (bytes.size < 10) return null
        if (bytes[0] != 0x49.toByte() || bytes[1] != 0x44.toByte() || bytes[2] != 0x33.toByte()) {
            return null // Not ID3v2
        }

        val majorVersion = bytes[3].toInt() and 0xFF
        if (majorVersion !in 2..4) return null

        val flags = bytes[5].toInt() and 0xFF
        val hasExtendedHeader = (flags and 0x40) != 0

        val tagDataSize = ((bytes[6].toInt() and 0x7F) shl 21) or
                          ((bytes[7].toInt() and 0x7F) shl 14) or
                          ((bytes[8].toInt() and 0x7F) shl 7) or
                          (bytes[9].toInt() and 0x7F)

        var offset = 10
        val end = minOf(bytes.size, 10 + tagDataSize)

        if (hasExtendedHeader && offset + 4 <= end) {
            val extHeaderSize = if (majorVersion == 4) {
                ((bytes[offset].toInt() and 0x7F) shl 21) or
                ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
                ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
                (bytes[offset + 3].toInt() and 0x7F)
            } else {
                ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
            }
            offset += extHeaderSize
        }

        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var artworkBytes: ByteArray? = null

        while (offset + 10 <= end) {
            val frameId = String(bytes, offset, 4, Charsets.US_ASCII)
            if (frameId.isBlank() || frameId[0] == '\u0000') break // Padding reached

            val frameSize = if (majorVersion == 4) {
                ((bytes[offset + 4].toInt() and 0x7F) shl 21) or
                ((bytes[offset + 5].toInt() and 0x7F) shl 14) or
                ((bytes[offset + 6].toInt() and 0x7F) shl 7) or
                (bytes[offset + 7].toInt() and 0x7F)
            } else {
                ((bytes[offset + 4].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 5].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 6].toInt() and 0xFF) shl 8) or
                (bytes[offset + 7].toInt() and 0xFF)
            }

            offset += 10
            if (frameSize <= 0 || offset + frameSize > end) {
                break
            }

            when (frameId) {
                "TIT2" -> title = decodeTextFrame(bytes, offset, frameSize)
                "TPE1" -> artist = decodeTextFrame(bytes, offset, frameSize)
                "TALB" -> album = decodeTextFrame(bytes, offset, frameSize)
                "APIC" -> if (artworkBytes == null) {
                    artworkBytes = decodeApicFrame(bytes, offset, frameSize)
                }
            }

            offset += frameSize
        }

        return ParsedId3(title, artist, album, artworkBytes)
    }

    private fun decodeTextFrame(bytes: ByteArray, offset: Int, length: Int): String? {
        if (length <= 1) return null
        val encoding = bytes[offset].toInt() and 0xFF
        val textBytes = bytes.copyOfRange(offset + 1, offset + length)
        return try {
            when (encoding) {
                0 -> String(textBytes, Charsets.UTF_8).trim('\u0000', ' ')
                1 -> String(textBytes, Charsets.UTF_16).trim('\u0000', ' ')
                2 -> String(textBytes, Charsets.UTF_16BE).trim('\u0000', ' ')
                3 -> String(textBytes, Charsets.UTF_8).trim('\u0000', ' ')
                else -> String(textBytes, Charsets.UTF_8).trim('\u0000', ' ')
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeApicFrame(bytes: ByteArray, offset: Int, length: Int): ByteArray? {
        if (length < 10) return null
        var p = offset
        val end = offset + length
        val encoding = bytes[p++].toInt() and 0xFF

        // Find end of MIME type (null terminated)
        while (p < end && bytes[p] != 0.toByte()) { p++ }
        if (p >= end) return null
        p++ // skip null byte

        // Skip picture type byte
        if (p >= end) return null
        p++

        // Skip description string
        if (encoding == 1 || encoding == 2) {
            while (p + 1 < end && !(bytes[p] == 0.toByte() && bytes[p + 1] == 0.toByte())) {
                p += 2
            }
            p += 2
        } else {
            while (p < end && bytes[p] != 0.toByte()) {
                p++
            }
            p++
        }

        if (p >= end) return null
        return bytes.copyOfRange(p, end)
    }
}
