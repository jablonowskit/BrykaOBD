#Requires -Version 5.1
<#
.SYNOPSIS
  Wait for CI for current HEAD, download BrykaOBD APK, install on phone.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts/wgraj-na-telefon.ps1
  powershell -ExecutionPolicy Bypass -File scripts/wgraj-na-telefon.ps1 -AllowUninstall
#>
[CmdletBinding()]
param(
    [string] $Device,
    [switch] $AllowUninstall,
    [switch] $NoLaunch,
    [switch] $SkipGitCheck,
    [int] $WaitSeconds = 900
)

$ErrorActionPreference = "Stop"
$Package = "app.brykaobd"
$Activity = "app.brykaobd/app.brykaobd.android.MainActivity"
$ArtifactName = "brykaobd-android-debug-apk"
$WorkflowName = "BrykaOBD CI"
$Root = Split-Path -Parent $PSScriptRoot
$ArtifactsDir = Join-Path $Root "artifacts"
$ApkPath = Join-Path $ArtifactsDir "androidApp-debug.apk"

function Write-Step([string] $Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Get-AdbPath {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($adb) { return $adb.Source }
    $candidates = @(
        (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"),
        (Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe")
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    throw "adb not found. Install platform-tools or add adb to PATH."
}

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]] $AdbArgs)
    $all = @()
    if ($Device) { $all += @("-s", $Device) }
    $all += $AdbArgs
    & $script:AdbExe @all
    if ($LASTEXITCODE -ne 0) {
        throw ("adb failed: " + ($AdbArgs -join " ") + " (exit $LASTEXITCODE)")
    }
}

function Ensure-DeviceOnline {
    $out = & $script:AdbExe devices
    $online = @()
    $unauthorized = $false
    foreach ($line in ($out -split "`n")) {
        $t = $line.Trim()
        if (-not $t -or $t.StartsWith("List")) { continue }
        if ($t -match "^(\S+)\s+device$") { $online += $Matches[1] }
        if ($t -match "unauthorized") { $unauthorized = $true }
    }
    if ($online.Count -eq 0) {
        if ($unauthorized) {
            throw "Phone unauthorized - unlock screen and accept USB debugging."
        }
        throw "No adb device in 'device' state."
    }
    if ($Device) {
        if ($online -notcontains $Device) {
            throw "Device $Device not online. Available: $($online -join ', ')"
        }
        return
    }
    if ($online.Count -gt 1) {
        throw "Multiple devices: $($online -join ', '). Pass -Device <serial>."
    }
    $script:Device = $online[0]
    Write-Host "Device: $Device"
}

function Ensure-GitPushed {
    if ($SkipGitCheck) {
        Write-Host "Skipped git check (-SkipGitCheck)."
        return
    }
    Push-Location $Root
    try {
        $status = git status --porcelain
        if ($status) {
            throw "Working tree dirty - CI would build OLD origin code. Commit+push first, or -SkipGitCheck.`n$status"
        }
        git fetch origin main --quiet 2>$null
        $ahead = git rev-list --count "origin/main..HEAD" 2>$null
        if ($ahead -and [int]$ahead -gt 0) {
            Write-Step "Pushing local commits to origin"
            git push origin HEAD
        }
        $behind = git rev-list --count "HEAD..origin/main" 2>$null
        if ($behind -and [int]$behind -gt 0) {
            throw "HEAD is behind origin/main by $behind commits. Pull/rebase first."
        }
    }
    finally {
        Pop-Location
    }
}

function Get-HeadSha {
    Push-Location $Root
    try { return (git rev-parse HEAD).Trim() }
    finally { Pop-Location }
}

function Find-CiRun([string] $Sha) {
    $json = gh run list --commit $Sha --limit 10 --json databaseId,status,conclusion,workflowName,url,createdAt,headSha |
        ConvertFrom-Json
    if (-not $json) { return $null }
    $match = $json | Where-Object { $_.workflowName -eq $WorkflowName } | Select-Object -First 1
    if (-not $match) { $match = $json | Select-Object -First 1 }
    return $match
}

function Wait-ForCi([string] $Sha) {
    Write-Step "Waiting for CI commit $Sha"
    $deadline = (Get-Date).AddSeconds($WaitSeconds)
    $run = $null
    while ((Get-Date) -lt $deadline) {
        $run = Find-CiRun $Sha
        if ($run) { break }
        Write-Host "No run yet - sleep 10s..."
        Start-Sleep -Seconds 10
    }
    if (-not $run) {
        throw "No GitHub Actions run for $Sha within ${WaitSeconds}s."
    }
    $runId = [string]$run.databaseId
    Write-Host "Run: $($run.url)"
    if ($run.status -ne "completed") {
        Write-Step "gh run watch $runId"
        gh run watch $runId --exit-status
        if ($LASTEXITCODE -ne 0) {
            throw "CI run $runId did not succeed."
        }
    }
    else {
        if ($run.conclusion -ne "success") {
            throw "CI run $runId conclusion=$($run.conclusion)"
        }
        Write-Host "CI already success."
    }
    return $runId
}

function Download-Apk([string] $RunId) {
    Write-Step "Download artifact $ArtifactName"
    New-Item -ItemType Directory -Force -Path $ArtifactsDir | Out-Null
    Get-ChildItem $ArtifactsDir -Filter "*.apk" -ErrorAction SilentlyContinue | Remove-Item -Force
    gh run download $RunId -n $ArtifactName -D $ArtifactsDir
    if (-not (Test-Path $ApkPath)) {
        $found = Get-ChildItem $ArtifactsDir -Recurse -Filter "*.apk" | Select-Object -First 1
        if (-not $found) { throw "No .apk under artifacts/ after download." }
        Copy-Item $found.FullName $ApkPath -Force
    }
    $len = (Get-Item $ApkPath).Length
    if ($len -lt 1MB) {
        throw "APK suspiciously small ($len bytes): $ApkPath"
    }
    Write-Host ("APK: {0} ({1} MB)" -f $ApkPath, [math]::Round($len / 1MB, 2))
}

function Install-Apk {
    Write-Step "adb install -r"
    $adbArgs = @()
    if ($Device) { $adbArgs += @("-s", $Device) }
    $adbArgs += @("install", "-r", $ApkPath)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $installOut = & $script:AdbExe @adbArgs 2>&1 | ForEach-Object { "$_" } | Out-String
    $code = $LASTEXITCODE
    $ErrorActionPreference = $prev
    Write-Host $installOut
    if ($installOut -match "INSTALL_FAILED_UPDATE_INCOMPATIBLE|signatures do not match") {
        if (-not $AllowUninstall) {
            throw @"
Signature mismatch. Re-run with -AllowUninstall (DELETES app data including diag logs), or:
  adb uninstall $Package
  adb install $ApkPath
"@
        }
        Write-Host "WARNING: uninstall $Package (deletes diag logs)" -ForegroundColor Yellow
        Invoke-Adb uninstall $Package
        Invoke-Adb install $ApkPath
        return
    }
    if ($code -ne 0 -and $installOut -notmatch "Success") {
        throw "adb install failed."
    }
}

function Assert-FreshInstall {
    Write-Step "Verify lastUpdateTime"
    $adbArgs = @()
    if ($Device) { $adbArgs += @("-s", $Device) }
    $adbArgs += @("shell", "dumpsys", "package", $Package)
    $dump = & $script:AdbExe @adbArgs | Out-String
    $vn = [regex]::Match($dump, "versionName=(\S+)").Groups[1].Value
    $lu = [regex]::Match($dump, "lastUpdateTime=(.+)").Groups[1].Value.Trim()
    if (-not $lu) {
        throw "lastUpdateTime missing from dumpsys - package may not be installed."
    }
    Write-Host "versionName=$vn"
    Write-Host "lastUpdateTime=$lu"
    Write-Host ("now={0}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"))
    $parsed = $null
    $formats = @("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.fffffff", "MMM dd, yyyy HH:mm:ss")
    foreach ($f in $formats) {
        try {
            $parsed = [DateTime]::ParseExact($lu, $f, [Globalization.CultureInfo]::InvariantCulture)
            break
        }
        catch { }
    }
    if (-not $parsed) {
        try { $parsed = [DateTime]::Parse($lu) } catch { }
    }
    if (-not $parsed) {
        Write-Host "WARNING: could not parse lastUpdateTime - verify manually." -ForegroundColor Yellow
        return
    }
    $age = (Get-Date) - $parsed
    if ($age.TotalSeconds -gt 180 -or $age.TotalSeconds -lt -120) {
        throw ("lastUpdateTime not fresh (age {0}s). Do NOT report success." -f [int]$age.TotalSeconds)
    }
    Write-Host ("OK: fresh install ({0}s ago)." -f [int]$age.TotalSeconds) -ForegroundColor Green
}

function Start-App {
    if ($NoLaunch) { return }
    Write-Step "Launch app"
    Invoke-Adb shell am force-stop $Package
    Invoke-Adb shell am start -n $Activity
}

Set-Location $Root
Write-Step "BrykaOBD -> phone"
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "gh CLI missing (winget install GitHub.cli)."
}
$script:AdbExe = Get-AdbPath
Ensure-GitPushed
Ensure-DeviceOnline
$sha = Get-HeadSha
Write-Host "HEAD: $sha"
$runId = Wait-ForCi $sha
Download-Apk $runId
Install-Apk
Assert-FreshInstall
Start-App
Write-Host ""
Write-Host "Done: $Package on $Device (CI run $runId)." -ForegroundColor Green
