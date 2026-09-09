package app.brykaobd

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.brykaobd.obd.FileDiagArchive
import app.brykaobd.obd.NoOpDiagShare
import java.io.File

fun main() = application {
    val archive = FileDiagArchive(File(System.getProperty("user.home"), ".brykaobd/diag"))
    Window(onCloseRequest = ::exitApplication, title = "BrykaOBD") {
        App(diagArchive = archive, diagShare = NoOpDiagShare())
    }
}
