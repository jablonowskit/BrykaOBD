# Change: Keep-screen-on toggle on Sesja tab

## Why

Users run BrykaOBD as an in-car dashboard while driving. The device's normal screen timeout dims/locks the display, hiding live gauges. There was no way to prevent this from within the app.

## What Changes

- Add a "Nie wygaszaj ekranu" switch on the **Sesja** tab.
- When enabled, the Android app sets `FLAG_KEEP_SCREEN_ON` on the window; when disabled, the flag is cleared. Default is off (matches normal OS screen-timeout behavior).
- Wired through a platform callback (`onKeepScreenOnChanged`) from `App`/`ObdDashboardScreen` (common) down to `MainActivity` (Android), since screen-on control is Android-specific and `composeApp` has no Android source set.

## Capabilities

- Modified: `obd-session`
