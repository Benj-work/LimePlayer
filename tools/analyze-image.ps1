param([string]$Path, [string]$Tag = 'img')
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($Path)
$w = $bmp.Width; $h = $bmp.Height
$rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $h)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data); $bmp.Dispose()

function At($x, $y) { $o = $y * $stride + $x * 4; return @($bytes[$o+2], $bytes[$o+1], $bytes[$o]) }

"== $Tag ${w}x${h} =="
# 1. rows average luminance (every 20px) to find overlay bands
"--- row luminance (y:avg) ---"
$line = ''
for ($y = 0; $y -lt $h; $y += 25) {
  $s = 0; $n = 0
  for ($x = 0; $x -lt $w; $x += 20) { $c = At $x $y; $s += ($c[0]*0.299 + $c[1]*0.587 + $c[2]*0.114); $n++ }
  $line += ('{0}:{1} ' -f $y, [int]($s/$n))
}
$line

# 2. green accent pixels (iQIYI green ~ #00BE06)
$gcount = 0; $minX = 99999; $maxX = -1; $minY = 99999; $maxY = -1
$samples = @{}
for ($y = 0; $y -lt $h; $y += 2) {
  $ro = $y * $stride
  for ($x = 0; $x -lt $w; $x += 2) {
    $o = $ro + $x * 4
    $b = $bytes[$o]; $g = $bytes[$o+1]; $r = $bytes[$o+2]
    if ($g -gt 140 -and $g -gt ($r + 60) -and $g -gt ($b + 60)) {
      $gcount++
      if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
      if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
      $samples['#{0:X2}{1:X2}{2:X2}' -f $r,$g,$b] = 1 + $samples['#{0:X2}{1:X2}{2:X2}' -f $r,$g,$b]
    }
  }
}
"GREEN pixels: $gcount bbox=($minX,$minY)-($maxX,$maxY)"
$samples.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 6 | ForEach-Object { "  $($_.Key) x$($_.Value)" }

# 3. bottom band detail: average per 20-row slice in bottom 300px, plus per-column variation
"--- bottom 300px row slices ---"
for ($y = $h - 300; $y -lt $h; $y += 25) {
  $sr=0;$sg=0;$sb=0;$n=0
  for ($x = 20; $x -lt $w-20; $x += 10) { $c = At $x $y; $sr+=$c[0];$sg+=$c[1];$sb+=$c[2];$n++ }
  "y=$y avg #{0:X2}{1:X2}{2:X2}" -f [int]($sr/$n),[int]($sg/$n),[int]($sb/$n)
}
