package app.brykaobd.obd

/**
 * Minimal ELM327 session: AT init + Mode 01/22 PID + Mode 03/04 DTC over [Transport].
 * Pass [diag] (and prefer [LoggingTransport]) for detailed Aveo / clone diagnostics.
 */
class Elm327Session(
    private val transport: Transport,
    val diag: ObdDiagLog = ObdDiagLog(),
) {
    private var initialized = false
    private var address = ObdAddress.Functional

    suspend fun initialize() {
        diag.info("SESSION", "ELM init start (ATZ…ATSP0)")
        val atCommands = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
        for (cmd in atCommands) {
            sendAt(cmd)
        }
        address = ObdAddress.Functional
        initialized = true
        diag.info("SESSION", "ELM init done")
    }

    suspend fun setAddress(target: ObdAddress) {
        ensureInit()
        if (address == target) return
        when (target) {
            ObdAddress.EcmPhysical -> {
                // Car Scanner / Torque GM DPF PIDs use header 7E0 (ECM physical).
                sendAt("ATSH7E0")
                diag.info("AT", "Address → ECM physical ATSH7E0 (Mode 22)")
            }
            ObdAddress.Functional -> {
                sendAt("ATSH7DF")
                diag.info("AT", "Address → functional ATSH7DF (Mode 01)")
            }
        }
        address = target
    }

    suspend fun readPid(pid: PidDefinition): PidReading {
        ensureInit()
        setAddress(ObdAddress.Functional)
        transport.write("${pid.mode01Request()}\r")
        val reply = transport.readUntilPrompt()
        val err = ElmParser.isErrorResponse(reply)
        if (err != null) {
            diag.pidResult(pid.idHex, pid.nameEn, null, err)
            return PidReading(pid, value = null, error = err)
        }
        val data = ElmParser.extractMode01Data(reply, pid.id)
        if (data == null) {
            diag.pidResult(pid.idHex, pid.nameEn, null, "PARSE")
            return PidReading(pid, value = null, error = "PARSE")
        }
        val value = pid.decode(data)
        if (value == null) {
            diag.pidResult(pid.idHex, pid.nameEn, null, "DECODE")
            return PidReading(pid, value = null, error = "DECODE")
        }
        diag.pidResult(pid.idHex, pid.nameEn, value, null)
        return PidReading(pid, value = value)
    }

    suspend fun readDashboard(pids: List<PidDefinition> = StandardPids.dashboard): List<PidReading> =
        pids.map { readPid(it) }

    suspend fun readExtPid(
        pid: ExtPidDefinition,
        address: ObdAddress = defaultAddressFor(pid),
    ): ExtPidReading {
        ensureInit()
        setAddress(address)
        transport.write("${pid.request}\r")
        val reply = transport.readUntilPrompt()
        val err = ElmParser.isErrorResponse(reply)
        if (err != null) {
            diag.pidResult(pid.idHex, pid.nameEn, null, err)
            return ExtPidReading(pid, value = null, error = err)
        }
        val data = ElmParser.extractPositiveResponseData(
            reply,
            responseService = pid.responseService,
            matchIds = pid.matchIds,
        )
        if (data == null) {
            diag.pidResult(pid.idHex, pid.nameEn, null, "PARSE")
            return ExtPidReading(pid, value = null, error = "PARSE")
        }
        val value = pid.decode(data)
        if (value == null) {
            diag.pidResult(pid.idHex, pid.nameEn, null, "DECODE")
            return ExtPidReading(pid, value = null, error = "DECODE")
        }
        diag.pidResult(pid.idHex, pid.nameEn, value, null)
        return ExtPidReading(pid, value = value)
    }

    suspend fun readExtList(
        pids: List<ExtPidDefinition>,
        address: ObdAddress? = null,
    ): List<ExtPidReading> {
        val target = address ?: pids.firstOrNull()?.let { defaultAddressFor(it) } ?: ObdAddress.Functional
        ensureInit()
        setAddress(target)
        return pids.map { readExtPid(it, target) }
    }

    /**
     * One-shot probe of curated Mode 01 / Mode 22 candidates (log raw RX for Aveo mapping).
     * Groups by address to avoid flipping ATSH every request.
     */
    suspend fun probeDiscovery(
        candidates: List<DiscoveryCandidate> = PidDiscoveryMap.aveoFirstProbe,
    ): List<DiscoveryResult> {
        ensureInit()
        val results = ArrayList<DiscoveryResult>(candidates.size)
        for ((addr, group) in candidates.groupBy { it.address }) {
            setAddress(addr)
            for (c in group) {
                transport.write("${c.request}\r")
                val reply = transport.readUntilPrompt()
                val result = classifyDiscovery(c, reply)
                results += result
                val tag = if (result.isHit) "HIT" else result.kind.name
                diag.info(
                    "DISCOVERY",
                    "$tag ${c.request} ${c.nameEn} ← ${result.payloadHex ?: result.kind.name}",
                )
            }
        }
        val hits = results.count { it.isHit }
        diag.info("DISCOVERY", "Probe done: $hits hit(s) / ${results.size} requests")
        return results
    }

    /**
     * Mode 09 VIN (`0902`) over functional addressing — first step of automatic vehicle ID.
     */
    suspend fun readVehicleIdentity(): VehicleIdentity {
        ensureInit()
        setAddress(ObdAddress.Functional)
        transport.write("0902\r")
        val reply = transport.readUntilPrompt()
        val err = ElmParser.isErrorResponse(reply)
        if (err != null) {
            diag.warn("VEHICLE", "VIN 0902 → $err")
            return VehicleIdentity(error = err)
        }
        val vin = VehicleIdentityParser.parseVin(reply)
        if (vin == null) {
            diag.warn("VEHICLE", "VIN 0902 → PARSE (raw logged as RX)")
            return VehicleIdentity(error = "PARSE")
        }
        val hint = VehicleIdentityParser.manufacturerHint(vin)
        diag.info("VEHICLE", "VIN $vin" + (hint?.let { " ($it)" } ?: ""))
        return VehicleIdentity(vin = vin, manufacturerHint = hint)
    }

    suspend fun readStoredDtcs(): DtcReadResult {
        ensureInit()
        setAddress(ObdAddress.Functional)
        transport.write("03\r")
        val reply = transport.readUntilPrompt()
        val err = ElmParser.isErrorResponse(reply)
        if (err != null && err != "NO DATA") {
            diag.warn("DTC", "Mode 03 → $err")
            return DtcReadResult(error = err)
        }
        if (err == "NO DATA") {
            diag.info("DTC", "Mode 03 → brak kodów (NO DATA)")
            return DtcReadResult(codes = emptyList())
        }
        val codes = ElmParser.parseMode03Dtcs(reply).map { DtcCode(it) }
        diag.info("DTC", "Mode 03 → ${codes.size} kod(ów): ${codes.joinToString { it.code }}")
        return DtcReadResult(codes = codes)
    }

    suspend fun clearStoredDtcs(): DtcReadResult {
        ensureInit()
        setAddress(ObdAddress.Functional)
        transport.write("04\r")
        val reply = transport.readUntilPrompt()
        if (!ElmParser.isMode04Success(reply)) {
            val err = ElmParser.isErrorResponse(reply) ?: "CLEAR_FAILED"
            diag.warn("DTC", "Mode 04 → $err")
            return DtcReadResult(error = err)
        }
        diag.info("DTC", "Mode 04 → wyczyszczono, ponowny odczyt 03")
        return readStoredDtcs()
    }

    fun close() {
        diag.info("SESSION", "Session close")
        transport.close()
        initialized = false
        address = ObdAddress.Functional
    }

    private fun defaultAddressFor(pid: ExtPidDefinition): ObdAddress =
        if (pid.responseService == 0x62) ObdAddress.EcmPhysical else ObdAddress.Functional

    private fun classifyDiscovery(c: DiscoveryCandidate, reply: String): DiscoveryResult {
        val compact = reply.replace("\r", " ").replace("\n", " ").trim()
        ElmParser.udsNegativeResponse(reply)?.let {
            return DiscoveryResult(c, compact, DiscoveryKind.UdsNeg, payloadHex = it)
        }
        if (ElmParser.isErrorResponse(reply) == "NO DATA") {
            return DiscoveryResult(c, compact, DiscoveryKind.NoData, payloadHex = null)
        }
        val bytes = ElmParser.extractPositiveResponseData(
            reply,
            responseService = when {
                c.request.startsWith("22") -> 0x62
                c.request.startsWith("01") -> 0x41
                c.request.startsWith("1A") -> 0x5A
                else -> 0x62
            },
            matchIds = matchIdsForRequest(c.request),
        )
        if (bytes != null) {
            val kind = when {
                c.request.startsWith("22") -> DiscoveryKind.Positive62
                c.request.startsWith("1A") -> DiscoveryKind.Positive5A
                else -> DiscoveryKind.Positive41
            }
            val hex = bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }.uppercase()
            return DiscoveryResult(c, compact, kind, payloadHex = hex)
        }
        return DiscoveryResult(c, compact, DiscoveryKind.Other, payloadHex = null)
    }

    private fun matchIdsForRequest(request: String): IntArray {
        val hex = request.uppercase().removePrefix("22").removePrefix("01").removePrefix("1A")
        return when {
            request.uppercase().startsWith("22") && hex.length >= 4 ->
                intArrayOf(hex.substring(0, 2).toInt(16), hex.substring(2, 4).toInt(16))
            request.uppercase().startsWith("01") && hex.length >= 2 ->
                intArrayOf(hex.substring(0, 2).toInt(16))
            request.uppercase().startsWith("1A") && hex.length >= 2 ->
                intArrayOf(hex.substring(0, 2).toInt(16))
            else -> intArrayOf()
        }
    }

    private suspend fun ensureInit() {
        if (!initialized) initialize()
    }

    private suspend fun sendAt(cmd: String) {
        transport.write("$cmd\r")
        val reply = transport.readUntilPrompt()
        val err = ElmParser.isErrorResponse(reply)
        if (err != null) {
            diag.warn("AT", "$cmd → flagged: $err (raw logged as RX)")
        } else {
            diag.info("AT", "$cmd → OK / banner")
        }
    }
}
