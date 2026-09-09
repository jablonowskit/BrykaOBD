package app.brykaobd.obd

/** ELM request addressing for OBD vs manufacturer (UDS) Mode 22. */
enum class ObdAddress {
    /** Functional broadcast — standard Mode 01/03. */
    Functional,

    /** Physical ECM header `7E0` — GM/Opel Mode 22 DPF DIDs (Car Scanner / Torque). */
    EcmPhysical,
}
