# 004 — Smoke: sonda mapy PID/DID

## Kroki

1. Połącz ELM (lub Demo).
2. Status pokazuje `sonda N/M hit`.
3. Zakładka **DPF** → lista hitów + przycisk **Sonda mapy**.
4. W logu kategoria `DISCOVERY` (HIT / NoData / UdsNeg).

## Expect

- Na Aveo: Mode 01 hity + ewentualne Mode 22 po `ATSH7E0`.
- Pełny dump 0x0000–FFFF nie jest celem (za wolne na ELM).
