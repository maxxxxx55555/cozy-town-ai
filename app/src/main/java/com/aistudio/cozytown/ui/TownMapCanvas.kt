package com.aistudio.cozytown.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cozytown.R
import com.aistudio.cozytown.core.NpcView
import com.aistudio.cozytown.ui.theme.ColorBorder
import com.aistudio.cozytown.ui.theme.ColorGrass
import com.aistudio.cozytown.ui.theme.ColorInk
import com.aistudio.cozytown.ui.theme.ColorRiver
import com.aistudio.cozytown.ui.theme.ColorSand
import com.aistudio.cozytown.ui.theme.MoodAngry
import com.aistudio.cozytown.ui.theme.MoodHappy
import com.aistudio.cozytown.ui.theme.MoodNeutral
import com.aistudio.cozytown.ui.theme.MoodSleepy

private const val MAP_W = 360f
private const val MAP_H = 310f

data class MapPlace(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val emoji: String
)

private val MAP_PLACES = listOf(
    MapPlace("home", "Дом", 150f, 40f, "🏠"),
    MapPlace("пекарня", "Пекарня", 68f, 62f, "🥐"),
    MapPlace("рынок", "Рынок", 264f, 46f, "🧺"),
    MapPlace("мастерская", "Мастерская", 288f, 126f, "🔨"),
    MapPlace("площадь", "Площадь", 176f, 166f, "⛲"),
    MapPlace("таверна", "Таверна", 58f, 198f, "🍵"),
    MapPlace("сад", "Сад", 248f, 206f, "🌱"),
    MapPlace("причал", "Причал", 112f, 243f, "⛵"),
    MapPlace("маяк", "Маяк", 316f, 232f, "🗼")
)

private val MAP_ROUTES = listOf(
    "home" to "пекарня", "home" to "рынок", "home" to "площадь",
    "пекарня" to "площадь", "рынок" to "мастерская", "мастерская" to "площадь",
    "площадь" to "таверна", "площадь" to "сад", "сад" to "маяк",
    "таверна" to "причал", "сад" to "причал", "площадь" to "причал"
)

/** Маленькая эмодзи-деталь на карте (городок меняется после улучшений). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEmoji(
    textMeasurer: TextMeasurer,
    emoji: String,
    x: Float,
    y: Float,
    sizeSp: Int
) {
    val style = TextStyle(fontSize = sizeSp.sp)
    val layout = textMeasurer.measure(text = emoji, style = style)
    drawText(
        textMeasurer = textMeasurer,
        text = emoji,
        topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f),
        style = style
    )
}

/** Место закрыто, пока не куплено соответствующее улучшение. */
private val PLACE_UPGRADE = mapOf("сад" to "garden", "маяк" to "lighthouse")

private fun isPlaceLocked(placeId: String, upgrades: List<String>): Boolean =
    PLACE_UPGRADE[placeId]?.let { it !in upgrades } == true

/** Позиция каждого NPC на карте: у своего места, веером, если их несколько. */
private fun npcCenters(
    npcs: List<NpcView>,
    scaleX: Float,
    scaleY: Float
): Map<String, Offset> {
    val result = mutableMapOf<String, Offset>()
    val groups = npcs.groupBy { it.place }
    for ((placeId, group) in groups) {
        val place = MAP_PLACES.find { it.id == placeId } ?: MAP_PLACES[4]
        group.forEachIndexed { rank, npc ->
            val col = rank % 2
            val row = rank / 2
            val offX = (col * 34f - 17f) * scaleX
            val offY = (row * 30f - 13f) * scaleY
            result[npc.name] = Offset(place.x * scaleX + offX, place.y * scaleY + offY)
        }
    }
    return result
}

@Composable
fun TownMapCanvas(
    npcs: List<NpcView>,
    currentPlace: String,
    upgrades: List<String> = emptyList(),
    onNpcClicked: (String) -> Unit,
    onPlaceClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val homeBmp = ImageBitmap.imageResource(R.drawable.home)
    val bakeryBmp = ImageBitmap.imageResource(R.drawable.bakery)
    val marketBmp = ImageBitmap.imageResource(R.drawable.market)
    val workshopBmp = ImageBitmap.imageResource(R.drawable.workshop)
    val squareBmp = ImageBitmap.imageResource(R.drawable.square)
    val tavernBmp = ImageBitmap.imageResource(R.drawable.tavern)
    val pierBmp = ImageBitmap.imageResource(R.drawable.pier)
    val gardenBmp = ImageBitmap.imageResource(R.drawable.garden)
    val lighthouseBmp = ImageBitmap.imageResource(R.drawable.lighthouse)

    val martaBmp = ImageBitmap.imageResource(R.drawable.npc_marta)
    val borisBmp = ImageBitmap.imageResource(R.drawable.npc_boris)
    val lukaBmp = ImageBitmap.imageResource(R.drawable.npc_luka)
    val anyaBmp = ImageBitmap.imageResource(R.drawable.npc_anya)
    val osipBmp = ImageBitmap.imageResource(R.drawable.npc_osip)
    val sonyaBmp = ImageBitmap.imageResource(R.drawable.npc_sonya)

    val placeBitmaps = remember(
        homeBmp, bakeryBmp, marketBmp, workshopBmp, squareBmp, tavernBmp, pierBmp, gardenBmp, lighthouseBmp
    ) {
        mapOf(
            "home" to homeBmp,
            "пекарня" to bakeryBmp,
            "рынок" to marketBmp,
            "мастерская" to workshopBmp,
            "площадь" to squareBmp,
            "таверна" to tavernBmp,
            "причал" to pierBmp,
            "сад" to gardenBmp,
            "маяк" to lighthouseBmp
        )
    }

    val npcBitmaps = remember(martaBmp, borisBmp, lukaBmp, anyaBmp, osipBmp, sonyaBmp) {
        mapOf(
            "Марта" to martaBmp,
            "Борис" to borisBmp,
            "Лука" to lukaBmp,
            "Аня" to anyaBmp,
            "Осип" to osipBmp,
            "Соня" to sonyaBmp
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, ColorBorder, RoundedCornerShape(16.dp))
            .background(ColorGrass)
            .testTag("town_map")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(npcs, currentPlace, upgrades) {
                    detectTapGestures { tap ->
                        val scaleX = size.width / MAP_W
                        val scaleY = size.height / MAP_H
                        val centers = npcCenters(npcs, scaleX, scaleY)

                        // сначала жители, потом места — чтобы тап по NPC не «уводил» в переход
                        val tappedNpc = npcs.firstOrNull { npc ->
                            val center = centers[npc.name] ?: return@firstOrNull false
                            val dx = tap.x - center.x
                            val dy = tap.y - center.y
                            dx * dx + dy * dy < (30f * scaleX) * (30f * scaleX)
                        }
                        val tappedPlace = MAP_PLACES.firstOrNull { place ->
                            val dx = tap.x - place.x * scaleX
                            val dy = tap.y - place.y * scaleY
                            dx * dx + dy * dy < (34f * scaleX) * (34f * scaleX)
                        }
                        when {
                            tappedNpc != null -> onNpcClicked(tappedNpc.name)
                            tappedPlace != null -> onPlaceClicked(tappedPlace.id)
                        }
                    }
                }
        ) {
            val scaleX = size.width / MAP_W
            val scaleY = size.height / MAP_H
            fun pt(place: MapPlace) = Offset(place.x * scaleX, place.y * scaleY)

            // 1. Река
            val riverHeight = 70f * scaleY
            drawRect(
                color = ColorRiver,
                topLeft = Offset(0f, size.height - riverHeight),
                size = Size(size.width, riverHeight)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, size.height - riverHeight + 5f),
                end = Offset(size.width, size.height - riverHeight + 5f),
                strokeWidth = 3f
            )
            for (i in 0 until 5) {
                val y = size.height - riverHeight + 22f + i * 10f
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(size.width * (0.06f + 0.17f * i), y),
                    end = Offset(size.width * (0.24f + 0.17f * i), y),
                    strokeWidth = 2f
                )
            }

            // 2. Дороги
            val pathColor = Color(0xFFC7B18E)
            for ((from, to) in MAP_ROUTES) {
                val p1 = MAP_PLACES.find { it.id == from } ?: continue
                val p2 = MAP_PLACES.find { it.id == to } ?: continue
                drawLine(
                    color = pathColor,
                    start = pt(p1),
                    end = pt(p2),
                    strokeWidth = 7f * scaleX,
                    cap = StrokeCap.Round
                )
            }

            // 2.5 Декорации: городок меняется после улучшений
            if ("lanterns" in upgrades) {
                for ((from, to) in MAP_ROUTES) {
                    val p1 = MAP_PLACES.find { it.id == from } ?: continue
                    val p2 = MAP_PLACES.find { it.id == to } ?: continue
                    val mid = Offset((pt(p1).x + pt(p2).x) / 2f, (pt(p1).y + pt(p2).y) / 2f)
                    drawCircle(color = Color(0xFFFFE9A8), radius = 3.5f * scaleX, center = mid)
                    drawCircle(
                        color = Color(0xFFE8B23C).copy(alpha = 0.6f),
                        radius = 6.5f * scaleX,
                        center = mid,
                        style = Stroke(width = 1.5f * scaleX)
                    )
                }
            }
            MAP_PLACES.find { it.id == "площадь" }?.let { square ->
                val c = pt(square)
                if ("fountain" in upgrades) {
                    drawCircle(color = Color(0xFFBFE3F5), radius = 13f * scaleX, center = Offset(c.x, c.y + 14f * scaleY))
                    drawCircle(
                        color = Color(0xFF6FA8D8),
                        radius = 15f * scaleX,
                        center = Offset(c.x, c.y + 14f * scaleY),
                        style = Stroke(width = 2f * scaleX)
                    )
                    for (i in 0 until 5) {
                        val angle = i * 72f
                        val rx = c.x + 15f * scaleX * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                        val ry = c.y + 14f * scaleY + 15f * scaleX * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                        drawCircle(color = Color.White.copy(alpha = 0.75f), radius = 2f * scaleX, center = Offset(rx, ry))
                    }
                }
                if ("fair" in upgrades) drawEmoji(textMeasurer, "🎪", c.x + 22f * scaleX, c.y - 22f * scaleY, 11)
                if ("cat" in upgrades) drawEmoji(textMeasurer, "🐈", c.x - 32f * scaleX, c.y + 24f * scaleY, 10)
            }
            // лавка сладостей у рынка, паром у причала, библиотека у дома, телескоп у маяка, ратуша на площади
            MAP_PLACES.find { it.id == "рынок" }?.let { market ->
                if ("sweet_shop" in upgrades) drawEmoji(textMeasurer, "🍬", pt(market).x + 24f * scaleX, pt(market).y + 20f * scaleY, 11)
            }
            MAP_PLACES.find { it.id == "причал" }?.let { pier ->
                if ("ferry" in upgrades) drawEmoji(textMeasurer, "⛴", pt(pier).x + 26f * scaleX, pt(pier).y + 14f * scaleY, 11)
            }
            MAP_PLACES.find { it.id == "home" }?.let { home ->
                if ("library" in upgrades) drawEmoji(textMeasurer, "📚", pt(home).x - 26f * scaleX, pt(home).y + 12f * scaleY, 11)
            }
            MAP_PLACES.find { it.id == "маяк" }?.let { lh ->
                if ("telescope" in upgrades) drawEmoji(textMeasurer, "🔭", pt(lh).x - 22f * scaleX, pt(lh).y - 6f * scaleY, 11)
            }
            MAP_PLACES.find { it.id == "площадь" }?.let { square ->
                if ("townhall" in upgrades) drawEmoji(textMeasurer, "🏛", pt(square).x + 30f * scaleX, pt(square).y + 30f * scaleY, 11)
            }

            MAP_PLACES.find { it.id == "рынок" }?.let { market ->
                if ("flowerbeds" in upgrades) {
                    val c = pt(market)
                    for (i in 0 until 4) {
                        drawCircle(
                            color = if (i % 2 == 0) Color(0xFFE58AA8) else Color(0xFFF2C14E),
                            radius = 3f * scaleX,
                            center = Offset(c.x - 18f * scaleX + i * 12f * scaleX, c.y + 26f * scaleY)
                        )
                    }
                }
            }

            // 3. Места
            for (place in MAP_PLACES) {
                val center = pt(place)
                val isHere = place.id == currentPlace

                if (isHere) {
                    drawCircle(color = ColorSand.copy(alpha = 0.30f), radius = 22f * scaleX, center = center)
                    drawCircle(
                        color = ColorSand,
                        radius = 22f * scaleX,
                        center = center,
                        style = Stroke(width = 3f * scaleX)
                    )
                } else {
                    drawCircle(
                        color = Color(0xFF8B6C48).copy(alpha = 0.32f),
                        radius = 17f * scaleX,
                        center = center
                    )
                }

                placeBitmaps[place.id]?.let { bitmap ->
                    val iconSize = (30f * scaleX).toInt().coerceAtLeast(12)
                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset(
                            (center.x - iconSize / 2f).toInt(),
                            (center.y - iconSize / 2f).toInt()
                        ),
                        dstSize = IntSize(iconSize, iconSize),
                        alpha = if (isPlaceLocked(place.id, upgrades)) 0.45f else 1f
                    )
                }
                if (isPlaceLocked(place.id, upgrades)) drawEmoji(textMeasurer, "🔒", center.x, center.y, 10)

                val labelText = if (isHere) "• ${place.name}" else place.name
                val style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                val layout = textMeasurer.measure(text = labelText, style = style)
                val labelW = layout.size.width + 10f
                val labelH = layout.size.height + 3f
                val labelX = center.x - labelW / 2f
                val labelY = center.y + 14f * scaleY

                val pill = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(labelX, labelY, labelX + labelW, labelY + labelH),
                            cornerRadius = CornerRadius(7f, 7f)
                        )
                    )
                }
                drawPath(pill, if (isHere) Color(0xFFFFF3D9) else Color(0xFFFFF8E7).copy(alpha = 0.9f))
                drawPath(pill, if (isHere) ColorSand else ColorBorder, style = Stroke(width = 1.2f))
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    topLeft = Offset(labelX + 5f, labelY + 1.5f),
                    style = style
                )
            }

            // 4. NPC поверх карты
            val centers = npcCenters(npcs, scaleX, scaleY)
            for (npc in npcs) {
                val center = centers[npc.name] ?: continue
                val moodColor = when (npc.mood) {
                    "happy" -> MoodHappy
                    "angry" -> MoodAngry
                    "sleepy" -> MoodSleepy
                    else -> MoodNeutral
                }

                drawCircle(
                    color = Color.Black.copy(alpha = 0.18f),
                    radius = 15f * scaleX,
                    center = Offset(center.x, center.y + 3f)
                )
                drawCircle(
                    color = moodColor,
                    radius = 17f * scaleX,
                    center = center,
                    style = Stroke(width = 3f * scaleX)
                )
                drawCircle(color = Color(0xFFFFFDF8), radius = 15f * scaleX, center = center)

                val sprite = npcBitmaps[npc.name]
                if (sprite != null) {
                    val spriteSize = (24f * scaleX).toInt().coerceAtLeast(10)
                    drawImage(
                        image = sprite,
                        dstOffset = IntOffset(
                            (center.x - spriteSize / 2f).toInt(),
                            (center.y - spriteSize / 2f).toInt()
                        ),
                        dstSize = IntSize(spriteSize, spriteSize)
                    )
                }

                val nameStyle = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = ColorInk)
                val nameLayout = textMeasurer.measure(text = npc.name, style = nameStyle)
                val nw = nameLayout.size.width + 8f
                val nh = nameLayout.size.height + 3f
                val nx = center.x - nw / 2f
                val ny = center.y - 27f * scaleY

                val tag = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(nx, ny, nx + nw, ny + nh),
                            cornerRadius = CornerRadius(5f, 5f)
                        )
                    )
                }
                drawPath(tag, Color(0xFFFFF9EE).copy(alpha = 0.95f))
                drawPath(tag, moodColor, style = Stroke(width = 1.2f))
                drawText(
                    textMeasurer = textMeasurer,
                    text = npc.name,
                    topLeft = Offset(nx + 4f, ny + 1.5f),
                    style = nameStyle
                )
            }
        }
    }
}
