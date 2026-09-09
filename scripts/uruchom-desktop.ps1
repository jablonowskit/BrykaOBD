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

# Compile first so run never hits ClassNotFoundException on a wiped/locked Kotlin cache.
$argsList = @(":composeApp:compileKotlinDesktop", ":composeApp:run")
if ($NoDaemon) {
    $argsList = @("--no-daemon") + $argsList
}

& $gradlew @argsList
if ($LASTEXITCODE -ne 0) {
    throw "gradlew desktop run failed (exit $LASTEXITCODE)"
}
