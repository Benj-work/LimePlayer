param(
    [Parameter(Mandatory = $true)][string]$Path,
    [int]$X0 = 0, [int]$Y0 = 0, [int]$X1 = 1920, [int]$Y1 = 1200,
    [int]$Threshold = 480
)
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($Path)
$rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $bmp.Height)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data); $bmp.Dispose()

$minX = 99999; $maxX = -1; $minY = 99999; $maxY = -1; $count = 0
for ($y = $Y0; $y -lt $Y1; $y++) {
    $ro = $y * $stride
    for ($x = $X0; $x -lt $X1; $x++) {
        $o = $ro + $x * 4
        $sum = [int]$bytes[$o] + [int]$bytes[$o + 1] + [int]$bytes[$o + 2]
        if ($sum -gt $Threshold) {
            $count++
            if ($x -lt $minX) { $minX = $x }
            if ($x -gt $maxX) { $maxX = $x }
            if ($y -lt $minY) { $minY = $y }
            if ($y -gt $maxY) { $maxY = $y }
        }
    }
}
if ($count -eq 0) { "empty" } else {
    "ink [{0},{1}]-[{2},{3}] w={4} h={5} px={6}" -f $minX, $minY, $maxX, $maxY, ($maxX - $minX + 1), ($maxY - $minY + 1), $count
}
