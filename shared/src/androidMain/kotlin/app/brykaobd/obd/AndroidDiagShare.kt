package app.brykaobd.obd

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class AndroidDiagShare(
    context: Context,
) : DiagShareFacade {
    private val app = context.applicationContext

    override fun shareSessionFile(absolutePath: String, fileName: String) {
        val file = File(absolutePath)
        require(file.isFile) { "Brak pliku: $absolutePath" }
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Udostępnij log BrykaOBD").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(chooser)
    }

    override fun shareText(text: String, title: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        val chooser = Intent.createChooser(send, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(chooser)
    }
}
