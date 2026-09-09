package app.brykaobd.obd

import kotlin.time.TimeSource

enum class DiagLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/**
 * One diagnostic line for ELM/OBD troubleshooting (Aveo, clone adapters, NO DATA).
 */
data class DiagEntry(
    val seq: Int,
    val elapsedMs: Long,
    val level: DiagLevel,
    val category: String,
    val message: String,
) {
    fun formatLine(): String {
        val t = elapsedMs.toString().padStart(6, ' ')
        val lvl = level.name.padEnd(5)
        return "$t ms | $lvl | $category | $message"
    }
}

/**
 * Ring-buffer session log (+ optional durable [archive] for post-drive retrieval).
 */
class ObdDiagLog(
    private val capacity: Int = 500,
    private val archive: DiagArchive? = null,
    private val onAppend: ((DiagEntry) -> Unit)? = null,
) {
    private val lock = Any()
    private val entries = ArrayDeque<DiagEntry>(capacity.coerceAtLeast(16))
    private var nextSeq = 1
    private val started = TimeSource.Monotonic.markNow()

    fun beginPersistedSession(label: String): DiagSessionInfo? =
        archive?.beginSession(label)

    fun endPersistedSession() {
        archive?.endSession()
    }

    fun clear() {
        synchronized(lock) {
            entries.clear()
            nextSeq = 1
        }
    }

    fun snapshot(): List<DiagEntry> = synchronized(lock) { entries.toList() }

    fun asText(): String = snapshot().joinToString("\n") { it.formatLine() }

    fun debug(category: String, message: String) = append(DiagLevel.DEBUG, category, message)

    fun info(category: String, message: String) = append(DiagLevel.INFO, category, message)

    fun warn(category: String, message: String) = append(DiagLevel.WARN, category, message)

    fun error(category: String, message: String) = append(DiagLevel.ERROR, category, message)

    fun tx(command: String) {
        val compact = command.replace("\r", "\\r").replace("\n", "\\n")
        debug("TX", compact)
    }

    fun rx(raw: String) {
        val compact = raw.replace("\r", "\\r").replace("\n", "\\n").take(240)
        debug("RX", compact)
    }

    fun pidResult(pidHex: String, name: String, value: Double?, error: String?) {
        when {
            error != null -> warn("PID", "$pidHex $name → $error")
            value != null -> info("PID", "$pidHex $name → $value")
            else -> warn("PID", "$pidHex $name → —")
        }
    }

    private fun append(level: DiagLevel, category: String, message: String) {
        val entry = synchronized(lock) {
            val e = DiagEntry(
                seq = nextSeq++,
                elapsedMs = started.elapsedNow().inWholeMilliseconds,
                level = level,
                category = category,
                message = message,
            )
            while (entries.size >= capacity) {
                entries.removeFirst()
            }
            entries.addLast(e)
            e
        }
        runCatching { archive?.appendLine(entry.formatLine()) }
        platformDiagLog(level, category, message)
        onAppend?.invoke(entry)
    }
}
