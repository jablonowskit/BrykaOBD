package app.brykaobd.obd

/**
 * In-memory transport for unit tests (no hardware).
 */
class FakeTransport(
    private val responses: MutableList<String> = mutableListOf(),
) : Transport {
    val written: MutableList<String> = mutableListOf()

    fun enqueue(response: String) {
        responses.add(response)
    }

    override suspend fun write(data: String) {
        written.add(data)
    }

    override suspend fun readUntilPrompt(timeoutMs: Long): String {
        if (responses.isEmpty()) {
            error("FakeTransport: no queued response (timeoutMs=$timeoutMs)")
        }
        return responses.removeAt(0)
    }

    override fun close() {
        responses.clear()
    }
}
