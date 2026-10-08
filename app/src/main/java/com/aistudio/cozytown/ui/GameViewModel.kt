package com.aistudio.cozytown.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.cozytown.audio.AudioManager
import com.aistudio.cozytown.core.AchievementView
import com.aistudio.cozytown.core.GameEngine
import com.aistudio.cozytown.core.ItemView
import com.aistudio.cozytown.core.Items
import com.aistudio.cozytown.core.NpcView
import com.aistudio.cozytown.core.PUZZLE_TOTAL
import com.aistudio.cozytown.core.Places
import com.aistudio.cozytown.core.RecipeView
import com.aistudio.cozytown.core.RequestView
import com.aistudio.cozytown.core.SaveCodec
import com.aistudio.cozytown.core.TownEvents
import com.aistudio.cozytown.core.UpgradeView
import com.aistudio.cozytown.core.GoalView
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

data class PlaceChip(
    val id: String,
    val name: String,
    val emoji: String,
    val locked: Boolean,
    val here: Boolean,
    val npcCount: Int
)

data class GameUiState(
    val day: Int = 1,
    val timeString: String = "08:00",
    val hour: Int = 8,
    val coins: Int = 0,
    val level: Int = 1,
    val xp: Int = 0,
    val xpNeed: Int = 60,
    val energy: Int = 10,
    val maxEnergy: Int = 10,
    val streak: Int = 0,
    val shards: Int = 0,
    val puzzleTotal: Int = PUZZLE_TOTAL,

    val playerName: String = "",
    val nameInputText: String = "",
    val isNameSubmitted: Boolean = false,
    val isIntroVisible: Boolean = true,

    val tab: Int = 0,
    val currentPlaceId: String = "площадь",
    val currentPlaceName: String = "Площадь",
    val currentPlaceFlavor: String = "",
    val places: List<PlaceChip> = emptyList(),

    val eventId: String = TownEvents.NONE,
    val eventName: String = "",
    val eventText: String = "",
    val eventActionLabel: String? = null,

    val npcs: List<NpcView> = emptyList(),
    val npcsHere: List<NpcView> = emptyList(),
    val items: List<ItemView> = emptyList(),
    val recipes: List<RecipeView> = emptyList(),
    val requests: List<RequestView> = emptyList(),
    val upgrades: List<UpgradeView> = emptyList(),
    val achievements: List<AchievementView> = emptyList(),
    val goals: List<GoalView> = emptyList(),
    val goalsClaimed: Boolean = false,
    val questsDone: Int = 0,
    val questsTotal: Int = 0,
    val achievementsDone: Int = 0,
    val achievementsTotal: Int = 0,
    val itemsTotal: Int = 0,

    val journal: List<String> = emptyList(),
    val journalExpanded: Boolean = false,

    val isMusicEnabled: Boolean = true,
    val isPrivacyDialogOpen: Boolean = false,
    val isReportDialogOpen: Boolean = false,
    val isPuzzleDialogOpen: Boolean = false,
    val giftTarget: String? = null,
    val pickTargetFor: String? = null,
    val giftTargetLoved: List<String> = emptyList(),
    val toast: String? = null,

    val nextHint: String = "",
    val tabBadges: List<Int> = listOf(0, 0, 0, 0, 0, 0),

    val raresFound: Int = 0,
    val giftsGiven: Int = 0,
    val craftsMade: Int = 0,
    val gathersMade: Int = 0,
    val petsMade: Int = 0,
    val storyLines: List<String> = emptyList(),
    val upgradeIds: List<String> = emptyList()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val saveGame = SaveGame(application)
    private val reportService = ReportService(application)
    val audioManager = AudioManager(application)

    val engine = GameEngine(seed = System.currentTimeMillis() and 0xFFFFFF)

    private var tickerJob: Job? = null
    private var autosaveTime = 0f
    private var heavyDirty = true
    private var lastCoins = 0
    private var lastLevel = 1
    private var lastAchievements = 0
    private var lastUpgrades = 0

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    companion object {
        const val AUTOSAVE_SEC = 20f
        const val MAX_JOURNAL_SHOWN = 200
    }

    init {
        val loaded = loadGame()
        if (!loaded) {
            engine.newGame()
        } else {
            engine.onLoaded(System.currentTimeMillis())
        }
        audioManager.setMusicEnabled(engine.musicEnabled)
        lastCoins = engine.coins
        lastLevel = engine.player.level
        lastAchievements = engine.achievements.size
        lastUpgrades = engine.upgrades.size
        refresh(heavy = true)
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L) // 1 реальная секунда = 1 игровая минута
                engine.tick(1f)
                autosaveTime += 1f
                if (autosaveTime >= AUTOSAVE_SEC) {
                    autosaveTime = 0f
                    saveGame()
                }
                if (engine.coins != lastCoins || engine.player.level != lastLevel) {
                    heavyDirty = true
                }
                refresh(heavy = heavyDirty)
            }
        }
    }

    // ------------------------------------------------------------------ refresh
    private fun refresh(heavy: Boolean) {
        val e = engine
        val place = Places.byId(e.currentPlace)
        val views = if (heavy) e.npcViews() else _uiState.value.npcs
        val event = TownEvents.byId(e.eventId)
        val here = views.filter { it.isHere }

        if (heavy) {
            heavyDirty = false
            lastAchievements = e.achievements.size
        }

        val chips = Places.ALL.map { p ->
            PlaceChip(
                id = p.id,
                name = p.name,
                emoji = placeEmoji(p.id),
                locked = p.requiresUpgrade != null && p.requiresUpgrade !in e.upgrades,
                here = p.id == e.currentPlace,
                npcCount = views.count { it.place == p.id }
            )
        }

        _uiState.update { st ->
            st.copy(
                day = e.clock.day,
                timeString = "%02d:%02d".format(e.clock.hour(), e.clock.minute()),
                hour = e.clock.hour(),
                coins = e.coins,
                level = e.player.level,
                xp = e.player.xp,
                xpNeed = e.player.xpNeed(),
                energy = e.player.energy,
                maxEnergy = e.player.maxEnergy(),
                streak = e.player.streak,
                shards = e.shards,
                playerName = e.playerName,
                isNameSubmitted = e.playerName.isNotEmpty(),
                isIntroVisible = e.playerName.isEmpty(),
                currentPlaceId = e.currentPlace,
                currentPlaceName = place?.name ?: e.currentPlace,
                currentPlaceFlavor = place?.flavor ?: "",
                places = chips,
                eventId = e.eventId,
                eventName = event?.name ?: "",
                eventText = event?.log ?: "",
                eventActionLabel = when {
                    e.eventId == "cat_visit" && e.pettedDay != e.clock.day -> "Погладить кота"
                    e.eventId == "merchant" -> "К торговцу"
                    else -> null
                },
                npcs = views,
                npcsHere = here,
                items = if (heavy) e.itemViews() else st.items,
                recipes = if (heavy) e.recipeViews() else st.recipes,
                requests = if (heavy) e.requestViews() else st.requests,
                upgrades = if (heavy) e.upgradeViews() else st.upgrades,
                achievements = if (heavy) e.achievementViews() else st.achievements,
                goals = e.goalViews(),
                goalsClaimed = e.ritual.claimed,
                questsDone = e.counters.questsDone,
                questsTotal = 6,
                achievementsDone = e.achievements.size,
                achievementsTotal = e.achievementViews().size,
                itemsTotal = e.inventory.items.values.sum(),
                journal = e.journal.takeLast(MAX_JOURNAL_SHOWN).map { lineText(it) },
                isMusicEnabled = e.musicEnabled,
                nextHint = if (heavy) nextHint(e) else st.nextHint,
                tabBadges = if (heavy) computeBadges(e) else st.tabBadges,
                raresFound = if (heavy) e.counters.rares else st.raresFound,
                giftsGiven = if (heavy) e.counters.gifts else st.giftsGiven,
                craftsMade = if (heavy) e.counters.crafts else st.craftsMade,
                gathersMade = if (heavy) e.counters.gathers else st.gathersMade,
                petsMade = if (heavy) e.counters.pets else st.petsMade,
                storyLines = if (heavy) storyLines(e, views) else st.storyLines,
                upgradeIds = if (heavy) e.upgrades.toList() else st.upgradeIds
            )
        }
    }

    /** Подсказка «что делать дальше» — чтобы игрок никогда не терялся. */
    private fun nextHint(e: GameEngine): String {
        if (e.playerName.isEmpty()) return "Введи имя — его запомнят все жители."
        if (e.requestViews().any { it.canDo }) {
            val r = e.requestViews().first { it.canDo }
            return "Заказ ${r.npc} готов: ${r.itemName} ×${r.need} — загляни в «Дела»."
        }
        val readyQuest = e.npcViews().firstOrNull { it.questReady }
        if (readyQuest != null) return "У ${readyQuest.name} готова часть истории ($readyQuest.questTitle)."
        if (e.ritual.allDone() && !e.ritual.claimed) return "Цели дня выполнены — забери 25 монет в «Делах»."
        val undoneGoal = e.goalViews().firstOrNull { !it.done }
        if (undoneGoal != null) return "Цель дня: ${undoneGoal.text} (${undoneGoal.current}/${undoneGoal.target})."
        val affordable = e.upgradeViews().firstOrNull { !it.owned && it.affordable }
        if (affordable != null) return "Хватает монет на «${affordable.name}» — раздел «Город»."
        if (e.player.energy == 0) return "Силы на исходе: отдохни в доме или загляни позже."
        return "Собери ресурсы, поговори с жителями или помоги кому-нибудь."
    }

    /** Личная история игрока в городке — то, чем приятно поделиться. */
    private fun storyLines(e: GameEngine, views: List<NpcView>): List<String> {
        val friends = views.filter { it.tier >= 1 }.sortedByDescending { it.trust }
        val friendNames = friends.take(3).joinToString(", ") { "${it.name} (${it.tierName.lowercase()})" }
        val topItem = e.inventory.items.entries
            .mapNotNull { (id, cnt) -> Items.byId(id)?.let { def -> def to cnt } }
            .maxByOrNull { it.first.rarity * 100 + it.second }
        return listOf(
            "День ${e.clock.day}, серия ${e.player.streak} дн., уровень ${e.player.level}.",
            "Друзья: " + (friendNames.ifEmpty { "пока только знакомые — подари кому-нибудь любимый предмет" }),
            "Историй жителей рассказано: ${e.counters.questsDone} из 6.",
            "Редких находок: ${e.counters.rares} · подарков: ${e.counters.gifts} · собрано ресурсов: ${e.counters.gathers}.",
            "В сумке редкость: " + (topItem?.first?.let { "${it.emoji} ${it.name}" } ?: "—"),
            "Мозаика городка: ${e.shards} из $PUZZLE_TOTAL осколков."
        )
    }

    /** Числа-подсказки на вкладках: где сейчас есть готовое действие. */
    private fun computeBadges(e: GameEngine): List<Int> {
        val questsReady = e.npcViews().count { it.questReady }
        val craftable = e.recipeViews().count { it.canCraft }
        val requestsReady = e.requestViews().count { it.canDo } + if (e.ritual.allDone() && !e.ritual.claimed) 1 else 0
        val upgradesReady = e.upgradeViews().count { !it.owned && it.affordable }
        return listOf(0, questsReady, 0, craftable, requestsReady, upgradesReady)
    }

    private fun lineText(line: com.aistudio.cozytown.core.JournalLine): String {
        return when {
            line.highlight -> "★ ${line.text}"
            line.recall -> "… ${line.text}"
            else -> line.text
        }
    }

    private fun placeEmoji(id: String): String = when (id) {
        "home" -> "🏠"
        "пекарня" -> "🥐"
        "рынок" -> "🧺"
        "мастерская" -> "🔨"
        "площадь" -> "⛲"
        "таверна" -> "🍵"
        "причал" -> "⛵"
        "сад" -> "🌱"
        "маяк" -> "🗼"
        else -> "📍"
    }

    /** Звуки и тосты по результату действия. */
    private fun afterAction(playClick: Boolean = true, prevCoins: Int = -1) {
        if (playClick) audioManager.playSfx("click")
        val e = engine
        if (prevCoins >= 0 && e.coins > prevCoins) audioManager.playSfx("coin")
        if (e.player.level > lastLevel) {
            audioManager.playSfx("success")
            _uiState.update { it.copy(toast = "Уровень ${e.player.level}! Силы восстановлены.") }
            saveGame()
        }
        if (e.achievements.size > lastAchievements) {
            audioManager.playSfx("success")
        }
        val claimed = e.ritual.claimed
        if (claimed && !_uiState.value.goalsClaimed) {
            audioManager.playSfx("coin")
            saveGame()
        }
        if (e.upgrades.size != lastUpgrades) {
            lastUpgrades = e.upgrades.size
            saveGame()
        }
        val lastLine = e.journal.lastOrNull()?.text
        val milestone = lastLine != null &&
            (lastLine.startsWith("🏆") || lastLine.startsWith("📖") || lastLine.startsWith("🧩"))
        if (milestone) {
            audioManager.playSfx("success")
            // важные вехи сохраняем сразу, не дожидаясь автосейва
            saveGame()
        }
        lastCoins = e.coins
        lastLevel = e.player.level
        lastAchievements = e.achievements.size
        heavyDirty = true
        refresh(heavy = true)
    }

    // ------------------------------------------------------------------ действия
    fun onNameChange(text: String) {
        _uiState.update { it.copy(nameInputText = text) }
    }

    fun onNameSubmitted() {
        val ok = engine.setName(_uiState.value.nameInputText)
        if (!ok) {
            _uiState.update { it.copy(toast = "Имя должно быть от 2 до 24 символов.") }
            return
        }
        audioManager.playSfx("success")
        _uiState.update { it.copy(isIntroVisible = false, tab = 0) }
        afterAction(playClick = false)
        saveGame()
    }

    fun selectTab(index: Int) {
        audioManager.playSfx("click")
        _uiState.update { it.copy(tab = index) }
    }

    fun moveTo(placeId: String) {
        if (engine.moveTo(placeId)) {
            audioManager.playSfx("click")
            afterAction(playClick = false)
        } else {
            audioManager.playSfx("click")
            refresh(heavy = false)
        }
    }

    fun gather() {
        val coins = engine.coins
        engine.gather()
        afterAction(prevCoins = coins)
    }

    fun rest() {
        val coins = engine.coins
        engine.rest()
        afterAction(prevCoins = coins)
    }

    fun petCat() {
        val coins = engine.coins
        engine.petCat()
        afterAction(prevCoins = coins)
    }

    fun openMerchant() {
        audioManager.playSfx("click")
        _uiState.update { it.copy(tab = 4, toast = "Торговец разложил товар: жемчуг и янтарь — в разделе «Дела».") }
    }

    fun talkTo(npcName: String) {
        val coins = engine.coins
        engine.talkTo(npcName)
        afterAction(prevCoins = coins)
    }

    fun helpNpc(npcName: String) {
        val coins = engine.coins
        engine.helpNpc(npcName)
        afterAction(prevCoins = coins)
    }

    fun openGift(npcName: String) {
        audioManager.playSfx("click")
        val loved = _uiState.value.npcs.firstOrNull { it.name == npcName }?.lovedIds ?: emptyList()
        _uiState.update { it.copy(giftTarget = npcName, giftTargetLoved = loved) }
    }

    /** Выбор, кому подарить предмет из сумки. */
    fun openGiftTargetPicker(itemId: String) {
        audioManager.playSfx("click")
        _uiState.update { it.copy(pickTargetFor = itemId) }
    }

    fun closeGiftTargetPicker() {
        _uiState.update { it.copy(pickTargetFor = null) }
    }

    fun giftToNpc(npcName: String) {
        val itemId = _uiState.value.pickTargetFor
        _uiState.update { it.copy(pickTargetFor = null) }
        if (itemId == null) return
        val coins = engine.coins
        if (engine.gift(npcName, itemId)) {
            afterAction(playClick = false, prevCoins = coins)
        } else {
            audioManager.playSfx("click")
            _uiState.update { it.copy(toast = "Не получилось подарить — предмета нет в сумке.") }
        }
    }

    fun closeGift() {
        _uiState.update { it.copy(giftTarget = null, giftTargetLoved = emptyList()) }
    }

    fun gift(itemId: String) {
        val target = _uiState.value.giftTarget ?: return
        val coins = engine.coins
        if (engine.gift(target, itemId)) {
            _uiState.update { it.copy(giftTarget = null, giftTargetLoved = emptyList()) }
            afterAction(prevCoins = coins)
        } else {
            audioManager.playSfx("click")
            _uiState.update { it.copy(toast = "Этого предмета нет в сумке.") }
        }
    }

    fun craft(recipeId: String) {
        val coins = engine.coins
        val ok = engine.craft(recipeId)
        if (ok) audioManager.playSfx("success")
        afterAction(playClick = !ok, prevCoins = coins)
    }

    fun fulfillRequest(index: Int) {
        val coins = engine.coins
        engine.fulfillRequest(index)
        afterAction(prevCoins = coins)
    }

    fun sellItem(itemId: String) {
        val coins = engine.coins
        engine.sellItem(itemId)
        afterAction(prevCoins = coins)
    }

    fun buyUpgrade(id: String) {
        val coins = engine.coins
        if (engine.buyUpgrade(id)) {
            audioManager.playSfx("success")
        }
        afterAction(playClick = false, prevCoins = coins)
    }

    fun buyMerchant(itemId: String) {
        val coins = engine.coins
        engine.buyFromMerchant(itemId)
        afterAction(prevCoins = coins)
    }

    fun advanceQuest(npcName: String) {
        val coins = engine.coins
        if (engine.advanceQuest(npcName)) {
            audioManager.playSfx("success")
        }
        afterAction(playClick = false, prevCoins = coins)
    }

    fun claimGoals() {
        val coins = engine.coins
        engine.claimRitual()
        afterAction(playClick = false, prevCoins = coins)
    }

    fun toggleJournal() {
        _uiState.update { it.copy(journalExpanded = !it.journalExpanded) }
    }

    fun openPuzzle() {
        audioManager.playSfx("click")
        _uiState.update { it.copy(isPuzzleDialogOpen = true) }
    }

    fun closePuzzle() {
        _uiState.update { it.copy(isPuzzleDialogOpen = false) }
    }

    fun onMusicToggle() {
        val newValue = !engine.musicEnabled
        engine.musicEnabled = newValue
        audioManager.setMusicEnabled(newValue)
        saveGame()
        refresh(heavy = false)
    }

    fun openPrivacyDialog() {
        audioManager.playSfx("click")
        _uiState.update { it.copy(isPrivacyDialogOpen = true) }
    }

    fun closePrivacyDialog() {
        _uiState.update { it.copy(isPrivacyDialogOpen = false) }
    }

    fun openReportDialog() {
        audioManager.playSfx("click")
        _uiState.update { it.copy(isReportDialogOpen = true) }
    }

    fun closeReportDialog() {
        _uiState.update { it.copy(isReportDialogOpen = false) }
    }

    fun submitReport(reason: String, text: String) {
        val ok = reportService.submit(
            reason = reason,
            text = text,
            contextInfo = mapOf(
                "day" to engine.clock.day.toString(),
                "time" to "%02d:%02d".format(engine.clock.hour(), engine.clock.minute()),
                "place" to engine.currentPlace
            )
        )
        if (ok) {
            _uiState.update { it.copy(isReportDialogOpen = false, toast = "Спасибо, сообщение сохранено локально.") }
            audioManager.playSfx("success")
        }
    }

    fun resetProgress() {
        saveGame.resetProgress()
        engine.newGame()
        lastCoins = 0
        lastLevel = 1
        lastAchievements = 0
        lastUpgrades = 0
        saveGame()
        audioManager.playSfx("success")
        _uiState.update { it.copy(isPrivacyDialogOpen = false, tab = 0, toast = "Новая история начинается.") }
        refresh(heavy = true)
    }

    fun consumeToast() {
        _uiState.update { it.copy(toast = null) }
    }

    fun saveGame() {
        val data = SaveCodec.encode(engine.toState())
        saveGame.saveRaw(data)
    }

    /** Сворачивание: останавливаем игровое время, сохраняемся. */
    fun onAppPaused() {
        tickerJob?.cancel()
        tickerJob = null
        engine.lastSeenMs = System.currentTimeMillis()
        saveGame()
    }

    /** Возврат в игру: начисляем оффлайн-заботу и снова запускаем время. */
    fun onAppResumed() {
        engine.onLoaded(System.currentTimeMillis())
        refresh(heavy = true)
        if (tickerJob == null) startTicker()
    }

    private fun loadGame(): Boolean {
        val raw = saveGame.loadRaw() ?: return false
        val state = SaveCodec.parse(raw.first, raw.second) ?: return false
        engine.loadState(state)
        if (engine.playerName.isEmpty()) return false
        return true
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        engine.lastSeenMs = System.currentTimeMillis()
        saveGame()
        audioManager.release()
    }
}
