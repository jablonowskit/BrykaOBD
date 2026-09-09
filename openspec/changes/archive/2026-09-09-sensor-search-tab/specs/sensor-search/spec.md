## ADDED Requirements

### Requirement: Sensor search catalog and live selection

The UI SHALL provide a **Szukaj** tab that filters a curated `SensorCatalog` of Mode 01 / Mode 22 metrics by Polish name, English name, or request hex (case-insensitive). Selecting a hit SHALL add it to an in-memory active poll list without duplicates. Active metrics MUST be polled through the shared session mutex (Mode 01 → functional address, Mode 22 → ECM physical) while connected, independent of the selected tab, and the UI MUST allow removing an active metric. When the filtered hit count is between 1 and 19 inclusive and a session is connected, the Szukaj tab SHALL preview-poll those hits (skipping ones already active) and show decoded values in the hit list; when there are 20 or more hits, preview polling MUST NOT run.

#### Scenario: Demo search oil and add

- GIVEN a connected Demo session
- WHEN the user opens Szukaj and filters by `olej`
- THEN preview values appear for oil-related hits and adding one shows it under active readings with a live value

#### Scenario: Wide filter skips preview

- GIVEN a connected Demo session
- WHEN the Szukaj filter matches 20 or more catalog entries
- THEN the hit list shows names without live preview polling of the full set
