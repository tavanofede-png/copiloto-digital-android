$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskJdk = Get-ChildItem -LiteralPath "$taskRoot\.tools\jdk" -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
if ($taskJdk) { $env:JAVA_HOME = $taskJdk.FullName }
if (-not $env:JAVA_HOME) { throw 'Falta JDK. Ejecutá scripts/setup-android.ps1 o configurá JAVA_HOME.' }
$env:ANDROID_HOME = "$taskRoot\.tools\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = "$taskRoot\.tools\android-user"
$env:GRADLE_USER_HOME = "$taskRoot\.cache\gradle"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
