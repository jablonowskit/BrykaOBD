package app.brykaobd.obd

/**
 * Fake ELM that answers AT with OK and Mode 01 with canned universal-PID frames.
 * Used for UI demo until Bluetooth Classic is wired.
 */
class DemoElmTransport : Transport {
    private var lastCommand: String = ""

    override suspend fun write(data: String) {
        lastCommand = data.trim().uppercase().removeSuffix("\r")
    }

    override suspend fun readUntilPrompt(timeoutMs: Long): String {
        if (lastCommand.startsWith("AT")) {
            return if (lastCommand == "ATZ") "ELM327 v1.5\r\n>" else "OK\r\n>"
        }
        return when (lastCommand) {
            "0104" -> "41 04 80\r\n>" // ~50% load
            "0105" -> "41 05 7B\r\n>" // 83°C
            "010C" -> "41 0C 1A F8\r\n>" // 1726 rpm
            "010D" -> "41 0D 32\r\n>" // 50 km/h
            "010F" -> "41 0F 4B\r\n>" // 35°C
            "0111" -> "41 11 40\r\n>" // ~25% throttle
            "0142" -> "41 42 36 B0\r\n>" // 14.000 V
            else -> "NO DATA\r\n>"
        }
    }

    override fun close() {
        lastCommand = ""
    }
}
