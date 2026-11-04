param([string]$Path, [int]$Y0 = 0, [int]$Y1 = 120)
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($Path)
$rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $bmp.Height)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data); $bmp.Dispose()

$w = 1920; $h = 1200
if ($Y0 -lt 0) { $Y0 = 0 }
if ($Y1 -gt $h) { $Y1 = $h }
$bright = 0; $green = 0; $total = 0
$minX = 99999; $maxX = -1; $minY = 99999; $maxY = -1
for ($y = $Y0; $y -lt $Y1; $y++) {
    $ro = $y * $stride
    for ($x = 0; $x -lt $w; $x++) {
        $o = $ro + $x * 4
        $b = $bytes[$o]; $g = $bytes[$o + 1]; $r = $bytes[$o + 2]
        $total++
        if (($r + $g + $b) -gt 480) {
            $bright++
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
        if ($g -gt 140 -and $g -gt ($r + 50) -and $g -gt ($b + 50)) { $green++ }
    }
}
"band y=$Y0..$Y1 bright=$bright green=$green bbox=($minX,$minY)-($maxX,$maxY)"
