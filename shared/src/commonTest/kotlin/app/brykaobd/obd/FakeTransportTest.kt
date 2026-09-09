package app.brykaobd.obd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class FakeTransportTest {
    @Test
    fun writeAndReadQueuedResponse() = runBlocking {
        val transport = FakeTransport()
        transport.enqueue("OK\r\n>")
        transport.write("ATI\r")
        assertEquals(listOf("ATI\r"), transport.written)
        assertEquals("OK\r\n>", transport.readUntilPrompt())
    }
}
