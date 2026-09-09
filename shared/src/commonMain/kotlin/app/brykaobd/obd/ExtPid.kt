package app.brykaobd.obd

/**
 * Generic OBD metric: Mode 01 (`01xx`) or Mode 22 DID (`22xxxx`).
 */
data class ExtPidDefinition(
    val request: String,
    val idHex: String,
    val namePl: String,
    val nameEn: String,
    val unit: String,
    val responseService: Int,
    val matchIds: IntArray,
    val decode: (ByteArray) -> Double?,
) {
    fun mode01Compatible(): PidDefinition? =
        if (responseService == 0x41 && matchIds.size == 1) {
            PidDefinition(matchIds[0], namePl, nameEn, unit, decode)
        } else {
            null
        }
}

data class ExtPidReading(
    val pid: ExtPidDefinition,
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

object ExtPids {
    fun mode01(
        pid: Int,
        namePl: String,
        nameEn: String,
        unit: String,
        decode: (ByteArray) -> Double?,
    ): ExtPidDefinition {
        val hex = pid.toString(16).padStart(2, '0').uppercase()
        return ExtPidDefinition(
            request = "01$hex",
            idHex = hex,
            namePl = namePl,
            nameEn = nameEn,
            unit = unit,
            responseService = 0x41,
            matchIds = intArrayOf(pid),
            decode = decode,
        )
    }

    fun mode22(
        did: Int,
        namePl: String,
        nameEn: String,
        unit: String,
        decode: (ByteArray) -> Double?,
    ): ExtPidDefinition {
        val hex = did.toString(16).padStart(4, '0').uppercase()
        return ExtPidDefinition(
            request = "22$hex",
            idHex = hex,
            namePl = namePl,
            nameEn = nameEn,
            unit = unit,
            responseService = 0x62,
            matchIds = intArrayOf((did shr 8) and 0xFF, did and 0xFF),
            decode = decode,
        )
    }
}
