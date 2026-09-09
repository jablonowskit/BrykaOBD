---
name: wgraj-na-telefon
description: >-
  Zbuduj przez CI i zainstaluj APK BrykaOBD na podłączonym telefonie, z
  potwierdzeniem, że instalacja faktycznie się wykonała. Użyj, gdy użytkownik
  mówi "wgraj", "zainstaluj", "wrzuć na telefon" albo prosi o przetestowanie
  zmian OBD/UI na urządzeniu.
---

# Wgranie BrykaOBD na telefon

Lokalnie zwykle **brak Android SDK** — APK buduje GitHub Actions (`:androidApp:assembleDebug`).
Szczegóły referencyjne: [`__README/003_release_phone.md`](../../__README/003_release_phone.md).
Shell: **PowerShell** — nie używaj `&&`; użyj `;` lub osobnych komend.

| | |
|---|---|
| Package | `app.brykaobd` |
| Artifact CI | `brykaobd-android-debug-apk` |
| Plik lokalny | `artifacts/androidApp-debug.apk` |
| Gałąź | `main` |

## Kroki

1. **Sprawdź, że zmiany są wypchnięte.** CI buduje z `origin/main`, nie z brudnego drzewa.
   ```powershell
   git status --short
   git log --oneline -1
   ```
   Niezacommitowane / niepushnięte zmiany → najpierw commit + `git push origin HEAD`
   (przy poleceniu „wgraj na telefon” to jest OK i konieczne). Inaczej wgrasz starą wersję.

2. **Sprawdź telefon:**
   ```powershell
   adb devices
   ```
   Musi być stan `device`. `unauthorized` / brak wpisu → poproś użytkownika o USB debugging;
   nie obchodź.

3. **Poczekaj na CI (run z najnowszego pusha):**
   ```powershell
   gh run list --limit 3
   gh run watch <RUN_ID> --exit-status
   ```
   Timeout ~900 s. Zielone CI = testy `shared` + zbudowany APK.

4. **Pobierz APK:**
   ```powershell
   New-Item -ItemType Directory -Force -Path artifacts | Out-Null
   Remove-Item -Force artifacts\androidApp-debug.apk -ErrorAction SilentlyContinue
   gh run download <RUN_ID> -n brykaobd-android-debug-apk -D artifacts
   ```

5. **Zainstaluj (preferuj zachowanie danych):**
   ```powershell
   adb install -r artifacts\androidApp-debug.apk
   ```

6. **Konflikt podpisów** — tylko gdy:
   `INSTALL_FAILED_UPDATE_INCOMPATIBLE` / *signatures do not match*
   ```powershell
   adb uninstall app.brykaobd
   adb install artifacts\androidApp-debug.apk
   ```
   **Ostrzeż użytkownika:** uninstall kasuje dane apki, w tym logi w
   `Android/data/app.brykaobd/files/diag/`. Nie uninstall „dla czystości”.

7. **POTWIERDŹ instalację — nie pomijaj:**
   ```powershell
   adb shell dumpsys package app.brykaobd | Select-String -Pattern "versionName|lastUpdateTime"
   Get-Date -Format "yyyy-MM-dd HH:mm:ss"
   ```
   `lastUpdateTime` musi być sprzed kilku–kilkunastu sekund. Jeśli jest starszy,
   instalacja się **nie** wykonała mimo braku błędu adb — nie melduluj sukcesu.

## Pułapka (z Nuty, obowiązuje też tu)

Sam komunikat `Success` z `adb install` albo „pobrałem APK” **nie wystarczy**.
Zawsze sprawdzaj `lastUpdateTime`. Potwierdzone w Nucie 09.09.2026: agent zgłaszał
wgranie, a na telefonie stała wersja sprzed godzin.

## Czego NIE robić

- Nie zakładaj lokalnego `:androidApp:assembleDebug` bez `ANDROID_HOME` / SDK.
- Nie force-push na `main`.
- Nie commituj `artifacts/*.apk`.
- Nie pomijaj kroku 7 (weryfikacja `lastUpdateTime`).

## Smoke po wgraniu (opcjonalnie, gdy user testuje)

1. Ikona BrykaOBD na launcherze.
2. **Demo PID** → dashboard + Diagnostyka + wpis w Zapisane sesje.
3. Live: sparowany ELM → **Połącz ELM**.
4. Logcat na żywo: `adb logcat -s BrykaOBD`.
