param(
    [Parameter(Mandatory = $true)][string]$Path,
    [int]$Y = 60,
    [Parameter(Mandatory = $true)][int[]]$XRange
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Bitmap]::FromFile($Path)
$x0 = $XRange[0]; $x1 = $XRange[1]
$prev = ''
for ($x = $x0; $x -le $x1; $x++) {
    $p = $bmp.GetPixel($x, $Y)
    $hex = '#{0:X2}{1:X2}{2:X2}' -f $p.R, $p.G, $p.B
    if ($hex -ne $prev) { "x=$x $hex"; $prev = $hex }
}
$bmp.Dispose()
