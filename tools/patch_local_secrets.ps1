# Вставляет локальные keystore-реквизиты в export_presets.cfg для локальной сборки AAB.
# ВАЖНО: после экспорта вернуть файл: git checkout export_presets.cfg
$cfg = "export_presets.cfg"
$store = (Resolve-Path "release.keystore").Path -replace "\\", "/"
$user = $env:ANDROID_KEYSTORE_USER
$pass = $env:ANDROID_KEYSTORE_PASSWORD
if ([string]::IsNullOrWhiteSpace($user) -or [string]::IsNullOrWhiteSpace($pass)) {
  throw "Set ANDROID_KEYSTORE_USER and ANDROID_KEYSTORE_PASSWORD before building."
}
$content = Get-Content $cfg -Raw
$content = $content -replace 'keystore/release=""', ('keystore/release="' + $store + '"')
$content = $content -replace 'keystore/release_user=""', ('keystore/release_user="' + $user + '"')
$content = $content -replace 'keystore/release_password=""', ('keystore/release_password="' + $pass + '"')
Set-Content $cfg $content -NoNewline -Encoding UTF8
Write-Host "Keystore path configured. Password was not printed."
