package app.brykaobd.obd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class PidDecodeTest {
    @Test
    fun rpmDecode() {
        // 0x1AF8 -> ((0x1A*256)+0xF8)/4 = 1726
        val value = StandardPids.rpm.decode(byteArrayOf(0x1A, 0xF8.toByte()))
        assertEquals(1726.0, value)
    }

    @Test
    fun speedDecode() {
        assertEquals(80.0, StandardPids.speed.decode(byteArrayOf(80)))
    }

    @Test
    fun coolantDecode() {
        assertEquals(90.0, StandardPids.coolantTemp.decode(byteArrayOf(130.toByte())))
    }

    @Test
    fun extractRpmFromElmResponse() {
        val data = ElmParser.extractMode01Data("41 0C 1A F8\r\n>", 0x0C)
        assertEquals(2, data!!.size)
        assertEquals(1726.0, StandardPids.rpm.decode(data))
    }

    @Test
    fun noDataError() {
        assertEquals("NO DATA", ElmParser.isErrorResponse("NO DATA\r\n>"))
        assertNull(ElmParser.extractMode01Data("NO DATA\r\n>", 0x0C))
    }

    @Test
    fun extractCoolantFromAts0ContinuousHex() {
        // Real Aveo/V-LINK RX with spaces off (ATS0): 0x3B → 19°C
        val data = ElmParser.extractMode01Data("41053B\r\r>", 0x05)
        assertEquals(1, data!!.size)
        assertEquals(19.0, StandardPids.coolantTemp.decode(data))
    }

    @Test
    fun extractVoltageContinuousHex() {
        val data = ElmParser.extractMode01Data("414230E8\r\r>", 0x42)
        assertEquals(12.52, StandardPids.controlModuleVoltage.decode(data!!))
    }
}