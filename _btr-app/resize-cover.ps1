Add-Type -AssemblyName System.Drawing
$cover = [System.Drawing.Image]::FromFile('D:\Users\a3614\Downloads\btr-app-cover.png')
function Save-Scaled($src, $w, $h, $out, $jpg) {
  $bmp = New-Object System.Drawing.Bitmap($w, $h)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $g.DrawImage($src, 0, 0, $w, $h)
  $g.Dispose()
  if ($jpg) { $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Jpeg) }
  else { $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png) }
  $bmp.Dispose()
  Write-Host "saved $out ($w x $h)"
}
Save-Scaled $cover 720 1280 'D:\Users\a3614\Documents\默认项目\_btr-app\app\src\main\res\drawable\splash_img.jpg' $true
Save-Scaled $cover 600 1067 'D:\Users\a3614\Downloads\btr-cover-600.jpg' $true
$cover.Dispose()
Write-Host "DONE"
