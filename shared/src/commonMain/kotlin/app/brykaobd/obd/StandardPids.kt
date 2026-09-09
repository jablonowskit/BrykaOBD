package app.brykaobd.obd

/**
 * Mode 01 PIDs that are widely supported on OBD-II vehicles (petrol and diesel).
 * First dashboard set for Aveo / any car — no manufacturer-specific PIDs.
 */
object StandardPids {
    val engineLoad = PidDefinition(
        id = 0x04,
        namePl = "Obciążenie silnika",
        nameEn = "Engine load",
        unit = "%",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
    )

    val coolantTemp = PidDefinition(
        id = 0x05,
        namePl = "Temp. chłodziwa",
        nameEn = "Coolant temperature",
        unit = "°C",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it - 40.0 } },
    )

    val rpm = PidDefinition(
        id = 0x0C,
        namePl = "Obroty",
        nameEn = "Engine RPM",
        unit = "rpm",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@PidDefinition null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@PidDefinition null
            ((a * 256) + b) / 4.0
        },
    )

    val speed = PidDefinition(
        id = 0x0D,
        namePl = "Prędkość",
        nameEn = "Vehicle speed",
        unit = "km/h",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.toDouble() },
    )

    val intakeTemp = PidDefinition(
        id = 0x0F,
        namePl = "Temp. powietrza ssącego",
        nameEn = "Intake air temperature",
        unit = "°C",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it - 40.0 } },
    )

    val throttle = PidDefinition(
        id = 0x11,
        namePl = "Przepustnica",
        nameEn = "Throttle position",
        unit = "%",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
    )

    val controlModuleVoltage = PidDefinition(
        id = 0x42,
        namePl = "Napięcie sterownika",
        nameEn = "Control module voltage",
        unit = "V",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@PidDefinition null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@PidDefinition null
            ((a * 256) + b) / 1000.0
        },
    )

    /** Parameters shown on the main live screen. */
    val dashboard: List<PidDefinition> = listOf(
        rpm,
        speed,
        coolantTemp,
        engineLoad,
        throttle,
        intakeTemp,
        controlModuleVoltage,
    )
}
