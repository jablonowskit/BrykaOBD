package app.brykaobd.obd

/**
 * Parses ELM327 text responses for Mode 01 (e.g. "41 0C 1A F8").
 */
object ElmParser {
    private val hexByte = Regex("""\b([0-9A-Fa-f]{2})\b""")

    fun isErrorResponse(text: String): String? {
        val u = text.uppercase()
        return when {
            "NO DATA" in u -> "NO DATA"
            "UNABLE TO CONNECT" in u -> "UNABLE TO CONNECT"
            "BUS INIT" in u && "ERROR" in u -> "BUS INIT ERROR"
            "CAN ERROR" in u -> "CAN ERROR"
            "STOPPED" in u -> "STOPPED"
            "?" in text.trim() && text.lines().none { it.trim().startsWith("41", ignoreCase = true) } -> "?"
            else -> null
        }
    }

    /**
     * Extracts data bytes after `41 <PID>` from a multi-line ELM response.
     */
    fun extractMode01Data(response: String, pidId: Int): ByteArray? {
        isErrorResponse(response)?.let { return null }
        val pidHex = pidId.toString(16).padStart(2, '0')
        val bytes = hexByte.findAll(response.replace("\r", " ").replace("\n", " "))
            .map { it.groupValues[1].toInt(16).toByte() }
            .toList()
        if (bytes.size < 2) return null
        for (i in 0 until bytes.size - 1) {
            val service = bytes[i].toInt() and 0xFF
            val pid = bytes[i + 1].toInt() and 0xFF
            if (service == 0x41 && pid == pidId) {
                return bytes.drop(i + 2).toByteArray()
            }
            // Some adapters echo without spaces as continuous stream — also match pidHex in text path
            if (service == 0x41 && pidHex.equals(pid.toString(16).padStart(2, '0'), ignoreCase = true)) {
                return bytes.drop(i + 2).toByteArray()
            }
        }
        return null
    }
}
