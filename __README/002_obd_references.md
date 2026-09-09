# 002 — Wzorce i źródła OBD / ELM327

Gdzie szukać gotowych projektów i dokumentacji (inspiracja, nie kopiuj 1:1 bez licencji).

## Najbardziej pasujące do BrykaOBD (Kotlin + Android Classic SPP)

| Projekt | Co brać | Link |
|---------|---------|------|
| **kotlin-obd-api** | Komendy ELM, parsowanie PID/DTC, API na `InputStream`/`OutputStream` (osobny transport) | https://github.com/eltonvs/kotlin-obd-api |
| **obd-scanner-android** | Sample Compose + BT Classic SPP + polling PID (używa kotlin-obd-api) | https://github.com/ETSoftwareStudio/obd-scanner-android |
| **AndroidOBD** (barnhill) | Init ELM + Bluetooth socket, formuły PID | https://github.com/barnhill/androidobd |

## Pełne aplikacje (architektura / UX / edge cases)

| Projekt | Co brać | Link |
|---------|---------|------|
| **AndrOBD** | Dojrzała apka FOSS: BT/USB/WiFi, dashboard, DTC, demo mode | https://github.com/fr3ts0n/AndrOBD |
| **OBDForge** | Nowoczesny Kotlin/Compose, multi-transport (SPP+BLE+…), GPL | https://github.com/edwardlthompson/OBDForge |

## Protokół i mapy (referencje, niekoniecznie Kotlin)

| Źródło | Po co |
|--------|--------|
| [ELM327 datasheet](https://www.elmelectronics.com/wp-content/uploads/2016/07/ELM327DS.pdf) | Komendy AT, format odpowiedzi, prompt `>` |
| [python-OBD](https://github.com/brendan-w/python-OBD) | Klasyczna mapa komend/PID, dekoder, timeouty |
| [py-obdii](https://github.com/PaulMarisOUMary/OBDII) | Nowszy Python + emulator ELM do testów bez auta |
| Wikipedia: OBD-II PIDs | Formuły Mode 01 (J1979) |
| SAE J1979 / J2012 | Oficjalne PID / DTC (płatne PDF; treść często w OSS) |

## Co czytać w jakiej kolejności (dla nas)

1. **Datasheet ELM327** — sesja AT + request/response (nasz `Elm327Session`).
2. **kotlin-obd-api** + **obd-scanner-android** — wzorzec BT SPP na Androidzie i podział transport vs protokół.
3. **python-OBD** — cross-check dekodowania PID i obsługi `NO DATA`.
4. **AndrOBD / OBDForge** — UX, reconnect, dziwne klony adapterów (nie na start).

## Licencje (uwaga)

- Przed wklejeniem kodu sprawdź licencję (Apache/MIT vs GPL).
- Bezpiecznie: czytać jako wzorzec zachowania, własne API w `shared/` (tak jak teraz).

## Znane problemy przy korzystaniu z OSS

- Wiele „ELM327” to klony — zachowanie AT różni się.
- Classic SPP ≠ BLE — wzorce BLE nie pomogą przy naszym dongle.
- Nie każdy PID istnieje w każdym aucie (Aveo diesel: logować `NO DATA`).
