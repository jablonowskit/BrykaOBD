package app.brykaobd.obd

/**
 * Paired Classic Bluetooth adapter (ELM327), shown in the connect UI.
 */
data class BluetoothAdapterInfo(
    val name: String,
    val address: String,
)

/**
 * Platform bridge for Classic SPP. Null / unavailable on desktop until serial is wired.
 */
interface BluetoothElmFacade {
    fun isBluetoothUsable(): Boolean
    fun bondedAdapters(): List<BluetoothAdapterInfo>
    suspend fun connect(address: String): Transport
}
