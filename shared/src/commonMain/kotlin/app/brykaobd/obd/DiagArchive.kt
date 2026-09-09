package app.brykaobd.obd

/**
 * Metadata of a persisted diagnostic session file.
 */
data class DiagSessionInfo(
    val fileName: String,
    val absolutePath: String,
    val bytes: Long,
)

/**
 * Durable session logs so they survive app kill / return from the car.
 */
interface DiagArchive {
    /** Start a new file; further [appendLine] calls go there. */
    fun beginSession(label: String): DiagSessionInfo

    fun appendLine(line: String)

    fun endSession()

    fun currentSession(): DiagSessionInfo?

    fun listSessions(): List<DiagSessionInfo>

    fun readText(fileName: String): String

    /** Human-readable directory (for UI / adb pull hint). */
    fun directoryHint(): String
}

/**
 * Share / export a saved session (Android Intent; desktop may be no-op).
 */
interface DiagShareFacade {
    fun shareSessionFile(absolutePath: String, fileName: String)

    fun shareText(text: String, title: String = "BrykaOBD diag")
}
