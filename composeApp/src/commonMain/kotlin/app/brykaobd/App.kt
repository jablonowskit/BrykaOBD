package app.brykaobd

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.brykaobd.obd.BluetoothElmFacade
import app.brykaobd.ui.ObdDashboardScreen

@Composable
fun App(bluetooth: BluetoothElmFacade? = null) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ObdDashboardScreen(bluetooth = bluetooth)
        }
    }
}
