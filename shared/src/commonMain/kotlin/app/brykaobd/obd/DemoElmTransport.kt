package app.brykaobd.obd

/**
 * Fake ELM that answers AT / Mode 01 / Mode 03–04 for UI demo without hardware.
 */
class DemoElmTransport : Transport {
    private var lastCommand: String = ""
    private var demoDtcsCleared: Boolean = false

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
            "010B" -> "41 0B 64\r\n>" // 100 kPa MAP
            "010C" -> "41 0C 1A F8\r\n>" // 1726 rpm
            "010D" -> "41 0D 32\r\n>" // 50 km/h
            "010F" -> "41 0F 4B\r\n>" // 35°C
            "0111" -> "41 11 40\r\n>" // ~25% throttle
            "011F" -> "41 1F 02 58\r\n>" // 600 s run time
            "012F" -> "41 2F C0\r\n>" // ~75% fuel
            "0133" -> "41 33 65\r\n>" // 101 kPa BARO
            "0142" -> "41 42 36 B0\r\n>" // 14.000 V
            "0146" -> "41 46 3C\r\n>" // 20°C ambient
            "015C" -> "41 5C 6E\r\n>" // 70°C oil
            "015E" -> "41 5E 00 64\r\n>" // 5.0 L/h
            "01A6" -> "41 A6 00 12 D6 80\r\n>" // 123456.0 km
            "017C" -> "41 7C 0E 10\r\n>" // ((3600)/10)-40 = 320 °C
            "0902" -> "49 02 01 4B 4C 31 4A 46 35 36 45 39 42 4B 31 32 33 34 35 36\r\n>" // VIN KL1JF56E9BK123456
            "223275" -> "62 32 75 2D\r\n>" // soot 45%
            "223273" -> "62 32 73 0C\r\n>" // pressure 12 kPa
            "223277" -> "62 32 77 00 04 B0\r\n>" // 1200 km
            "223274" -> "62 32 74 80\r\n>" // status ~50%
            "223279" -> "62 32 79 3C\r\n>" // 60*5-40 = 260 °C
            "221470" -> "62 14 70 2D\r\n>" // 45 psi ≈ 3.1 bar oil
            "03" -> if (demoDtcsCleared) {
                "NO DATA\r\n>"
            } else {
                // count=2, P0301 (03 01), P0420 (04 20)
                "43 02 03 01 04 20\r\n>"
            }
            "04" -> {
                demoDtcsCleared = true
                "44\r\n>"
            }
            else -> "NO DATA\r\n>"
        }
    }

    override fun close() {
        lastCommand = ""
        demoDtcsCleared = false
    }
}
