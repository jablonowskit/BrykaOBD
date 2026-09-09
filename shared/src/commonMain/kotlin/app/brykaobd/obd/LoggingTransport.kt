package app.brykaobd.obd

/**
 * Wraps any [Transport] and records raw TX/RX (and failures) into [diag].
 */
class LoggingTransport(
    private val inner: Transport,
    private val diag: ObdDiagLog,
) : Transport {
    override suspend fun write(data: String) {
        diag.tx(data)
        try {
            inner.write(data)
        } catch (e: Exception) {
            diag.error("TX", e.message ?: e.toString())
            throw e
        }
    }

    override suspend fun readUntilPrompt(timeoutMs: Long): String {
        return try {
            val reply = inner.readUntilPrompt(timeoutMs)
            diag.rx(reply)
            reply
        } catch (e: Exception) {
            diag.error("RX", e.message ?: e.toString())
            throw e
        }
    }

    override fun close() {
        diag.info("SESSION", "Transport closed")
        inner.close()
    }
}
