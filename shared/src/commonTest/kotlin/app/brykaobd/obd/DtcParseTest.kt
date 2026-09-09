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
    fun parseMode03ContinuousHexFromAveoVlink() {
        // SEARCHING... then 43 02 04 03 04 05 without spaces
        val codes = ElmParser.parseMode03Dtcs("SEARCHING...\r430204030405\r\r>")
        assertEquals(listOf("P0403", "P0405"), codes)
    }

    @Test
    fun catalogHasAveoAndDemoCodes() {
        assertTrue(DtcCatalog.size >= 1500)
        assertEquals("EGR — obwód zaworu / sterowanie", DtcCatalog.descriptionPl("P0403"))
        assertEquals("Czujnik pozycji EGR A — sygnał za niski", DtcCatalog.descriptionPl("p0405"))
        assertEquals("Sprawność katalizatora poniżej progu (bank 1)", DtcCatalog.descriptionPl("P0420"))
        assertEquals("Driver Frontal Stage 1 Deployment Control", DtcCatalog.descriptionPl("B0001"))
        assertEquals("Zapisany kod usterki", DtcCatalog.descriptionPl("P9999"))
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
