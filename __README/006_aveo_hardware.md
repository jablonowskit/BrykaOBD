# 006 — Sprzęt i pierwsze odczyty (Aveo)

Notatki z realnych sesji — żeby nie zgadywać przy kolejnych zmianach protokołu / UI.

## Adapter (kostka)

| | |
|---|---|
| Nazwa BT | **V-LINK** |
| Adres | `10:21:3E:4B:91:FE` |
| Banner po `ATZ` | **`ELM327 v2.2`** |
| Uwaga | Typowy klon — string wersji nie gwarantuje oryginalnego ELM Electronics |

Źródło: log sesji `brykaobd_20260909_091142_V-LINK.txt` (i wcześniejsza `…090852…`):

```text
TX | ATZ\r
RX | ELM327 v2.2
```

Init AT (`ATE0`…`ATSP0`) przechodził z `OK`. Odpowiedzi Mode 01 bez spacji (`ATS0`) — np. `41053B` (naprawione w parserze continuous hex).

## Samochód

- **Chevrolet Aveo 1.3D (diesel), 2012**
- Porównanie: Car Scanner pokazywał **temp. płynu ~19 °C** — w surowym RX `41053B` → `0x3B` = 59 → **19 °C** (zgodne).

## Mode 03 (ta sesja)

Surowy RX (continuous): `SEARCHING...` + `430204030405` → po poprawnym parse: **P0403**, **P0405** (EGR). W UI przed fixem parsera lista DTC była pusta.

## Znane problemy

- Przed fixem continuous hex wszystkie PID leciały jako `PARSE` mimo poprawnych ramek.
- Reinstall APK z innym podpisem CI kasuje lokalne pliki `diag/` na telefonie — warto `adb pull` przed uninstall.
- Numer COM na Windows bywa zmienny po ponownym sparowaniu V-LINK.
- **Mode 22 DPF bez `ATSH7E0`**: RX `7F2222` (UDS reject) → w UI wyglądało jak brak danych; Car Scanner działa, bo stawia header ECM `7E0`. Fix: zakładka DPF → physical header.

## Sonda mapy (odkrywanie)

Przy połączeniu apka odpytuje **curated** listę (nie cały UDS):

- Mode 01 support `0100`…`01A0` + typowe PID diesla
- Mode 22 DPF z [Torque Astra-J 1.3](https://torque-bhp.com/community/main-forum/vauxhall-opel-gm-dpf-pid/paged/14/) (`223035`…`22327A`, `223047`)
- Alternatywy Astra-K (`22336A`, `2220F4`, …)

Wyniki: log `DISCOVERY` + zakładka DPF. Źródło mapy: społeczność Torque / Car Scanner profile — weryfikacja na Aveo LDV.
