package app.brykaobd.obd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class VehicleIdentityTest {
    @Test
    fun parseDemoVin() {
        val vin = VehicleIdentityParser.parseVin(
            "49 02 01 4B 4C 31 4A 46 35 36 45 39 42 4B 31 32 33 34 35 36\r\n>",
        )
        assertEquals("KL1JF56E9BK123456", vin)
        assertEquals("Chevrolet", VehicleIdentityParser.manufacturerHint(vin!!))
    }

    @Test
    fun sessionReadsDemoIdentity() = runBlocking {
        val diag = ObdDiagLog()
        val session = Elm327Session(LoggingTransport(DemoElmTransport(), diag), diag)
        session.initialize()
        val id = session.readVehicleIdentity()
        assertNull(id.error)
        assertEquals("KL1JF56E9BK123456", id.vin)
        assertEquals("Chevrolet", id.manufacturerHint)
    }
}
