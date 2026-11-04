param([string]$A, [string]$B, [int]$Y0 = 250, [int]$Y1 = 950)
Add-Type -AssemblyName System.Drawing
function Get-Pixels([string]$p) {
    $bmp = New-Object System.Drawing.Bitmap($p)
    $rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $stride = $data.Stride
    $bytes = New-Object byte[] ($stride * $bmp.Height)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
    $bmp.UnlockBits($data)
    $bmp.Dispose()
    Write-Output ("{0}|{1}" -f $stride, [Convert]::ToBase64String($bytes))
}
$pa = Get-Pixels $A
$pb = Get-Pixels $B
$sa = [int]($pa.Split('|')[0]); $ba = [Convert]::FromBase64String($pa.Split('|')[1])
$sb = [int]($pb.Split('|')[0]); $bb = [Convert]::FromBase64String($pb.Split('|')[1])
$changed = 0; $total = 0
for ($y = $Y0; $y -lt $Y1; $y += 3) {
    for ($x = 100; $x -lt 1820; $x += 3) {
        $d = [Math]::Abs([int]$ba[$y * $sa + $x * 4 + 1] - [int]$bb[$y * $sb + $x * 4 + 1])
        $total++
        if ($d -gt 12) { $changed++ }
    }
}
"changed={0:P2} of {1} samples" -f ($changed / $total), $total
