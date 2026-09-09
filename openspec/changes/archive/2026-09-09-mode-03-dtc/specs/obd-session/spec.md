## ADDED Requirements

### Requirement: Mode 03 and 04 on Elm327Session

`Elm327Session` SHALL expose suspend functions to read stored DTCs (Mode 03) and clear them (Mode 04) using the same initialized Transport and diagnostic logging as Mode 01 PID reads.

#### Scenario: Session logs DTC request

- GIVEN an initialized session with LoggingTransport
- WHEN `readStoredDtcs()` runs
- THEN diagnostic log contains TX for `03` and a DTC category result line
