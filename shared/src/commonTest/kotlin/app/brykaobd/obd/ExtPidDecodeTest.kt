package app.brykaobd.obd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class ExtPidDecodeTest {
    @Test
    fun mode22_soot_and_km_parse() {
        val soot = ElmParser.extractPositiveResponseData(
            "62 32 75 2D",
            responseService = 0x62,
            matchIds = intArrayOf(0x32, 0x75),
        )
        assertEquals(45.0, DpfPids.sootLoad.decode(soot!!))

        val km = ElmParser.extractPositiveResponseData(
            "6232770004B0",
            responseService = 0x62,
            matchIds = intArrayOf(0x32, 0x77),
        )
        assertEquals(1200.0, DpfPids.kmSinceRegen.decode(km!!))
    }

    @Test
    fun fuel_rate_and_instant_l100() {
        val data = ElmParser.extractMode01Data("41 5E 00 64", 0x5E)
        assertEquals(5.0, GaugePids.fuelRate.decode(data!!))
        assertEquals(10.0, GaugePids.instantLitersPer100km(5.0, 50.0))
        assertNull(GaugePids.instantLitersPer100km(5.0, 4.0))
    }

    @Test
    fun demo_ext_session_reads_gauge_and_dpf() = runBlocking {
        val session = Elm327Session(DemoElmTransport())
        session.initialize()
        val gauges = session.readExtList(GaugePids.pollList)
        assertEquals(50.0, gauges.first { it.pid.request == GaugePids.speed.request }.value)
        assertEquals(5.0, gauges.first { it.pid.request == GaugePids.fuelRate.request }.value)
        val dpf = session.readExtList(DpfPids.pollList)
        assertEquals(45.0, dpf.first { it.pid.request == DpfPids.sootLoad.request }.value)
        assertEquals(1200.0, dpf.first { it.pid.request == DpfPids.kmSinceRegen.request }.value)
        session.close()
    }
}
