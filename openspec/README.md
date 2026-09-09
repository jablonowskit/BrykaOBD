# OpenSpec (BrykaOBD)

Lekki **spec-driven** workflow dla agentów LLM. Źródło prawdy o zachowaniu aplikacji.

Wzór: [Fission-AI/OpenSpec](https://github.com/Fission-AI/OpenSpec) — bez obowiązkowego CLI na start.

## Struktura

```
openspec/
  specs/           ← stan BIEŻĄCY (co system SHALL robić teraz)
    <capability>/spec.md
  changes/         ← aktywne jednostki pracy (propozycja + delta)
    <change-id>/
      proposal.md
      tasks.md      (opcjonalnie)
      specs/        (delty ADDED/MODIFIED/REMOVED)
  changes/archive/ ← po skończeniu zmiany
```

## Kiedy używać

| Typ pracy | OpenSpec? |
|-----------|-----------|
| Nowe / zmienione zachowanie OBD, BT, logi, UI flow | **Tak** — change + delta |
| Literówka, ikona, docs-only, drobny refactor bez zmiany zachowania | **Nie** |
| Procedura APK na telefon | Nie tu — [__README/003_release_phone.md](../__README/003_release_phone.md) |

## Cykl zmiany

1. Utwórz `openspec/changes/<kebab-id>/proposal.md` (Why ≥ sensowny opis, What, Capabilities).
2. Napisz delty w `changes/<id>/specs/<capability>/spec.md` (`## ADDED|MODIFIED|REMOVED Requirements`).
3. Zaimplementuj + testy (`:shared:jvmTest`).
4. Zmerge’uj delty do `openspec/specs/<capability>/spec.md`.
5. Przenieś folder zmiany do `openspec/changes/archive/YYYY-MM-DD-<id>/`.

## Format `spec.md`

- `## Purpose` — kontekst (min. ~50 znaków).
- `## Requirements` — co najmniej jedno wymaganie.
- `### Requirement: …` — zdanie z **SHALL** / **MUST**.
- `#### Scenario: …` — dokładnie 4 `#`; treść WHEN/THEN (opcjonalnie GIVEN).

## Capability (obecne)

| Katalog | Zakres |
|---------|--------|
| [obd-session](./specs/obd-session/spec.md) | ELM327 init, Mode 01 PID, dashboard, demo |
| [android-bt](./specs/android-bt/spec.md) | Classic SPP, uprawnienia, wybór urządzenia |
| [diag-archive](./specs/diag-archive/spec.md) | Log UI/Logcat + trwały plik sesji / share |
| [dtc](./specs/dtc/spec.md) | Mode 03 odczyt / Mode 04 kasowanie DTC |

Plany produktowe / stack: [__README/001_plan.md](../__README/001_plan.md) — **nie** zastępują specs.
