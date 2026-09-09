package app.brykaobd.obd

/**
 * Minimal ELM327 session: AT init + Mode 01 PID reads over [Transport].
 */
class Elm327Session(
    private val transport: Transport,
) {
    private var initialized = false

    suspend fun initialize() {
        val atCommands = listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
        for (cmd in atCommands) {
            transport.write("$cmd\r")
            val reply = transport.readUntilPrompt()
            val err = ElmParser.isErrorResponse(reply)
            // ATZ banners are fine; real link errors matter later on OBD requests
            if (err != null && cmd != "ATZ" && cmd != "ATSP0") {
                // continue — many clones still work after noisy AT replies
            }
        }
        initialized = true
    }

    suspend fun readPid(pid: PidDefinition): PidReading {
        if (!initialized) initialize()
        transport.write("${pid.mode01Request()}\r")
        val reply = transport.readUntilPrompt()
        val err = ElmParser.isErrorResponse(reply)
        if (err != null) {
            return PidReading(pid, value = null, error = err)
        }
        val data = ElmParser.extractMode01Data(reply, pid.id)
            ?: return PidReading(pid, value = null, error = "PARSE")
        val value = pid.decode(data)
            ?: return PidReading(pid, value = null, error = "DECODE")
        return PidReading(pid, value = value)
    }

    suspend fun readDashboard(pids: List<PidDefinition> = StandardPids.dashboard): List<PidReading> =
        pids.map { readPid(it) }

    fun close() {
        transport.close()
        initialized = false
    }
}
