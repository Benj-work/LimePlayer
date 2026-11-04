$ErrorActionPreference = 'Stop'
$tools = 'D:\demo_player_workspace\tools'
$dl = "$tools\dl"
$sdk = "$tools\sdk"

# --- JDK ---
$jdkOut = "$tools\jdk"
if (-not (Test-Path "$jdkOut\jdk-17.0.13+11\bin\java.exe")) {
    New-Item -ItemType Directory -Force -Path $jdkOut | Out-Null
    Expand-Archive -Path "$dl\jdk17.zip" -DestinationPath $jdkOut -Force
}
$javaHome = Get-ChildItem $jdkOut -Directory | Where-Object { $_.Name -like 'jdk-17*' } | Select-Object -First 1
"JAVA_HOME=$($javaHome.FullName)"

# --- cmdline-tools ---
New-Item -ItemType Directory -Force -Path "$sdk\cmdline-tools" | Out-Null
if (-not (Test-Path "$sdk\cmdline-tools\latest\bin\sdkmanager.bat")) {
    $tmp = "$tools\dl\cmdtmp"
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path "$dl\cmdline.zip" -DestinationPath $tmp -Force
    Move-Item "$tmp\cmdline-tools" "$sdk\cmdline-tools\latest" -Force
    Remove-Item $tmp -Recurse -Force
}
"SDK=$sdk"

$env:JAVA_HOME = $javaHome.FullName
$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$env:Path = "$($javaHome.FullName)\bin;$sdk\cmdline-tools\latest\bin;$env:Path"

& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --version

# accept licenses
$y = ("y`n" * 60)
$y | & "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses --sdk_root="$sdk" 2>&1 | Select-Object -Last 5

& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="$sdk" "platforms;android-34" "build-tools;34.0.0" "platform-tools" 2>&1 | Select-Object -Last 15
"SDK INSTALL DONE"
& "$javaHome.FullName\bin\java.exe" -version 2>&1
