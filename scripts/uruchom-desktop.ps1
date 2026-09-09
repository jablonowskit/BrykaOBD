#Requires -Version 5.1
<#
.SYNOPSIS
  Uruchamia BrykaOBD desktop (Compose Multiplatform / Windows).

.EXAMPLE
  .\scripts\uruchom-desktop.cmd
  powershell -ExecutionPolicy Bypass -File scripts/uruchom-desktop.ps1
#>
[CmdletBinding()]
param(
    [switch] $NoDaemon
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$gradlew = Join-Path $Root "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    throw "Brak gradlew.bat w $Root"
}

Write-Host "==> BrykaOBD desktop (:composeApp:run)" -ForegroundColor Cyan
Write-Host "    Logi diag: $env:USERPROFILE\.brykaobd\diag\"
Write-Host "    ELM: Połącz ELM → port COM (domyslnie 38400 baud)"
Write-Host ""

$argsList = @("--no-daemon", ":composeApp:run")
if (-not $NoDaemon) {
    # Default: allow daemon for faster relaunch; -NoDaemon forces clean CI-like run
    $argsList = @(":composeApp:run")
}

& $gradlew @argsList
if ($LASTEXITCODE -ne 0) {
    throw "gradlew :composeApp:run failed (exit $LASTEXITCODE)"
}
