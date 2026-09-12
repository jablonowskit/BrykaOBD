package app.brykaobd.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import app.brykaobd.App
import app.brykaobd.obd.AndroidBluetoothElmFacade
import app.brykaobd.obd.AndroidDiagShare
import app.brykaobd.obd.createAndroidDiagArchive

class MainActivity : ComponentActivity() {
    private val bluetoothFacade by lazy { AndroidBluetoothElmFacade(this) }
    private val diagArchive by lazy { createAndroidDiagArchive(this) }
    private val diagShare by lazy { AndroidDiagShare(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        showApp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val needed = bluetoothPermissions().filterNot { granted(it) }
        if (needed.isEmpty()) {
            showApp()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun showApp() {
        setContent {
            App(
                bluetooth = bluetoothFacade,
                diagArchive = diagArchive,
                diagShare = diagShare,
                onKeepScreenOnChanged = { keepOn ->
                    if (keepOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                },
            )
        }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun bluetoothPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            emptyList()
        }
    }
}
