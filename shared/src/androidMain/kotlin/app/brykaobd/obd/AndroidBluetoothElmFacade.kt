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
    override suspend fun connect(address: String, diag: ObdDiagLog): Transport = withContext(Dispatchers.IO) {
        diag.info("BT", "Connecting SPP to $address")
        val adapter = adapterOrNull() ?: run {
            diag.error("BT", "Brak adaptera Bluetooth w telefonie")
            error("Brak adaptera Bluetooth w telefonie")
        }
        if (!adapter.isEnabled) {
            diag.error("BT", "Bluetooth wyłączony")
            error("Włącz Bluetooth")
        }
        adapter.cancelDiscovery()
        val device = adapter.getRemoteDevice(address)
        val socket = device.createRfcommSocketToServiceRecord(SppUuid)
        try {
            socket.connect()
            diag.info("BT", "SPP connected: ${device.name ?: "?"} ($address)")
        } catch (e: Exception) {
            diag.error("BT", "Connect failed $address: ${e.message}")
            runCatching { socket.close() }
            throw IllegalStateException("Nie połączono z $address: ${e.message}", e)
        }
        BluetoothClassicTransport(socket)
    }
}
