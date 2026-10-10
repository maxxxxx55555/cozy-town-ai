package com.aistudio.cozytown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cozytown.ui.theme.ColorBorder
import com.aistudio.cozytown.ui.theme.ColorInk
import com.aistudio.cozytown.ui.theme.ColorInkLight
import com.aistudio.cozytown.ui.theme.ColorMint
import com.aistudio.cozytown.ui.theme.ColorPaper
import com.aistudio.cozytown.ui.theme.ColorPaperCard
import com.aistudio.cozytown.ui.theme.ColorSand
import com.aistudio.cozytown.ui.theme.ColorTerra

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Auto scroll journal to bottom
    LaunchedEffect(uiState.journalLines.size) {
        if (uiState.journalLines.isNotEmpty()) {
            listState.animateScrollToItem(uiState.journalLines.size - 1)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ColorPaper
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Header Bar: Title, Time, Coins
            HeaderBar(
                timeString = uiState.timeString,
                coins = uiState.coins
            )

            // 2. Town Map Canvas
            TownMapCanvas(
                npcs = uiState.npcs,
                hour = uiState.hour,
                onNpcClicked = { viewModel.onNpcClicked(it) },
                onPlaceClicked = { viewModel.onPlaceClicked(it) }
            )

            // 3. Cozy Journal Card (Scrollable)
            JournalCard(
                journalLines = uiState.journalLines,
                hint = uiState.journalHint,
                emptyHint = uiState.emptyJournalHint,
                listState = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // 4. Name input row
            NameInputRow(
                playerName = uiState.playerName,
                inputText = uiState.nameInputText,
                isSubmitted = uiState.isNameSubmitted,
                onTextChange = { viewModel.onNameChange(it) },
                onSubmit = { viewModel.onNameSubmitted() }
            )

            // 5. Action Buttons Grid
            ActionsGrid(
                isMusicEnabled = uiState.isMusicEnabled,
                onTalk = { viewModel.onTalk() },
                onHelp = { viewModel.onHelp() },
                onReport = { viewModel.openReportDialog() },
                onPrivacy = { viewModel.openPrivacyDialog() },
                onToggleMusic = { viewModel.onMusicToggle() }
            )
        }
    }

    // Dialogs
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
}

@Composable
private fun HeaderBar(
    timeString: String,
    coins: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ПАЗЛ ИЗ ЖИЗНИ",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = ColorTerra,
            letterSpacing = 0.5.sp,
            modifier = Modifier.testTag("app_title")
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ColorPaperCard)
                .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clearAndSetSemantics {
                    contentDescription = "Время в игре: $timeString"
                }
        ) {
            Text("⏰", fontSize = 12.sp)
            Text(
                text = timeString,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ColorInk,
                modifier = Modifier.testTag("time_label")
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ColorPaperCard)
                .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clearAndSetSemantics {
                    contentDescription = "Монеты: $coins"
                }
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = ColorSand,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$coins",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorInk,
                modifier = Modifier.testTag("coins_label")
            )
        }
    }
}

@Composable
private fun JournalCard(
    journalLines: List<JournalEntry>,
    hint: String,
    emptyHint: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.5.dp, ColorBorder, RoundedCornerShape(14.dp))
            .testTag("journal_panel")
            .semantics {
                contentDescription = "Журнал событий городка"
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ColorPaperCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (journalLines.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyHint,
                            fontSize = 14.sp,
                            color = ColorInkLight,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("journal_list"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(journalLines, key = { it.id }) { entry ->
                            val textColor = when {
                                entry.isHighlight -> Color(0xFF2E7D32)
                                entry.isSystem -> ColorInkLight
                                else -> ColorInk
                            }
                            val textWeight = if (entry.isHighlight) FontWeight.Bold else FontWeight.Normal
                            val textStyle = if (entry.isRecall) FontStyle.Italic else FontStyle.Normal

                            Text(
                                text = entry.text,
                                fontSize = 14.sp,
                                color = textColor,
                                fontWeight = textWeight,
                                fontStyle = textStyle,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hint row
            Text(
                text = hint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTerra,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_hint")
            )
        }
    }
}

@Composable
private fun NameInputRow(
    playerName: String,
    inputText: String,
    isSubmitted: Boolean,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = if (isSubmitted) playerName else inputText,
            onValueChange = onTextChange,
            enabled = !isSubmitted,
            label = { Text("Имя персонажа") },
            placeholder = { Text("Твоё имя…", color = ColorInkLight) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFFDF8),
                unfocusedContainerColor = ColorPaperCard,
                disabledContainerColor = Color(0xFFF3ECE0),
                focusedBorderColor = ColorTerra,
                unfocusedBorderColor = ColorBorder,
                focusedTextColor = ColorInk,
                unfocusedTextColor = ColorInk,
                disabledTextColor = ColorInk
            ),
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .testTag("name_input")
                .semantics {
                    contentDescription = "Поле ввода имени персонажа"
                }
        )

        Button(
            onClick = onSubmit,
            enabled = !isSubmitted && inputText.trim().length >= 2,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorTerra,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFD3C5B5),
                disabledContentColor = Color(0xFF81786C)
            ),
            modifier = Modifier
                .height(52.dp)
                .testTag("name_button")
        ) {
            Text(
                text = "Представиться",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ActionsGrid(
    isMusicEnabled: Boolean,
    onTalk: () -> Unit,
    onHelp: () -> Unit,
    onReport: () -> Unit,
    onPrivacy: () -> Unit,
    onToggleMusic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                text = "Поговорить",
                icon = Icons.Default.Email,
                onClick = onTalk,
                containerColor = ColorMint,
                modifier = Modifier
                    .weight(1f)
                    .testTag("talk_button")
            )
            ActionButton(
                text = "Помочь",
                icon = Icons.Default.Favorite,
                onClick = onHelp,
                containerColor = ColorMint,
                modifier = Modifier
                    .weight(1f)
                    .testTag("help_button")
            )
            ActionButton(
                text = "Сообщить",
                icon = Icons.Default.Warning,
                onClick = onReport,
                containerColor = ColorMint,
                modifier = Modifier
                    .weight(1f)
                    .testTag("report_button")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                text = "Данные",
                icon = Icons.Default.Info,
                onClick = onPrivacy,
                containerColor = Color(0xFF6B8E80),
                modifier = Modifier
                    .weight(1f)
                    .testTag("privacy_button")
            )
            ActionButton(
                text = if (isMusicEnabled) "Музыка: вкл" else "Музыка: выкл",
                icon = Icons.Default.PlayArrow,
                onClick = onToggleMusic,
                containerColor = Color(0xFF6B8E80),
                modifier = Modifier
                    .weight(1f)
                    .testTag("music_button")
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        modifier = modifier.height(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun PrivacyDialog(
    onDismiss: () -> Unit,
    onResetProgress: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "О данных и памяти NPC",
                fontWeight = FontWeight.Bold,
                color = ColorInk
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("• Всё хранится только на твоём устройстве.", color = ColorInk)
                Text("• Приложение не требует интернет и не передаёт данные.", color = ColorInk)
                Text("• Память NPC: твои поступки и разговоры, до 10 событий краткосрочно + важные надолго.", color = ColorInk)
                Text("• Удали приложение — удалятся все сохранения.", color = ColorInk)
                Text("• Вопросы: кнопка «Сообщить».", color = ColorInk)
            }
        },
        confirmButton = {
            Button(
                onClick = onResetProgress,
                colors = ButtonDefaults.buttonColors(containerColor = ColorTerra),
                modifier = Modifier.testTag("reset_progress_button")
            ) {
                Text("Сбросить прогресс")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_privacy_button")
            ) {
                Text("Закрыть", color = ColorInk)
            }
        },
        containerColor = ColorPaperCard
    )
}

@Composable
private fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (reason: String, text: String) -> Unit
) {
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
        title = {
            Text(
                text = "Сообщить о проблеме",
                fontWeight = FontWeight.Bold,
                color = ColorInk
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Причина обращения:", fontSize = 13.sp, color = ColorInkLight)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reasons.forEach { (key, label) ->
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
                    placeholder = { Text("Опиши, что произошло (до 1000 символов)…", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("report_text_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFFFDF8),
                        unfocusedContainerColor = ColorPaperCard,
                        focusedBorderColor = ColorTerra,
                        unfocusedBorderColor = ColorBorder
                    )
                )
                Text(
                    text = "${reportText.length}/1000",
                    fontSize = 11.sp,
                    color = ColorInkLight,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reportText.isNotBlank()) {
                        onSubmit(selectedReason, reportText)
                    }
                },
                enabled = reportText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorMint),
                modifier = Modifier.testTag("submit_report_button")
            ) {
                Text("Отправить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_report_button")
            ) {
                Text("Отмена", color = ColorInk)
            }
        },
        containerColor = ColorPaperCard
    )
}
