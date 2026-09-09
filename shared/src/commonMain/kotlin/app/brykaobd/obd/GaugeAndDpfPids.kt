package app.brykaobd.obd

/**
 * Gauge-style live metrics (what you see on typical instrument cluster + fuel use).
 */
object GaugePids {
    val speed = ExtPids.mode01(
        pid = 0x0D,
        namePl = "Prędkość",
        nameEn = "Speed",
        unit = "km/h",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.toDouble() },
    )

    val rpm = ExtPids.mode01(
        pid = 0x0C,
        namePl = "Obroty",
        nameEn = "RPM",
        unit = "rpm",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode01 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode01 null
            ((a * 256) + b) / 4.0
        },
    )

    val coolant = ExtPids.mode01(
        pid = 0x05,
        namePl = "Temp. płynu",
        nameEn = "Coolant",
        unit = "°C",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it - 40.0 } },
    )

    /** Engine oil temperature — not on every ECU (NO DATA if unsupported). */
    val oilTemp = ExtPids.mode01(
        pid = 0x5C,
        namePl = "Temp. oleju",
        nameEn = "Oil temp",
        unit = "°C",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it - 40.0 } },
    )

    val throttle = ExtPids.mode01(
        pid = 0x11,
        namePl = "Przepustnica",
        nameEn = "Throttle",
        unit = "%",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
    )

    val voltage = ExtPids.mode01(
        pid = 0x42,
        namePl = "Akumulator",
        nameEn = "Battery",
        unit = "V",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode01 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode01 null
            ((a * 256) + b) / 1000.0
        },
    )

    /** Engine fuel rate — not on every ECU; Aveo may return NO DATA. */
    val fuelRate = ExtPids.mode01(
        pid = 0x5E,
        namePl = "Zużycie (L/h)",
        nameEn = "Fuel rate",
        unit = "L/h",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode01 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode01 null
            ((a * 256) + b) / 20.0
        },
    )

    /**
     * Odometer (SAE Mode 01 PID A6) — km, 0.1 km resolution.
     * Not supported on every older diesel ECU (NO DATA then).
     */
    val odometer = ExtPids.mode01(
        pid = 0xA6,
        namePl = "Przebieg",
        nameEn = "Odometer",
        unit = "km",
        decode = { bytes ->
            if (bytes.size < 4) return@mode01 null
            val a = bytes[0].toUByte().toLong()
            val b = bytes[1].toUByte().toLong()
            val c = bytes[2].toUByte().toLong()
            val d = bytes[3].toUByte().toLong()
            (a * 16777216L + b * 65536L + c * 256L + d) / 10.0
        },
    )

    val pollList: List<ExtPidDefinition> =
        listOf(speed, rpm, coolant, oilTemp, throttle, voltage, fuelRate, odometer)

    /**
     * Instant L/100km from fuel rate and speed (null if speed too low or missing data).
     */
    fun instantLitersPer100km(fuelRateLh: Double?, speedKmh: Double?): Double? {
        if (fuelRateLh == null || speedKmh == null) return null
        if (speedKmh < 5.0) return null
        return fuelRateLh / speedKmh * 100.0
    }

    /**
     * Engine oil pressure — not in SAE Mode 01; GM community DID `221470` (byte A = psi → bar).
     * Many diesels only have a switch → NO DATA.
     */
    val oilPressure = ExtPids.mode22(
        did = 0x1470,
        namePl = "Ciśnienie oleju",
        nameEn = "Oil pressure",
        unit = "bar",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode22 null
            a / 14.503774
        },
    )
}

/**
 * DPF metrics: SAE Mode 01 where available + GM/Opel Mode 22 (header ATSH7E0 required).
 * DIDs aligned with Torque Astra-J 1.3 / Car Scanner GM profiles — verify on Aveo 1.3D.
 */
object DpfPids {
    val dpfTempMode01 = ExtPids.mode01(
        pid = 0x7C,
        namePl = "Temp. DPF (std)",
        nameEn = "DPF temp SAE",
        unit = "°C",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode01 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode01 null
            ((a * 256) + b) / 10.0 - 40.0
        },
    )

    /** Differential pressure (Torque: 223273, equation A, kPa). */
    val dpfPressure = ExtPids.mode22(
        did = 0x3273,
        namePl = "Ciśnienie DPF",
        nameEn = "DPF pressure",
        unit = "kPa",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.toDouble() },
    )

    /** Torque/Car Scanner GM-style soot fill %. */
    val sootLoad = ExtPids.mode22(
        did = 0x3275,
        namePl = "Zapełnienie DPF",
        nameEn = "DPF soot",
        unit = "%",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.toDouble() },
    )

    val kmSinceRegen = ExtPids.mode22(
        did = 0x3277,
        namePl = "Km od regeneracji",
        nameEn = "Km since regen",
        unit = "km",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode22 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode22 null
            val c = bytes.getOrNull(2)?.toUByte()?.toInt() ?: return@mode22 null
            (a * 65536 + b * 256 + c).toDouble()
        },
    )

    val dpfStatus = ExtPids.mode22(
        did = 0x3274,
        namePl = "Status DPF",
        nameEn = "DPF status",
        unit = "",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
    )

    val dpfTempGm = ExtPids.mode22(
        did = 0x3279,
        namePl = "Temp. DPF (GM)",
        nameEn = "DPF temp GM",
        unit = "°C",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 5.0 - 40.0 } },
    )

    val pollList: List<ExtPidDefinition> = listOf(
        sootLoad,
        dpfPressure,
        kmSinceRegen,
        dpfStatus,
        dpfTempGm,
        dpfTempMode01,
    )
}
