package app.brykaobd.obd

/**
 * Standard Mode 01 PID (SAE J1979).
 * [id] is the PID byte, e.g. 0x0C for RPM.
 */
data class PidDefinition(
    val id: Int,
    val namePl: String,
    val nameEn: String,
    val unit: String,
    val decode: (ByteArray) -> Double?,
) {
    val idHex: String get() = id.toString(16).padStart(2, '0').uppercase()

    fun mode01Request(): String = "01$idHex"
}

data class PidReading(
    val pid: PidDefinition,
    val value: Double?,
    val error: String? = null,
) {
    val displayValue: String
        get() = when {
            error != null -> "—"
            value == null -> "—"
            else -> formatValue(value)
        }

    private fun formatValue(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else ((v * 10).toLong() / 10.0).toString()
}
