# Change: Gauge + DPF tabs

## Why

After the first Aveo connection, the single flat PID list is hard to read in the car. Drivers expect a gauges-style view (speed, RPM, coolant, fuel use) and a separate DPF view for diesel soot/regen data. DPF values are often Mode 22 manufacturer DIDs (GM/Opel-style) that must tolerate NO DATA on Aveo 1.3D while still logging raw RX for discovery.

## What Changes

- UI tabs: **Zegary** (dashboard gauges) and **DPF**.
- Mode 01 fuel rate (5E) + instant L/100km when speed > 0.
- Mode 01 DPF temperature (7C) where supported.
- Mode 22 GM/Opel candidate DIDs: soot %, km since regen, status, DPF temp estimate.
- Demo responses for new requests; unit tests for Mode 22 parse.
- OpenSpec capability `dpf` + UI note in docs.

## Capabilities

- New: `dpf`
- Modified: `obd-session` (Mode 22 read path)
