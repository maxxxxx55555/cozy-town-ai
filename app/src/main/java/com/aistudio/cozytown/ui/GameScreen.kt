package com.aistudio.cozytown.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cozytown.R
import com.aistudio.cozytown.core.AchievementView
import com.aistudio.cozytown.core.ItemView
import com.aistudio.cozytown.core.NpcView
import com.aistudio.cozytown.core.RecipeView
import com.aistudio.cozytown.core.RequestView
import com.aistudio.cozytown.core.UpgradeView
import com.aistudio.cozytown.ui.theme.ColorBorder
import com.aistudio.cozytown.ui.theme.ColorInk
import com.aistudio.cozytown.ui.theme.ColorInkLight
import com.aistudio.cozytown.ui.theme.ColorMint
import com.aistudio.cozytown.ui.theme.ColorPaper
import com.aistudio.cozytown.ui.theme.ColorPaperCard
import com.aistudio.cozytown.ui.theme.ColorSand
import com.aistudio.cozytown.ui.theme.ColorSky
import com.aistudio.cozytown.ui.theme.ColorTerra
import com.aistudio.cozytown.ui.theme.MoodAngry
import com.aistudio.cozytown.ui.theme.MoodHappy
import com.aistudio.cozytown.ui.theme.MoodNeutral
import com.aistudio.cozytown.ui.theme.MoodSleepy
import kotlinx.coroutines.delay

private val TABS = listOf(
    "🗺" to "Карта",
    "👥" to "Жители",
    "🎒" to "Сумка",
    "⚒" to "Крафт",
    "📋" to "Дела",
    "🏘" to "Город"
)

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(modifier = modifier.fillMaxSize(), color = ColorPaper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderBar(uiState)

            EventBanner(
                state = uiState,
                onAction = {
                    when (uiState.eventId) {
                        "cat_visit" -> viewModel.petCat()
                        "merchant" -> viewModel.openMerchant()
                    }
                }
            )

            TabBar(current = uiState.tab, badges = uiState.tabBadges, onSelect = { viewModel.selectTab(it) })

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (uiState.tab) {
                    0 -> MapTab(uiState, viewModel)
                    1 -> NpcTab(uiState, viewModel)
                    2 -> BagTab(uiState, viewModel)
                    3 -> CraftTab(uiState, viewModel)
                    4 -> RequestsTab(uiState, viewModel)
                    else -> TownTab(uiState, viewModel)
                }
            }

            JournalStrip(uiState, onToggle = { viewModel.toggleJournal() })
        }
    }

    if (uiState.isIntroVisible) {
        IntroDialog(
            name = uiState.nameInputText,
            onNameChange = { viewModel.onNameChange(it) },
            onSubmit = { viewModel.onNameSubmitted() }
        )
    }

    uiState.pickTargetFor?.let { itemId ->
        val itemName = uiState.items.firstOrNull { it.id == itemId }?.let { "${it.emoji} ${it.name}" } ?: itemId
        GiftTargetDialog(
            itemName = itemName,
            npcs = uiState.npcs,
            onDismiss = { viewModel.closeGiftTargetPicker() },
            onPick = { viewModel.giftToNpc(it) }
        )
    }

    uiState.giftTarget?.let { target ->
        GiftDialog(
            target = target,
            items = uiState.items,
            loved = uiState.giftTargetLoved,
            onDismiss = { viewModel.closeGift() },
            onGift = { viewModel.gift(it) }
        )
    }

    if (uiState.isPuzzleDialogOpen) {
        PuzzleDialog(
            shards = uiState.shards,
            total = uiState.puzzleTotal,
            onDismiss = { viewModel.closePuzzle() }
        )
    }

    if (uiState.isPrivacyDialogOpen) {
        PrivacyDialog(
            onDismiss = { viewModel.closePrivacyDialog() },
            onResetProgress = { viewModel.resetProgress() }
        )
    }

    if (uiState.isReportDialogOpen) {
        ReportDialog(
            onDismiss = { viewModel.closeReportDialog() },
            onSubmit = { reason, text -> viewModel.submitReport(reason, text) }
        )
    }

    uiState.toast?.let { message ->
        LaunchedEffect(message) {
            delay(2600)
            viewModel.consumeToast()
        }
        Box(
            modifier = Modifier.fillMaxSize().padding(bottom = 90.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180))
            ) {
                Surface(
                    color = ColorInk.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

// --------------------------------------------------------------------- header

@Composable
private fun HeaderBar(state: GameUiState) {
    val animatedCoins by animateIntAsState(
        targetValue = state.coins,
        animationSpec = tween(700),
        label = "coins"
    )
    val animatedXp by animateFloatAsState(
        targetValue = if (state.xpNeed > 0) state.xp.toFloat() / state.xpNeed else 0f,
        animationSpec = tween(600),
        label = "xp"
    )
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ПАЗЛ ИЗ ЖИЗНИ",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = ColorTerra,
                modifier = Modifier.testTag("app_title")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(emoji = "🕗", value = state.timeString, testTag = "time_label")
                Pill(emoji = "📅", value = "День ${state.day}", testTag = "day_label")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill(emoji = "🪙", value = "$animatedCoins", accent = ColorSand, testTag = "coins_label")
            Pill(emoji = "⚡", value = "${state.energy}/${state.maxEnergy}", accent = ColorSky, testTag = "energy_label")
            Pill(emoji = "🧩", value = "${state.shards}/${state.puzzleTotal}", accent = ColorMint, testTag = "shards_label")
            if (state.streak >= 2) {
                Pill(emoji = "🔥", value = "${state.streak}", accent = ColorTerra, testTag = "streak_label")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Ур. ${state.level}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ColorInk
            )
            LinearProgressIndicator(
                progress = { animatedXp },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ColorMint,
                trackColor = ColorBorder.copy(alpha = 0.4f)
            )
            Text(
                text = "${state.xp}/${state.xpNeed}",
                fontSize = 11.sp,
                color = ColorInkLight
            )
        }
    }
}

@Composable
private fun Pill(emoji: String, value: String, accent: Color = ColorInkLight, testTag: String = "") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ColorPaperCard)
            .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    ) {
        Text(text = emoji, fontSize = 11.sp)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ColorInk)
    }
}

@Composable
private fun EventBanner(state: GameUiState, onAction: () -> Unit) {
    if (state.eventName.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth().testTag("event_banner"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "☀", fontSize = 16.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = state.eventName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                Text(text = state.eventText, fontSize = 11.sp, color = ColorInkLight, lineHeight = 14.sp)
            }
            state.eventActionLabel?.let { label ->
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorTerra, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp).testTag("event_action")
                ) {
                    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TabBar(current: Int, badges: List<Int>, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TABS.forEachIndexed { index, (emoji, label) ->
            val selected = index == current
            val badge = badges.getOrElse(index) { 0 }
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) ColorMint else ColorPaperCard)
                        .border(1.dp, if (selected) ColorMint else ColorBorder, RoundedCornerShape(12.dp))
                        .clickable { onSelect(index) }
                        .padding(vertical = 5.dp)
                        .testTag("tab_$index"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(text = emoji, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = if (selected) Color.White else ColorInk
                    )
                }
                if (badge > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 1.dp, end = 3.dp)
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(ColorTerra),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "$badge", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = ColorInk)
        Text(text = subtitle, fontSize = 12.sp, color = ColorInkLight, lineHeight = 15.sp)
    }
}

@Composable
private fun ScrollColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

// --------------------------------------------------------------------- карта

@Composable
private fun MapTab(state: GameUiState, vm: GameViewModel) {
    ScrollColumn {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Ты сейчас: ${state.currentPlaceName}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorInk
                )
                Text(text = state.currentPlaceFlavor, fontSize = 12.sp, color = ColorInkLight)
            }
        }

        TownMapCanvas(
            npcs = state.npcs,
            currentPlace = state.currentPlaceId,
            upgrades = state.upgradeIds,
            onNpcClicked = { vm.talkTo(it) },
            onPlaceClicked = { vm.moveTo(it) }
        )

        Text(
            text = "Тапни по жителю — поговорить, по месту — перейти. Кольцо вокруг жителя: зелёное — радость, " +
                    "жёлтое — спокойствие, красное — не в духе, синее — сон.",
            fontSize = 11.sp,
            color = ColorInkLight,
            lineHeight = 15.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { vm.gather() },
                enabled = state.energy > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorMint, contentColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp).testTag("gather_button")
            ) {
                Text(
                    text = if (state.currentPlaceId == "home") "Отдохнуть" else "Собрать (1⚡)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = { vm.selectTab(1) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorTerra, contentColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp).testTag("to_people_button")
            ) {
                Text("К жителям", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text("Места городка", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorInk)
        state.places.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { place ->
                    PlaceChipCard(place, onClick = { vm.moveTo(place.id) }, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }

        if (state.npcsHere.isNotEmpty()) {
            Text("Сейчас здесь", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorInk)
            state.npcsHere.forEach { npc ->
                NpcQuickRow(npc = npc, onTalk = { vm.talkTo(npc.name) }, onHelp = { vm.helpNpc(npc.name) })
            }
        } else {
            Text(
                text = "Здесь пока никого. Жители ходят по расписанию — загляни в «Жители».",
                fontSize = 12.sp,
                color = ColorInkLight
            )
        }
    }
}

@Composable
private fun PlaceChipCard(place: PlaceChip, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (place.here) ColorMint.copy(alpha = 0.18f) else ColorPaperCard)
            .border(
                1.dp,
                if (place.here) ColorMint else ColorBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = if (place.locked) "🔒" else place.emoji,
            fontSize = 20.sp
        )
        Text(
            text = place.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ColorInk,
            textAlign = TextAlign.Center
        )
        val hint = when {
            place.locked -> "не открыто"
            place.npcCount > 0 -> "жителей: ${place.npcCount}"
            else -> " "
        }
        Text(text = hint, fontSize = 10.sp, color = ColorInkLight)
    }
}

@Composable
private fun NpcQuickRow(npc: NpcView, onTalk: () -> Unit, onHelp: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MoodAvatar(npc)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = npc.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                Text(
                    text = "${npc.activity} · ${npc.moodText}",
                    fontSize = 11.sp,
                    color = ColorInkLight
                )
            }
            Button(
                onClick = onTalk,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorMint, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Спросить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onHelp,
                enabled = npc.isHere,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorTerra, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Помочь", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MoodAvatar(npc: NpcView) {
    val moodColor = when (npc.mood) {
        "happy" -> MoodHappy
        "angry" -> MoodAngry
        "sleepy" -> MoodSleepy
        else -> MoodNeutral
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(ColorPaper)
            .border(2.dp, moodColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = npcEmoji(npc.name), fontSize = 20.sp)
    }
}

private fun npcEmoji(name: String): String = when (name) {
    "Марта" -> "🧑‍🍳"
    "Борис" -> "🧔"
    "Лука" -> "🎣"
    "Аня" -> "🌸"
    "Осип" -> "⚓"
    "Соня" -> "🐚"
    else -> "🙂"
}

// --------------------------------------------------------------------- жители

@Composable
private fun NpcTab(state: GameUiState, vm: GameViewModel) {
    ScrollColumn {
        TabTitle(
            title = "Жители городка",
            subtitle = "Доверие растёт от разговоров, помощи и подарков. Истории открываются по шагам."
        )
        state.npcs.forEach { npc ->
            NpcCard(npc = npc, energy = state.energy, vm = vm)
        }
    }
}

@Composable
private fun NpcCard(npc: NpcView, energy: Int, vm: GameViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("npc_card_${npc.name}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoodAvatar(npc)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = npc.name, fontSize = 16.sp, fontWeight = FontWeight.Black, color = ColorInk)
                        Text(
                            text = npc.tierName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(tierColor(npc.tier))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${npc.placeName}: ${npc.activity} · ${npc.moodText}",
                        fontSize = 11.sp,
                        color = ColorInkLight
                    )
                }
            }

            Text(text = npc.bio, fontSize = 11.sp, color = ColorInkLight, lineHeight = 14.sp)

            val animatedTrust by animateFloatAsState(
                targetValue = (npc.trust + 1f) / 2f,
                animationSpec = tween(600),
                label = "trust"
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Доверие", fontSize = 11.sp, color = ColorInkLight)
                LinearProgressIndicator(
                    progress = { animatedTrust },
                    modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp)),
                    color = tierColor(npc.tier),
                    trackColor = ColorBorder.copy(alpha = 0.4f)
                )
                Text(text = "${(npc.trust * 100).toInt()}%", fontSize = 11.sp, color = ColorInkLight)
            }

            if (npc.questTitle.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF6E4))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (npc.questDone) "📖 «${npc.questTitle}» — история рассказана"
                        else "📖 «${npc.questTitle}» · часть ${npc.questStepIndex + 1}/${npc.questSteps}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorInk
                    )
                    if (!npc.questDone) {
                        Text(text = npc.questText, fontSize = 11.sp, color = ColorInkLight, lineHeight = 14.sp)
                        Button(
                            onClick = { vm.advanceQuest(npc.name) },
                            enabled = npc.questReady,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorSand,
                                contentColor = Color.White,
                                disabledContainerColor = ColorBorder.copy(alpha = 0.5f),
                                disabledContentColor = ColorInkLight
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp).testTag("quest_button_${npc.name}")
                        ) {
                            Text(
                                text = if (npc.questReady) "Продолжить историю" else "Условие ещё не выполнено",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(text = npc.lovesHint, fontSize = 11.sp, color = ColorInkLight, lineHeight = 14.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { vm.talkTo(npc.name) },
                    enabled = npc.isHere,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorMint,
                        contentColor = Color.White,
                        disabledContainerColor = ColorBorder.copy(alpha = 0.5f),
                        disabledContentColor = ColorInkLight
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(38.dp).testTag("talk_button_${npc.name}")
                ) {
                    Text(if (npc.isHere) "Поговорить" else "Не здесь", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { vm.helpNpc(npc.name) },
                    enabled = npc.isHere && energy >= 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorTerra,
                        contentColor = Color.White,
                        disabledContainerColor = ColorBorder.copy(alpha = 0.5f),
                        disabledContentColor = ColorInkLight
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(38.dp).testTag("help_button_${npc.name}")
                ) {
                    Text("Помочь (2⚡)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { vm.openGift(npc.name) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorSky, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.weight(1f).height(38.dp).testTag("gift_button_${npc.name}")
                ) {
                    Text("Подарить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun tierColor(tier: Int): Color = when (tier) {
    0 -> ColorInkLight
    1 -> ColorSky
    2 -> ColorMint
    else -> ColorSand
}

// --------------------------------------------------------------------- сумка

@Composable
private fun BagTab(state: GameUiState, vm: GameViewModel) {
    ScrollColumn {
        TabTitle(
            title = "Сумка",
            subtitle = "Всего предметов: ${state.itemsTotal}. Продавай лишнее или дари жителям — подарки открывают их истории."
        )

        MosaicPanel(
            shards = state.shards,
            total = state.puzzleTotal,
            onClick = { vm.openPuzzle() }
        )

        if (state.items.isEmpty()) {
            Text(
                text = "Сумка пуста. Собери ресурсы на карте: пекарня, рынок, мастерская, причал — у каждого места свои находки.",
                fontSize = 12.sp,
                color = ColorInkLight,
                lineHeight = 16.sp
            )
        } else {
            state.items.forEach { item ->
                ItemRow(
                    item = item,
                    onSell = { vm.sellItem(item.id) },
                    onGift = { vm.openGiftTargetPicker(item.id) }
                )
            }
        }
    }
}

@Composable
private fun ItemRow(item: ItemView, onSell: () -> Unit, onGift: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = item.emoji, fontSize = 20.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.name} ×${item.count}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorInk
                )
                Text(text = "цена: ${item.sellPrice} монет", fontSize = 11.sp, color = ColorInkLight)
            }
            if (item.rarity >= 2) {
                Text(text = "✨", fontSize = 14.sp)
            }
            Button(
                onClick = onGift,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorTerra, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp).testTag("gift_item_${item.id}")
            ) {
                Text("Подарить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onSell,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorSand, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Продать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MosaicPanel(shards: Int, total: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("mosaic_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🧩 Мозаика городка",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorInk
                )
                Text(text = "$shards / $total осколков", fontSize = 12.sp, color = ColorInkLight)
            }
            MosaicImage(shards = shards, total = total)
            Text(
                text = "Осколки дают достижения, заказы, истории жителей и улучшения. " +
                        "Нажми, чтобы увидеть, откуда берутся осколки.",
                fontSize = 11.sp,
                color = ColorInkLight,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun MosaicImage(shards: Int, total: Int) {
    val bitmap: ImageBitmap = ImageBitmap.imageResource(R.drawable.town_mosaic)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 11f)
            .clip(RoundedCornerShape(12.dp))
            .background(ColorPaper)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Мозаика городка",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 6
            val rows = 5
            val tileW = size.width / cols
            val tileH = size.height / rows
            val progress = if (total > 0) shards.toFloat() / total else 0f
            val revealed = (cols * rows * progress).toInt()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val index = r * cols + c
                    if (index >= revealed) {
                        drawRect(
                            color = ColorPaper.copy(alpha = 0.94f),
                            topLeft = Offset(c * tileW, r * tileH),
                            size = Size(tileW - 1f, tileH - 1f)
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------- крафт

@Composable
private fun CraftTab(state: GameUiState, vm: GameViewModel) {
    ScrollColumn {
        TabTitle(
            title = "Мастерская (крафт)",
            subtitle = "Готовые вещи стоят дороже и нравятся жителям больше. Каждое изготовление — 1 ⚡."
        )
        state.recipes.forEach { recipe ->
            RecipeRow(recipe = recipe, onCraft = { vm.craft(recipe.id) })
        }
    }
}

@Composable
private fun RecipeRow(recipe: RecipeView, onCraft: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = recipe.emoji, fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = recipe.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                Text(text = recipe.inputsText, fontSize = 11.sp, color = ColorInkLight)
            }
            Button(
                onClick = onCraft,
                enabled = recipe.canCraft,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorMint,
                    contentColor = Color.White,
                    disabledContainerColor = ColorBorder.copy(alpha = 0.5f),
                    disabledContentColor = ColorInkLight
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp).testTag("craft_${recipe.id}")
            ) {
                Text("Сделать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --------------------------------------------------------------------- дела

@Composable
private fun RequestsTab(state: GameUiState, vm: GameViewModel) {
    ScrollColumn {
        TabTitle(
            title = "Дела дня",
            subtitle = "Заказы жителей обновляются каждое утро. Выполняй — монеты, опыт и их доверие."
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "Цели дня", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                state.goals.forEach { goal ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = if (goal.done) "☑" else "☐", fontSize = 14.sp, color = if (goal.done) ColorMint else ColorInkLight)
                        Text(
                            text = "${goal.text} (${goal.current}/${goal.target})",
                            fontSize = 12.sp,
                            color = if (goal.done) ColorMint else ColorInk
                        )
                    }
                }
                if (state.goals.all { it.done }) {
                    if (state.goalsClaimed) {
                        Text(text = "Награда за сегодня получена. Завтра — новые цели.", fontSize = 12.sp, color = ColorMint)
                    } else {
                        Button(
                            onClick = { vm.claimGoals() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSand, contentColor = Color.White),
                            modifier = Modifier.height(38.dp).testTag("claim_goals_button")
                        ) {
                            Text("Забрать 25 монет", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        state.requests.forEach { request ->
            RequestRow(request = request, onFulfill = { vm.fulfillRequest(request.index) })
        }

        if (state.eventId == "merchant") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3D9))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "🧳 Странствующий торговец", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                    Text(
                        text = "Пока он здесь, можно купить редкое: жемчуг за 25 и янтарь за 30 монет (по одному в день).",
                        fontSize = 11.sp,
                        color = ColorInkLight
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.buyMerchant("pearl") },
                            enabled = state.coins >= 25,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSky, contentColor = Color.White),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("⚪ Жемчуг · 25", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { vm.buyMerchant("amber") },
                            enabled = state.coins >= 30,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSand, contentColor = Color.White),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("🟠 Янтарь · 30", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestRow(request: RequestView, onFulfill: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = request.itemEmoji, fontSize = 20.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${request.npc}: ${request.text}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorInk, lineHeight = 15.sp)
                Text(
                    text = "${request.itemName}: ${request.have}/${request.need} · награда ${request.coins} монет",
                    fontSize = 11.sp,
                    color = if (request.canDo) ColorMint else ColorInkLight
                )
            }
            Button(
                onClick = onFulfill,
                enabled = request.canDo,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorMint,
                    contentColor = Color.White,
                    disabledContainerColor = ColorBorder.copy(alpha = 0.5f),
                    disabledContentColor = ColorInkLight
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp).testTag("fulfill_${request.index}")
            ) {
                Text("Отдать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --------------------------------------------------------------------- город

@Composable
private fun TownTab(state: GameUiState, vm: GameViewModel) {
    ScrollColumn {
        TabTitle(
            title = "Городок",
            subtitle = "Улучшения меняют город и открывают новые места, а достижения — память о твоём пути."
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("story_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6E4))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "📖 Твоя история в городке", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                state.storyLines.forEach { line ->
                    Text(text = "• $line", fontSize = 12.sp, color = ColorInkLight, lineHeight = 15.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Статистика", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                Text(text = "Имя: ${state.playerName.ifEmpty { "—" }}", fontSize = 12.sp, color = ColorInkLight)
                Text(text = "День ${state.day} · серия ${state.streak} дн. · уровень ${state.level}", fontSize = 12.sp, color = ColorInkLight)
                Text(text = "Монеты: ${state.coins} · предметов: ${state.itemsTotal}", fontSize = 12.sp, color = ColorInkLight)
                Text(
                    text = "Собрано ресурсов: ${state.gathersMade} · крафтов: ${state.craftsMade} · " +
                            "подарков: ${state.giftsGiven} · редких находок: ${state.raresFound}" +
                            if (state.petsMade > 0) " · кот обласкан: ${state.petsMade}" else "",
                    fontSize = 12.sp,
                    color = ColorInkLight
                )
                Text(
                    text = "Истории жителей: ${state.questsDone}/${state.questsTotal} · достижения: ${state.achievementsDone}/${state.achievementsTotal}",
                    fontSize = 12.sp,
                    color = ColorInkLight
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { vm.onMusicToggle() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorSky, contentColor = Color.White),
                modifier = Modifier.weight(1f).height(42.dp).testTag("music_button")
            ) {
                Text(if (state.isMusicEnabled) "🎵 Музыка: вкл" else "🔇 Музыка: выкл", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { vm.openPrivacyDialog() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorInkLight, contentColor = Color.White),
                modifier = Modifier.weight(1f).height(42.dp).testTag("privacy_button")
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Данные", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { vm.openReportDialog() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorTerra, contentColor = Color.White),
                modifier = Modifier.weight(1f).height(42.dp).testTag("report_button")
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Сообщить", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text("Улучшения", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorInk)
        state.upgrades.forEach { upgrade ->
            UpgradeRow(upgrade = upgrade, onBuy = { vm.buyUpgrade(upgrade.id) })
        }

        Text("Достижения", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorInk)
        state.achievements.forEach { achievement ->
            AchievementRow(achievement)
        }

        Text(
            text = "Игра работает офлайн, все данные — только на устройстве. NPC помнят тебя и рассказывают друг другу о твоих поступках.",
            fontSize = 11.sp,
            color = ColorInkLight,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun UpgradeRow(upgrade: UpgradeView, onBuy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (upgrade.owned) ColorMint.copy(alpha = 0.15f) else ColorPaperCard
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = upgrade.emoji, fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = upgrade.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                Text(text = upgrade.description, fontSize = 11.sp, color = ColorInkLight, lineHeight = 14.sp)
            }
            if (upgrade.owned) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ColorMint, modifier = Modifier.size(20.dp))
            } else {
                Button(
                    onClick = onBuy,
                    enabled = upgrade.affordable,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorSand,
                        contentColor = Color.White,
                        disabledContainerColor = ColorBorder.copy(alpha = 0.5f),
                        disabledContentColor = ColorInkLight
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp).testTag("upgrade_${upgrade.id}")
                ) {
                    Text("${upgrade.price} 🪙", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: AchievementView) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (achievement.done) ColorMint.copy(alpha = 0.14f) else ColorPaperCard)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = if (achievement.done) "🏆" else "🔒", fontSize = 15.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = achievement.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorInk)
            Text(text = achievement.desc, fontSize = 11.sp, color = ColorInkLight)
        }
        Text(text = "+${achievement.coins}", fontSize = 11.sp, color = ColorSand, fontWeight = FontWeight.Bold)
    }
}

// --------------------------------------------------------------------- журнал

@Composable
private fun JournalStrip(state: GameUiState, onToggle: () -> Unit) {
    val lines = state.journal
    val visible = if (state.journalExpanded) lines.takeLast(12) else lines.takeLast(2)
    Card(
        modifier = Modifier.fillMaxWidth().testTag("journal_panel"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ColorPaperCard)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.journalExpanded) "📜 Журнал городка (свернуть)" else "📜 Журнал городка",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTerra,
                    modifier = Modifier.clickable { onToggle() }
                )
                Text(
                    text = if (state.npcsHere.isNotEmpty()) "здесь: " + state.npcsHere.joinToString(", ") { it.name } else "",
                    fontSize = 10.sp,
                    color = ColorInkLight
                )
            }
            if (state.nextHint.isNotEmpty()) {
                Text(
                    text = "💡 ${state.nextHint}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorMint,
                    lineHeight = 15.sp,
                    modifier = Modifier.testTag("next_hint")
                )
            }
            visible.forEach { line ->
                Text(
                    text = line,
                    fontSize = 12.sp,
                    color = if (line.startsWith("★")) ColorMint else ColorInk,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// --------------------------------------------------------------------- диалоги

@Composable
private fun IntroDialog(name: String, onNameChange: (String) -> Unit, onSubmit: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(text = "Добро пожаловать в городок!", fontWeight = FontWeight.Bold, color = ColorInk)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Марта выглядывает из пекарни: «Привет! Как тебя зовут?»",
                    fontSize = 13.sp,
                    color = ColorInk
                )
                Text(
                    text = "Твоё имя запомнят ВСЕ жители — навсегда. Они будут вспоминать твои поступки и рассказывать друг другу.",
                    fontSize = 12.sp,
                    color = ColorInkLight,
                    lineHeight = 16.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) onNameChange(it) },
                    placeholder = { Text("Твоё имя…") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFFFDF8),
                        unfocusedContainerColor = ColorPaperCard,
                        focusedBorderColor = ColorTerra,
                        unfocusedBorderColor = ColorBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = name.trim().length >= 2,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorTerra, contentColor = Color.White),
                modifier = Modifier.testTag("name_button")
            ) {
                Text("Представиться")
            }
        },
        containerColor = ColorPaperCard
    )
}

@Composable
private fun GiftTargetDialog(
    itemName: String,
    npcs: List<NpcView>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Кому подарить $itemName?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorInk) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                npcs.forEach { npc ->
                    val loves = npc.lovedIds.isNotEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (npc.isHere) ColorMint.copy(alpha = 0.12f) else ColorPaperCard)
                            .clickable { onPick(npc.name) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = npcEmoji(npc.name), fontSize = 18.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = npc.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                            Text(
                                text = "${npc.tierName} · ${if (npc.isHere) "здесь" else npc.placeName}",
                                fontSize = 11.sp,
                                color = ColorInkLight
                            )
                        }
                        if (loves) Text(text = "❤", fontSize = 12.sp, color = ColorTerra)
                    }
                }
                Text(
                    text = "❤ — доверие уже открыло вкусы жителя (подсветка любимых подарков — в карточке жителя).",
                    fontSize = 10.sp,
                    color = ColorInkLight
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_gift_target")) {
                Text("Отмена", color = ColorInk)
            }
        },
        containerColor = ColorPaperCard
    )
}

@Composable
private fun GiftDialog(
    target: String,
    items: List<ItemView>,
    loved: List<String>,
    onDismiss: () -> Unit,
    onGift: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Подарить $target", fontWeight = FontWeight.Bold, color = ColorInk) },
        text = {
            if (items.isEmpty()) {
                Text(text = "Сумка пуста — сначала собери что-нибудь на карте.", fontSize = 13.sp, color = ColorInkLight)
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (loved.isEmpty())
                            "Любимые подарки дают +25% доверия, приятные — +15%. Подружись ближе — и узнаешь вкусы."
                        else
                            "Сердечком помечены любимые подарки (+25% доверия), приятные дают +15%.",
                        fontSize = 11.sp,
                        color = ColorInkLight
                    )
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ColorPaperCard)
                                .clickable { onGift(item.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = item.emoji, fontSize = 18.sp)
                            Text(
                                text = "${item.name} ×${item.count}",
                                fontSize = 13.sp,
                                color = ColorInk,
                                modifier = Modifier.weight(1f)
                            )
                            if (item.id in loved) {
                                Text(text = "любит", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorTerra)
                            }
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (item.id in loved) ColorTerra else ColorBorder,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_gift_button")) {
                Text("Закрыть", color = ColorInk)
            }
        },
        containerColor = ColorPaperCard
    )
}

@Composable
private fun PuzzleDialog(shards: Int, total: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "🧩 Мозаика городка", fontWeight = FontWeight.Bold, color = ColorInk) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MosaicImage(shards = shards, total = total)
                Text(text = "Собрано: $shards из $total осколков", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ColorInk)
                Text(
                    text = "Осколки дают: заказы жителей, истории NPC (2–3 за шаг), достижения и улучшения городка. " +
                            "Когда мозаика соберётся целиком, городок вспомнит о тебе навсегда — и наградит.",
                    fontSize = 12.sp,
                    color = ColorInkLight,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_puzzle_button")) {
                Text("Закрыть", color = ColorInk)
            }
        },
        containerColor = ColorPaperCard
    )
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit, onResetProgress: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Данные и память NPC", fontWeight = FontWeight.Bold, color = ColorInk) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("• Всё хранится только на твоём устройстве, интернет не нужен.", fontSize = 12.sp, color = ColorInk)
                Text("• Память NPC: до 10 свежих событий и важные — надолго.", fontSize = 12.sp, color = ColorInk)
                Text("• Сплетни: жители передают новости друг другу, важность падает.", fontSize = 12.sp, color = ColorInk)
                Text("• Удали приложение — удалится вся история городка.", fontSize = 12.sp, color = ColorInk)
                Text("• Нашёл ошибку или грубость — кнопка «Сообщить».", fontSize = 12.sp, color = ColorInk)
            }
        },
        confirmButton = {
            Button(
                onClick = onResetProgress,
                colors = ButtonDefaults.buttonColors(containerColor = ColorTerra, contentColor = Color.White),
                modifier = Modifier.testTag("reset_progress_button")
            ) {
                Text("Начать заново", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_privacy_button")) {
                Text("Закрыть", color = ColorInk)
            }
        },
        containerColor = ColorPaperCard
    )
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onSubmit: (reason: String, text: String) -> Unit) {
    var selectedReason by remember { mutableStateOf("bug") }
    var reportText by remember { mutableStateOf("") }
    val reasons = listOf(
        "bug" to "Ошибка",
        "rude" to "Грубость",
        "wrong_memory" to "Неверная память",
        "other" to "Другое"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Сообщить о проблеме", fontWeight = FontWeight.Bold, color = ColorInk) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Причина:", fontSize = 12.sp, color = ColorInkLight)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.take(2).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedReason == key,
                            onClick = { selectedReason = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.drop(2).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedReason == key,
                            onClick = { selectedReason = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = reportText,
                    onValueChange = { if (it.length <= 1000) reportText = it },
                    placeholder = { Text("Опиши, что произошло…", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("report_text_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFFFDF8),
                        unfocusedContainerColor = ColorPaperCard,
                        focusedBorderColor = ColorTerra,
                        unfocusedBorderColor = ColorBorder
                    )
                )
                Text("${reportText.length}/1000", fontSize = 11.sp, color = ColorInkLight)
            }
        },
        confirmButton = {
            Button(
                onClick = { if (reportText.isNotBlank()) onSubmit(selectedReason, reportText) },
                enabled = reportText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorMint, contentColor = Color.White),
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Отправить", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_report_button")) {
                Text("Отмена", color = ColorInk)
            }
        },
        containerColor = ColorPaperCard
    )
}
