# Установка Android build template из шаблонов Godot (без GUI).
# Требует: export_templates/4.7.stable/android_source.zip + apks (tools/fetch_templates.py extract)
$tpl = Join-Path $env:APPDATA "Godot/export_templates/4.7.stable"
$src = Join-Path $tpl "android_source.zip"
if (-not (Test-Path $src)) { Write-Error "нет $src"; exit 1 }

if (Test-Path "android/build") { Remove-Item "android/build" -Recurse -Force }
New-Item -ItemType Directory -Force "android" | Out-Null
Expand-Archive -Path $src -DestinationPath "android/build" -Force
if (Test-Path "android/build/build") { # zip может содержать вложенную папку
    Move-Item "android/build/build/*" "android/build" -Force
}
Set-Content (Join-Path $tpl "version.txt") "4.7.stable" -NoNewline
Write-Host "android/build готов:"; Get-ChildItem "android/build" | Select-Object -First 8 -ExpandProperty Name
