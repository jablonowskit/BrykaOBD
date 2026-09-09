package app.brykaobd.obd

/** Desktop / tests: no system share sheet. */
class NoOpDiagShare : DiagShareFacade {
    override fun shareSessionFile(absolutePath: String, fileName: String) = Unit

    override fun shareText(text: String, title: String) = Unit
}
