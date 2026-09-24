Add-Type -AssemblyName System.Drawing
$dir = "assets/store"
New-Item -ItemType Directory -Force $dir | Out-Null

function Grad($w, $h, $c1, $c2) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $rect = New-Object System.Drawing.Rectangle(0, 0, $w, $h)
    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, $c1, $c2, 90)
    $g.FillRectangle($brush, $rect)
    $brush.Dispose()
    return $bmp, $g
}

function House($g, $x, $y, $s) {
    $wall = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 240, 220, 190))
    $roof = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 190, 90, 70))
    $g.FillRectangle($wall, $x, $y, $s, $s)
    $pts = @([System.Drawing.Point]::new($x - $s/8, $y), [System.Drawing.Point]::new($x + $s/2, $y - $s/2), [System.Drawing.Point]::new($x + $s + $s/8, $y))
    $g.FillPolygon($roof, $pts)
    $wall.Dispose(); $roof.Dispose()
}

function Face($g, $x, $y, $r) {
    $skin = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 220, 170))
    $dark = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::Black)
    $g.FillEllipse($skin, $x - $r, $y - $r, 2*$r, 2*$r)
    $g.FillEllipse($dark, $x - $r/2, $y - $r/4, $r/4, $r/4)
    $g.FillEllipse($dark, $x + $r/4, $y - $r/4, $r/4, $r/4)
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::Black, [Math]::Max(2, $r/10))
    $g.DrawArc($pen, $x - $r/2, $y, $r, $r/2, 0, 180)
    $skin.Dispose(); $dark.Dispose(); $pen.Dispose()
}

function Txt($g, $s, $x, $y, $size, $color) {
    $f = New-Object System.Drawing.Font("Arial", $size, [System.Drawing.FontStyle]::Bold)
    $b = New-Object System.Drawing.SolidBrush($color)
    $g.DrawString($s, $f, $b, $x, $y)
    $f.Dispose(); $b.Dispose()
}

# Icon 1024x1024
$bmp, $g = Grad 1024 1024 ([System.Drawing.Color]::FromArgb(255,140,200,240)) ([System.Drawing.Color]::FromArgb(255,255,235,190))
$sun = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255,255,220,110))
$g.FillEllipse($sun, 700, 100, 220, 220); $sun.Dispose()
House $g 200 560 420
Face $g 640 600 150
Txt $g "Пазл" 330 780 130 ([System.Drawing.Color]::FromArgb(255,90,60,40))
Txt $g "из Жизни" 250 910 60 ([System.Drawing.Color]::FromArgb(255,90,60,40))
$bmp.Save("$dir/icon_1024.png"); $g.Dispose(); $bmp.Dispose()

# Feature graphic 1024x500
$bmp, $g = Grad 1024 500 ([System.Drawing.Color]::FromArgb(255,120,180,230)) ([System.Drawing.Color]::FromArgb(255,255,230,180))
House $g 60 300 180
Face $g 880 200 90
Txt $g "Пазл из Жизни" 300 150 72 ([System.Drawing.Color]::FromArgb(255,80,50,35))
Txt $g "NPC помнит тебя и рассказывает другим" 260 280 28 ([System.Drawing.Color]::FromArgb(255,80,50,35))
$bmp.Save("$dir/feature_1024x500.png"); $g.Dispose(); $bmp.Dispose()

# 6 screenshots 1080x1920
$shots = @(
    @("«Я помню тебя, Макс!»", "NPC узнаёт тебя с первых секунд"),
    @("Марта рассказала Борису…", "Сплетни разносят твои поступки"),
    @("Добрый городок", "Уютная жизнь, расписание и настроение NPC"),
    @("Твои поступки меняют тон", "Помог — тепло, навредил — холодно"),
    @("Живая память", "NPC вспоминает прошлые сессии"),
    @("Каждый день — история", "Дни идут, отношения растут")
)
for ($i = 0; $i -lt 6; $i++) {
    $bmp, $g = Grad 1080 1920 ([System.Drawing.Color]::FromArgb(255,150,205,245)) ([System.Drawing.Color]::FromArgb(255,255,240,200))
    House $g 180 1250 480
    Face $g 760 1330 130
    $bubble = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
    $g.FillRectangle($bubble, 90, 500, 900, 420); $bubble.Dispose()
    Txt $g $shots[$i][0] 130 560 44 ([System.Drawing.Color]::FromArgb(255,70,45,30))
    Txt $g $shots[$i][1] 130 700 30 ([System.Drawing.Color]::FromArgb(255,110,90,70))
    $bmp.Save("$dir/shot$($i+1)_1080x1920.png"); $g.Dispose(); $bmp.Dispose()
}
Get-ChildItem $dir | Select-Object -ExpandProperty Name
