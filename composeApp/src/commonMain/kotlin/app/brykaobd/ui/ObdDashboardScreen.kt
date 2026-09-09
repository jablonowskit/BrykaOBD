package app.brykaobd.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.brykaobd.obd.BluetoothAdapterInfo
import app.brykaobd.obd.BluetoothElmFacade
import app.brykaobd.obd.DemoElmTransport
import app.brykaobd.obd.DiagArchive
import app.brykaobd.obd.DiagEntry
import app.brykaobd.obd.DiagLevel
import app.brykaobd.obd.DiagSessionInfo
import app.brykaobd.obd.DiagShareFacade
import app.brykaobd.obd.Elm327Session
import app.brykaobd.obd.LoggingTransport
import app.brykaobd.obd.ObdDiagLog
import app.brykaobd.obd.PidReading
import app.brykaobd.obd.StandardPids
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class LinkMode {
    Disconnected,
    Demo,
    Live,
}

@Composable
fun ObdDashboardScreen(
    bluetooth: BluetoothElmFacade? = null,
    diagArchive: DiagArchive? = null,
    diagShare: DiagShareFacade? = null,
) {
    var mode by remember { mutableStateOf(LinkMode.Disconnected) }
    var readings by remember {
        mutableStateOf(StandardPids.dashboard.map { PidReading(it, value = null) })
    }
    var status by remember { mutableStateOf("Brak połączenia z ELM327") }
    var showDevicePicker by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<BluetoothAdapterInfo>>(emptyList()) }
    var diagLines by remember { mutableStateOf<List<DiagEntry>>(emptyList()) }
    var showDiag by remember { mutableStateOf(true) }
    var showSessions by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf<List<DiagSessionInfo>>(emptyList()) }
    var previewText by remember { mutableStateOf<String?>(null) }
    var previewName by remember { mutableStateOf<String?>(null) }
    var activeFile by remember { mutableStateOf<String?>(null) }
    val diag = remember(diagArchive) {
        ObdDiagLog(capacity = 600, archive = diagArchive)
    }
    val scope = rememberCoroutineScope()
    var pollJob by remember { mutableStateOf<Job?>(null) }
    val scroll = rememberScrollState()
    val diagScroll = rememberScrollState()

    fun refreshDiag() {
        diagLines = diag.snapshot()
        activeFile = diagArchive?.currentSession()?.fileName
    }

    fun refreshSessions() {
        sessions = diagArchive?.listSessions().orEmpty()
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun startSession(label: String, linkMode: LinkMode, open: suspend () -> Elm327Session) {
        stopPolling()
        mode = linkMode
        diag.clear()
        val saved = diag.beginPersistedSession(label)
        diag.info("UI", "Start: $label ($linkMode)")
        if (saved != null) {
            diag.info("FILE", "Zapis: ${saved.fileName}")
            diag.info("FILE", "Katalog: ${diagArchive?.directoryHint()}")
        }
        refreshDiag()
        status = "Łączenie: $label…"
        pollJob = scope.launch {
            var session: Elm327Session? = null
            try {
                session = open()
                session.initialize()
                status = "Połączono: $label" +
                    (saved?.let { " · log ${it.fileName}" } ?: "")
                refreshDiag()
                while (isActive) {
                    readings = session.readDashboard()
                    refreshDiag()
                    delay(400)
                }
            } catch (e: Exception) {
                diag.error("UI", e.message ?: e.toString())
                status = "Błąd: ${e.message}"
                mode = LinkMode.Disconnected
                readings = StandardPids.dashboard.map { PidReading(it, value = null) }
                refreshDiag()
            } finally {
                session?.close()
                diag.endPersistedSession()
                refreshDiag()
            }
        }
    }

    fun startDemo() {
        startSession("Demo PID", LinkMode.Demo) {
            val transport = LoggingTransport(DemoElmTransport(), diag)
            Elm327Session(transport, diag)
        }
    }

    fun connectLive(device: BluetoothAdapterInfo) {
        val facade = bluetooth ?: return
        showDevicePicker = false
        startSession(device.name, LinkMode.Live) {
            val raw = facade.connect(device.address, diag)
            Elm327Session(LoggingTransport(raw, diag), diag)
        }
    }

    fun openDevicePicker() {
        val facade = bluetooth
        if (facade == null) {
            status = "Bluetooth Classic dostępny tylko na Androidzie"
            diag.warn("UI", status)
            refreshDiag()
            return
        }
        if (!facade.isBluetoothUsable()) {
            status = "Włącz Bluetooth w telefonie"
            diag.warn("UI", status)
            refreshDiag()
            return
        }
        devices = facade.bondedAdapters()
        diag.info("BT", "Bonded devices: ${devices.size}")
        refreshDiag()
        if (devices.isEmpty()) {
            status = "Brak sparowanych urządzeń — sparuj ELM w ustawieniach systemu"
            return
        }
        showDevicePicker = true
    }

    fun disconnect() {
        diag.info("UI", "Disconnected by user")
        stopPolling()
        mode = LinkMode.Disconnected
        readings = StandardPids.dashboard.map { PidReading(it, value = null) }
        status = "Brak połączenia z ELM327"
        refreshDiag()
    }

    fun openSessions() {
        refreshSessions()
        showSessions = true
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPolling()
            diag.endPersistedSession()
        }
    }

    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            title = { Text("Wybierz ELM327") },
            text = {
                Column {
                    Text(
                        "Urządzenia sparowane w systemie:",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    devices.forEach { device ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { connectLive(device) }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(device.name, fontWeight = FontWeight.SemiBold)
                            Text(device.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDevicePicker = false }) {
                    Text("Anuluj")
                }
            },
        )
    }

    if (showSessions) {
        AlertDialog(
            onDismissRequest = { showSessions = false },
            title = { Text("Zapisane sesje") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        diagArchive?.directoryHint() ?: "Brak archiwum plików",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (sessions.isEmpty()) {
                        Text("Brak plików — uruchom Demo lub Połącz ELM")
                    } else {
                        sessions.forEach { session ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        previewName = session.fileName
                                        previewText = runCatching {
                                            diagArchive?.readText(session.fileName)
                                        }.getOrElse { it.message }
                                        showSessions = false
                                    }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(session.fileName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${session.bytes} B",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (diagShare != null) {
                                    TextButton(
                                        onClick = {
                                            diagShare.shareSessionFile(
                                                session.absolutePath,
                                                session.fileName,
                                            )
                                        },
                                    ) {
                                        Text("Udostępnij")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSessions = false }) {
                    Text("Zamknij")
                }
            },
        )
    }

    previewText?.let { text ->
        AlertDialog(
            onDismissRequest = {
                previewText = null
                previewName = null
            },
            title = { Text(previewName ?: "Log") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                ) {
                    Text(
                        text.take(50_000),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                }
            },
            confirmButton = {
                Row {
                    if (diagShare != null) {
                        TextButton(
                            onClick = {
                                diagShare.shareText(text, previewName ?: "BrykaOBD diag")
                            },
                        ) {
                            Text("Udostępnij")
                        }
                    }
                    TextButton(
                        onClick = {
                            previewText = null
                            previewName = null
                        },
                    ) {
                        Text("Zamknij")
                    }
                }
            },
        )
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
                if (bluetooth != null) {
                    Button(onClick = { openDevicePicker() }) {
                        Text("Połącz ELM")
                    }
                }
                OutlinedButton(onClick = { startDemo() }) {
                    Text("Demo PID")
                }
            } else {
                OutlinedButton(onClick = { disconnect() }) {
                    Text("Rozłącz")
                }
            }
            OutlinedButton(onClick = { showDiag = !showDiag }) {
                Text(if (showDiag) "Ukryj log" else "Log diag")
            }
        }
        if (diagArchive != null) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { openSessions() }) {
                    Text("Zapisane sesje")
                }
                activeFile?.let {
                    Text(
                        "Plik: $it",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            readings.forEach { reading ->
                PidCard(reading)
            }

            if (showDiag) {
                DiagPanel(
                    lines = diagLines,
                    fileHint = activeFile,
                    onClear = {
                        diag.clear()
                        refreshDiag()
                    },
                    scrollState = diagScroll,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiagPanel(
    lines: List<DiagEntry>,
    fileHint: String?,
    onClear: () -> Unit,
    scrollState: ScrollState,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Diagnostyka (${lines.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onClear) {
                    Text("Wyczyść")
                }
            }
            Text(
                buildString {
                    append("TX/RX ELM, AT, PID, BT — Logcat: BrykaOBD")
                    if (fileHint != null) append(" · plik $fileHint")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 280.dp)
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(scrollState),
            ) {
                if (lines.isEmpty()) {
                    Text("Brak wpisów — uruchom Demo lub Połącz ELM", style = MaterialTheme.typography.bodySmall)
                } else {
                    lines.takeLast(200).forEach { entry ->
                        val color = when (entry.level) {
                            DiagLevel.ERROR -> MaterialTheme.colorScheme.error
                            DiagLevel.WARN -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Text(
                            entry.formatLine(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = color,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
            }
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
