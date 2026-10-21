param([switch]$AcceptLicenses)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskTools = Join-Path $taskRoot '.tools'
$taskDownloads = Join-Path $taskRoot '.cache\downloads'
New-Item -ItemType Directory -Force -Path $taskTools, $taskDownloads | Out-Null

function Get-ToolArchive($Url, $Name, $ExpectedHash) {
    $taskArchive = Join-Path $taskDownloads $Name
    if (Test-Path -LiteralPath $taskArchive) {
        try {
            $taskExistingZip = [System.IO.Compression.ZipFile]::OpenRead($taskArchive)
            $taskExistingZip.Dispose()
        } catch {
            Move-Item -LiteralPath $taskArchive -Destination "$taskArchive.partial" -Force
        }
    }
    if (-not (Test-Path -LiteralPath $taskArchive)) {
        & curl.exe --fail --location --retry 3 --retry-all-errors --connect-timeout 30 --max-time 600 --continue-at - --silent --show-error $Url --output "$taskArchive.partial"
        if ($LASTEXITCODE -ne 0) { throw "Falló descarga de $Name" }
        Move-Item -LiteralPath "$taskArchive.partial" -Destination $taskArchive -Force
    }
    if ($ExpectedHash -and (Get-FileHash -LiteralPath $taskArchive -Algorithm SHA256).Hash -ne $ExpectedHash) {
        throw "Checksum SHA256 inválido para $Name"
    }
    return $taskArchive
}

$taskInstalledJdk = Get-ChildItem -LiteralPath "$taskTools\jdk" -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
if (-not $taskInstalledJdk) {
    Write-Output 'Descargando JDK portable...'
    $taskZip = Get-ToolArchive 'https://aka.ms/download-jdk/microsoft-jdk-21.0.12.1-windows-x64.zip' 'jdk.zip' '192441a9d27da813bada974bb88b4cf64d37a9589ed37f204374d411ca5ce07f'
    Expand-Archive -LiteralPath $taskZip -DestinationPath "$taskTools\jdk" -Force
}
if (-not (Test-Path -LiteralPath "$taskTools\android-sdk\cmdline-tools\latest\bin\sdkmanager.bat")) {
    Write-Output 'Descargando herramientas oficiales de Android...'
    $taskZip = Get-ToolArchive 'https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip' 'android-commandline.zip' '90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a'
    Expand-Archive -LiteralPath $taskZip -DestinationPath "$taskTools\android-extract" -Force
    New-Item -ItemType Directory -Force -Path "$taskTools\android-sdk\cmdline-tools" | Out-Null
    Move-Item -LiteralPath "$taskTools\android-extract\cmdline-tools" -Destination "$taskTools\android-sdk\cmdline-tools\latest"
}
. "$PSScriptRoot\android-env.ps1"
Write-Output 'Instalando SDK 36, Build Tools 35 y ADB. Se solicitarán las licencias oficiales de Google.'
if ($AcceptLicenses) {
    1..30 | ForEach-Object { 'y' } | & "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "--sdk_root=$env:ANDROID_HOME" --licenses
} else {
    & "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "--sdk_root=$env:ANDROID_HOME" --licenses
}
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron aceptar las licencias del SDK.' }
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "--sdk_root=$env:ANDROID_HOME" 'platform-tools' 'platforms;android-36' 'build-tools;35.0.0'
if ($LASTEXITCODE -ne 0) { throw 'No se pudo instalar el SDK.' }
Write-Output 'Herramientas listas. Ejecutá scripts/build.ps1.'
