Add-Type -AssemblyName System.Drawing

function Add-Point($list, [double]$cx, [double]$cy, [double]$deg, [double]$radius) {
    $rad = $deg * [Math]::PI / 180.0
    $list.Add([System.Drawing.PointF]::new(
        [single]($cx + [Math]::Cos($rad) * $radius),
        [single]($cy + [Math]::Sin($rad) * $radius)
    ))
}

function New-Seal([int]$size, [bool]$transparent) {
    $bmp = New-Object System.Drawing.Bitmap $size, $size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    if ($transparent) {
        $g.Clear([System.Drawing.Color]::Transparent)
    } else {
        $g.Clear([System.Drawing.Color]::FromArgb(255, 42, 86, 246))
    }

    $white = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::White, [single]($size * 0.011))
    $cx = $size / 2.0
    $cy = $size / 2.0
    $outer = $size * 0.31
    $g.DrawEllipse($pen, [single]($cx - $outer), [single]($cy - $outer), [single]($outer * 2), [single]($outer * 2))

    $codes = @(
        0x0411,0x0420,0x042F,0x041D,0x0421,0x041A,0x0418,0x0419,0x20,
        0x0413,0x041E,0x0421,0x0423,0x0414,0x0410,0x0420,0x0421,0x0422,0x0412,0x0415,0x041D,0x041D,0x042B,0x0419,0x20,
        0x00B7,0x20,
        0x0422,0x0415,0x0425,0x041D,0x0418,0x0427,0x0415,0x0421,0x041A,0x0418,0x0419,0x20,
        0x0423,0x041D,0x0418,0x0412,0x0415,0x0420,0x0421,0x0418,0x0422,0x0415,0x0422,0x20,
        0x00B7,0x20
    )
    $legend = -join ($codes | ForEach-Object { [char]$_ })
    $font = New-Object System.Drawing.Font("Segoe UI", [single]($size * 0.026), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $textR = $size * 0.268
    $step = 360.0 / $legend.Length
    $start = -90.0 - (($legend.Length - 1) * $step / 2.0)
    for ($i = 0; $i -lt $legend.Length; $i++) {
        $ch = [string]$legend[$i]
        if ($ch -eq " ") { continue }
        $ang = $start + ($i * $step)
        $rad = $ang * [Math]::PI / 180.0
        $x = $cx + [Math]::Cos($rad) * $textR
        $y = $cy + [Math]::Sin($rad) * $textR
        $state = $g.Save()
        $g.TranslateTransform([single]$x, [single]$y)
        $g.RotateTransform([single]($ang + 90.0))
        $sizeF = $g.MeasureString($ch, $font)
        $g.DrawString($ch, $font, $white, [single](-$sizeF.Width / 2.0), [single](-$sizeF.Height * 0.78))
        $g.Restore($state)
    }

    $points = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'
    $teeth = 12
    $tip = $size * 0.188
    $root = $size * 0.152
    for ($i = 0; $i -lt $teeth; $i++) {
        $mid = -90.0 + ($i * 360.0 / $teeth)
        Add-Point $points $cx $cy ($mid - 11.2) $root
        Add-Point $points $cx $cy ($mid - 6.4) $tip
        Add-Point $points $cx $cy ($mid + 6.4) $tip
        Add-Point $points $cx $cy ($mid + 11.2) $root
    }
    $g.FillPolygon($white, $points.ToArray())

    $hole = $size * 0.108
    $blue = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 42, 86, 246))
    $g.FillEllipse($blue, [single]($cx - $hole), [single]($cy - $hole), [single]($hole * 2), [single]($hole * 2))

    $titleFont = New-Object System.Drawing.Font("Segoe UI", [single]($size * 0.058), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $title = -join (@(0x0411,0x0413,0x0422,0x0423) | ForEach-Object { [char]$_ })
    $ts = $g.MeasureString($title, $titleFont)
    $g.DrawString($title, $titleFont, $white, [single]($cx - $ts.Width / 2.0), [single]($cy - $ts.Height / 2.0))

    $g.Dispose()
    $pen.Dispose()
    $white.Dispose()
    $blue.Dispose()
    $font.Dispose()
    $titleFont.Dispose()
    return $bmp
}

$root = Split-Path $PSScriptRoot -Parent
$nodpi = Join-Path $root "app\src\main\res\drawable-nodpi"
New-Item -ItemType Directory -Force -Path $nodpi | Out-Null
$fg = New-Seal 1024 $true
$fg.Save((Join-Path $nodpi "ic_launcher_foreground.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$fg.Dispose()
$preview = New-Seal 1024 $false
$previewDir = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $previewDir | Out-Null
$preview.Save((Join-Path $previewDir "icon-preview.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$preview.Dispose()
Write-Host "ICON_OK"
