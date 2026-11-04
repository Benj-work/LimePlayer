$ErrorActionPreference = 'Continue'
$env:JAVA_HOME='D:\demo_player_workspace\tools\jdk\jdk-17.0.13+11'
$env:ANDROID_HOME='D:\demo_player_workspace\tools\sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:ANDROID_USER_HOME='D:\demo_player_workspace\androidhome'
$env:TEMP='D:\demo_player_workspace\tmp'; $env:TMP=$env:TEMP
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

$gradle = 'D:\demo_player_workspace\tools\gradle\gradle-8.7\bin\gradle.bat'
& $gradle -p 'D:\demo_player_workspace\app' -g 'D:\demo_player_workspace\gh' assembleRelease --no-daemon --console=plain 2>&1 | Out-String -Width 300 | Write-Output

$apk = 'D:\demo_player_workspace\app\app\build\outputs\apk\release\app-release.apk'
if (Test-Path $apk) {
    $adb = 'D:\tools\adb\adb.exe'
    $dev = '10.139.95.4:5555'
    & $adb -s $dev install -r $apk 2>&1 | Write-Output
    Copy-Item $apk 'D:\demo_player_workspace\青柠播放器.apk' -Force
    & $adb -s $dev shell appops set com.lime.player MANAGE_EXTERNAL_STORAGE allow 2>&1 | Write-Output
    # 语音助手后台唤起播放器需要（Android 12 后台启动 Activity 的豁免条件）
    & $adb -s $dev shell appops set com.lime.player SYSTEM_ALERT_WINDOW allow 2>&1 | Write-Output
    & $adb -s $dev shell am force-stop com.lime.player
    & $adb -s $dev shell am start -W -n com.lime.player/.ui.LibraryActivity 2>&1 | Write-Output
}
