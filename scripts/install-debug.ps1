param([string]$Serial)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\android-env.ps1"
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskApk = Join-Path $taskRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $taskApk)) { throw 'Primero ejecutá scripts/build.ps1.' }
$taskAdbArgs = @()
if ($Serial) { $taskAdbArgs += @('-s', $Serial) }
& adb @taskAdbArgs get-state
if ($LASTEXITCODE -ne 0) { throw 'Conectá un teléfono y aceptá la depuración USB. Si hay varios, usá -Serial.' }
& adb @taskAdbArgs install -r $taskApk
if ($LASTEXITCODE -ne 0) { throw 'No se pudo instalar el APK.' }
& adb @taskAdbArgs shell am start -n ar.com.copiloto/.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'No se pudo abrir Copiloto.' }
