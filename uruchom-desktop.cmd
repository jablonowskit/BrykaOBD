@echo off
REM Przelotka: uruchom desktop BrykaOBD (omija ExecutionPolicy).
call "%~dp0scripts\uruchom-desktop.cmd" %*
exit /b %ERRORLEVEL%
