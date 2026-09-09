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
    private val pl = mapOf(
        "P0300" to "Wypadanie zapłonu (losowe)",
        "P0301" to "Wypadanie zapłonu — cylinder 1",
        "P0302" to "Wypadanie zapłonu — cylinder 2",
        "P0303" to "Wypadanie zapłonu — cylinder 3",
        "P0304" to "Wypadanie zapłonu — cylinder 4",
        "P0420" to "Sprawność katalizatora poniżej progu (bank 1)",
        "P0171" to "Układ paliwowy zbyt ubogi (bank 1)",
        "P0172" to "Układ paliwowy zbyt bogaty (bank 1)",
        "P0401" to "Niewystarczający przepływ EGR",
        "P0403" to "Obwód zaworu EGR / sterowanie",
        "P0405" to "Czujnik pozycji EGR A — niski sygnał",
        "P0113" to "Czujnik IAT — sygnał za wysoki",
        "P0128" to "Termostat chłodzenia — temperatura poniżej regulacji",
        "P0700" to "Usterka skrzyni (żądanie MIL)",
    )

    fun descriptionPl(code: String): String = pl[code.uppercase()] ?: "Zapisany kod usterki"
}
