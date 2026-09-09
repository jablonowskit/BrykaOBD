package app.brykaobd.obd

import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ELM327 over a host serial/COM port (Windows Bluetooth SPP virtual COM, USB-TTL, etc.).
 */
class SerialTransport(
    private val port: SerialPort,
) : Transport {
    override suspend fun write(data: String) = withContext(Dispatchers.IO) {
        val bytes = data.toByteArray(Charsets.US_ASCII)
        var sent = 0
        while (sent < bytes.size) {
            val n = port.writeBytes(bytes, bytes.size - sent, sent)
            if (n < 0) error("Serial write failed on ${port.systemPortName}")
            sent += n
        }
    }

    override suspend fun readUntilPrompt(timeoutMs: Long): String = withContext(Dispatchers.IO) {
        val deadline = System.currentTimeMillis() + timeoutMs
        val local = StringBuilder()
        val buf = ByteArray(256)
        while (System.currentTimeMillis() < deadline) {
            val n = port.readBytes(buf, buf.size)
            if (n > 0) {
                for (i in 0 until n) {
                    val ch = buf[i].toInt().toChar()
                    local.append(ch)
                    if (ch == '>') {
                        return@withContext local.toString()
                    }
                }
            } else {
                Thread.sleep(15)
            }
        }
        error(
            "Timeout ${timeoutMs}ms waiting for '>' on ${port.systemPortName} " +
                "(got: ${local.toString().take(120)})",
        )
    }

    override fun close() {
        runCatching { port.closePort() }
    }

    companion object {
        fun open(
            systemPortName: String,
            baudRate: Int,
            diag: ObdDiagLog,
        ): SerialTransport {
            diag.info("SERIAL", "Opening $systemPortName @ $baudRate 8N1")
            val port = SerialPort.getCommPort(systemPortName)
            port.setComPortParameters(
                baudRate,
                8,
                SerialPort.ONE_STOP_BIT,
                SerialPort.NO_PARITY,
            )
            // Semi-blocking reads with short timeout so we can poll until ELM '>'
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 50, 0)
            if (!port.openPort()) {
                diag.error("SERIAL", "Cannot open $systemPortName")
                error("Nie otwarto portu $systemPortName (zajęty lub brak uprawnień)")
            }
            diag.info("SERIAL", "Port open: ${port.descriptivePortName ?: systemPortName}")
            return SerialTransport(port)
        }
    }
}
