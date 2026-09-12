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

    /** Accelerator pedal position D (SAE Mode 01 PID 49) — driver's actual pedal input. */
    val accelPedal = ExtPids.mode01(
        pid = 0x49,
        namePl = "Pedał gazu",
        nameEn = "Accel pedal",
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

    /** Calculated engine load (SAE Mode 01 PID 04). */
    val engineLoad = ExtPids.mode01(
        pid = 0x04,
        namePl = "Obciążenie silnika",
        nameEn = "Engine load",
        unit = "%",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
    )

    /** Intake manifold absolute pressure (SAE Mode 01 PID 0B). */
    val intakeManifoldPressure = ExtPids.mode01(
        pid = 0x0B,
        namePl = "Ciśnienie kolektora",
        nameEn = "Intake MAP",
        unit = "kPa",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.toDouble() },
    )

    /** Intake air temperature (SAE Mode 01 PID 0F). */
    val intakeAirTemp = ExtPids.mode01(
        pid = 0x0F,
        namePl = "Temp. powietrza",
        nameEn = "Intake air temp",
        unit = "°C",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it - 40.0 } },
    )

    /** Mass air flow rate (SAE Mode 01 PID 10). */
    val mafRate = ExtPids.mode01(
        pid = 0x10,
        namePl = "Przepływ powietrza",
        nameEn = "MAF",
        unit = "g/s",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode01 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode01 null
            ((a * 256) + b) / 100.0
        },
    )

    /** Timing advance (SAE Mode 01 PID 0E) — mostly meaningless on diesel, still standard. */
    val timingAdvance = ExtPids.mode01(
        pid = 0x0E,
        namePl = "Wyprzedzenie zapłonu",
        nameEn = "Timing advance",
        unit = "°",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it / 2.0 - 64.0 } },
    )

    /** Fuel tank level input (SAE Mode 01 PID 2F). */
    val fuelLevel = ExtPids.mode01(
        pid = 0x2F,
        namePl = "Poziom paliwa",
        nameEn = "Fuel level",
        unit = "%",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
    )

    /** Time since engine start (SAE Mode 01 PID 1F). */
    val runTimeSinceStart = ExtPids.mode01(
        pid = 0x1F,
        namePl = "Czas od startu",
        nameEn = "Run time since start",
        unit = "s",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode01 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode01 null
            ((a * 256) + b).toDouble()
        },
    )

    /** Absolute barometric pressure (SAE Mode 01 PID 33). */
    val barometricPressure = ExtPids.mode01(
        pid = 0x33,
        namePl = "Ciśnienie atm.",
        nameEn = "Barometric pressure",
        unit = "kPa",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.toDouble() },
    )

    /** Ambient air temperature (SAE Mode 01 PID 46). */
    val ambientAirTemp = ExtPids.mode01(
        pid = 0x46,
        namePl = "Temp. otoczenia",
        nameEn = "Ambient air temp",
        unit = "°C",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it - 40.0 } },
    )

    /** Relative accelerator pedal position (SAE Mode 01 PID 5A) — alternative pedal sensor. */
    val relativeAccelPedal = ExtPids.mode01(
        pid = 0x5A,
        namePl = "Pedał gazu (wzgl.)",
        nameEn = "Relative accel pedal",
        unit = "%",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
    )

    /** Commanded EGR (SAE Mode 01 PID 2C) — relevant for diesel EGR/DPF diagnostics. */
    val commandedEgr = ExtPids.mode01(
        pid = 0x2C,
        namePl = "Sterowanie EGR",
        nameEn = "Commanded EGR",
        unit = "%",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 255.0 } },
    )

    /** EGR error (SAE Mode 01 PID 2D). */
    val egrError = ExtPids.mode01(
        pid = 0x2D,
        namePl = "Błąd EGR",
        nameEn = "EGR error",
        unit = "%",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 100.0 / 128.0 - 100.0 } },
    )

    val pollList: List<ExtPidDefinition> = listOf(
        speed, rpm, coolant, oilTemp, throttle, accelPedal, voltage, fuelRate, odometer,
        engineLoad, intakeManifoldPressure, intakeAirTemp, mafRate, timingAdvance,
        fuelLevel, runTimeSinceStart, barometricPressure, ambientAirTemp,
        relativeAccelPedal, commandedEgr, egrError,
    )

    /**
     * Instant L/100km from fuel rate and speed (null if speed too low or missing data).
     */
    fun instantLitersPer100km(fuelRateLh: Double?, speedKmh: Double?): Double? {
        if (fuelRateLh == null || speedKmh == null) return null
        if (speedKmh < 5.0) return null
        return fuelRateLh / speedKmh * 100.0
    }

    /** Typical diesel stoichiometric air-fuel ratio (mass air : mass fuel). */
    private const val DIESEL_AFR = 14.5

    /** Diesel fuel density, g/mL. */
    private const val DIESEL_DENSITY_G_PER_ML = 0.832

    /**
     * Approximate fuel rate (L/h) from MAF (g/s), for ECUs that don't support PID 0x5E directly.
     * Rough estimate only — assumes a fixed AFR, ignores idle/DPF-regen fuel enrichment.
     */
    fun estimatedFuelRateLhFromMaf(mafGramsPerSec: Double?): Double? {
        if (mafGramsPerSec == null || mafGramsPerSec <= 0.0) return null
        val fuelGramsPerSec = mafGramsPerSec / DIESEL_AFR
        val fuelMlPerSec = fuelGramsPerSec / DIESEL_DENSITY_G_PER_ML
        return fuelMlPerSec * 3600.0 / 1000.0
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

    /**
     * Km since DPF filter replacement (Torque Astra-J 1.3: 223276, 3-byte km, like kmSinceRegen).
     * Unverified formula on Aveo 1.3D — confirmed responding via discovery probe.
     */
    val kmSinceDpfReplace = ExtPids.mode22(
        did = 0x3276,
        namePl = "Km od wymiany DPF",
        nameEn = "Km since DPF replace",
        unit = "km",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode22 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode22 null
            val c = bytes.getOrNull(2)?.toUByte()?.toInt() ?: return@mode22 null
            (a * 65536 + b * 256 + c).toDouble()
        },
    )

    /**
     * Average distance between regenerations (Torque Astra-J 1.3: 223278, 3-byte km).
     * Unverified formula on Aveo 1.3D — confirmed responding via discovery probe.
     */
    val avgKmBetweenRegen = ExtPids.mode22(
        did = 0x3278,
        namePl = "Śr. dystans między regen.",
        nameEn = "Avg km between regen",
        unit = "km",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode22 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode22 null
            val c = bytes.getOrNull(2)?.toUByte()?.toInt() ?: return@mode22 null
            (a * 65536 + b * 256 + c).toDouble()
        },
    )

    /**
     * Average regen duration (Torque Astra-J 1.3: 22327A, 2-byte, unit unconfirmed — likely seconds).
     * Unverified formula on Aveo 1.3D — confirmed responding via discovery probe.
     */
    val avgRegenDuration = ExtPids.mode22(
        did = 0x327A,
        namePl = "Śr. czas regeneracji",
        nameEn = "Avg regen duration",
        unit = "s",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode22 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode22 null
            ((a * 256) + b).toDouble()
        },
    )

    /**
     * Interrupted regeneration count (Torque Astra-J 1.3: 223047, 2-byte raw count).
     * Unverified formula on Aveo 1.3D — confirmed responding via discovery probe.
     */
    val interruptedRegenCount = ExtPids.mode22(
        did = 0x3047,
        namePl = "Przerwane regeneracje",
        nameEn = "Interrupted regen count",
        unit = "",
        decode = { bytes ->
            val a = bytes.getOrNull(0)?.toUByte()?.toInt() ?: return@mode22 null
            val b = bytes.getOrNull(1)?.toUByte()?.toInt() ?: return@mode22 null
            ((a * 256) + b).toDouble()
        },
    )

    /**
     * DPF differential pressure sensor voltage (Torque Astra-J 1.3: 223035, 1-byte, 0.02V/step guess).
     * Unverified formula on Aveo 1.3D — confirmed responding via discovery probe.
     */
    val dpfDeltaPressureSensorV = ExtPids.mode22(
        did = 0x3035,
        namePl = "Czujnik ΔP DPF",
        nameEn = "DPF ΔP sensor",
        unit = "V",
        decode = { bytes -> bytes.getOrNull(0)?.toUByte()?.toInt()?.let { it * 0.02 } },
    )

    val pollList: List<ExtPidDefinition> = listOf(
        sootLoad,
        dpfPressure,
        kmSinceRegen,
        dpfStatus,
        dpfTempGm,
        dpfTempMode01,
        kmSinceDpfReplace,
        avgKmBetweenRegen,
        avgRegenDuration,
        interruptedRegenCount,
        dpfDeltaPressureSensorV,
    )
}
