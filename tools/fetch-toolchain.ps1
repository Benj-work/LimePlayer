$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$tools = 'D:\demo_player_workspace\tools'
$dl = Join-Path $tools 'dl'
New-Item -ItemType Directory -Force -Path $dl | Out-Null

$items = @(
    @{ Name = 'jdk17.zip';     Url = 'https://aka.ms/download-jdk/microsoft-jdk-17.0.13-windows-x64.zip' },
    @{ Name = 'cmdline.zip';   Url = 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' },
    @{ Name = 'gradle.zip';    Url = 'https://services.gradle.org/distributions/gradle-8.7-bin.zip' }
)

foreach ($it in $items) {
    $dest = Join-Path $dl $it.Name
    if (Test-Path $dest) { "SKIP exists $($it.Name)"; continue }
    "DOWNLOAD $($it.Name) from $($it.Url)"
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-WebRequest -Uri $it.Url -OutFile $dest -UseBasicParsing -TimeoutSec 1800
    "DONE $($it.Name) $([math]::Round((Get-Item $dest).Length/1MB,1))MB in $([math]::Round($sw.Elapsed.TotalSeconds))s"
}

foreach ($pair in @(@('jdk17.zip','jdk'), @('cmdline.zip','sdk-cmdline'), @('gradle.zip','gradle'))) {
    $zip = Join-Path $dl $pair[0]
    $out = Join-Path $tools $pair[1]
    if (Test-Path $out) { "SKIP unzip $($pair[1])"; continue }
    New-Item -ItemType Directory -Force -Path $out | Out-Null
    Expand-Archive -Path $zip -DestinationPath $out -Force
    "UNZIPPED $($pair[1])"
}
"ALL DONE"
