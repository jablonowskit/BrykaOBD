package app.brykaobd.obd

import android.util.Log

private const val TAG = "BrykaOBD"

internal actual fun platformDiagLog(level: DiagLevel, category: String, message: String) {
    val line = "$category | $message"
    when (level) {
        DiagLevel.DEBUG -> Log.d(TAG, line)
        DiagLevel.INFO -> Log.i(TAG, line)
        DiagLevel.WARN -> Log.w(TAG, line)
        DiagLevel.ERROR -> Log.e(TAG, line)
    }
}
