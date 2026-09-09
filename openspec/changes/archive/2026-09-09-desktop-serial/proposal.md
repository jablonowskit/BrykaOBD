# Change: Desktop serial ELM (Windows laptop in car)

## Why

Testing with a phone in the car is awkward for reading logs and iterating. A Windows laptop with the same Compose UI and an ELM327 on a virtual COM port (Bluetooth SPP pair or USB-serial) is often easier for Aveo sessions. Shared Elm327Session already works over Transport — desktop only lacked a serial implementation and a port picker wired like Android BT.

## What Changes

- `SerialTransport` + `SerialElmFacade` (jSerialComm) on JVM/desktop.
- Desktop `App` passes the facade so **Połącz ELM** lists COM ports.
- Default baud **38400** (classic ELM); documented override.
- OpenSpec capability `desktop-serial`.
- Plan/docs: laptop-in-car path.

## Capabilities

- New: `desktop-serial`
- Modified: none required for `android-bt` (unchanged)
