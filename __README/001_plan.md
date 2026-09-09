# 001 — Plan BrykaOBD

## Cel

Odczyt OBD-II przez adapter **ELM327**: komunikacja + mapa kodów (PID/DTC) + jeden UI Compose.

## Stack

- Kotlin Multiplatform + Compose Multiplatform
- Transport Android: Bluetooth Classic SPP — **pierwsze testy na telefonie**
- Transport desktop: serial (Windows/Linux/macOS) — później

## GitHub

| Element | Wartość |
|---------|---------|
| Konto | `jablonowskit` |
| Lokalnie | `D:\github\BrykaOBD` |
| Remote | https://github.com/jablonowskit/BrykaOBD.git |
| Gałąź | `main` |
| Protokół | HTTPS |

Wzór jak [Nuta](https://github.com/jablonowskit/Nuta) (`D:\github\Nuta`). Reguł „tylko Docker / bez Gradle na hoście” z Nuty **nie** stosujemy tutaj — budowanie APK i testy z telefonem + ELM lokalnie.

## Samochód testowy

- **Chevrolet Aveo 1.3D (diesel), 2012**
- MVP: Mode 01 (standardowe PID) + Mode 03 (DTC)
- DPF / PID producenta GM — później
- Tu logować: co działa / `NO DATA` na Aveo

## Architektura (docelowa)

```
shared/          ELM, mapy PID/DTC, Transport
  commonMain/
  jvmMain/       SerialTransport
  androidMain/   BluetoothClassicTransport
composeApp/      jeden UI (android + desktop)
```

## Specyfikacja

- ELM327 datasheet: https://www.elmelectronics.com/wp-content/uploads/2016/07/ELM327DS.pdf
- PID/DTC: publiczne zestawienia J1979 / J2012

## Kolejność

1. Bootstrap GitHub — done
2. Szkielet KMP + Compose (`shared`, `composeApp`, `androidApp`) — done (minimalny UI)
3. Mapy + ELM + testy — next
4. **Android Classic SPP** — budowa na telefonie, E2E z ELM
5. Aveo 1.3D 2012 checklist (telefon w aucie)
6. Desktop serial (Windows / Linux / macOS)

## Znane problemy

- Numer COM na Windows bywa zmienny po ponownym sparowaniu
- Linux: grupa `dialout` / RFCOMM
- Klony ELM — timeouty i retry w protokole
- Część PID na Aveo diesel może zwracać `NO DATA`
- iOS + Classic SPP — poza MVP
