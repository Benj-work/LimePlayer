param(
    [Parameter(Mandatory = $true)][string]$Path,
    [int]$X0 = 0, [int]$Y0 = 0, [int]$X1 = 1920, [int]$Y1 = 1200,
    [string]$Mode = 'bright'
)
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($Path)
$rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $bmp.Height)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data); $bmp.Dispose()

$rows = @()
for ($y = $Y0; $y -lt $Y1; $y++) {
    $c = 0
    $ro = $y * $stride
    for ($x = $X0; $x -lt $X1; $x++) {
        $o = $ro + $x * 4
        $b = [int]$bytes[$o]; $g = [int]$bytes[$o + 1]; $r = [int]$bytes[$o + 2]
        if ($Mode -eq 'green') {
            if ($g -gt 120 -and $g -gt ($r + 40) -and $g -gt ($b + 40)) { $c++ }
        } else {
            if (($r + $g + $b) -gt 480) { $c++ }
        }
    }
    if ($c -gt 0) { $rows += "$y=$c" }
}
if ($rows.Count -eq 0) { 'none' } else { $rows -join ' ' }
