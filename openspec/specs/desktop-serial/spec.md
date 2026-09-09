# desktop-serial

## Purpose

Defines how BrykaOBD on Windows/Linux/macOS desktop connects to an ELM327 through a host serial port (Bluetooth Classic SPP exposed as COMx, or USB-to-serial), using the same shared Elm327Session and connect UI as Android Bluetooth.

## Requirements

### Requirement: Serial ports as ELM links on desktop

On JVM/desktop the app SHALL expose available serial/COM ports through the same connect UI used for Android bonded adapters (name + system port id). Selecting a port MUST open an ELM `Transport` at a configured baud rate (default 38400, 8N1).

#### Scenario: List COM ports

- GIVEN at least one serial device is visible to the OS
- WHEN the user taps Połącz ELM on desktop
- THEN the picker lists port system names (e.g. `COM3`) for selection

#### Scenario: Open port and init ELM

- GIVEN the user selects a COM port with a live ELM327
- WHEN connect + `Elm327Session.initialize` run
- THEN AT init commands are exchanged over serial and the dashboard can poll Mode 01
