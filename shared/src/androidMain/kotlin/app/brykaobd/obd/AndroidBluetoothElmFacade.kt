package app.brykaobd.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private val SppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

class AndroidBluetoothElmFacade(
    context: Context,
) : BluetoothElmFacade {
    private val appContext = context.applicationContext

    private fun adapterOrNull(): BluetoothAdapter? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = appContext.getSystemService(BluetoothManager::class.java)
            manager?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }
    }

    override fun isBluetoothUsable(): Boolean {
        val adapter = adapterOrNull() ?: return false
        return adapter.isEnabled
    }

    @SuppressLint("MissingPermission")
    override fun bondedAdapters(): List<BluetoothAdapterInfo> {
        val adapter = adapterOrNull() ?: return emptyList()
        return adapter.bondedDevices
            .map { device ->
                BluetoothAdapterInfo(
                    name = device.name?.ifBlank { null } ?: "ELM / BT",
                    address = device.address,
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Transport = withContext(Dispatchers.IO) {
        val adapter = adapterOrNull() ?: error("Brak adaptera Bluetooth w telefonie")
        if (!adapter.isEnabled) error("Włącz Bluetooth")
        adapter.cancelDiscovery()
        val device = adapter.getRemoteDevice(address)
        val socket = device.createRfcommSocketToServiceRecord(SppUuid)
        try {
            socket.connect()
        } catch (e: Exception) {
            runCatching { socket.close() }
            throw IllegalStateException("Nie połączono z $address: ${e.message}", e)
        }
        BluetoothClassicTransport(socket)
    }
}
