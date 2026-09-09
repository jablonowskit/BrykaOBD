## Dobór modelu do zadań

- Do prostych, jednoznacznych zadań (proste wyszukiwanie, drobne poprawki, mechaniczne zmiany) używaj prostszych/tańszych modeli LLM.
- Do zadań złożonych (wieloetapowe zmiany, analiza architektury, trudne decyzje projektowe) używaj bardziej zaawansowanych modeli LLM.

## Dostarczanie na telefon (obowiązkowa wiedza)

Pełna procedura: [__README/003_release_phone.md](__README/003_release_phone.md).

Skrót:

1. Commit (tylko na prośbę usera / przy „wgraj na telefon”) → `git push origin HEAD`.
2. CI buduje APK (`brykaobd-android-debug-apk`) — lokalnie zwykle **bez** Android SDK.
3. `gh run watch` → `gh run download … -D artifacts` → `adb install -r artifacts\androidApp-debug.apk`.
4. Przy `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: `adb uninstall app.brykaobd`, potem `adb install` (kasuje dane/logi na telefonie).
5. PowerShell: nie używaj `&&` — `;` lub osobne komendy.
6. Package: `app.brykaobd`. Testy bez SDK: `.\gradlew --no-daemon :shared:jvmTest`.
