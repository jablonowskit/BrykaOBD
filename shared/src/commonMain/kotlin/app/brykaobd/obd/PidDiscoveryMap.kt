package app.brykaobd.obd

/**
 * Curated OBD requests to probe on Aveo 1.3D / Opel GM diesels during discovery.
 *
 * Sources (community — verify per ECU):
 * - Torque forum: Astra-J 1.3 2012 Mode 22 DPF set (header 7E0)
 * - Torque forum: Astra-K Mode 22 alternatives (may differ on Aveo)
 * - SAE Mode 01 support bitmasks (`0100`…`01A0`)
 *
 * Not a full UDS DID dump (0x0000–0xFFFF) — that would take hours on ELM.
 */
data class DiscoveryCandidate(
    val request: String,
    val namePl: String,
    val nameEn: String,
    val address: ObdAddress,
    val source: String,
)

enum class DiscoveryKind {
    Positive62,
    Positive41,
    Positive5A,
    NoData,
    UdsNeg,
    Other,
}

data class DiscoveryResult(
    val candidate: DiscoveryCandidate,
    val raw: String,
    val kind: DiscoveryKind,
    val payloadHex: String?,
) {
    val isHit: Boolean
        get() = kind == DiscoveryKind.Positive62 ||
            kind == DiscoveryKind.Positive41 ||
            kind == DiscoveryKind.Positive5A
}

object PidDiscoveryMap {
    /** Astra-J 1.3 2012 DPF set (cintakc / Torque) — closest public map to Aveo 1.3D. */
    val astraJ13Dpf: List<DiscoveryCandidate> = listOf(
        c22(0x3035, "Czujnik ΔP DPF (V)", "DPF ΔP sensor V", "Torque Astra-J 1.3"),
        c22(0x3273, "Ciśnienie DPF", "DPF pressure", "Torque Astra-J 1.3"),
        c22(0x3274, "Stan regeneracji DPF", "DPF regen state", "Torque Astra-J 1.3"),
        c22(0x3275, "Zapełnienie DPF", "DPF soot", "Torque Astra-J 1.3"),
        c22(0x3276, "Km od wymiany DPF", "Km since DPF replace", "Torque Astra-J 1.3"),
        c22(0x3277, "Km od regeneracji", "Km since regen", "Torque Astra-J 1.3"),
        c22(0x3278, "Śr. dystans między regen", "Avg km between regen", "Torque Astra-J 1.3"),
        c22(0x3279, "Temp. wlotu DPF (śr.)", "DPF inlet temp avg", "Torque Astra-J 1.3"),
        c22(0x327A, "Śr. czas regeneracji", "Avg regen duration", "Torque Astra-J 1.3"),
        c22(0x3047, "Licznik przerwanych regen", "Interrupted regen count", "Torque Astra-J 1.3"),
    )

    /** Astra-K / newer GM alternatives — probe in case Aveo ECU differs. */
    val astraKAlternates: List<DiscoveryCandidate> = listOf(
        c22(0x3039, "Km od regen (K)", "Km since regen K", "Torque Astra-K"),
        c22(0x20F4, "Ciśnienie DPF (K)", "DPF pressure K", "Torque Astra-K"),
        c22(0x20F5, "Przepływ DPF (K)", "DPF flow K", "Torque Astra-K"),
        c22(0x20F8, "Temp. wlotu DPF (K)", "DPF inlet temp K", "Torque Astra-K"),
        c22(0x20FA, "Status regen (K)", "DPF regen status K", "Torque Astra-K"),
        c22(0x20FD, "O2 DPF HO2S1 (K)", "DPF HO2S1 conc K", "Torque Astra-K"),
        c22(0x336A, "Zapełnienie DPF (K)", "DPF soot K", "Torque Astra-K"),
        c22(0x23AD, "O2 DPF HO2S2 (K)", "DPF HO2S2 conc K", "Torque Astra-K"),
        c22(0x0034, "Lambda HO2S1 (K)", "HO2S1 lambda K", "Torque Astra-K"),
    )

    /** Mode 01 support bitmasks (functional). */
    val mode01Support: List<DiscoveryCandidate> = listOf(
        c01("0100", "Wsparcie PID 01–20", "PID support 01-20"),
        c01("0120", "Wsparcie PID 21–40", "PID support 21-40"),
        c01("0140", "Wsparcie PID 41–60", "PID support 41-60"),
        c01("0160", "Wsparcie PID 61–80", "PID support 61-80"),
        c01("0180", "Wsparcie PID 81–A0", "PID support 81-A0"),
        c01("01A0", "Wsparcie PID A1–C0", "PID support A1-C0"),
    )

    /** Useful Mode 01 singles often present on diesel (functional). */
    val mode01Extras: List<DiscoveryCandidate> = listOf(
        c01("0104", "Obciążenie", "Engine load"),
        c01("0105", "Chłodziwo", "Coolant"),
        c01("010B", "MAP", "MAP"),
        c01("010C", "RPM", "RPM"),
        c01("010D", "Prędkość", "Speed"),
        c01("010F", "IAT", "Intake air temp"),
        c01("0111", "Przepustnica", "Throttle"),
        c01("011C", "OBD standard", "OBD standard"),
        c01("011F", "Czas od startu", "Run time"),
        c01("012F", "Poziom paliwa", "Fuel level"),
        c01("0133", "BARO", "Barometric"),
        c01("0142", "Napięcie", "Control module V"),
        c01("0146", "Temp. powietrza amb.", "Ambient air"),
        c01("015E", "Zużycie L/h", "Fuel rate"),
        c01("017C", "Temp. DPF SAE", "DPF temp SAE"),
    )

    val aveoFirstProbe: List<DiscoveryCandidate> =
        mode01Support + mode01Extras + astraJ13Dpf + astraKAlternates

    private fun c22(did: Int, namePl: String, nameEn: String, source: String) =
        DiscoveryCandidate(
            request = "22" + did.toString(16).padStart(4, '0').uppercase(),
            namePl = namePl,
            nameEn = nameEn,
            address = ObdAddress.EcmPhysical,
            source = source,
        )

    private fun c01(request: String, namePl: String, nameEn: String) =
        DiscoveryCandidate(
            request = request.uppercase(),
            namePl = namePl,
            nameEn = nameEn,
            address = ObdAddress.Functional,
            source = "SAE J1979",
        )
}
