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

    suspend fun initialize() {
        diag.info("SESSION", "ELM init start (ATZ…ATSP0)")
        val atCommands = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
        for (cmd in atCommands) {
            transport.write("$cmd\r")
            val reply = transport.readUntilPrompt()
            val err = ElmParser.isErrorResponse(reply)
            if (err != null) {
                diag.warn("AT", "$cmd → flagged: $err (raw logged as RX)")
            } else {
                diag.info("AT", "$cmd → OK / banner")
            }
        }
        initialized = true
        diag.info("SESSION", "ELM init done")
    }

    suspend fun readPid(pid: PidDefinition): PidReading {
        ensureInit()
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

    suspend fun readExtPid(pid: ExtPidDefinition): ExtPidReading {
        ensureInit()
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

    suspend fun readExtList(pids: List<ExtPidDefinition>): List<ExtPidReading> =
        pids.map { readExtPid(it) }

    suspend fun readStoredDtcs(): DtcReadResult {
        ensureInit()
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
    }

    private suspend fun ensureInit() {
        if (!initialized) initialize()
    }
}
