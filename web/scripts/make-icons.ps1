Add-Type -AssemblyName System.Drawing

$root = Split-Path $PSScriptRoot -Parent
$source = Join-Path (Split-Path $root -Parent) "app\src\main\res\drawable-nodpi\ic_launcher_foreground.png"
$out = Join-Path $root "public\icons"
New-Item -ItemType Directory -Force -Path $out | Out-Null
$fg = [System.Drawing.Image]::FromFile($source)

function Save-Icon([int]$size, [double]$scale, [string]$name) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::FromArgb(255, 42, 86, 246))
    $drawn = $size * $scale
    $offset = ($size - $drawn) / 2.0
    $g.DrawImage($fg, [single]$offset, [single]$offset, [single]$drawn, [single]$drawn)
    $g.Dispose()
    $bmp.Save((Join-Path $out $name), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

# The seal occupies 62% of the foreground; maskable keeps it inside the 80% safe zone.
Save-Icon 180 1.4 "apple-touch-icon.png"
Save-Icon 192 1.4 "icon-192.png"
Save-Icon 512 1.4 "icon-512.png"
Save-Icon 512 1.22 "icon-512-maskable.png"
Save-Icon 32 1.5 "favicon-32.png"
$fg.Dispose()
Write-Host "ICONS_OK"
