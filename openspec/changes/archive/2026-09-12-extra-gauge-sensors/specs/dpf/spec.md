## MODIFIED Requirements

### Requirement: Gauge and DPF dashboard tabs

The UI SHALL present live-data tabs: **Zegary** (gauge metrics), **DPF**, **Szukaj** (sensor catalog search), and **Odczyty** (user-selected sensors). While connected (Demo or live ELM), the active Zegary/DPF tab SHALL poll its metric list through the shared session mutex. Instant L/100km SHALL be derived from Mode 01 fuel rate (`5E`) and speed when speed is at least 5 km/h; when fuel rate `5E` is unavailable (`NO DATA`), the UI SHALL fall back to an estimated fuel rate derived from MAF (`0x10`) under a fixed diesel air-fuel-ratio assumption, and MUST visually mark FUEL/INST tiles as estimated (not measured) in that case. When both fuel rate and MAF are unavailable, the UI MUST show an empty value. The Zegary tab's accelerator-pedal tile (GAS, Mode 01 `0x49`) SHALL reflect actual driver pedal input, distinct from the throttle/EGR-valve tile (THR, Mode 01 `0x11`) which does not track pedal position on this diesel ECU. Sensor search / active custom polls are specified by the `sensor-search` capability.

#### Scenario: Switch to DPF tab

- GIVEN a connected Demo or live session
- WHEN the user selects the DPF tab
- THEN the UI polls DPF metrics (Mode 01 `7C` and Mode 22 candidates) and shows NO DATA / PARSE without crashing when the ECU omits a DID

#### Scenario: Fuel rate falls back to MAF estimate

- GIVEN a live session where Mode 01 PID `5E` returns NO DATA but MAF (`0x10`) returns a value
- WHEN the Zegary tab renders FUEL and INST tiles
- THEN both tiles show a value prefixed to indicate it is an estimate, not a direct sensor reading

### Requirement: Mode 22 manufacturer DID reads

The session SHALL send Mode 22 requests as `22` + four hex DID digits and decode positive responses starting with `62` + DID bytes. Before Mode 22 DPF polls the session MUST set ELM header to ECM physical address (`ATSH7E0`), matching Car Scanner / Torque GM profiles, and restore functional addressing (`ATSH7DF`) for standard Mode 01/03. UDS negative responses (`7F …`) MUST surface as a per-metric error (not a generic PARSE). Missing DIDs (`NO DATA`) MUST NOT abort the rest of the poll cycle. Raw RX MUST remain available via the existing diag log / archive path for Aveo discovery. The DPF poll list SHALL include, in addition to soot load / pressure / status / temps already specified: km since DPF replacement (`3276`), average km between regenerations (`3278`), average regen duration (`327A`), interrupted regen count (`3047`), and DPF ΔP sensor voltage (`3035`) — all previously confirmed responsive via the discovery probe on this ECU, with formulas marked unverified pending manufacturer documentation.

#### Scenario: Demo soot load

- GIVEN Demo ELM transport
- WHEN the session reads DID `3275` with ECM physical addressing
- THEN the decoded soot load is a percentage value suitable for the DPF tab

#### Scenario: Mode 22 without ECM header yields UDS reject

- GIVEN Aveo live log RX `7F2222` for `223275`
- WHEN parsed as an error
- THEN the metric error identifies UDS negative response `conditionsNotCorrect` (not PARSE alone)
