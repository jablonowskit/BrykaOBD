package app.brykaobd.obd

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class FileDiagArchiveTest {
    @Test
    fun persistsSessionAcrossReads() {
        val dir = createTempDir(prefix = "brykaobd-diag-")
        try {
            val archive = FileDiagArchive(dir, maxSessions = 5)
            val info = archive.beginSession("Aveo test")
            archive.appendLine("hello")
            archive.appendLine("world")
            archive.endSession()

            val text = archive.readText(info.fileName)
            assertTrue(text.contains("hello"))
            assertTrue(text.contains("world"))
            assertTrue(text.contains("end session"))
            assertEquals(1, archive.listSessions().size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun diagLogWritesArchive() = runBlocking {
        val dir = createTempDir(prefix = "brykaobd-diag-log-")
        try {
            val archive = FileDiagArchive(dir)
            val diag = ObdDiagLog(capacity = 50, archive = archive)
            val saved = diag.beginPersistedSession("unit")!!
            diag.info("TEST", "line-a")
            diag.endPersistedSession()
            val text = archive.readText(saved.fileName)
            assertTrue(text.contains("TEST"), text)
            assertTrue(text.contains("line-a"), text)
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun createTempDir(prefix: String): File =
        File(System.getProperty("java.io.tmpdir"), prefix + System.nanoTime()).also { it.mkdirs() }
}
