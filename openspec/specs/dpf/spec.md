## Purpose

Diesel particulate filter (DPF) and related soot/regen metrics for BrykaOBD, including SAE Mode 01 where available and GM/Opel-style Mode 22 DIDs that may return NO DATA on Chevrolet Aveo 1.3D 2012 while still logging raw RX for discovery.

## Requirements

### Requirement: Gauge and DPF dashboard tabs

The UI SHALL present two primary live-data tabs: **Zegary** (gauge metrics) and **DPF**. While connected (Demo or live ELM), the active tab SHALL poll its metric list through the shared session mutex. Instant L/100km SHALL be derived from Mode 01 fuel rate (`5E`) and speed when speed is at least 5 km/h; otherwise the UI MUST show an empty value.

#### Scenario: Switch to DPF tab

- GIVEN a connected Demo or live session
- WHEN the user selects the DPF tab
- THEN the UI polls DPF metrics (Mode 01 `7C` and Mode 22 candidates) and shows NO DATA / PARSE without crashing when the ECU omits a DID

### Requirement: Mode 22 manufacturer DID reads

The session SHALL send Mode 22 requests as `22` + four hex DID digits and decode positive responses starting with `62` + DID bytes. Before Mode 22 DPF polls the session MUST set ELM header to ECM physical address (`ATSH7E0`), matching Car Scanner / Torque GM profiles, and restore functional addressing (`ATSH7DF`) for standard Mode 01/03. UDS negative responses (`7F …`) MUST surface as a per-metric error (not a generic PARSE). Missing DIDs (`NO DATA`) MUST NOT abort the rest of the poll cycle. Raw RX MUST remain available via the existing diag log / archive path for Aveo discovery.

#### Scenario: Demo soot load

- GIVEN Demo ELM transport
- WHEN the session reads DID `3275` with ECM physical addressing
- THEN the decoded soot load is a percentage value suitable for the DPF tab

#### Scenario: Mode 22 without ECM header yields UDS reject

- GIVEN Aveo live log RX `7F2222` for `223275`
- WHEN parsed as an error
- THEN the metric error identifies UDS negative response `conditionsNotCorrect` (not PARSE alone)

### Requirement: Curated PID/DID discovery probe

After ELM init on Demo or live connect, the session SHALL run a one-shot discovery probe over a curated candidate map (SAE Mode 01 support/extras + Torque Astra-J 1.3 DPF Mode 22 + Astra-K alternates) using the correct address mode per request. Each reply MUST be classified (positive / NO DATA / UDS negative / other) and logged under category `DISCOVERY` with raw-derived payload when positive. The UI MUST show hit count/payload summary on the DPF tab and allow re-running the probe. Full DID space brute-force is out of scope.

#### Scenario: Demo probe hits known requests

- GIVEN Demo ELM transport
- WHEN `probeDiscovery` runs the Aveo first-probe map
- THEN at least Mode 01 speed `010D` and Mode 22 soot `223275` are classified as hits
