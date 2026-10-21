param([string[]]$Tasks = @('assembleDebug'))
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\android-env.ps1"
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & .\gradlew.bat @Tasks
    if ($LASTEXITCODE -ne 0) { throw "Gradle terminó con código $LASTEXITCODE" }
} finally {
    Pop-Location
}
