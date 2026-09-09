# 005 — Desktop Windows: ELM przez COM

## Po co

Łatwiej debugować w aucie z laptopem (logi, klawiatura) niż tylko z telefonem.

## Jak

1. Sparuj ELM327 w Windows (Classic) **albo** podłącz USB-ELM.
2. W Menedżerze urządzeń sprawdź port (`COMx`).
3. Uruchom desktop:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts/uruchom-desktop.ps1
   ```
   (albo `.\gradlew :composeApp:run`)
4. **Połącz ELM** → wybierz `COMx`.
5. Baud domyślnie **38400**. Logi: `%USERPROFILE%\.brykaobd\diag\`.

## Znane problemy

- Zły baud → timeout na `>` (klony bywają 9600/115200).
- Port zajęty przez inny program (Car Scanner) → nie otworzy COM.
- Numer COM zmienia się po ponownym sparowaniu.
