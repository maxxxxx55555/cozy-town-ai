package com.aistudio.cozytown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.cozytown.audio.AudioManager
import com.aistudio.cozytown.model.DailyRitual
import com.aistudio.cozytown.model.DaySchedule
import com.aistudio.cozytown.model.DialogueComposer
import com.aistudio.cozytown.model.GameClock
import com.aistudio.cozytown.model.Inventory
import com.aistudio.cozytown.model.NPC
import com.aistudio.cozytown.model.NPCIdentity
import com.aistudio.cozytown.model.NPCMemory
import com.aistudio.cozytown.storage.GameSaveState
import com.aistudio.cozytown.storage.ReportService
import com.aistudio.cozytown.storage.SaveGame
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class JournalEntry(
    val id: Long,
    val text: String,
    val isSystem: Boolean = false,
    val isHighlight: Boolean = false,
    val isRecall: Boolean = false
)

data class GameUiState(
    val day: Int = 1,
    val timeString: String = "День 1, 08:00",
    val hour: Int = 8,
    val coins: Int = 0,
    val playerName: String = "",
    val nameInputText: String = "",
    val isNameSubmitted: Boolean = false,
    val journalLines: List<JournalEntry> = emptyList(),
    val journalHint: String = "Подсказка: введи имя — все жители городка его запомнят.",
    val emptyJournalHint: String = "Здесь появится история городка.\nПознакомься с жителями — и начнётся.",
    val isMusicEnabled: Boolean = true,
    val isPrivacyDialogOpen: Boolean = false,
    val isReportDialogOpen: Boolean = false,
    val reportSubmittedMessage: String? = null,
    val ritualStatusText: String = "",
    val allRitualsDone: Boolean = false,
    val selectedNpc: NPC? = null,
    val npcs: List<NPC> = emptyList()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val saveGame = SaveGame(application)
    private val reportService = ReportService(application)
    val audioManager = AudioManager(application)

    private val clock = GameClock()
    private val ritual = DailyRitual()
    private val inventory = Inventory()
    private val npcsList = mutableListOf<NPC>()

    private var sessionTime: Float = 0f
    private var autosaveTime: Float = 0f
    private var recallShown: Boolean = false
    private var lastTalkTime: Float = -999f
    private var lastRitualLog: String = ""
    private var nextEntryId: Long = 0

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    companion object {
        const val RECALL_DELAY_SEC = 300.0f
        const val AUTOSAVE_SEC = 30.0f
        const val TALK_COOLDOWN = 5.0f
        const val MAX_LOG_LINES = 300
    }

    init {
        spawnTown()
        loadGame()
        if (ritual.resetForDay(clock.day)) {
            logRitual()
        }
        updateIntro()
        updateUiState()
        audioManager.setMusicEnabled(_uiState.value.isMusicEnabled)
        startTicker()
    }

    private fun spawnTown() {
        npcsList.clear()

        // Marta
        val marta = NPC(
            identity = NPCIdentity(
                npcName = "Марта",
                traits = listOf("добрая", "болтливая")
            ),
            memory = NPCMemory(),
            schedule = DaySchedule().apply {
                addSlot(8, "пекарня", "печёт хлеб")
                addSlot(13, "рынок", "покупает цветы")
                addSlot(18, "площадь", "гуляет")
            }
        )
        npcsList.add(marta)

        // Boris
        val boris = NPC(
            identity = NPCIdentity(
                npcName = "Борис",
                traits = listOf("ворчливый")
            ),
            memory = NPCMemory(),
            schedule = DaySchedule().apply {
                addSlot(9, "мастерская", "чинит вещи")
                addSlot(19, "таверна", "читает газету")
            }
        )
        npcsList.add(boris)

        // Luka
        val luka = NPC(
            identity = NPCIdentity(
                npcName = "Лука",
                traits = listOf("мечтательный", "тихий")
            ),
            memory = NPCMemory(),
            schedule = DaySchedule().apply {
                addSlot(10, "причал", "ловит рыбу")
            }
        )
        npcsList.add(luka)

        // Anya
        val anya = NPC(
            identity = NPCIdentity(
                npcName = "Аня",
                traits = listOf("энергичная", "любопытная")
            ),
            memory = NPCMemory(),
            schedule = DaySchedule().apply {
                addSlot(7, "рынок", "торгует цветами")
                addSlot(20, "площадь", "танцует")
            }
        )
        npcsList.add(anya)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L) // 1 real second = 1 game minute
                tick(1.0f)
            }
        }
    }

    private fun tick(delta: Float) {
        sessionTime += delta
        autosaveTime += delta
        clock.advance(delta)

        for (npc in npcsList) {
            npc.tick(clock.hour())
        }

        if (ritual.resetForDay(clock.day)) {
            lastRitualLog = ""
            logMessage("День ${clock.day}. Цели обновлены.", isSystem = true)
            logRitual()
            updateJournalHint()
        }

        if (!recallShown && sessionTime >= RECALL_DELAY_SEC) {
            showRecall()
        }

        if (autosaveTime >= AUTOSAVE_SEC) {
            autosaveTime = 0f
            saveGame()
        }

        updateUiState()
    }

    private fun updateIntro() {
        if (_uiState.value.playerName.isEmpty()) {
            logMessage("Марта: Привет! Как тебя зовут?")
        } else {
            logMessage("Марта: ${npcsList[0].greet(clock.day)}")
        }
    }

    fun onNameChange(text: String) {
        _uiState.update { it.copy(nameInputText = text) }
    }

    fun onNameSubmitted() {
        val sanitized = SaveGame.sanitizeName(_uiState.value.nameInputText)
        if (sanitized.length < 2 || _uiState.value.isNameSubmitted) {
            return
        }
        audioManager.playSfx("click")
        for (npc in npcsList) {
            npc.memory.playerName = sanitized
            npc.memory.addEvent("познакомился(ась) с игроком $sanitized", 8, clock.day, aboutPlayer = true)
        }
        logMessage("Марта: Запомню, $sanitized! Заходи в гости.")
        _uiState.update {
            it.copy(
                playerName = sanitized,
                nameInputText = sanitized,
                isNameSubmitted = true
            )
        }
        updateJournalHint()
        saveGame()
    }

    fun onTalk() {
        if (npcsList.isEmpty()) return
        audioManager.playSfx("click")
        val npc = npcAtHour(clock.hour())
        val spot = npc.schedule.placeAt(clock.hour())
        val dialogue = DialogueComposer.compose(npc, clock.day, clock.hour())
        logMessage(dialogue)
        logMessage("${npc.identity.npcName} (${spot.place})", isSystem = true)

        if (sessionTime - lastTalkTime >= TALK_COOLDOWN) {
            ritual.record("talk")
            lastTalkTime = sessionTime
        }

        val others = othersAt(npc, spot.place)
        if (others.isNotEmpty()) {
            val other = others[0]
            val rel = npc.identity.relationships[other.identity.npcName] ?: 0.0f
            if (rel > 0.5f) {
                logMessage("${npc.identity.npcName} дружелюбно встречает ${other.identity.npcName}.", isSystem = true)
            }
            val gossipLine = npc.gossipWith(other, clock.day)
            logMessage(gossipLine)
            if (npc.memory.recallAboutPlayer().isNotEmpty()) {
                ritual.record("gossip")
            }
        }
        afterEvent()
    }

    fun onHelp() {
        if (npcsList.isEmpty()) return
        audioManager.playSfx("click")
        val npc = npcAtHour(clock.hour())
        val reaction = npc.reactToAction("помог по хозяйству", 5, clock.day)
        logMessage("${npc.identity.npcName}: $reaction")
        ritual.record("kind")
        afterEvent()
    }

    private fun afterEvent() {
        logRitual()
        val reward = ritual.claim()
        if (reward > 0) {
            _uiState.update { it.copy(coins = it.coins + reward) }
            logMessage("Цели дня выполнены! +$reward монет", isHighlight = true)
            audioManager.playSfx("coin")
            audioManager.playSfx("success")
            saveGame()
        }
        updateJournalHint()
        updateUiState()
    }

    private fun logRitual() {
        val parts = ritual.status().map { s ->
            val mark = if (s.done) "☑" else "☐"
            "$mark ${s.text} (${s.current}/${s.target})"
        }
        val line = "Цели дня: " + parts.joinToString(" · ")
        if (line != lastRitualLog) {
            logMessage(line, isSystem = true)
            lastRitualLog = line
        }
    }

    private fun updateJournalHint() {
        val hint = when {
            _uiState.value.playerName.isEmpty() -> "Подсказка: введи имя — все жители городка его запомнят."
            ritual.allDone() -> "Подсказка: цели дня выполнены. Можно вернуться к жителям и узнать новости."
            else -> "Подсказка: поговори с жителями, помоги по хозяйству и узнай свежую сплетню."
        }
        val emptyHint = if (_uiState.value.journalLines.size <= 2) {
            if (_uiState.value.journalLines.isNotEmpty())
                "История уже началась. Здесь появятся новые события."
            else
                "Здесь появится история городка.\nПознакомься с жителями — и начнётся."
        } else ""

        _uiState.update {
            it.copy(
                journalHint = hint,
                emptyJournalHint = emptyHint
            )
        }
    }

    private fun showRecall() {
        recallShown = true
        for (npc in npcsList) {
            val evs = npc.memory.recallAboutPlayer()
            if (evs.isNotEmpty()) {
                logMessage("${npc.identity.npcName} вспоминает: ${evs[0].text}", isRecall = true)
                return
            }
        }
    }

    private fun othersAt(npc: NPC, place: String): List<NPC> {
        return npcsList.filter { it != npc && it.schedule.placeAt(clock.hour()).place == place }
    }

    private fun npcAtHour(hour: Int): NPC {
        for (npc in npcsList) {
            if (npc.schedule.placeAt(hour).place != "home") {
                return npc
            }
        }
        return npcsList[0]
    }

    fun onNpcClicked(npc: NPC) {
        audioManager.playSfx("click")
        _uiState.update { it.copy(selectedNpc = npc) }
        val spot = npc.schedule.placeAt(clock.hour())
        logMessage("Ты подошёл(ла) к ${npc.identity.npcName}. Сейчас в: ${spot.place} (${spot.activity}).", isSystem = true)
        logMessage(DialogueComposer.compose(npc, clock.day, clock.hour()))
    }

    fun onPlaceClicked(placeId: String) {
        audioManager.playSfx("click")
        val npcsHere = npcsList.filter { it.schedule.placeAt(clock.hour()).place == placeId }
        val names = if (npcsHere.isNotEmpty()) npcsHere.joinToString(", ") { it.identity.npcName } else "никого нет"
        logMessage("Место: $placeId. Здесь сейчас: $names.", isSystem = true)
    }

    fun onMusicToggle() {
        val newMusic = !_uiState.value.isMusicEnabled
        audioManager.setMusicEnabled(newMusic)
        _uiState.update { it.copy(isMusicEnabled = newMusic) }
        saveGame()
    }

    fun openPrivacyDialog() {
        audioManager.playSfx("click")
        _uiState.update { it.copy(isPrivacyDialogOpen = true) }
    }

    fun closePrivacyDialog() {
        audioManager.playSfx("click")
        _uiState.update { it.copy(isPrivacyDialogOpen = false) }
    }

    fun openReportDialog() {
        audioManager.playSfx("click")
        _uiState.update { it.copy(isReportDialogOpen = true, reportSubmittedMessage = null) }
    }

    fun closeReportDialog() {
        _uiState.update { it.copy(isReportDialogOpen = false) }
    }

    fun submitReport(reason: String, text: String) {
        val ok = reportService.submit(
            reason = reason,
            text = text,
            contextInfo = mapOf("day" to clock.day.toString(), "time" to clock.timeString())
        )
        if (ok) {
            val total = reportService.count()
            logMessage("Спасибо, сообщение сохранено (всего: $total).", isHighlight = true)
            _uiState.update { it.copy(isReportDialogOpen = false) }
        }
    }

    fun resetProgress() {
        audioManager.playSfx("click")
        saveGame.resetProgress()
        spawnTown()
        clock.day = 1
        clock.totalMinutes = 8 * 60
        inventory.items.clear()
        ritual.resetForDay(clock.day)
        sessionTime = 0f
        autosaveTime = 0f
        recallShown = false
        lastTalkTime = -999f
        lastRitualLog = ""

        _uiState.update {
            it.copy(
                day = clock.day,
                timeString = clock.timeString(),
                hour = clock.hour(),
                coins = 0,
                playerName = "",
                nameInputText = "",
                isNameSubmitted = false,
                journalLines = emptyList(),
                isPrivacyDialogOpen = false,
                selectedNpc = null,
                npcs = npcsList.toList()
            )
        }
        logMessage("Прогресс сброшен. Городок ждёт нового знакомства.", isHighlight = true)
        updateIntro()
        updateJournalHint()
        updateUiState()
    }

    private fun logMessage(text: String, isSystem: Boolean = false, isHighlight: Boolean = false, isRecall: Boolean = false) {
        val entry = JournalEntry(
            id = ++nextEntryId,
            text = text,
            isSystem = isSystem,
            isHighlight = isHighlight,
            isRecall = isRecall
        )
        _uiState.update { current ->
            val updated = current.journalLines.toMutableList().apply {
                add(entry)
                if (size > MAX_LOG_LINES) {
                    removeAt(0)
                }
            }
            current.copy(journalLines = updated)
        }
        updateJournalHint()
    }

    fun saveGame() {
        val state = GameSaveState(
            player_name = _uiState.value.playerName,
            coins = _uiState.value.coins,
            clock = clock.toState(),
            inventory = inventory.toState(),
            ritual = ritual.toState(),
            music_enabled = _uiState.value.isMusicEnabled,
            unlocks = emptyList(),
            npcs = npcsList.map { it.toState() }
        )
        saveGame.saveState(state)
    }

    private fun loadGame() {
        val state = saveGame.loadState() ?: return
        clock.loadState(state.clock)
        inventory.loadState(state.inventory)
        ritual.loadState(state.ritual)

        val pName = state.player_name
        if (pName.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    playerName = pName,
                    nameInputText = pName,
                    isNameSubmitted = true
                )
            }
        }

        _uiState.update {
            it.copy(
                coins = state.coins,
                isMusicEnabled = state.music_enabled
            )
        }

        for (i in 0 until minOf(state.npcs.size, npcsList.size)) {
            npcsList[i].loadState(state.npcs[i])
        }
    }

    private fun updateUiState() {
        _uiState.update {
            it.copy(
                day = clock.day,
                timeString = clock.timeString(),
                hour = clock.hour(),
                npcs = npcsList.toList(),
                allRitualsDone = ritual.allDone()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        saveGame()
        audioManager.release()
    }
}
