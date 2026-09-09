package app.brykaobd.obd

import android.content.Context
import java.io.File

/** App-private `diag/` folder (also under Android/data/.../files/diag for adb pull). */
fun createAndroidDiagArchive(context: Context): DiagArchive {
    val app = context.applicationContext
    val dir = app.getExternalFilesDir("diag") ?: File(app.filesDir, "diag")
    return FileDiagArchive(dir)
}
