package app.brykaobd.obd

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * File-based [DiagArchive] shared by Android and JVM (desktop / tests).
 */
class FileDiagArchive(
    rootDir: File,
    private val maxSessions: Int = 40,
) : DiagArchive {
    private val dir = rootDir.also { it.mkdirs() }
    private val lock = Any()
    private var current: File? = null

    override fun beginSession(label: String): DiagSessionInfo = synchronized(lock) {
        endSessionUnlocked()
        val safe = label.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40).ifBlank { "session" }
        val stamp = LocalDateTime.now().format(STAMP)
        val file = File(dir, "brykaobd_${stamp}_$safe.txt")
        file.writeText("BrykaOBD diag session\nlabel=$label\nstarted=$stamp\n\n")
        current = file
        pruneOldUnlocked()
        toInfo(file)
    }

    override fun appendLine(line: String) {
        synchronized(lock) {
            val file = current ?: return
            file.appendText(line + "\n")
        }
    }

    override fun endSession() = synchronized(lock) {
        endSessionUnlocked()
    }

    override fun currentSession(): DiagSessionInfo? = synchronized(lock) {
        current?.let { toInfo(it) }
    }

    override fun listSessions(): List<DiagSessionInfo> = synchronized(lock) {
        dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { toInfo(it) }
            .orEmpty()
    }

    override fun readText(fileName: String): String = synchronized(lock) {
        val file = File(dir, fileName)
        require(file.canonicalFile.parentFile == dir.canonicalFile) { "Invalid session name" }
        require(file.isFile) { "Session not found: $fileName" }
        file.readText()
    }

    override fun directoryHint(): String = dir.absolutePath

    private fun endSessionUnlocked() {
        val file = current ?: return
        file.appendText("\n--- end session ---\n")
        current = null
    }

    private fun pruneOldUnlocked() {
        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }.orEmpty()
            .sortedByDescending { it.lastModified() }
        files.drop(maxSessions).forEach { it.delete() }
    }

    private fun toInfo(file: File) = DiagSessionInfo(
        fileName = file.name,
        absolutePath = file.absolutePath,
        bytes = file.length(),
    )

    companion object {
        private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}
