## ADDED Requirements

### Requirement: Keep-screen-on toggle

The **Sesja** tab SHALL present a switch labeled "Nie wygaszaj ekranu" (default off). On Android, toggling it on MUST set `FLAG_KEEP_SCREEN_ON` on the activity window; toggling it off MUST clear that flag. The switch state MUST NOT affect PID polling, DTC handling, or diag logging.

#### Scenario: Enable keep-screen-on

- GIVEN the Sesja tab is visible
- WHEN the user turns on "Nie wygaszaj ekranu"
- THEN the Android window gets `FLAG_KEEP_SCREEN_ON` and the display no longer times out while the app is foregrounded

#### Scenario: Disable keep-screen-on

- GIVEN the switch is on
- WHEN the user turns it off
- THEN the Android window's `FLAG_KEEP_SCREEN_ON` is cleared and normal OS screen-timeout behavior resumes
