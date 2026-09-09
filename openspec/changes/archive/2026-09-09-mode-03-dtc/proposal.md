# Change: Mode 03 stored DTCs

## Why

Dashboard Mode 01 alone is not enough for Aveo troubleshooting. Drivers need to see stored Diagnostic Trouble Codes (Mode 03) and optionally clear them (Mode 04) after a repair, with the same ELM session, diagnostic logging, and demo path used for live PIDs.

## What Changes

- Parse Mode 03 ELM responses into SAE-style codes (P0xxx / C0xxx / B0xxx / U0xxx).
- `Elm327Session.readStoredDtcs()` and `clearStoredDtcs()` (Mode 04).
- Dashboard UI: list DTCs, refresh, clear with confirmation.
- Demo transport returns sample DTCs; unit tests cover decode / empty / clear.
- OpenSpec: new capability `dtc` + note in `obd-session` if needed.

## Capabilities

- New: `dtc`
- Modified: `obd-session` (session exposes DTC ops over same Transport)
