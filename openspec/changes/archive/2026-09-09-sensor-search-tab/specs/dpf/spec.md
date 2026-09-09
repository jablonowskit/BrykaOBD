## MODIFIED Requirements

### Requirement: Gauge and DPF dashboard tabs

The UI SHALL present live-data tabs: **Zegary** (gauge metrics), **DPF**, and **Szukaj** (sensor catalog search). While connected (Demo or live ELM), the active Zegary/DPF tab SHALL poll its metric list through the shared session mutex. Instant L/100km SHALL be derived from Mode 01 fuel rate (`5E`) and speed when speed is at least 5 km/h; otherwise the UI MUST show an empty value. Sensor search / active custom polls are specified by the `sensor-search` capability.

#### Scenario: Switch to DPF tab

- GIVEN a connected Demo or live session
- WHEN the user selects the DPF tab
- THEN the UI polls DPF metrics (Mode 01 `7C` and Mode 22 candidates) and shows NO DATA / PARSE without crashing when the ECU omits a DID
