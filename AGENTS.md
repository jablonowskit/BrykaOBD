## Dobór modelu do zadań

- Do prostych, jednoznacznych zadań (proste wyszukiwanie, drobne poprawki, mechaniczne zmiany) używaj prostszych/tańszych modeli LLM.
- Do zadań złożonych (wieloetapowe zmiany, analiza architektury, trudne decyzje projektowe) używaj bardziej zaawansowanych modeli LLM.

## OpenSpec (zachowanie systemu)

Źródło prawdy o tym, **co apka SHALL robić**: [openspec/README.md](openspec/README.md).

- Bieżące capability: `openspec/specs/{obd-session,android-bt,diag-archive,dtc,desktop-serial}/spec.md`
- **Nowe / zmienione zachowanie** → utwórz `openspec/changes/<kebab-id>/` (proposal + delta), zaimplementuj, zmerguj delty do `specs/`, zarchiwizuj change.
- **Drobnica** (literówka, ikona, docs-only, refactor bez zmiany zachowania) → bez OpenSpec.
- Szablon: [openspec/changes/_template/](openspec/changes/_template/).

Plany / notatki w `__README/` **nie** zastępują `openspec/specs/`.

## Dostarczanie na telefon (obowiązkowa wiedza)

**Skill:** [`.cursor/skills/wgraj-na-telefon/SKILL.md`](.cursor/skills/wgraj-na-telefon/SKILL.md)  
**Skrypt:** `powershell -ExecutionPolicy Bypass -File scripts/wgraj-na-telefon.ps1`  
Referencja: [__README/003_release_phone.md](__README/003_release_phone.md).

Gdy user mówi „wgraj / zainstaluj / na telefon” → przeczytaj skill → **uruchom skrypt** (CI dla HEAD + `lastUpdateTime`). Konflikt podpisów: `-AllowUninstall` dopiero po ostrzeżeniu o kasowaniu logów diag.
