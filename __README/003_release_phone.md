# 003 — Commit / push / CI / APK na telefon

Instrukcja dla **agentów LLM** i developerów: jak dostarczyć zmianę na telefon testowy.
Lokalnie zwykle **brak Android SDK** — APK buduje **GitHub Actions**, nie Gradle na hoście.

**Skill operacyjny:** [`.claude/skills/wgraj-na-telefon/SKILL.md`](../.claude/skills/wgraj-na-telefon/SKILL.md)  

**Preferowany one-liner:**
```powershell
powershell -ExecutionPolicy Bypass -File scripts/wgraj-na-telefon.ps1
```
Skrypt: CI dla dokładnego `HEAD` → download APK → `adb install -r` → assert świeżego `lastUpdateTime` → start apki.  
Konflikt podpisów: `-AllowUninstall` (kasuje `files/diag/` — ostrzeż użytkownika).

## Kontekst repo

| | |
|---|---|
| Katalog | `D:\github\BrykaOBD` |
| Remote | `https://github.com/jablonowskit/BrykaOBD.git` |
| Gałąź | `main` |
| CI | [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) |
| Artifact APK | `brykaobd-android-debug-apk` |
| Package | `app.brykaobd` |
| Shell | PowerShell — **nie** używaj `&&` (użyj `;` lub osobnych komend) |

## Zasady

1. **Commit / push tylko gdy użytkownik o to prosi** (albo gdy wprost każe wgrać APK na telefon — wtedy commit+push jest konieczny, by CI zbudowało nowy artefakt).
2. Nie commituj sekretów (`.env`, keystore, klucze).
3. Przed commit: `git status`, `git diff`, `git log -5 --oneline` (styl wiadomości jak w historii).
4. Po zmianach w kodzie OBD/UI: lokalnie `.\gradlew --no-daemon :shared:jvmTest` (działa bez Android SDK).

## Pipeline (krok po kroku)

### 1. Commit

```powershell
git add <pliki>
git commit -m @"
Krótki tytuł (dlaczego).

Opcjonalnie 1–2 zdania kontekstu.
"@
git status -sb
```

### 2. Push (odpala CI)

```powershell
git push origin HEAD
```

### 3. Poczekaj na CI

```powershell
gh run list --limit 3
gh run watch <RUN_ID> --exit-status
```

Joby:

- `test-shared` → `./gradlew :shared:jvmTest`
- `build-android-apk` → `./gradlew :androidApp:assembleDebug` + upload artifact

Ręcznie: Actions → *BrykaOBD CI* → *Run workflow* (`workflow_dispatch`).

### 4. Pobierz APK

```powershell
New-Item -ItemType Directory -Force -Path artifacts | Out-Null
gh run download <RUN_ID> -n brykaobd-android-debug-apk -D artifacts
```

Plik zwykle: `artifacts/androidApp-debug.apk`  
(`artifacts/` jest w `.gitignore` — OK, nie commituj APK).

### 5. Telefon + adb

```powershell
adb devices
```

Musi być `device` (nie `unauthorized`). USB debugging włączone.

### 6. Instalacja

```powershell
adb install -r artifacts\androidApp-debug.apk
```

#### Konflikt podpisów

Jeśli:

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` / *signatures do not match*

to debug keystore z CI różni się od poprzedniej instalacji:

```powershell
adb uninstall app.brykaobd
adb install artifacts\androidApp-debug.apk
```

**Uwaga:** uninstall kasuje dane aplikacji (w tym lokalne logi w `files/diag/`). Ostrzeż użytkownika, jeśli jeść zależne od zapisanych sesji.

## Szybka ścieżka (gdy user: „wgraj na telefon”)

1. Commit wszystkich potrzebnych zmian (za zgodą / przy poleceniu wgrania).
2. `git push origin HEAD`
3. `gh run watch <nowy_run> --exit-status`
4. `gh run download … -n brykaobd-android-debug-apk -D artifacts`
5. `adb devices` → `adb install -r …` (lub uninstall + install przy mismatch).
6. Krótko potwierdź sukces + model telefonu z `adb devices -l` jeśli dostępny.

## Po instalacji — smoke na telefonie

1. Ikona **BrykaOBD** na launcherze.
2. Uprawnienia Bluetooth (Android 12+).
3. **Demo PID** — dashboard + panel diagnostyki + wpis w **Zapisane sesje**.
4. Live: sparowany ELM → **Połącz ELM**.
5. Logi trwałe: `Android/data/app.brykaobd/files/diag/`  
   `adb pull /sdcard/Android/data/app.brykaobd/files/diag/`  
   Logcat (tylko na żywo): `adb logcat -s BrykaOBD`

## Czego nie robić

- Nie zakładaj lokalnego `assembleDebug` bez `ANDROID_HOME` / `local.properties` (u nas zwykle brak SDK).
- Nie force-push na `main`.
- Nie pomijaj CI „bo lokalnie działa desktop” — APK Android = artifact CI.
- Nie commituj `artifacts/*.apk`.

## Znane problemy

- CI debug APK może mieć **inny podpis** przy kolejnych runnerach → czasem wymagany uninstall.
- Telefon odłączony / `unauthorized` → najpierw napraw `adb`, potem instaluj.
- Po uninstall znikają zapisane sesje diag na urządzeniu.

## Powiązane

- [001_plan.md](./001_plan.md)
- [002_obd_references.md](./002_obd_references.md)
- [../__TESTS/001_diag_log.test.md](../__TESTS/001_diag_log.test.md)
- [../AGENTS.md](../AGENTS.md)
