# android-bt

## Purpose

Defines Android Classic Bluetooth SPP access to a paired ELM327 adapter: runtime permissions, listing bonded devices, opening an RFCOMM socket, and exposing that link as the shared `Transport` used by `Elm327Session`. BLE-only adapters and iOS Classic SPP are out of scope for MVP.

## Requirements

### Requirement: Classic SPP only on Android MVP

On Android the app MUST connect to ELM327 using Bluetooth Classic RFCOMM/SPP (UUID `00001101-0000-1000-8000-00805F9B34FB`). Desktop serial bridging is a separate future capability and MUST NOT be required for the Android path.

#### Scenario: Facade available on phone

- GIVEN the Android application entry point
- WHEN the Compose UI starts after Bluetooth permission handling
- THEN a `BluetoothElmFacade` implementation is provided to the dashboard (non-null)

### Requirement: Runtime Bluetooth permissions

On Android 12+ (API 31+) the app MUST request `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` before using bonded-device APIs. Older API levels MAY rely on install-time Bluetooth permissions declared in the manifest.

#### Scenario: Permission gate before UI connect

- GIVEN a device on API 31+ without granted BT permissions
- WHEN MainActivity starts
- THEN the system permission prompt is shown before or as the UI becomes usable for Połącz ELM

### Requirement: Bonded device picker

The connect flow MUST list bonded (already paired) Bluetooth devices from the system and let the user pick one. The app MUST NOT require in-app discovery/pairing as the primary path; pairing is done in Android Settings.

#### Scenario: Empty bonded list

- GIVEN Bluetooth is on and no bonded devices exist
- WHEN the user taps Połącz ELM
- THEN the UI explains that the user must pair the ELM in system settings and does not open an empty misleading picker as a successful connect

#### Scenario: User selects adapter

- GIVEN one or more bonded devices
- WHEN the user selects a device in the picker
- THEN the facade opens an SPP socket to that address and returns a `Transport`

### Requirement: Connect failures are visible

If SPP connect fails (adapter off, device gone, socket error), the system MUST surface an error message in the UI status and MUST record the failure in the diagnostic log when a diag archive/session is active.

#### Scenario: Connect exception

- GIVEN a bonded address that refuses RFCOMM
- WHEN `connect` runs
- THEN the dashboard shows a connection error and does not leave the UI stuck in a false “connected” polling state
