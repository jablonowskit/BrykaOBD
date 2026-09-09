# 004 — Smoke: zakładka Szukaj

## Kroki

1. Demo PID (telefon lub desktop).
2. Zakładka **Szukaj** — puste pole: lista katalogu, bez podglądu live (katalog ≥20).
3. Wpisz `olej` — mniej niż 20 trafień; widać podgląd wartości (temp. oleju / ciśnienie).
4. **Dodaj** temp. oleju → sekcja **Aktywne odczyty** z żywą wartością.
5. **Usuń** — znika z aktywnych.
6. Przełącz na Zegary / DPF — aktywne (jeśli dodane ponownie) nadal mogą być pollowane w tle.

## Expect

- Brak crasha przy filtrze / dodaj / usuń.
- Preview tylko przy &lt;20 trafieniach.
