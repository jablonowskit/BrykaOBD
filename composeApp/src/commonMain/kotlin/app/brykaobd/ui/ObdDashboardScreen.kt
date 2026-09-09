package app.brykaobd.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.brykaobd.obd.DemoElmTransport
import app.brykaobd.obd.Elm327Session
import app.brykaobd.obd.PidReading
import app.brykaobd.obd.StandardPids
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class LinkMode {
    Disconnected,
    Demo,
}

@Composable
fun ObdDashboardScreen() {
    var mode by remember { mutableStateOf(LinkMode.Disconnected) }
    var readings by remember {
        mutableStateOf(StandardPids.dashboard.map { PidReading(it, value = null) })
    }
    var status by remember { mutableStateOf("Brak połączenia z ELM327") }
    val scope = rememberCoroutineScope()
    var pollJob by remember { mutableStateOf<Job?>(null) }
    val scroll = rememberScrollState()

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun startDemo() {
        stopPolling()
        mode = LinkMode.Demo
        status = "Demo (FakeTransport) — typowe PID Mode 01"
        val transport = DemoElmTransport()
        val session = Elm327Session(transport)
        pollJob = scope.launch {
            try {
                session.initialize()
                while (isActive) {
                    readings = session.readDashboard()
                    delay(500)
                }
            } catch (e: Exception) {
                status = "Błąd demo: ${e.message}"
                mode = LinkMode.Disconnected
            } finally {
                session.close()
            }
        }
    }

    fun disconnect() {
        stopPolling()
        mode = LinkMode.Disconnected
        readings = StandardPids.dashboard.map { PidReading(it, value = null) }
        status = "Brak połączenia z ELM327"
    }

    DisposableEffect(Unit) {
        onDispose { stopPolling() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("BrykaOBD", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mode == LinkMode.Disconnected) {
                Button(onClick = { startDemo() }) {
                    Text("Demo PID")
                }
            } else {
                OutlinedButton(onClick = { disconnect() }) {
                    Text("Rozłącz")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            readings.forEach { reading ->
                PidCard(reading)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PidCard(reading: PidReading) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(reading.pid.namePl, style = MaterialTheme.typography.titleMedium)
                Text(
                    "PID ${reading.pid.idHex} · ${reading.pid.nameEn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                reading.error?.let { err ->
                    Text(
                        err,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    reading.displayValue,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(reading.pid.unit, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
