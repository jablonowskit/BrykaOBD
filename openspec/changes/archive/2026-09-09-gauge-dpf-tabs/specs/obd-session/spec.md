## MODIFIED Requirements

### Requirement: Dashboard Mode 01 PID set

The live **Zegary** tab SHALL poll Mode 01 metrics for driving context: speed (`0D`), RPM (`0C`), coolant (`05`), throttle (`11`), control module voltage (`42`), and fuel rate (`5E`) when supported. Manufacturer-specific DPF DIDs are covered by the `dpf` capability (Mode 22 + optional Mode 01 `7C`), not this requirement alone. The session MUST still support the classic `StandardPids.dashboard` Mode 01 list for compatibility and tests.

#### Scenario: Fuel rate missing on diesel

- GIVEN an ELM reply containing `NO DATA` for PID `5E`
- WHEN the gauges tab polls fuel rate
- THEN that metric shows an error/empty value and other gauges continue updating
