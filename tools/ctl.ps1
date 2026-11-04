param([string]$Out = 'D:\demo_player_workspace\ref\_ctl.png')
$adb = 'D:\tools\adb\adb.exe'
$dev = '10.139.95.4:5555'
& $adb -s $dev shell screencap -p /sdcard/_c.png | Out-Null
& $adb -s $dev pull /sdcard/_c.png $Out 2>&1 | Out-Null
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($Out)
$rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $bmp.Height)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data); $bmp.Dispose()
$bright = 0
for ($y = 20; $y -lt 90; $y++) {
    $ro = $y * $stride
    for ($x = 100; $x -lt 900; $x++) {
        $o = $ro + $x * 4
        if (([int]$bytes[$o] + [int]$bytes[$o + 1] + [int]$bytes[$o + 2]) -gt 480) { $bright++ }
    }
}
if ($bright -gt 200) { 'VISIBLE' } else { "HIDDEN($bright)" }
