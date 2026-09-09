package app.brykaobd

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.brykaobd.obd.FileDiagArchive
import app.brykaobd.obd.NoOpDiagShare
import app.brykaobd.obd.SerialElmFacade
import java.io.File

fun main() = application {
    val archive = FileDiagArchive(File(System.getProperty("user.home"), ".brykaobd/diag"))
    val serial = SerialElmFacade()
    Window(onCloseRequest = ::exitApplication, title = "BrykaOBD") {
        App(
            bluetooth = serial,
            diagArchive = archive,
            diagShare = NoOpDiagShare(),
        )
    }
}
