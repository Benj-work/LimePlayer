param([string]$Path, [int[][]]$Points)
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($Path)
$rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $bmp.Height)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data); $bmp.Dispose()
foreach ($p in $Points) {
    $x = $p[0]; $y = $p[1]
    $o = $y * $stride + $x * 4
    '{0},{1} #{2:X2}{3:X2}{4:X2}' -f $x, $y, $bytes[$o+2], $bytes[$o+1], $bytes[$o]
}
