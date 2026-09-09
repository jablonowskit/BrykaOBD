package app.brykaobd.obd

/**
 * Minimal ELM327 session: AT init + Mode 01 PID reads over [Transport].
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
            // ATZ banners are fine; real link errors matter later on OBD requests
            if (err != null && cmd != "ATZ" && cmd != "ATSP0") {
                // continue — many clones still work after noisy AT replies
            }
        }
        initialized = true
        diag.info("SESSION", "ELM init done")
    }

    suspend fun readPid(pid: PidDefinition): PidReading {
        if (!initialized) initialize()
        transport.write("${pid.mode01Request()}\r")
        val reply = transport.readUntilPrompt()
        val err = ElmParser.isErrorResponse(reply)
        if (err != null) {
            val reading = PidReading(pid, value = null, error = err)
            diag.pidResult(pid.idHex, pid.nameEn, null, err)
            return reading
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

    fun close() {
        diag.info("SESSION", "Session close")
        transport.close()
        initialized = false
    }
}
