@echo off
REM Uruchamia BrykaOBD desktop (omija ExecutionPolicy przez -File + Bypass).
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0uruchom-desktop.ps1" %*
exit /b %ERRORLEVEL%
