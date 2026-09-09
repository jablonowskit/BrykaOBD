package app.brykaobd.obd

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

/**
 * ELM327 over Bluetooth Classic RFCOMM/SPP.
 */
class BluetoothClassicTransport(
    private val socket: BluetoothSocket,
) : Transport {
    private val input: InputStream = socket.inputStream
    private val output: OutputStream = socket.outputStream

    override suspend fun write(data: String) = withContext(Dispatchers.IO) {
        output.write(data.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    override suspend fun readUntilPrompt(timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + timeoutMs
        val local = StringBuilder()
        while (System.currentTimeMillis() < deadline) {
            while (input.available() > 0) {
                val value = input.read()
                if (value < 0) {
                    error("Bluetooth stream closed before ELM prompt")
                }
                val ch = value.toChar()
                local.append(ch)
                if (ch == '>') {
                    return@withContext local.toString()
                }
            }
            Thread.sleep(15)
        }
        error("Timeout ${timeoutMs}ms waiting for '>' (got: ${local.toString().take(120)})")
    }

    override fun close() {
        runCatching { input.close() }
        runCatching { output.close() }
        runCatching { socket.close() }
    }
}
