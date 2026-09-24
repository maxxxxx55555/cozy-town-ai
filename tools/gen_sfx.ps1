# Генерация простых SFX (WAV, 44.1 kHz, 16-bit mono) без внешних ассетов.
$dir = "assets/sfx"
New-Item -ItemType Directory -Force $dir | Out-Null
$rate = 44100

function New-Wav($path, $freqs, $ms, $amp) {
    $n = [int]($rate * $ms / 1000)
    $data = New-Object byte[] ($n * 2)
    $seg = $n / $freqs.Count
    for ($i = 0; $i -lt $n; $i++) {
        $idx = [Math]::Min([int]($i / $seg), $freqs.Count - 1)
        $t = ($i - $idx * $seg) / $rate
        $env = [Math]::Pow(1.0 - $i / $n, 2.0)
        $v = [Math]::Sin(2 * 3.14159265 * $freqs[$idx] * $t) * $env * $amp
        $b = [int16]$v
        $data[$i * 2] = [byte]($b -band 0xFF)
        $data[$i * 2 + 1] = [byte]((($b -shr 8) -band 0xFF))
    }
    $bytes = [System.Text.Encoding]::ASCII.GetBytes("WAVE")
    $fs = [System.IO.File]::Create($path)
    $bw = New-Object System.IO.BinaryWriter($fs)
    $bw.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF"))
    $bw.Write([int](36 + $n * 2)); $bw.Write($bytes)
    $bw.Write([System.Text.Encoding]::ASCII.GetBytes("fmt "))
    $bw.Write([int]16); $bw.Write([int16]1); $bw.Write([int16]1)
    $bw.Write([int]$rate); $bw.Write([int]($rate * 2)); $bw.Write([int16]2); $bw.Write([int16]16)
    $bw.Write([System.Text.Encoding]::ASCII.GetBytes("data")); $bw.Write([int]($n * 2))
    $bw.Write($data); $bw.Flush(); $fs.Close()
    Write-Host "$path ($([int]($ms))ms)"
}

New-Wav "$dir/click.wav" @(660, 880) 90 14000
New-Wav "$dir/coin.wav" @(988, 1319) 200 16000
New-Wav "$dir/success.wav" @(523, 784, 1046) 360 18000
