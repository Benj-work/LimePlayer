$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$sdk = 'D:\demo_player_workspace\tools\sdk'
$dl  = 'D:\demo_player_workspace\tools\dl'
New-Item -ItemType Directory -Force -Path $dl, "$sdk\platforms", "$sdk\build-tools" | Out-Null

function Get-SdkZip([string]$url, [string]$file) {
    $dest = Join-Path $dl $file
    if (-not (Test-Path $dest)) {
        Write-Host "GET $url"
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing -TimeoutSec 900
        Write-Host ("  {0}MB in {1}s" -f [math]::Round((Get-Item $dest).Length/1MB,1), [math]::Round($sw.Elapsed.TotalSeconds))
    } else {
        Write-Host "CACHED $file"
    }
    return $dest
}

if (-not (Test-Path "$sdk\platforms\android-34\android.jar")) {
    $z = Get-SdkZip 'https://dl.google.com/android/repository/platform-34-ext7_r03.zip' 'platform-34.zip'
    $tmp = "$dl\t_platform"
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path "$z" -DestinationPath $tmp -Force
    Move-Item "$tmp\android-34" "$sdk\platforms\android-34" -Force
    Remove-Item $tmp -Recurse -Force
    Write-Host "platform-34 installed"
}

if (-not (Test-Path "$sdk\build-tools\34.0.0\aapt2.exe")) {
    $z = Get-SdkZip 'https://dl.google.com/android/repository/build-tools_r34-windows.zip' 'build-tools-34.zip'
    $tmp = "$dl\t_bt"
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path "$z" -DestinationPath $tmp -Force
    $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
    Move-Item $inner.FullName "$sdk\build-tools\34.0.0" -Force
    Remove-Item $tmp -Recurse -Force
    Write-Host "build-tools installed"
}

if (-not (Test-Path "$sdk\platform-tools\adb.exe")) {
    $z = Get-SdkZip 'https://dl.google.com/android/repository/platform-tools_r37.0.1-win.zip' 'platform-tools.zip'
    $tmp = "$dl\t_pt"
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path "$z" -DestinationPath $tmp -Force
    Move-Item "$tmp\platform-tools" "$sdk\platform-tools" -Force
    Remove-Item $tmp -Recurse -Force
    Write-Host "platform-tools installed"
}

New-Item -ItemType Directory -Force -Path "$sdk\licenses" | Out-Null
$lic = "24333f8a63b6825ea9c5514f83c2829b004d1fee`nd975f751698a77b662befdbca50b8a6f`n"
[System.IO.File]::WriteAllText("$sdk\licenses\android-sdk-license", $lic)
[System.IO.File]::WriteAllText("$sdk\licenses\android-sdk-preview-license", "84831b9409646a918e30573bab4c9c91346d8abd")

Write-Host "--- verify ---"
foreach ($p in @("$sdk\platforms\android-34\android.jar", "$sdk\build-tools\34.0.0\aapt2.exe", "$sdk\platform-tools\adb.exe")) {
    Write-Host ("{0} {1}" -f (Test-Path $p), $p)
}
Get-ChildItem "$sdk\build-tools\34.0.0" -Filter '*.exe' | Select-Object -ExpandProperty Name
Write-Host "SDK READY"
