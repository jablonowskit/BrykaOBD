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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import app.brykaobd.obd.DpfPids
import app.brykaobd.obd.DiscoveryResult
import app.brykaobd.obd.DtcCode
import app.brykaobd.obd.Elm327Session
import app.brykaobd.obd.ExtPidDefinition
import app.brykaobd.obd.ExtPidReading
import app.brykaobd.obd.GaugePids
import app.brykaobd.obd.LoggingTransport
import app.brykaobd.obd.ObdAddress
import app.brykaobd.obd.ObdDiagLog
import app.brykaobd.obd.PidDiscoveryMap
import app.brykaobd.obd.SensorCatalog
import app.brykaobd.obd.VehicleIdentity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private enum class LinkMode {
    Disconnected,
    Demo,
    Live,
}

private enum class DashTab {
    Session,
    Gauges,
    Dpf,
    Search,
    Custom,
}

@Composable
fun ObdDashboardScreen(
    bluetooth: BluetoothElmFacade? = null,
    diagArchive: DiagArchive? = null,
    diagShare: DiagShareFacade? = null,
    onKeepScreenOnChanged: ((Boolean) -> Unit)? = null,
) {
    var keepScreenOn by remember { mutableStateOf(false) }
    LaunchedEffect(keepScreenOn) { onKeepScreenOnChanged?.invoke(keepScreenOn) }
    var mode by remember { mutableStateOf(LinkMode.Disconnected) }
    var dashTab by remember { mutableStateOf(DashTab.Session) }
    var gaugeReadings by remember {
        mutableStateOf(GaugePids.pollList.map { ExtPidReading(it, value = null) })
    }
    var dpfReadings by remember {
        mutableStateOf(DpfPids.pollList.map { ExtPidReading(it, value = null) })
    }
    var searchQuery by remember { mutableStateOf("") }
    var activeSensors by remember { mutableStateOf<List<ExtPidDefinition>>(emptyList()) }
    var activeReadings by remember { mutableStateOf<List<ExtPidReading>>(emptyList()) }
    var previewReadings by remember { mutableStateOf<Map<String, ExtPidReading>>(emptyMap()) }
    var instantL100 by remember { mutableStateOf<Double?>(null) }
    var instantL100Estimated by remember { mutableStateOf(false) }
    var oilPressureBar by remember { mutableStateOf<Double?>(null) }
    var vehicleIdentity by remember { mutableStateOf<VehicleIdentity?>(null) }
    var discoveryHits by remember { mutableStateOf<List<DiscoveryResult>>(emptyList()) }
    var discoveryBusy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Brak połączenia z ELM327") }
    var showDevicePicker by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<BluetoothAdapterInfo>>(emptyList()) }
    var diagLines by remember { mutableStateOf<List<DiagEntry>>(emptyList()) }
    var showSessions by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf<List<DiagSessionInfo>>(emptyList()) }
    var previewText by remember { mutableStateOf<String?>(null) }
    var previewName by remember { mutableStateOf<String?>(null) }
    var activeFile by remember { mutableStateOf<String?>(null) }
    var dtcCodes by remember { mutableStateOf<List<DtcCode>>(emptyList()) }
    var dtcError by remember { mutableStateOf<String?>(null) }
    var dtcBusy by remember { mutableStateOf(false) }
    var confirmClearDtc by remember { mutableStateOf(false) }
    var liveSession by remember { mutableStateOf<Elm327Session?>(null) }
    val ioMutex = remember { Mutex() }
    val diag = remember(diagArchive) {
        ObdDiagLog(capacity = 600, archive = diagArchive)
    }
    val scope = rememberCoroutineScope()
    var pollJob by remember { mutableStateOf<Job?>(null) }
    val scroll = rememberScrollState()
    val diagScroll = rememberScrollState()

    fun emptyGauges() = GaugePids.pollList.map { ExtPidReading(it, value = null) }
    fun emptyDpf() = DpfPids.pollList.map { ExtPidReading(it, value = null) }

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
                liveSession = session
                session.initialize()
                status = "Odczyt pojazdu…"
                refreshDiag()
                vehicleIdentity = ioMutex.withLock { session.readVehicleIdentity() }
                status = "Połączono: $label · ${vehicleIdentity?.displayLine ?: "Pojazd: —"}" +
                    (saved?.let { " · log ${it.fileName}" } ?: "")
                refreshDiag()
                status = "Sonda mapy PID/DID…"
                discoveryBusy = true
                discoveryHits = emptyList()
                val probed = ioMutex.withLock {
                    session.probeDiscovery(PidDiscoveryMap.aveoFirstProbe)
                }
                discoveryHits = probed.filter { it.isHit }
                discoveryBusy = false
                status = "Połączono: $label · ${vehicleIdentity?.displayLine ?: "Pojazd: —"} · sonda ${discoveryHits.size}/${probed.size}" +
                    (saved?.let { " · log ${it.fileName}" } ?: "")
                refreshDiag()
                val firstDtcs = ioMutex.withLock { session.readStoredDtcs() }
                dtcCodes = firstDtcs.codes
                dtcError = firstDtcs.error
                refreshDiag()
                while (isActive) {
                    if (activeSensors.isNotEmpty()) {
                        val functional = activeSensors.filter { it.responseService == 0x41 }
                        val ecm = activeSensors.filter { it.responseService == 0x62 }
                        val collected = mutableListOf<ExtPidReading>()
                        if (functional.isNotEmpty()) {
                            collected += ioMutex.withLock {
                                session.readExtList(functional, ObdAddress.Functional)
                            }
                        }
                        if (ecm.isNotEmpty()) {
                            collected += ioMutex.withLock {
                                session.readExtList(ecm, ObdAddress.EcmPhysical)
                            }
                        }
                        activeReadings = collected
                    } else if (activeReadings.isNotEmpty()) {
                        activeReadings = emptyList()
                    }
                    when (dashTab) {
                        DashTab.Session -> {
                            // connect / DTC / log — no live PID poll
                        }
                        DashTab.Gauges -> {
                            val next = ioMutex.withLock {
                                session.readExtList(GaugePids.pollList, ObdAddress.Functional)
                            }
                            gaugeReadings = next
                            val rate = next.firstOrNull { it.pid.request == GaugePids.fuelRate.request }?.value
                            val spd = next.firstOrNull { it.pid.request == GaugePids.speed.request }?.value
                            val maf = next.firstOrNull { it.pid.request == GaugePids.mafRate.request }?.value
                            val effectiveRate = rate ?: GaugePids.estimatedFuelRateLhFromMaf(maf)
                            instantL100Estimated = rate == null
                            instantL100 = GaugePids.instantLitersPer100km(effectiveRate, spd)
                            val ecmExtras = ioMutex.withLock {
                                session.readExtList(
                                    listOf(DpfPids.sootLoad, GaugePids.oilPressure),
                                    ObdAddress.EcmPhysical,
                                )
                            }
                            ecmExtras.forEach { hit ->
                                when (hit.pid.request) {
                                    DpfPids.sootLoad.request -> {
                                        dpfReadings = dpfReadings.map { r ->
                                            if (r.pid.request == hit.pid.request) hit else r
                                        }
                                    }
                                    GaugePids.oilPressure.request -> {
                                        oilPressureBar = hit.value
                                    }
                                }
                            }
                        }
                        DashTab.Dpf -> {
                            dpfReadings = ioMutex.withLock {
                                session.readExtList(DpfPids.pollList, ObdAddress.EcmPhysical)
                            }
                        }
                        DashTab.Search -> {
                            val filtered = SensorCatalog.search(searchQuery)
                            if (filtered.size in 1..19) {
                                val activeReqs = activeSensors.map { it.request }.toSet()
                                val toPreview = filtered.filter { it.request !in activeReqs }
                                val map = mutableMapOf<String, ExtPidReading>()
                                activeReadings
                                    .filter { r -> filtered.any { it.request == r.pid.request } }
                                    .forEach { map[it.pid.request] = it }
                                val functional = toPreview.filter { it.responseService == 0x41 }
                                val ecm = toPreview.filter { it.responseService == 0x62 }
                                if (functional.isNotEmpty()) {
                                    ioMutex.withLock {
                                        session.readExtList(functional, ObdAddress.Functional)
                                    }.forEach { map[it.pid.request] = it }
                                }
                                if (ecm.isNotEmpty()) {
                                    ioMutex.withLock {
                                        session.readExtList(ecm, ObdAddress.EcmPhysical)
                                    }.forEach { map[it.pid.request] = it }
                                }
                                previewReadings = map
                            } else {
                                previewReadings = emptyMap()
                            }
                        }
                        DashTab.Custom -> {
                            // live cards use activeReadings polled above
                        }
                    }
                    refreshDiag()
                    delay(400)
                }
            } catch (e: Exception) {
                diag.error("UI", e.message ?: e.toString())
                status = "Błąd: ${e.message}"
                mode = LinkMode.Disconnected
                gaugeReadings = emptyGauges()
                dpfReadings = emptyDpf()
                activeSensors = emptyList()
                activeReadings = emptyList()
                previewReadings = emptyMap()
                searchQuery = ""
                instantL100 = null
                oilPressureBar = null
                vehicleIdentity = null
                discoveryHits = emptyList()
                discoveryBusy = false
                dtcCodes = emptyList()
                dtcError = null
                liveSession = null
                refreshDiag()
            } finally {
                discoveryBusy = false
                liveSession = null
                session?.close()
                diag.endPersistedSession()
                refreshDiag()
            }
        }
    }

    fun refreshDtcs() {
        val session = liveSession ?: return
        if (dtcBusy) return
        dtcBusy = true
        scope.launch {
            try {
                val result = ioMutex.withLock { session.readStoredDtcs() }
                dtcCodes = result.codes
                dtcError = result.error
                if (result.error != null) {
                    status = "DTC: ${result.error}"
                }
                refreshDiag()
            } catch (e: Exception) {
                dtcError = e.message
                status = "DTC błąd: ${e.message}"
                refreshDiag()
            } finally {
                dtcBusy = false
            }
        }
    }

    fun clearDtcsConfirmed() {
        confirmClearDtc = false
        val session = liveSession ?: return
        if (dtcBusy) return
        dtcBusy = true
        scope.launch {
            try {
                val result = ioMutex.withLock { session.clearStoredDtcs() }
                dtcCodes = result.codes
                dtcError = result.error
                status = if (result.error != null) {
                    "Kasowanie DTC: ${result.error}"
                } else {
                    "Kody DTC wyczyszczone"
                }
                refreshDiag()
            } catch (e: Exception) {
                dtcError = e.message
                status = "DTC clear błąd: ${e.message}"
                refreshDiag()
            } finally {
                dtcBusy = false
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
            status = "Brak warstwy łącza ELM (BT/serial)"
            diag.warn("UI", status)
            refreshDiag()
            return
        }
        if (!facade.isBluetoothUsable()) {
            status = "Brak łącza (włącz Bluetooth / podłącz adapter COM)"
            diag.warn("UI", status)
            refreshDiag()
            return
        }
        devices = facade.bondedAdapters()
        diag.info("LINK", "Dostępne łącza: ${devices.size}")
        refreshDiag()
        if (devices.isEmpty()) {
            status = "Brak portów/urządzeń — sparuj ELM (BT) albo podłącz USB-serial"
            return
        }
        showDevicePicker = true
    }

    fun disconnect() {
        diag.info("UI", "Disconnected by user")
        stopPolling()
        mode = LinkMode.Disconnected
        gaugeReadings = emptyGauges()
        dpfReadings = emptyDpf()
        activeSensors = emptyList()
        activeReadings = emptyList()
        previewReadings = emptyMap()
        searchQuery = ""
        instantL100 = null
        oilPressureBar = null
        discoveryHits = emptyList()
        discoveryBusy = false
        vehicleIdentity = null
        dtcCodes = emptyList()
        dtcError = null
        liveSession = null
        status = "Brak połączenia z ELM327"
        refreshDiag()
    }

    fun runDiscoveryAgain() {
        val session = liveSession ?: return
        if (discoveryBusy) return
        discoveryBusy = true
        scope.launch {
            try {
                status = "Sonda mapy PID/DID…"
                val probed = ioMutex.withLock {
                    session.probeDiscovery(PidDiscoveryMap.aveoFirstProbe)
                }
                discoveryHits = probed.filter { it.isHit }
                status = "Sonda: ${discoveryHits.size}/${probed.size} hit (szczegóły w logu)"
                refreshDiag()
            } catch (e: Exception) {
                status = "Sonda błąd: ${e.message}"
                refreshDiag()
            } finally {
                discoveryBusy = false
            }
        }
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

    if (confirmClearDtc) {
        AlertDialog(
            onDismissRequest = { confirmClearDtc = false },
            title = { Text("Skasować kody DTC?") },
            text = {
                Text(
                    "Mode 04 wyczyści zapisane błędy w sterowniku. " +
                        "Używaj po naprawie — na Aveo potwierdź, że to zamierzone.",
                )
            },
            confirmButton = {
                TextButton(onClick = { clearDtcsConfirmed() }) {
                    Text("Kasuj DTC")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDtc = false }) {
                    Text("Anuluj")
                }
            },
        )
    }

    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            title = { Text("Wybierz ELM / port") },
            text = {
                Column {
                    Text(
                        "Android: urządzenia BT sparowane w systemie. Windows: porty COM (BT SPP lub USB).",
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        PrimaryTabRow(
            selectedTabIndex = when (dashTab) {
                DashTab.Session -> 0
                DashTab.Gauges -> 1
                DashTab.Dpf -> 2
                DashTab.Search -> 3
                DashTab.Custom -> 4
            },
        ) {
            Tab(
                selected = dashTab == DashTab.Session,
                onClick = { dashTab = DashTab.Session },
                text = { Text("Sesja") },
            )
            Tab(
                selected = dashTab == DashTab.Gauges,
                onClick = { dashTab = DashTab.Gauges },
                text = { Text("Zegary") },
            )
            Tab(
                selected = dashTab == DashTab.Dpf,
                onClick = { dashTab = DashTab.Dpf },
                text = { Text("DPF") },
            )
            Tab(
                selected = dashTab == DashTab.Search,
                onClick = { dashTab = DashTab.Search },
                text = { Text("Szukaj") },
            )
            Tab(
                selected = dashTab == DashTab.Custom,
                onClick = { dashTab = DashTab.Custom },
                text = {
                    Text(
                        if (activeSensors.isEmpty()) "Odczyty" else "Odczyty (${activeSensors.size})",
                    )
                },
            )
        }

        Spacer(Modifier.height(8.dp))

        when (dashTab) {
            DashTab.Gauges -> {
                CarDashboardCluster(
                    readings = gaugeReadings,
                    instantL100 = instantL100,
                    instantL100Estimated = instantL100Estimated,
                    sootLoad = dpfReadings.firstOrNull { it.pid.request == DpfPids.sootLoad.request }?.value,
                    oilPressureBar = oilPressureBar,
                    dtcWarn = mode != LinkMode.Disconnected && (dtcCodes.isNotEmpty() || dtcError != null),
                    dtcCount = dtcCodes.size,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            else -> {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (dashTab) {
                        DashTab.Session -> {
                            Text(
                                status,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Nie wygaszaj ekranu",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Switch(
                                    checked = keepScreenOn,
                                    onCheckedChange = { keepScreenOn = it },
                                )
                            }
                            if (diagArchive != null) {
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
                            VehicleIdentityCard(vehicleIdentity)
                            if (mode != LinkMode.Disconnected) {
                                DtcPanel(
                                    codes = dtcCodes,
                                    error = dtcError,
                                    busy = dtcBusy,
                                    onRefresh = { refreshDtcs() },
                                    onClear = { confirmClearDtc = true },
                                )
                            }
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
                        DashTab.Dpf -> {
                            Text(
                                "Po połączeniu: auto-sonda mapy (Astra-J 1.3 + Mode 01). Mode 22 = ATSH7E0.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { runDiscoveryAgain() },
                                    enabled = mode != LinkMode.Disconnected && !discoveryBusy,
                                ) {
                                    Text(if (discoveryBusy) "Sonda…" else "Sonda mapy")
                                }
                                Text(
                                    "Hity: ${discoveryHits.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                )
                            }
                            if (discoveryHits.isNotEmpty()) {
                                Card(
                                    Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(
                                            "Odkryte odpowiedzi (payload)",
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        discoveryHits.take(40).forEach { hit ->
                                            Text(
                                                "${hit.candidate.request} ${hit.candidate.namePl}: ${hit.payloadHex}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            dpfReadings.forEach { ExtPidCard(it) }
                        }
                        DashTab.Search -> {
                            val filtered = SensorCatalog.search(searchQuery)
                            val displayHits = filtered.take(40)
                            val activeReqs = activeSensors.map { it.request }.toSet()
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Szukaj czujnika") },
                                placeholder = { Text("np. olej, DPF, 015C") },
                            )
                            Text(
                                if (filtered.size < 20 && mode != LinkMode.Disconnected) {
                                    "Trafienia: ${filtered.size} · podgląd live"
                                } else {
                                    "Trafienia: ${filtered.size}" +
                                        if (filtered.size >= 20) " (zwęż filtr, aby zobaczyć podgląd)" else ""
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (activeSensors.isNotEmpty()) {
                                Text(
                                    "Dodane (${activeSensors.size}) → zakładka Odczyty",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { dashTab = DashTab.Custom },
                                )
                            }
                            displayHits.forEach { pid ->
                                val already = pid.request in activeReqs
                                val preview = previewReadings[pid.request]
                                SensorHitRow(
                                    pid = pid,
                                    preview = preview,
                                    alreadyActive = already,
                                    onAdd = {
                                        if (!already) {
                                            activeSensors = activeSensors + pid
                                        }
                                    },
                                )
                            }
                        }
                        DashTab.Custom -> {
                            if (activeSensors.isEmpty()) {
                                Text(
                                    "Brak dodanych czujników. Użyj zakładki Szukaj → Dodaj.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(onClick = { dashTab = DashTab.Search }) {
                                    Text("Przejdź do Szukaj")
                                }
                            } else {
                                Text(
                                    "Dodane czujniki (${activeSensors.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                activeSensors.forEach { pid ->
                                    val reading = activeReadings.firstOrNull { it.pid.request == pid.request }
                                        ?: ExtPidReading(pid, value = null)
                                    ActiveSensorCard(
                                        reading = reading,
                                        onRemove = {
                                            activeSensors = activeSensors.filter { it.request != pid.request }
                                            activeReadings = activeReadings.filter { it.pid.request != pid.request }
                                        },
                                    )
                                }
                            }
                        }
                        DashTab.Gauges -> Unit
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun VehicleIdentityCard(identity: VehicleIdentity?) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Pojazd",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            when {
                identity == null -> Text(
                    "Połącz ELM, aby odczytać VIN (Mode 09)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                identity.vin != null -> {
                    identity.manufacturerHint?.let {
                        Text(it, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        "VIN ${identity.vin}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                else -> Text(
                    identity.displayLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ExtPidCard(reading: ExtPidReading) {
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
                    "${reading.pid.request} · ${reading.pid.nameEn}",
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
                if (reading.pid.unit.isNotBlank()) {
                    Text(reading.pid.unit, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SensorHitRow(
    pid: ExtPidDefinition,
    preview: ExtPidReading?,
    alreadyActive: Boolean,
    onAdd: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(pid.namePl, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${pid.request} · ${pid.nameEn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (preview != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        preview.displayValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (pid.unit.isNotBlank()) {
                        Text(pid.unit, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (alreadyActive) {
                Text(
                    "Dodano",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                OutlinedButton(onClick = onAdd) {
                    Text("Dodaj")
                }
            }
        }
    }
}

@Composable
private fun ActiveSensorCard(
    reading: ExtPidReading,
    onRemove: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(reading.pid.namePl, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${reading.pid.request} · ${reading.pid.nameEn}",
                    style = MaterialTheme.typography.labelSmall,
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
                if (reading.pid.unit.isNotBlank()) {
                    Text(reading.pid.unit, style = MaterialTheme.typography.labelMedium)
                }
            }
            TextButton(onClick = onRemove) {
                Text("Usuń")
            }
        }
    }
}

@Composable
private fun DtcPanel(
    codes: List<DtcCode>,
    error: String?,
    busy: Boolean,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
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
                    "Błędy DTC (${codes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onRefresh, enabled = !busy) {
                        Text(if (busy) "…" else "Odśwież")
                    }
                    TextButton(onClick = onClear, enabled = !busy) {
                        Text("Kasuj")
                    }
                }
            }
            Text(
                "Mode 03 odczyt / Mode 04 kasowanie (po potwierdzeniu)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            if (codes.isEmpty() && error == null) {
                Text("Brak zapisanych kodów", style = MaterialTheme.typography.bodyMedium)
            } else {
                codes.forEach { dtc ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(dtc.code, fontWeight = FontWeight.SemiBold)
                        Text(
                            dtc.descriptionPl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
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
