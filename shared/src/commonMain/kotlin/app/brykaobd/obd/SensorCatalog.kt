package app.brykaobd.obd

/**
 * Searchable catalog of known sensors (Mode 01 + Mode 22) with decode.
 * Deduped by [ExtPidDefinition.request].
 */
object SensorCatalog {
    private val extras: List<ExtPidDefinition> = listOf(
        ExtPids.mode01(
            pid = 0x04,
            namePl = "Obciążenie silnika",
            nameEn = "Engine load",
            unit = "%",
            decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
        ),
        ExtPids.mode01(
            pid = 0x0B,
            namePl = "MAP",
            nameEn = "Manifold absolute pressure",
            unit = "kPa",
            decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.toDouble() },
        ),
        ExtPids.mode01(
            pid = 0x0F,
            namePl = "Temp. powietrza ssącego",
            nameEn = "Intake air temperature",
            unit = "°C",
            decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it - 40.0 } },
        ),
        ExtPids.mode01(
            pid = 0x1F,
            namePl = "Czas od startu",
            nameEn = "Run time since engine start",
            unit = "s",
            decode = { bytes ->
                val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode01 null
                val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode01 null
                (a * 256 + b).toDouble()
            },
        ),
        ExtPids.mode01(
            pid = 0x2F,
            namePl = "Poziom paliwa",
            nameEn = "Fuel level",
            unit = "%",
            decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
        ),
        ExtPids.mode01(
            pid = 0x33,
            namePl = "Ciśnienie baro",
            nameEn = "Barometric pressure",
            unit = "kPa",
            decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.toDouble() },
        ),
        ExtPids.mode01(
            pid = 0x46,
            namePl = "Temp. powietrza amb.",
            nameEn = "Ambient air temperature",
            unit = "°C",
            decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it - 40.0 } },
        ),
    )

    val all: List<ExtPidDefinition> = buildList {
        addAll(GaugePids.pollList)
        add(GaugePids.oilPressure)
        addAll(DpfPids.pollList)
        addAll(extras)
    }.distinctBy { it.request }

    /**
     * Case-insensitive filter on Polish/English name and request hex.
     * Empty [query] returns the full catalog.
     */
    fun search(query: String): List<ExtPidDefinition> {
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter { pid ->
            pid.namePl.contains(q, ignoreCase = true) ||
                pid.nameEn.contains(q, ignoreCase = true) ||
                pid.request.contains(q, ignoreCase = true)
        }
    }
}
