# obd-session

## Purpose

Defines how BrykaOBD talks to an ELM327 adapter over an abstract byte-stream Transport: AT initialization, Mode 01 PID requests, decoding of universal dashboard parameters, demo mode without hardware, and graceful handling of missing or error responses such as NO DATA on diesel vehicles like the Chevrolet Aveo 1.3D 2012.

## Requirements

### Requirement: Transport abstraction

The system SHALL perform all ELM327 I/O through a `Transport` that can write ASCII commands and read until the ELM prompt `>`, so the same session logic works on Android Bluetooth Classic SPP and future desktop serial.

#### Scenario: Session uses injected transport

- GIVEN a `Transport` implementation (live, demo, or fake)
- WHEN an `Elm327Session` is created with that transport
- THEN all AT and OBD requests go only through that transport

### Requirement: ELM AT initialization

Before the first Mode 01 PID read, the session MUST send the AT init sequence `ATZ`, `ATE0`, `ATL0`, `ATS0`, `ATH0`, `ATSP0` (each terminated for ELM) and wait for a prompt after every command. Noisy or non-fatal AT replies MUST NOT abort initialization; OBD link errors are evaluated on subsequent PID requests.

#### Scenario: Init completes then PID read

- GIVEN a transport that answers each AT command with a prompt
- WHEN `initialize()` or the first `readPid` runs
- THEN all six AT commands are written in order and the session is marked initialized

### Requirement: Universal Mode 01 dashboard PIDs

The live **Zegary** tab SHALL poll Mode 01 metrics for driving context: speed (`0D`), RPM (`0C`), coolant (`05`), throttle (`11`), control module voltage (`42`), and fuel rate (`5E`) when supported. The session MUST still support the classic `StandardPids.dashboard` list (including engine load `04` and intake `0F`) for compatibility and tests. Manufacturer-specific DPF DIDs are covered by the `dpf` capability (Mode 22 + optional Mode 01 `7C`).

#### Scenario: RPM decode

- GIVEN Mode 01 response data bytes `1A F8` for PID `0C`
- WHEN the RPM decoder runs
- THEN the value is `1726` rpm

#### Scenario: Fuel rate missing on diesel

- GIVEN an ELM reply containing `NO DATA` for PID `5E`
- WHEN the gauges tab polls fuel rate
- THEN that metric shows an error/empty value and other gauges continue updating

### Requirement: Error and empty OBD responses

When the ELM reply indicates an error or empty data (`NO DATA`, `UNABLE TO CONNECT`, `BUS INIT ERROR`, `CAN ERROR`, `STOPPED`, or unparsable payload), the corresponding `PidReading` MUST expose a non-null `error` (or equivalent) and MUST NOT invent a numeric value. The parser MUST accept both spaced hex (`41 05 3B`) and continuous hex (`41053B`) as returned when `ATS0` disables spaces (common on V-LINK / clones).

#### Scenario: NO DATA on PID

- GIVEN an ELM reply containing `NO DATA` for a requested PID
- WHEN `readPid` completes
- THEN `value` is null and `error` is `NO DATA`

#### Scenario: Coolant from continuous hex ATS0

- GIVEN ELM reply `41053B`
- WHEN Mode 01 PID `05` is decoded
- THEN coolant temperature is `19` °C

### Requirement: Demo mode without hardware

The UI MUST offer a Demo PID mode that drives `Elm327Session` through an in-process demo transport returning plausible Mode 01 frames so developers can exercise the dashboard without an adapter or vehicle.

#### Scenario: Demo starts dashboard values

- GIVEN the user taps Demo PID
- WHEN the demo session initializes and polls
- THEN dashboard cards show decoded demo values (not all em dashes) without Bluetooth

### Requirement: Mode 03 and 04 on Elm327Session

`Elm327Session` SHALL expose suspend functions to read stored DTCs (Mode 03) and clear them (Mode 04) using the same initialized Transport and diagnostic logging as Mode 01 PID reads.

#### Scenario: Session logs DTC request

- GIVEN an initialized session with LoggingTransport
- WHEN `readStoredDtcs()` runs
- THEN diagnostic log contains TX for `03` and a DTC category result line
