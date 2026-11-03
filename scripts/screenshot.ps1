<#
.SYNOPSIS
  Builds, installs and captures one known demo state of the glass panel.

.DESCRIPTION
  The screenshot regression loop in one command: build -> install -> launch with the
  launcher extras that pin the state -> screencap. No UI needs to be tapped.

.EXAMPLE
  powershell -File scripts/screenshot.ps1 -State 70lens

.EXAMPLE
  powershell -File scripts/screenshot.ps1 -State 70opaque -SkipBuild -OutDir artifacts/shots
#>
param(
    [ValidateSet('30lens', '70lens', '70dispersion', '70blur', '70opaque')]
    [string]$State = '70lens',
    [string]$OutDir = 'artifacts/shots',
    [switch]$SkipBuild,
    [int]$SettleSeconds = 8,
    [string]$Device = ''
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
$app = Join-Path $repo 'app'
$apk = Join-Path $app 'app\build\outputs\apk\debug\app-debug.apk'

# panel fraction / variant / engine per capture, mirroring docs/manual-acceptance-glass.md
$states = @{
    '30lens'       = @('--es', 'panel', '0.3', '--es', 'variant', 'lens')
    '70lens'       = @('--es', 'panel', '0.7', '--es', 'variant', 'lens')
    '70dispersion' = @('--es', 'panel', '0.7', '--es', 'variant', 'dispersion')
    '70blur'       = @('--es', 'panel', '0.7', '--es', 'engine', 'blur')
    '70opaque'     = @('--es', 'panel', '0.7', '--es', 'engine', 'opaque')
}

if (-not $SkipBuild) {
    if (-not $env:JAVA_HOME) {
        $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
    }
    Push-Location $app
    try {
        & .\gradlew assembleDebug --console=plain
        if ($LASTEXITCODE -ne 0) { throw "gradlew assembleDebug failed ($LASTEXITCODE)" }
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path $apk)) { throw "APK not found: $apk (run without -SkipBuild)" }

$adbArgs = @()
if ($Device) { $adbArgs += @('-s', $Device) }

& adb @adbArgs install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'adb install failed' }

& adb @adbArgs shell am force-stop com.smter.adv
Start-Sleep -Milliseconds 600
& adb @adbArgs shell am start -n com.smter.adv/.MainActivity @($states[$State])

# The first frame is slow: the backdrop is captured per frame, so give it time.
Start-Sleep -Seconds $SettleSeconds

New-Item -ItemType Directory -Force $OutDir | Out-Null
$target = Join-Path $OutDir "$State.png"
# Capture on the device and pull it: redirecting 'adb exec-out' through '>' corrupts the PNG in
# Windows PowerShell 5.1, which re-encodes the stream as text.
& adb @adbArgs shell screencap -p /sdcard/adv-shot.png
& adb @adbArgs pull /sdcard/adv-shot.png $target | Out-Null
& adb @adbArgs shell rm -f /sdcard/adv-shot.png
if ($LASTEXITCODE -ne 0) { throw 'screencap failed' }

Write-Output ("captured {0} -> {1}" -f $State, (Resolve-Path $target))
