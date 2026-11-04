param(
    [Parameter(Mandatory = $true)][string]$Query,
    [Parameter(Mandatory = $true)][string]$Out,
    [int]$MinWidth = 1200,
    [double]$MinRatio = 1.4,
    [double]$MaxRatio = 2.7,
    [int]$MaxTry = 12
)
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.Drawing

$q = [uri]::EscapeDataString($Query)
$url = "https://www.bing.com/images/search?q=$q&qft=+filterui:aspect-wide&form=IRFLTR&first=1"
$html = (Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 25).Content
$urls = [regex]::Matches($html, 'murl&quot;:&quot;(.*?)&quot;') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
"candidates: $($urls.Count)"

$tried = 0
foreach ($u in $urls) {
    if ($tried -ge $MaxTry) { break }
    $tried++
    try {
        $tmp = Join-Path $env:TEMP ("bd_" + [guid]::NewGuid().ToString('N') + ".img")
        Invoke-WebRequest -Uri $u -OutFile $tmp -UseBasicParsing -TimeoutSec 20
        $bmp = New-Object System.Drawing.Bitmap($tmp)
        $w = $bmp.Width; $h = $bmp.Height
        $bmp.Dispose()
        $ratio = if ($h -gt 0) { $w / $h } else { 0 }
        if ($w -ge $MinWidth -and $ratio -ge $MinRatio -and $ratio -le $MaxRatio) {
            Move-Item $tmp $Out -Force
            "OK  {0}x{1} ratio={2:N2}  {3}" -f $w, $h, $ratio, $u
            exit 0
        }
        Remove-Item $tmp -Force -ErrorAction SilentlyContinue
    } catch { }
}
"FAIL after $tried tries"
exit 1
