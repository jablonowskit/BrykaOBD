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

## CI

- Workflow: [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)
- Trigger: push, PR, `workflow_dispatch`
- Joby: `shared` unit testy + Android **debug** APK (artifact)
- Release/signing — później (na start bez keystore)

## Specyfikacja

- OpenSpec (zachowanie): [../openspec/README.md](../openspec/README.md) — patrz też [004_openspec.md](./004_openspec.md)
- ELM327 datasheet: https://www.elmelectronics.com/wp-content/uploads/2016/07/ELM327DS.pdf
- PID/DTC: publiczne zestawienia J1979 / J2012

## Kolejność

1. Bootstrap GitHub — done
2. Szkielet KMP + Compose (`shared`, `composeApp`, `androidApp`) — done (minimalny UI)
3. Mapy + ELM + ekran dashboardu (PID uniwersalne) — done; Demo PID
4. **Android Classic SPP** — Połącz ELM — done
5. **Diagnostyka szczegółowa** — panel + Logcat `BrykaOBD` — done
6. **Trwały zapis sesji** (plik + Zapisane sesje / Udostępnij) — done
7. **Mode 03/04 DTC** — panel błędów + kasowanie z potwierdzeniem — done
8. Aveo 1.3D 2012 checklist (telefon w aucie)
9. **Desktop serial (Windows COM)** — Połącz ELM na laptopie — done
10. Linux/macOS serial — ten sam kod JVM (weryfikacja później)

### Bluetooth na telefonie
1. Sparuj ELM327 w ustawieniach Androida.
2. Zainstaluj debug APK według [003_release_phone.md](./003_release_phone.md) (CI → `adb`).
3. Udziel uprawnień BT → **Połącz ELM** → wybierz adapter → dashboard na żywo.
4. Panel **Diagnostyka** + **Zapisane sesje** (pliki w `Android/data/app.brykaobd/files/diag/`) — po powrocie z auta podgląd / udostępnienie; Logcat tag `BrykaOBD` tylko na żywo.

## Znane problemy

- Numer COM na Windows bywa zmienny po ponownym sparowaniu
- Linux: grupa `dialout` / RFCOMM
- Klony ELM — timeouty i retry w protokole
- Część PID na Aveo diesel może zwracać `NO DATA`
- iOS + Classic SPP — poza MVP
- Polling dashboardu szybko wypełnia ring-buffer UI (~600 linii); **plik sesji** trzyma pełny przebieg (do limitu liczby sesji)
- Logcat nie jest trwałym archiwum — do diagnozy po jeździe używaj plików / Udostępnij
- CI debug APK: możliwy konflikt podpisów przy reinstall — patrz [003_release_phone.md](./003_release_phone.md)
