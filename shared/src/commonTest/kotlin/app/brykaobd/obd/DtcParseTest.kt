package app.brykaobd.obd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DtcParseTest {
    @Test
    fun decodeP0301() {
        assertEquals("P0301", ElmParser.decodeDtcPair(0x03, 0x01))
    }

    @Test
    fun decodeP0420() {
        assertEquals("P0420", ElmParser.decodeDtcPair(0x04, 0x20))
    }

    @Test
    fun skipNullPair() {
        assertNull(ElmParser.decodeDtcPair(0, 0))
    }

    @Test
    fun parseMode03WithCount() {
        val codes = ElmParser.parseMode03Dtcs("43 02 03 01 04 20\r\n>")
        assertEquals(listOf("P0301", "P0420"), codes)
    }

    @Test
    fun noDataMeansEmpty() {
        assertTrue(ElmParser.parseMode03Dtcs("NO DATA\r\n>").isEmpty())
    }

    @Test
    fun sessionReadsAndClearsDemoDtcs() = runBlocking {
        val diag = ObdDiagLog()
        val session = Elm327Session(LoggingTransport(DemoElmTransport(), diag), diag)
        session.initialize()
        val before = session.readStoredDtcs()
        assertNull(before.error)
        assertEquals(listOf("P0301", "P0420"), before.codes.map { it.code })
        val after = session.clearStoredDtcs()
        assertNull(after.error)
        assertTrue(after.codes.isEmpty())
        assertTrue(diag.asText().contains("DTC"))
    }
}
