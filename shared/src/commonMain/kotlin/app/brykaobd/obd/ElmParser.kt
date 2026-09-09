package app.brykaobd.obd

/**
 * Parses ELM327 text responses for Mode 01 (e.g. "41 0C 1A F8") and Mode 03 DTC frames.
 */
object ElmParser {
    fun isErrorResponse(text: String): String? {
        val u = text.uppercase()
        return when {
            "NO DATA" in u -> "NO DATA"
            "UNABLE TO CONNECT" in u -> "UNABLE TO CONNECT"
            "BUS INIT" in u && "ERROR" in u -> "BUS INIT ERROR"
            "CAN ERROR" in u -> "CAN ERROR"
            "STOPPED" in u -> "STOPPED"
            "?" in text.trim() &&
                text.lines().none {
                    val t = it.trim()
                    t.startsWith("41", ignoreCase = true) ||
                        t.startsWith("43", ignoreCase = true) ||
                        t.startsWith("44", ignoreCase = true) ||
                        t.startsWith("62", ignoreCase = true)
                } -> "?"
            else -> null
        }
    }

    /**
     * Extracts data bytes after `41 <PID>` from a multi-line ELM response.
     */
    fun extractMode01Data(response: String, pidId: Int): ByteArray? =
        extractPositiveResponseData(response, responseService = 0x41, matchIds = intArrayOf(pidId))

    /**
     * Extracts payload after positive response header, e.g. Mode 01 `41 XX …`
     * or Mode 22 `62 XX XX …` (DID hi/lo).
     */
    fun extractPositiveResponseData(
        response: String,
        responseService: Int,
        matchIds: IntArray,
    ): ByteArray? {
        isErrorResponse(response)?.let { return null }
        val bytes = allHexBytes(response)
        val headerLen = 1 + matchIds.size
        if (bytes.size < headerLen) return null
        for (i in 0..bytes.size - headerLen) {
            if ((bytes[i].toInt() and 0xFF) != responseService) continue
            var matched = true
            for (j in matchIds.indices) {
                if ((bytes[i + 1 + j].toInt() and 0xFF) != matchIds[j]) {
                    matched = false
                    break
                }
            }
            if (matched) return bytes.drop(i + headerLen).toByteArray()
        }
        return null
    }

    /**
     * Decodes Mode 03 payloads (`43 …`) into DTC strings. Skips `00 00`.
     * `NO DATA` → empty list (no codes). Other errors → empty; caller checks [isErrorResponse].
     */
    fun parseMode03Dtcs(response: String): List<String> {
        if (isErrorResponse(response) != null) {
            return emptyList()
        }
        val bytes = allHexBytes(response).map { it.toInt() and 0xFF }
        val codes = linkedSetOf<String>()
        var i = 0
        while (i < bytes.size) {
            if (bytes[i] != 0x43) {
                i++
                continue
            }
            i++
            var end = bytes.size
            for (k in i until bytes.size) {
                if (bytes[k] == 0x43) {
                    end = k
                    break
                }
            }
            val payload = bytes.subList(i, end)
            decodeMode03Payload(payload, codes)
            i = end
        }
        return codes.toList()
    }

    fun isMode04Success(response: String): Boolean {
        if (isErrorResponse(response) != null) return false
        val bytes = allHexBytes(response).map { it.toInt() and 0xFF }
        return bytes.any { it == 0x44 } || "OK" in response.uppercase()
    }

    /** SAE J2012 two-byte DTC → e.g. P0301. */
    fun decodeDtcPair(high: Int, low: Int): String? {
        if ((high and 0xFF) == 0 && (low and 0xFF) == 0) return null
        val type = when ((high shr 6) and 0x3) {
            0 -> "P"
            1 -> "C"
            2 -> "B"
            else -> "U"
        }
        val d1 = (high shr 4) and 0x3
        val d2 = high and 0xF
        val d3 = (low shr 4) and 0xF
        val d4 = low and 0xF
        return type + d1.toString() +
            d2.toString(16).uppercase() +
            d3.toString(16).uppercase() +
            d4.toString(16).uppercase()
    }

    private fun decodeMode03Payload(payload: List<Int>, out: MutableSet<String>) {
        if (payload.isEmpty()) return
        val count = payload[0]
        val rest = payload.drop(1)
        if (count in 1..16 && rest.size >= count * 2) {
            for (p in 0 until count) {
                decodeDtcPair(rest[p * 2], rest[p * 2 + 1])?.let { out.add(it) }
            }
            return
        }
        var j = 0
        while (j + 1 < payload.size) {
            decodeDtcPair(payload[j], payload[j + 1])?.let { out.add(it) }
            j += 2
        }
    }

    private fun allHexBytes(response: String): List<Byte> {
        // Spaced frames: "41 05 3B". Continuous (ATS0 / many V-LINK clones): "41053B".
        val cleaned = response.uppercase()
            .replace("SEARCHING...", " ")
            .replace(Regex("[^0-9A-F\\s]"), " ")
        val bytes = ArrayList<Byte>()
        for (token in cleaned.split(Regex("\\s+"))) {
            if (token.isEmpty()) continue
            if (token.length % 2 != 0) continue
            if (!token.all { it in '0'..'9' || it in 'A'..'F' }) continue
            var i = 0
            while (i < token.length) {
                bytes.add(token.substring(i, i + 2).toInt(16).toByte())
                i += 2
            }
        }
        return bytes
    }
}