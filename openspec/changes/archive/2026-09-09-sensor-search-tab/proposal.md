# Change: Sensor search tab

## Why

Users need a way to find sensors by name/PID and add them to a live poll list without cluttering the gauge cluster. Narrow filters should show live preview values so the user can see whether a sensor responds before adding it.

## What Changes

- New **Szukaj** tab with text filter over `SensorCatalog`.
- Click **Dodaj** → active poll list with live cards and **Usuń**.
- When filtered hits are &lt; 20 and connected, preview-poll those PIDs on the Szukaj tab.
- Active sensors continue polling via the shared mutex regardless of tab.

## Capabilities

- New: `sensor-search`
- Modified: `dpf` (tab row includes Szukaj)
