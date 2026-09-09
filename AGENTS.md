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

Procedura operacyjna (skill): [`.claude/skills/wgraj-na-telefon/SKILL.md`](.claude/skills/wgraj-na-telefon/SKILL.md)
(kopia Cursor: [`.cursor/skills/wgraj-na-telefon/`](.cursor/skills/wgraj-na-telefon/)).

Referencja: [__README/003_release_phone.md](__README/003_release_phone.md).

Gdy user mówi „wgraj / zainstaluj / na telefon” → **najpierw przeczytaj skill**, potem wykonaj (w tym weryfikację `lastUpdateTime`).

Skrót:

1. Commit + `git push origin HEAD` (żeby CI zbudowało aktualny APK).
2. `gh run watch` → `gh run download … -n brykaobd-android-debug-apk -D artifacts`.
3. `adb install -r artifacts\androidApp-debug.apk`.
4. Przy konflikcie podpisów: `adb uninstall app.brykaobd` + install (kasuje logi diag — ostrzeż).
5. Potwierdź: `adb shell dumpsys package app.brykaobd` → świeże `lastUpdateTime`.
6. PowerShell: bez `&&`. Package: `app.brykaobd`. Testy host: `.\gradlew --no-daemon :shared:jvmTest`.
