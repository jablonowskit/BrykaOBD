## Dobór modelu do zadań

- Do prostych, jednoznacznych zadań (proste wyszukiwanie, drobne poprawki, mechaniczne zmiany) używaj prostszych/tańszych modeli LLM.
- Do zadań złożonych (wieloetapowe zmiany, analiza architektury, trudne decyzje projektowe) używaj bardziej zaawansowanych modeli LLM.

## OpenSpec (zachowanie systemu)

Źródło prawdy o tym, **co apka SHALL robić**: [openspec/README.md](openspec/README.md).

- Bieżące capability: `openspec/specs/{obd-session,android-bt,diag-archive}/spec.md`
- **Nowe / zmienione zachowanie** → utwórz `openspec/changes/<kebab-id>/` (proposal + delta), zaimplementuj, zmerguj delty do `specs/`, zarchiwizuj change.
- **Drobnica** (literówka, ikona, docs-only, refactor bez zmiany zachowania) → bez OpenSpec.
- Szablon: [openspec/changes/_template/](openspec/changes/_template/).

Plany / notatki w `__README/` **nie** zastępują `openspec/specs/`.

## Dostarczanie na telefon (obowiązkowa wiedza)

Pełna procedura: [__README/003_release_phone.md](__README/003_release_phone.md).

Skrót:

1. Commit (tylko na prośbę usera / przy „wgraj na telefon”) → `git push origin HEAD`.
2. CI buduje APK (`brykaobd-android-debug-apk`) — lokalnie zwykle **bez** Android SDK.
3. `gh run watch` → `gh run download … -D artifacts` → `adb install -r artifacts\androidApp-debug.apk`.
4. Przy `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: `adb uninstall app.brykaobd`, potem `adb install` (kasuje dane/logi na telefonie).
5. PowerShell: nie używaj `&&` — `;` lub osobne komendy.
6. Package: `app.brykaobd`. Testy bez SDK: `.\gradlew --no-daemon :shared:jvmTest`.
