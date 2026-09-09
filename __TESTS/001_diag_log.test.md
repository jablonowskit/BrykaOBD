# 001 — Smoke: diagnostyka OBD

## Cel

Potwierdzić log TX/RX/PID w UI, Logcat oraz **trwały plik sesji** (po powrocie z auta).

## Kroki

1. Zainstaluj debug APK (CI artifact).
2. Uruchom **Demo PID** → panel **Diagnostyka** pokazuje AT + TX/RX + PID.
3. Status pokazuje nazwę pliku logu; **Zapisane sesje** listuje `brykaobd_*.txt`.
4. Zamknij aplikację → otwórz ponownie → **Zapisane sesje** → podgląd / **Udostępnij**.
5. (Opcjonalnie) `adb pull /sdcard/Android/data/app.brykaobd/files/diag/`
6. (Opcjonalnie) Live ELM w aucie + `adb logcat -s BrykaOBD`.

## Oczekiwany wynik

- Demo: wpisy `SESSION`, `AT`, `TX`, `RX`, `PID`, `FILE`.
- Po restarcie apki poprzednia sesja nadal czytelna z dysku.
- Live Aveo: timeout / `NO DATA` w pliku sesji.

## Automat

`./gradlew :shared:jvmTest` — `ObdDiagLogTest`, `FileDiagArchiveTest`.
