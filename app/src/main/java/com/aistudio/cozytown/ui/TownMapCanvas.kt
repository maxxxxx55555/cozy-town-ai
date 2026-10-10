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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.aistudio.cozytown.model.NPC
import com.aistudio.cozytown.ui.theme.ColorBorder
import com.aistudio.cozytown.ui.theme.ColorGrass
import com.aistudio.cozytown.ui.theme.ColorInk
import com.aistudio.cozytown.ui.theme.ColorRiver
import com.aistudio.cozytown.ui.theme.MoodAngry
import com.aistudio.cozytown.ui.theme.MoodHappy
import com.aistudio.cozytown.ui.theme.MoodNeutral
import com.aistudio.cozytown.ui.theme.MoodSleepy

data class PlaceDef(
    val id: String,
    val name: String,
    val basePos: Offset,
    val iconRes: Int
)

@Composable
fun TownMapCanvas(
    npcs: List<NPC>,
    hour: Int,
    onNpcClicked: (NPC) -> Unit,
    onPlaceClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    val placeBitmaps = mapOf(
        "пекарня" to ImageBitmap.imageResource(R.drawable.bakery),
        "мастерская" to ImageBitmap.imageResource(R.drawable.workshop),
        "причал" to ImageBitmap.imageResource(R.drawable.pier),
        "рынок" to ImageBitmap.imageResource(R.drawable.market),
        "площадь" to ImageBitmap.imageResource(R.drawable.square),
        "таверна" to ImageBitmap.imageResource(R.drawable.tavern),
        "home" to ImageBitmap.imageResource(R.drawable.home)
    )

    val npcBitmaps = mapOf(
        "Марта" to ImageBitmap.imageResource(R.drawable.npc_marta),
        "Борис" to ImageBitmap.imageResource(R.drawable.npc_boris),
        "Лука" to ImageBitmap.imageResource(R.drawable.npc_luka),
        "Аня" to ImageBitmap.imageResource(R.drawable.npc_anya)
    )

    val places = remember {
        listOf(
            PlaceDef("home", "Дом", Offset(150f, 45f), R.drawable.home),
            PlaceDef("пекарня", "Пекарня", Offset(85f, 65f), R.drawable.bakery),
            PlaceDef("рынок", "Рынок", Offset(265f, 55f), R.drawable.market),
            PlaceDef("мастерская", "Мастерская", Offset(285f, 130f), R.drawable.workshop),
            PlaceDef("площадь", "Площадь", Offset(180f, 175f), R.drawable.square),
            PlaceDef("таверна", "Таверна", Offset(315f, 240f), R.drawable.tavern),
            PlaceDef("причал", "Причал", Offset(75f, 240f), R.drawable.pier)
        )
    }

    val routes = remember {
        listOf(
            "home" to "пекарня",
            "home" to "рынок",
            "пекарня" to "площадь",
            "рынок" to "мастерская",
            "мастерская" to "таверна",
            "причал" to "площадь",
            "площадь" to "таверна"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, ColorBorder, RoundedCornerShape(16.dp))
            .background(ColorGrass)
            .testTag("town_map")
            .semantics {
                contentDescription = "Интерактивная карта городка с жителями и локациями"
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(npcs, hour) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val scaleX = w / 360f
                        val scaleY = h / 310f

                        // Check NPC taps first
                        for ((idx, npc) in npcs.withIndex()) {
                            val spot = npc.schedule.placeAt(hour)
                            val placeDef = places.find { it.id == spot.place } ?: places[0]
                            val col = idx % 2
                            val row = idx / 2
                            val offX = (col * 50f - 25f) * scaleX
                            val offY = (row * 40f - 20f) * scaleY
                            val npcX = placeDef.basePos.x * scaleX + offX
                            val npcY = placeDef.basePos.y * scaleY + offY

                            val distSq = (tapOffset.x - npcX) * (tapOffset.x - npcX) +
                                    (tapOffset.y - npcY) * (tapOffset.y - npcY)
                            if (distSq < (28f * scaleX) * (28f * scaleX)) {
                                onNpcClicked(npc)
                                return@detectTapGestures
                            }
                        }

                        // Check place taps
                        for (place in places) {
                            val px = place.basePos.x * scaleX
                            val py = place.basePos.y * scaleY
                            val distSq = (tapOffset.x - px) * (tapOffset.x - px) +
                                    (tapOffset.y - py) * (tapOffset.y - py)
                            if (distSq < (36f * scaleX) * (36f * scaleX)) {
                                onPlaceClicked(place.id)
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val scaleX = canvasW / 360f
            val scaleY = canvasH / 310f

            fun mapPoint(base: Offset): Offset {
                return Offset(base.x * scaleX, base.y * scaleY)
            }

            // 1. Draw River at the bottom
            val riverHeight = 65f * scaleY
            drawRect(
                color = ColorRiver,
                topLeft = Offset(0f, canvasH - riverHeight),
                size = Size(canvasW, riverHeight)
            )
            // Soft river wave highlight
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(0f, canvasH - riverHeight + 4f),
                end = Offset(canvasW, canvasH - riverHeight + 4f),
                strokeWidth = 3f
            )

            // 2. Draw Road paths
            val pathColor = Color(0xFFC7B18E)
            val pathStroke = 6f * scaleX
            for (route in routes) {
                val p1 = places.find { it.id == route.first }?.basePos ?: Offset.Zero
                val p2 = places.find { it.id == route.second }?.basePos ?: Offset.Zero
                drawLine(
                    color = pathColor,
                    start = mapPoint(p1),
                    end = mapPoint(p2),
                    strokeWidth = pathStroke,
                    cap = StrokeCap.Round
                )
            }

            // 3. Draw Places
            for (place in places) {
                val center = mapPoint(place.basePos)
                // Draw place marker base
                drawCircle(
                    color = Color(0xFF8B6C48).copy(alpha = 0.4f),
                    radius = 16f * scaleX,
                    center = center
                )
                // Draw place sprite
                val bitmap = placeBitmaps[place.id]
                if (bitmap != null) {
                    val iconSize = (28f * scaleX).toInt()
                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset((center.x - iconSize / 2).toInt(), (center.y - iconSize / 2).toInt()),
                        dstSize = IntSize(iconSize, iconSize)
                    )
                }

                // Draw place label pill
                val labelText = place.name
                val textLayout = textMeasurer.measure(
                    text = labelText,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorInk
                    )
                )
                val labelW = textLayout.size.width + 12f
                val labelH = textLayout.size.height + 4f
                val labelX = center.x - labelW / 2
                val labelY = center.y + (16f * scaleY)

                val pillPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(labelX, labelY, labelX + labelW, labelY + labelH),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    )
                }
                drawPath(pillPath, Color(0xFFFFF8E7).copy(alpha = 0.9f))
                drawPath(pillPath, ColorBorder, style = Stroke(width = 1f))
                drawText(
                    textMeasurer = textMeasurer,
                    text = labelText,
                    topLeft = Offset(labelX + 6f, labelY + 2f),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorInk
                    )
                )
            }

            // 4. Draw NPCs at their current scheduled positions
            for ((idx, npc) in npcs.withIndex()) {
                val spot = npc.schedule.placeAt(hour)
                val placeDef = places.find { it.id == spot.place } ?: places[0]
                val col = idx % 2
                val row = idx / 2
                val offX = (col * 50f - 25f) * scaleX
                val offY = (row * 38f - 19f) * scaleY
                val npcCenter = Offset(placeDef.basePos.x * scaleX + offX, placeDef.basePos.y * scaleY + offY)

                // Mood ring color
                val moodColor = when (npc.identity.mood) {
                    "happy" -> MoodHappy
                    "angry" -> MoodAngry
                    "sleepy" -> MoodSleepy
                    else -> MoodNeutral
                }

                // Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.25f),
                    radius = 16f * scaleX,
                    center = Offset(npcCenter.x, npcCenter.y + 2f)
                )

                // Mood outer ring
                drawCircle(
                    color = moodColor,
                    radius = 18f * scaleX,
                    center = npcCenter,
                    style = Stroke(width = 3.5f * scaleX)
                )

                // Inner circle background
                drawCircle(
                    color = Color(0xFFFFFDF8),
                    radius = 16f * scaleX,
                    center = npcCenter
                )

                // Sprite
                val npcBitmap = npcBitmaps[npc.identity.npcName]
                if (npcBitmap != null) {
                    val spriteSize = (26f * scaleX).toInt()
                    drawImage(
                        image = npcBitmap,
                        dstOffset = IntOffset((npcCenter.x - spriteSize / 2).toInt(), (npcCenter.y - spriteSize / 2).toInt()),
                        dstSize = IntSize(spriteSize, spriteSize)
                    )
                }

                // Name tag
                val nameTag = npc.identity.npcName
                val nameLayout = textMeasurer.measure(
                    text = nameTag,
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorInk
                    )
                )
                val nw = nameLayout.size.width + 10f
                val nh = nameLayout.size.height + 4f
                val nx = npcCenter.x - nw / 2
                val ny = npcCenter.y - 28f * scaleY

                val tagRoundRect = RoundRect(
                    rect = Rect(nx, ny, nx + nw, ny + nh),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                val tagPath = Path().apply { addRoundRect(tagRoundRect) }
                drawPath(tagPath, Color(0xFFFFF9EE).copy(alpha = 0.95f))
                drawPath(tagPath, moodColor, style = Stroke(width = 1.2f))
                drawText(
                    textMeasurer = textMeasurer,
                    text = nameTag,
                    topLeft = Offset(nx + 5f, ny + 2f),
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorInk
                    )
                )
            }
        }
    }
}
