## ADDED Requirements

### Requirement: Read stored DTCs (Mode 03)

The system SHALL request Mode 03 (`03`) over the active ELM session and decode returned frames into SAE J2012-style codes (`P`/`C`/`B`/`U` + four hex digits). Empty responses and `NO DATA` MUST yield an empty list (not a fake code). Unparseable non-error payloads MUST be logged and surface as a session/UI error without crashing.

#### Scenario: Decode sample P0301

- GIVEN ELM reply containing `43 01 33` (or spaced equivalent with service `43` and payload encoding P0301)
- WHEN Mode 03 is parsed
- THEN the result includes code `P0301`

#### Scenario: No stored codes

- GIVEN ELM reply `NO DATA` or only null DTC pairs
- WHEN `readStoredDtcs` completes
- THEN the returned list is empty and no fabricated codes appear

### Requirement: Clear stored DTCs (Mode 04)

The system SHALL send Mode 04 (`04`) only after explicit user confirmation in the UI. A successful clear MUST be followed by a fresh Mode 03 read (or empty list indication). Failures MUST be shown in UI status and diagnostic log.

#### Scenario: Clear requires confirmation

- GIVEN stored DTCs are shown
- WHEN the user taps clear
- THEN a confirmation dialog appears before any `04` request is sent

### Requirement: DTC panel on connected dashboard

While Demo or live ELM is connected, the UI MUST show a DTC section with refresh and clear actions. DTC traffic MUST go through the same transport/session mutex so it does not corrupt concurrent Mode 01 polling.

#### Scenario: Refresh DTCs while polling PIDs

- GIVEN a live session polling the dashboard
- WHEN the user taps refresh DTCs
- THEN Mode 03 runs without crashing the session and the list updates
