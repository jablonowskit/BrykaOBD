package app.brykaobd.obd

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Desktop bridge: list COM/`tty` ports and open [SerialTransport].
 * Reuses [BluetoothElmFacade] so the shared Connect UI works without a second code path.
 */
class SerialElmFacade(
    private val baudRate: Int = DEFAULT_BAUD,
) : BluetoothElmFacade {
    override fun isBluetoothUsable(): Boolean = true

    override fun bondedAdapters(): List<BluetoothAdapterInfo> =
        SerialPort.getCommPorts()
            .map { port ->
                val sys = port.systemPortName
                val desc = port.descriptivePortName?.takeIf { it.isNotBlank() && it != sys }
                BluetoothAdapterInfo(
                    name = if (desc != null) "$sys — $desc" else sys,
                    address = sys,
                )
            }
            .sortedBy { it.address.lowercase() }

    override suspend fun connect(address: String, diag: ObdDiagLog): Transport =
        withContext(Dispatchers.IO) {
            SerialTransport.open(address, baudRate, diag)
        }

    companion object {
        /** Classic ELM327 UART default; many USB clones also use 38400. */
        const val DEFAULT_BAUD: Int = 38400
    }
}
