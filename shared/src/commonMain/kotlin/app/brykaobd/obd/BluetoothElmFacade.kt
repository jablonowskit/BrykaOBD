package app.brykaobd.obd

/**
 * Paired Classic Bluetooth adapter (ELM327), shown in the connect UI.
 */
data class BluetoothAdapterInfo(
    val name: String,
    val address: String,
)

/**
 * Platform bridge for Classic SPP (Android) or serial/COM (desktop).
 * [BluetoothAdapterInfo.address] is MAC on Android and system port name (e.g. COM3) on desktop.
 */
interface BluetoothElmFacade {
    fun isBluetoothUsable(): Boolean
    fun bondedAdapters(): List<BluetoothAdapterInfo>
    suspend fun connect(address: String, diag: ObdDiagLog): Transport
}
