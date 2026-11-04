param([string]$Path = 'D:\demo_player_workspace\ref\qiyi_01.png', [int]$Cols = 16, [int]$Rows = 10)
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($Path)
$w = $bmp.Width; $h = $bmp.Height
$rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $h)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data)
$bmp.Dispose()

"size=${w}x${h} stride=$stride"
"--- block average colors ($Cols x $Rows) ---"
for ($ry = 0; $ry -lt $Rows; $ry++) {
  $line = ''
  for ($cx = 0; $cx -lt $Cols; $cx++) {
    $x0 = [int]($cx * $w / $Cols); $x1 = [int](($cx + 1) * $w / $Cols)
    $y0 = [int]($ry * $h / $Rows); $y1 = [int](($ry + 1) * $h / $Rows)
    $sr = 0; $sg = 0; $sb = 0; $n = 0
    for ($y = $y0; $y -lt $y1; $y += 4) {
      $rowOff = $y * $stride
      for ($x = $x0; $x -lt $x1; $x += 4) {
        $o = $rowOff + $x * 4
        $sb += $bytes[$o]; $sg += $bytes[$o + 1]; $sr += $bytes[$o + 2]; $n++
      }
    }
    if ($n -gt 0) { $line += ('{0:X2}{1:X2}{2:X2} ' -f [int]($sr/$n), [int]($sg/$n), [int]($sb/$n)) }
  }
  "row$ry : $line"
}
"--- exact pixels ---"
function P($x, $y) { $o = $y * $stride + $x * 4; '{0},{1} #{2:X2}{3:X2}{4:X2}' -f $x, $y, $bytes[$o+2], $bytes[$o+1], $bytes[$o] }
P 10 10; P 300 170; P 100 500; P 960 1190; P 1870 60; P 130 175; P 520 175; P 208 300
