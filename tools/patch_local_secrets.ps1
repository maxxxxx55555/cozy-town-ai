# Вставляет локальные keystore-реквизиты в export_presets.cfg для локальной сборки AAB.
# ВАЖНО: после экспорта вернуть файл: git checkout export_presets.cfg
$cfg = "export_presets.cfg"
$content = Get-Content $cfg -Raw
$store = (Resolve-Path "release.keystore").Path -replace "\\", "/"
$content = $content -replace 'keystore/release=""', ('keystore/release="' + $store + '"')
$content = $content -replace 'keystore/release_user=""', 'keystore/release_user="puzzleoflife"'
$content = $content -replace 'keystore/release_password=""', 'keystore/release_password="puzzle123"'
Set-Content $cfg $content -NoNewline -Encoding UTF8
Select-String -Path $cfg -Pattern "keystore/release" | ForEach-Object { $_.Line } | Select-Object -First 3
