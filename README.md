# BrykaOBD

Aplikacja OBD-II (ELM327): jeden UI na **Android / Windows / Linux / macOS**.

Repozytorium: [https://github.com/jablonowskit/BrykaOBD](https://github.com/jablonowskit/BrykaOBD)

## Stack

- **Kotlin Multiplatform** + **Compose Multiplatform** (jeden UI)
- Shared: protokół ELM327 + mapa PID + mapa DTC
- Desktop: serial (COM / `/dev/tty*` / `cu.*`) po sparowaniu BT lub USB
- Android: Bluetooth **Classic SPP**
- Bez Electron / Expo. iOS OBD — poza MVP

## Priorytet rozwoju

1. Rdzeń OBD (`shared`) + wspólny UI
2. **Android first (telefon)** — Bluetooth Classic SPP + ELM
3. E2E na aucie: **Chevrolet Aveo 1.3D (2012)** z telefonu
4. Desktop Windows / Linux / macOS (serial) — później

## GitHub — co i jak

| | |
|---|---|
| Konto | `jablonowskit` |
| Lokalnie | `D:\github\BrykaOBD` |
| Remote | `https://github.com/jablonowskit/BrykaOBD.git` |
| Gałąź | `main` |
| Protokół | HTTPS |

Wzór katalogów jak w innych projektach (`D:\github\<Nazwa>` → `github.com/jablonowskit/<Nazwa>`).

## CI

Przy każdym pushu / PR ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)):

- `:shared:jvmTest`
- `:androidApp:assembleDebug` → artifact **brykaobd-android-debug-apk**

## Dokumentacja

- [__README/001_plan.md](__README/001_plan.md) — plan, architektura, znane problemy
- [AGENTS.md](AGENTS.md) — wskazówki dla agentów LLM

## Moduły

| Moduł | Rola |
|-------|------|
| `shared` | Transport, FakeTransport, (ELM/PID/DTC w toku) |
| `composeApp` | Wspólny UI Compose (desktop + Android library) |
| `androidApp` | Wejście aplikacji Android |

## Status

Repo na GitHub + szkielet KMP/Compose + ekran dashboardu (standardowe PID Mode 01). Następne: Bluetooth Classic SPP na telefonie.

