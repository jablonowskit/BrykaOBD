package app.brykaobd.obd

/** Platform sink (Android Logcat / JVM stdout). */
internal expect fun platformDiagLog(level: DiagLevel, category: String, message: String)
