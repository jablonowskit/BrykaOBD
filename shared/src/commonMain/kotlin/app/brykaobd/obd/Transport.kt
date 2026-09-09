package app.brykaobd.obd

/**
 * Byte stream to an ELM327 (serial on desktop, Classic SPP on Android).
 */
interface Transport {
    suspend fun write(data: String)
    suspend fun readUntilPrompt(timeoutMs: Long = 5_000): String
    fun close()
}
