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
2. **Windows first** — testy z ELM po COM
3. E2E na aucie: **Chevrolet Aveo 1.3D (2012)**
4. Linux / macOS
5. Android Classic SPP

## GitHub — co i jak

| | |
|---|---|
| Konto | `jablonowskit` |
| Lokalnie | `D:\github\BrykaOBD` |
| Remote | `https://github.com/jablonowskit/BrykaOBD.git` |
| Gałąź | `main` |
| Protokół | HTTPS |

Wzór katalogów jak w innych projektach (`D:\github\<Nazwa>` → `github.com/jablonowskit/<Nazwa>`).

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

Repo na GitHub + szkielet KMP/Compose. Następne: protokół ELM + mapy PID/DTC + serial Windows.
