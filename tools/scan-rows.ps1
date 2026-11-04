param([string]$Path = 'D:\demo_player_workspace\ref\ctrl.png', [int[]]$Rows = @(900,920,935,945,950,955,965,975,985,1000,1070,1090,1110,1150))
Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap($Path)
$w = $bmp.Width; $h = $bmp.Height
$rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $h)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data); $bmp.Dispose()

foreach ($y in $Rows) {
  $ro = $y * $stride
  $segs = @()
  $inSeg = $false; $start = 0; $lastX = 0
  for ($x = 0; $x -lt $w; $x++) {
    $o = $ro + $x * 4
    $b = $bytes[$o]; $g = $bytes[$o+1]; $r = $bytes[$o+2]
    $lum = 0.299*$r + 0.587*$g + 0.114*$b
    if ($lum -gt 22) {
      if (-not $inSeg) { $inSeg = $true; $start = $x }
      $lastX = $x
    } elseif ($inSeg) {
      if ($lastX - $start -ge 6) { $segs += ('{0}-{1}' -f $start, $lastX) }
      $inSeg = $false
    }
  }
  if ($inSeg -and $lastX - $start -ge 6) { $segs += ('{0}-{1}' -f $start, $lastX) }
  "y=$y segments: " + ($segs -join ', ')
}
