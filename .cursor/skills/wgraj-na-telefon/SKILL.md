---
name: wgraj-na-telefon
description: >-
  Zbuduj przez CI i zainstaluj APK BrykaOBD na podłączonym telefonie, z
  potwierdzeniem lastUpdateTime. Użyj przy: "wgraj", "zainstaluj",
  "wrzuć na telefon", test OBD/UI na urządzeniu.
---

# Wgranie BrykaOBD na telefon

Lokalnie zwykle **brak Android SDK** — APK buduje GitHub Actions.
**Domyślnie uruchom skrypt** (nie ręczny adb „na oko”).

| | |
|---|---|
| Package | `app.brykaobd` |
| Activity | `app.brykaobd/app.brykaobd.android.MainActivity` |
| Artifact | `brykaobd-android-debug-apk` |
| Skrypt | [`scripts/wgraj-na-telefon.ps1`](../../../scripts/wgraj-na-telefon.ps1) |
| Docs | [`__README/003_release_phone.md`](../../../__README/003_release_phone.md) |

Shell: PowerShell — **bez** `&&`.

## Ścieżka A — preferowana (skrypt)

1. Jeśli są lokalne zmiany wymagane na telefonie: **commit + push** (albo sam skrypt zrobi `git push`, gdy tree czyste a HEAD ahead of origin).
2. Telefon podłączony (`adb devices` → `device`).
3. Z katalogu repo:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts/wgraj-na-telefon.ps1
   ```
4. Konflikt podpisów (skrypt rzuci jasny błąd) → ostrzeż o kasowaniu logów diag, potem:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts/wgraj-na-telefon.ps1 -AllowUninstall
   ```
5. Skrypt **sam**:
   - pilnuje czystego gita / pusha
   - czeka na CI dla **dokładnie** `git rev-parse HEAD` (`gh run list --commit`)
   - pobiera artifact do `artifacts/androidApp-debug.apk`
   - `adb install -r`
   - weryfikuje świeże `lastUpdateTime` (fail jeśli stare)
   - uruchamia apkę

**Sukces = skrypt kończy się bez throw + zielone „OK: instalacja świeża”.**  
Sam `adb Success` **nie wystarczy** (pułapka z Nuty 09.09.2026).

### Flagi skryptu

| Flaga | Kiedy |
|-------|--------|
| `-AllowUninstall` | Tylko konflikt podpisów; kasuje dane/`files/diag/` |
| `-NoLaunch` | Zainstaluj bez startu UI |
| `-Device <serial>` | Wiele telefonów w `adb devices` |
| `-SkipGitCheck` | Awaryjnie; łatwo wgrać **stary** APK — unikaj |
| `-WaitSeconds 900` | Max czekania na CI |

## Ścieżka B — ręczna (gdy skrypt niedostępny)

1. `git status` / push HEAD.
2. `adb devices` → `device`.
3. `gh run list --commit $(git rev-parse HEAD)` → `gh run watch <ID> --exit-status`.
4. `gh run download <ID> -n brykaobd-android-debug-apk -D artifacts`
5. `adb install -r artifacts\androidApp-debug.apk`
6. Przy `UPDATE_INCOMPATIBLE`: uninstall tylko po zgodzie usera (logi).
7. **Obowiązkowo:**
   ```powershell
   adb shell dumpsys package app.brykaobd | Select-String "versionName|lastUpdateTime"
   Get-Date -Format "yyyy-MM-dd HH:mm:ss"
   ```
   `lastUpdateTime` musi być sprzed ~2 minut. Inaczej **nie** melduluj sukcesu.

## Decyzje agenta

| Sytuacja | Działanie |
|----------|-----------|
| User: „wgraj na telefon”, dirty tree | Commit (za zgodą / przy tym poleceniu) → push → skrypt |
| CI czerwone | Nie instaluj; pokaż link do runa / log |
| `unauthorized` | Poproś usera o dialog USB; nie obchodź |
| Kilka urządzeń | `-Device` z seriala |
| Chce zachować logi Aveo | **Bez** `-AllowUninstall`; rozwiąż podpis inaczej lub ostrzeż |

## Czego NIE robić

- Nie `:androidApp:assembleDebug` bez SDK „na skróty”.
- Nie uninstall „dla czystości”.
- Nie `gh run download` **najnowszego** runa z `main`, jeśli HEAD to inny commit — zawsze `--commit` bieżącego SHA.
- Nie kończ po samym `Success` bez weryfikacji czasu.
- Nie commituj `artifacts/*.apk`.

## Smoke (opcjonalnie)

1. Ikona / uruchomiona apka.
2. Demo PID → Diagnostyka + Zapisane sesje.
3. Live: Połącz ELM (sparowany dongle).
4. `adb logcat -s BrykaOBD`
