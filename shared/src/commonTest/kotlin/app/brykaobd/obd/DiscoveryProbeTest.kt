package app.brykaobd.obd

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DiscoveryProbeTest {
    @Test
    fun demo_probe_finds_mode01_and_mode22_hits() = runBlocking {
        val session = Elm327Session(DemoElmTransport())
        session.initialize()
        val results = session.probeDiscovery(PidDiscoveryMap.aveoFirstProbe)
        assertTrue(results.any { it.isHit && it.candidate.request == "010D" })
        assertTrue(results.any { it.isHit && it.candidate.request == "223275" })
        assertTrue(results.count { it.isHit } >= 5)
        session.close()
    }
}
