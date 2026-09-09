package app.brykaobd.obd

/**
 * Vehicle identity from Mode 09 (VIN) and light WMI hint — shown after connect.
 */
data class VehicleIdentity(
    val vin: String? = null,
    val manufacturerHint: String? = null,
    val error: String? = null,
) {
    val displayLine: String
        get() = when {
            vin != null && manufacturerHint != null -> "$manufacturerHint · VIN $vin"
            vin != null -> "VIN $vin"
            error != null -> "Pojazd: $error"
            else -> "Pojazd: nieznany"
        }
}

object VehicleIdentityParser {
    /**
     * Mode 09 PID 02 frames: `49 02 …` then ASCII VIN (may be multi-line / continuous / ISO-TP).
     */
    fun parseVin(response: String): String? {
        val bytes = ElmParser.hexBytes(response).map { it.toInt() and 0xFF }
        if (bytes.isEmpty()) return null
        var i = 0
        while (i < bytes.size - 1) {
            if (bytes[i] == 0x49 && bytes[i + 1] == 0x02) {
                val out = CharArray(17)
                var n = 0
                var j = i + 2
                while (j < bytes.size && n < 17) {
                    val b = bytes[j]
                    if ((b >= 0x30 && b <= 0x39) || (b >= 0x41 && b <= 0x5A)) {
                        out[n++] = b.toChar()
                    } else if (n > 0) {
                        break
                    }
                    j++
                }
                if (n == 17) return out.concatToString()
            }
            i++
        }
        return null
    }

    fun manufacturerHint(vin: String): String? {
        val wmi = vin.take(3).uppercase()
        return when (wmi) {
            "KL1", "KL7", "1G1", "2G1", "3G1" -> "Chevrolet"
            "W0L", "W0V", "W0S" -> "Opel / Vauxhall"
            "WF0", "WF1", "1FA", "1FT" -> "Ford"
            "TMB", "1VW", "3VW", "WVW" -> "VW / Skoda"
            "VF1", "VF3" -> "Renault / Peugeot"
            else -> null
        }
    }
}
