package app.brykaobd.obd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ObdDiagLogTest {
    @Test
    fun recordsTxRxAndPid() = runBlocking {
        val diag = ObdDiagLog(capacity = 50)
        val fake = FakeTransport()
        repeat(6) { fake.enqueue("OK\r\n>") }
        fake.enqueue("41 0C 1A F8\r\n>")
        fake.enqueue("NO DATA\r\n>")
        val transport = LoggingTransport(fake, diag)
        val session = Elm327Session(transport, diag)
        val rpm = session.readPid(StandardPids.rpm)
        assertEquals(1726.0, rpm.value)
        val speed = session.readPid(StandardPids.speed)
        assertEquals("NO DATA", speed.error)

        val text = diag.asText()
        assertTrue(text.contains("TX"), text)
        assertTrue(text.contains("RX"), text)
        assertTrue(text.contains("0C"), text)
        assertTrue(text.contains("NO DATA"), text)
        assertTrue(diag.snapshot().any { it.category == "AT" })
    }

    @Test
    fun ringBufferDropsOldest() {
        val diag = ObdDiagLog(capacity = 3)
        diag.info("A", "1")
        diag.info("A", "2")
        diag.info("A", "3")
        diag.info("A", "4")
        val messages = diag.snapshot().map { it.message }
        assertEquals(listOf("2", "3", "4"), messages)
    }
}
