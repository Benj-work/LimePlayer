param(
    [Parameter(Mandatory = $true)][string]$Path,
    [int]$X0 = 0, [int]$Y0 = 0, [int]$X1 = -1, [int]$Y1 = -1, [int]$Step = 3
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Bitmap]::FromFile($Path)
if ($X1 -lt 0) { $X1 = $bmp.Width }
if ($Y1 -lt 0) { $Y1 = $bmp.Height }

$sum = 0.0; $n = 0; $sumC = 0.0
for ($y = $Y0; $y -lt $Y1 - $Step; $y += $Step) {
    for ($x = $X0; $x -lt $X1 - $Step; $x += $Step) {
        $p = $bmp.GetPixel($x, $y)
        $q = $bmp.GetPixel($x + $Step, $y)
        $d = [math]::Abs($p.R - $q.R) + [math]::Abs($p.G - $q.G) + [math]::Abs($p.B - $q.B)
        $sum += $d; $sumC += ($p.R + $p.G + $p.B) / 3.0; $n++
    }
}
$bmp.Dispose()
"sharpness={0:N2}  avgLuma={1:N1}  samples={2}" -f ($sum / $n), ($sumC / $n), $n
