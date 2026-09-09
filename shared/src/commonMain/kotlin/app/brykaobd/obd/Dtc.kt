package app.brykaobd.obd

/**
 * Stored Diagnostic Trouble Code (Mode 03 / SAE J2012 style).
 */
data class DtcCode(
    val code: String,
    val descriptionPl: String = DtcCatalog.descriptionPl(code),
) {
    val display: String get() = if (descriptionPl.isBlank()) code else "$code — $descriptionPl"
}

data class DtcReadResult(
    val codes: List<DtcCode> = emptyList(),
    val error: String? = null,
) {
    val ok: Boolean get() = error == null
}

object DtcCatalog {
    /**
     * PL curated first, then broad EN catalog from [data/dtc_codes.psv].
     */
    fun descriptionPl(code: String): String {
        val key = code.uppercase()
        return DtcCatalogPl.map[key]
            ?: DtcCatalogEn.map[key]
            ?: "Zapisany kod usterki"
    }

    val size: Int get() = DtcCatalogPl.map.size + DtcCatalogEn.map.size
}
