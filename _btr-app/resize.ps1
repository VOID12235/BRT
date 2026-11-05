Add-Type -AssemblyName System.Drawing

$logo = [System.Drawing.Image]::FromFile('D:\Users\a3614\Downloads\btr-logo.png')
$cover = [System.Drawing.Image]::FromFile('D:\Users\a3614\Downloads\btr-app-cover.png')

function Save-Scaled($src, $w, $h, $out, $jpg) {
  $bmp = New-Object System.Drawing.Bitmap($w, $h)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $g.DrawImage($src, 0, 0, $w, $h)
  $g.Dispose()
  $dir = Split-Path $out
  if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
  if ($jpg) { $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Jpeg) }
  else { $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png) }
  $bmp.Dispose()
  Write-Host "saved $out ($w x $h)"
}

$base = 'D:\Users\a3614\Documents\默认项目\_btr-app\app\src\main\res'

Save-Scaled $logo 48 48 "$base\mipmap-mdpi\ic_launcher.png" $false
Save-Scaled $logo 72 72 "$base\mipmap-hdpi\ic_launcher.png" $false
Save-Scaled $logo 96 96 "$base\mipmap-xhdpi\ic_launcher.png" $false
Save-Scaled $logo 144 144 "$base\mipmap-xxhdpi\ic_launcher.png" $false
Save-Scaled $logo 192 192 "$base\mipmap-xxxhdpi\ic_launcher.png" $false

Save-Scaled $cover 720 1280 "$base\drawable\splash_img.jpg" $true

Save-Scaled $logo 512 512 'D:\Users\a3614\Downloads\btr-logo-512.png' $false
Save-Scaled $cover 600 1067 'D:\Users\a3614\Downloads\btr-cover-600.jpg' $true

$logo.Dispose(); $cover.Dispose()
Write-Host "ALL DONE"
