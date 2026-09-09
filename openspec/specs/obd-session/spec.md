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

The live dashboard SHALL poll at least these Mode 01 PIDs with SAE J1979-style decoding: RPM (`0C`), speed (`0D`), coolant (`05`), engine load (`04`), throttle (`11`), intake air temp (`0F`), control module voltage (`42`). Manufacturer-specific / GM DPF PIDs are out of scope for this capability.

#### Scenario: RPM decode

- GIVEN Mode 01 response data bytes `1A F8` for PID `0C`
- WHEN the RPM decoder runs
- THEN the value is `1726` rpm

### Requirement: Error and empty OBD responses

When the ELM reply indicates an error or empty data (`NO DATA`, `UNABLE TO CONNECT`, `BUS INIT ERROR`, `CAN ERROR`, `STOPPED`, or unparsable payload), the corresponding `PidReading` MUST expose a non-null `error` (or equivalent) and MUST NOT invent a numeric value.

#### Scenario: NO DATA on PID

- GIVEN an ELM reply containing `NO DATA` for a requested PID
- WHEN `readPid` completes
- THEN `value` is null and `error` is `NO DATA`

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
