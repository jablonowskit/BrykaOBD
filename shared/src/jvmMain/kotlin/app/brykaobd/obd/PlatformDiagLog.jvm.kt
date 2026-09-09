package app.brykaobd.obd

internal actual fun platformDiagLog(level: DiagLevel, category: String, message: String) {
    val stream = if (level == DiagLevel.ERROR || level == DiagLevel.WARN) System.err else System.out
    stream.println("BrykaOBD [${level.name}] $category | $message")
}
