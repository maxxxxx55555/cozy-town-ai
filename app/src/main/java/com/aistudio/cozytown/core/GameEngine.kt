package com.aistudio.cozytown.core

import com.aistudio.cozytown.model.DailyRitual
import com.aistudio.cozytown.model.GoalDef
import com.aistudio.cozytown.model.DaySchedule
import com.aistudio.cozytown.model.DialogueComposer
import com.aistudio.cozytown.model.GameClock
import com.aistudio.cozytown.model.Gossip
import com.aistudio.cozytown.model.Inventory
import com.aistudio.cozytown.model.NPC
import com.aistudio.cozytown.model.NPCIdentity
import com.aistudio.cozytown.model.NPCMemory
import com.aistudio.cozytown.model.ScheduleSlot
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Serializable
data class JournalLine(
    val id: Long,
    val text: String,
    val system: Boolean = false,
    val highlight: Boolean = false,
    val recall: Boolean = false
)

@Serializable
data class ActiveRequest(
    val npc: String,
    val text: String,
    val item: String,
    val count: Int,
    val coins: Int,
    val xp: Int
)

@Serializable
data class Counters(
    var meets: Int = 0,
    var talks: Int = 0,
    var helps: Int = 0,
    var gifts: Int = 0,
    var crafts: Int = 0,
    var gathers: Int = 0,
    var gossips: Int = 0,
    var requests: Int = 0,
    var rares: Int = 0,
    var questsDone: Int = 0,
    var pets: Int = 0
)

@Serializable
data class EngineState(
    val version: Int = 3,
    val playerName: String = "",
    val coins: Int = 0,
    val clock: com.aistudio.cozytown.model.GameClockState = com.aistudio.cozytown.model.GameClockState(),
    val inventory: com.aistudio.cozytown.model.InventoryState = com.aistudio.cozytown.model.InventoryState(),
    val ritual: com.aistudio.cozytown.model.DailyRitualState = com.aistudio.cozytown.model.DailyRitualState(),
    val player: PlayerState = PlayerState(),
    val rng: RngState = RngState(),
    val npcs: List<com.aistudio.cozytown.model.NPCState> = emptyList(),
    val counters: Counters = Counters(),
    val achievements: List<String> = emptyList(),
    val upgrades: List<String> = emptyList(),
    val questStep: Map<String, Int> = emptyMap(),
    val questTalkBase: Map<String, Int> = emptyMap(),
    val npcTalks: Map<String, Int> = emptyMap(),
    val requests: List<ActiveRequest> = emptyList(),
    val requestsDay: Int = 0,
    val shards: Int = 0,
    val currentPlace: String = "площадь",
    val eventId: String = TownEvents.NONE,
    val eventDay: Int = 0,
    val dailyGoals: List<com.aistudio.cozytown.model.GoalDef> = emptyList(),
    val pettedDay: Int = 0,
    val merchantPearlDay: Int = 0,
    val merchantAmberDay: Int = 0,
    val firstRequestDay: Int = 0,
    val musicEnabled: Boolean = true,
    val lastSeenMs: Long = 0L,
    val journal: List<JournalLine> = emptyList(),
    val nextEntryId: Long = 0L
)

/**
 * Ядро игры: время, NPC, энергия, заказы, крафт, подарки, квесты,
 * достижения, апгрейды, события, сплетни, мозаика-пазл, оффлайн-прогресс.
 * Чистый Kotlin — без Android, тестируется headless.
 */
class GameEngine(seed: Long = 42L) {

    val clock = GameClock()
    val player = Player()
    val inventory = Inventory()
    val ritual = DailyRitual()
    val rng = Rng(seed)
    val npcs = mutableListOf<NPC>()
    val counters = Counters()
    val achievements = mutableSetOf<String>()
    val upgrades = mutableSetOf<String>()
    val questStep = mutableMapOf<String, Int>()
    private val questTalkBase = mutableMapOf<String, Int>()
    private val npcTalks = mutableMapOf<String, Int>()
    val requests = mutableListOf<ActiveRequest>()
    var requestsDay: Int = 0
    var shards: Int = 0
    var currentPlace: String = "площадь"
    var eventId: String = TownEvents.NONE
    var eventDay: Int = 0
    var pettedDay: Int = 0
    var merchantPearlDay: Int = 0
    var merchantAmberDay: Int = 0
    private var firstRequestDay: Int = 0
    private val lastTalkMinute = mutableMapOf<String, Int>()
    var playerName: String = ""
    var coins: Int = 0
    var musicEnabled: Boolean = true
    var lastSeenMs: Long = 0L

    val journal = mutableListOf<JournalLine>()
    private var nextEntryId: Long = 0L

    private var lastRegenAbsHour: Int = 0
    private var lastHour: Int = 8

    init {
        newGame()
    }

    private fun spawnTown() {
        npcs.clear()
        for (def in NpcDefs.ALL) {
            val sched = DaySchedule()
            for ((h, p, a) in def.schedule) sched.addSlot(h, p, a)
            npcs.add(
                NPC(
                    identity = NPCIdentity(npcName = def.name, traits = def.traits),
                    memory = NPCMemory(),
                    schedule = sched
                )
            )
        }
    }

    fun npc(name: String): NPC? = npcs.find { it.identity.npcName == name }

    /**
     * Где житель на самом деле сейчас: закрытые для игрока места (сад, маяк)
     * заменяются на доступную площадь, чтобы игрок не упирался в тупик.
     */
    fun placeOf(n: NPC): ScheduleSlot {
        val slot = n.schedule.placeAt(clock.hour())
        val def = Places.byId(slot.place)
        val locked = def?.requiresUpgrade != null && def.requiresUpgrade !in upgrades
        return if (locked) ScheduleSlot(slot.hour, "площадь", "гуляет по площади") else slot
    }

    // ------------------------------------------------------------------ журнал
    fun log(text: String, system: Boolean = false, highlight: Boolean = false, recall: Boolean = false) {
        journal.add(JournalLine(++nextEntryId, text, system, highlight, recall))
        while (journal.size > MAX_JOURNAL) journal.removeAt(0)
    }

    // ------------------------------------------------------------------ время
    fun tick(deltaSec: Float) {
        val prevDay = clock.day
        clock.advance(deltaSec)

        for (n in npcs) n.tick(clock.hour())

        if (clock.hour() != lastHour) {
            onHourChanged()
            lastHour = clock.hour()
        }

        if (clock.day != prevDay) {
            onDayChanged(prevDay)
        }

        // реген сил: +1 каждые 2 игровых часа
        val absHour = clock.day * 24 + clock.hour()
        while (absHour - lastRegenAbsHour >= 2) {
            lastRegenAbsHour += 2
            player.regen(1)
        }
    }

    private fun onHourChanged() {
        // авто-сплетни: в местах, где встретились двое
        val byPlace = npcs.groupBy { placeOf(it).place }
        for ((place, group) in byPlace) {
            if (group.size >= 2) {
                val p = 0.3f + (if ("fountain" in upgrades) 0.2f else 0f) + (if (eventId == "festival") 0.15f else 0f)
                if (rng.chance(p)) {
                    val a = group[0]
                    val b = group[1]
                    val line = a.gossipWith(b, clock.day)
                    log(line, system = true)
                    counters.gossips += 1
                    ritual.record("gossip")
                }
            }
        }
    }

    private fun onDayChanged(prevDay: Int) {
        val day = clock.day
        log("День $day. Городок просыпается.", system = true)

        // серия возвращений
        if (player.lastActiveDay < day) {
            player.streak = if (player.lastActiveDay == day - 1) player.streak + 1 else 1
            player.lastActiveDay = day
            if (player.streak >= 2) {
                val bonus = 10 * min(player.streak, 7)
                addCoins(bonus)
                log("Серия: $player.streak дн. подряд! +$bonus монет.", highlight = true)
            }
        }

        // ратуша: городок расцвёл
        if ("townhall" in upgrades) {
            addCoins(10)
            log("Ратуша в порядке: +10 монет в городскую копилку.", system = true)
        }

        // традиция Марты
        if (questDone("Марта")) {
            addCoins(5)
            log("Кусочек пирога Марты с ярмарки: +5 монет.", system = true)
        }

        rollEvent()
        rollGoals()
        rollRequests()
        checkAchievements()
    }

    private fun rollEvent() {
        val pool = listOf(
            "quiet" to 20, "rain" to 15, "market_day" to 15, "cat_visit" to 12,
            "festival" to 10, "merchant" to 12, "storm" to 8
        )
        var total = 0
        for ((_, w) in pool) total += w
        var r = rng.nextInt(total)
        var chosen = "quiet"
        for ((id, w) in pool) {
            if (r < w) { chosen = id; break }
            r -= w
        }
        eventId = chosen
        eventDay = clock.day
        TownEvents.byId(chosen)?.let { log("☀ ${it.name}: ${it.log}", system = true) }
    }

    private fun rollGoals() {
        val pool = DailyGoalPool.ALL.shuffled(java.util.Random(rng.nextLong()))
        val chosen = pool.take(3).map { GoalDef(it.id, it.text, it.target) }
        ritual.resetForDay(clock.day, chosen)
        log("Цели дня: " + chosen.joinToString(" · ") { it.text }, system = true)
    }

    private fun rollRequests() {
        requests.clear()
        requestsDay = clock.day
        val pool = RequestTemplates.ALL.shuffled(java.util.Random(rng.nextLong()))
        val chosen = pool.take(3)
        for (t in chosen) {
            val text = String.format(t.text, Items.byId(t.item)?.accusative() ?: t.item)
            requests.add(ActiveRequest(t.npc, text, t.item, t.count, t.coins, t.xp))
        }
    }

    // ------------------------------------------------------------------ действия
    fun setName(rawName: String): Boolean {
        val name = TextSafe.sanitizeName(rawName)
        if (name.length < 2 || playerName.isNotEmpty()) return false
        playerName = name
        for (n in npcs) {
            n.memory.playerName = name
            n.memory.addEvent("познакомился(ась) с игроком $name", 8, clock.day, aboutPlayer = true)
        }
        counters.meets += 1
        log("Марта: Запомню, $name! Теперь все в городке знают твоё имя.", highlight = true)
        checkAchievements()
        return true
    }

    fun moveTo(placeId: String): Boolean {
        val def = Places.byId(placeId) ?: return false
        if (def.requiresUpgrade != null && def.requiresUpgrade !in upgrades) {
            log("Сюда пока не пройти — место ещё не обустроено.", system = true)
            return false
        }
        if (currentPlace == placeId) return true
        currentPlace = placeId
        log("Ты идёшь: ${def.name}. ${def.flavor}", system = true)
        return true
    }

    fun gather(): Boolean {
        val def = Places.byId(currentPlace) ?: return false
        if (def.id == "home") {
            rest()
            return true
        }
        if (def.yields.isEmpty()) return false
        if (eventId == "storm" && def.id == "причал") {
            log("Волны слишком шумные — сегодня у причала делать нечего.", system = true)
            return false
        }
        if (!player.spendEnergy(1)) {
            log("Силы на исходе. Отдохни или подожди — они возвращаются.", system = true)
            return false
        }
        var got = rng.pickItem(def.yields)
        var extra = 0
        if (def.id == "причал" && eventId == "rain" && got == "fish") extra += 1
        if (def.id == "причал" && "ferry" in upgrades && rng.chance(0.35f)) extra += 1
        if (def.id == "рынок" && "flowerbeds" in upgrades && rng.chance(0.3f)) extra += 1
        if (def.id == "рынок" && questDone("Аня")) extra += 1

        inventory.addItem(got, 1 + extra)
        counters.gathers += 1 + extra
        gainXp(2)
        val item = Items.byId(got)
        log("Собрано: ${item?.emoji} ${item?.name}" + if (extra > 0) " ×${1 + extra}" else "", system = true)

        // редкая находка
        if (def.rareYields.isNotEmpty() && rng.chance(rareChance())) {
            val rare = rng.pickItem(def.rareYields)
            inventory.addItem(rare, 1)
            counters.rares += 1
            counters.gathers += 1
            val ritem = Items.byId(rare)
            log("✨ Редкая находка: ${ritem?.emoji} ${ritem?.name}!", highlight = true)
        }
        ritual.record("gather")
        checkAchievements()
        return true
    }

    private fun rareChance(): Float {
        var p = 0.05f
        if ("cat" in upgrades) p += 0.04f
        if ("telescope" in upgrades) p += 0.05f
        if (eventId == "cat_visit") p += 0.04f
        if (questDone("Борис")) p += 0.04f
        if (questDone("Лука")) p += 0.03f
        return min(p, 0.25f)
    }

    fun talkTo(npcName: String): String? {
        val n = npc(npcName) ?: return null
        val spot = placeOf(n)
        if (spot.place != currentPlace) {
            log("${n.identity.npcName} сейчас в «${Places.byId(spot.place)?.name ?: spot.place}». Загляни туда.", system = true)
            return null
        }
        val def = NpcDefs.byName(npcName)
        val tier = TrustTiers.tier(n.identity.trust)
        val line = DialogueComposer.compose(n, clock.day, clock.hour(), tier, eventId)
        log("${npcName}: $line")
        log("(${def?.bio ?: ""})", system = true)

        // анти-спам: повторный разговор с тем же жителем в течение 15 игровых минут не идёт в зачёт
        val nowMinute = clock.day * 24 * 60 + clock.totalMinutes
        val last = lastTalkMinute[npcName] ?: Int.MIN_VALUE / 2
        val repeated = nowMinute - last < TALK_COOLDOWN_MINUTES
        if (repeated) {
            log("$npcName: Мы только что болтали — загляни попозже или займись делом.", system = true)
        } else {
            lastTalkMinute[npcName] = nowMinute
            counters.talks += 1
            npcTalks[npcName] = (npcTalks[npcName] ?: 0) + 1
            if (eventId == "festival") n.identity.trust = (n.identity.trust + 0.02f).coerceIn(-1f, 1f)
            gainXp(3)
            ritual.record("talk")
        }

        // сплетня, если рядом партнёр (засчитывается один раз за тот же интервал)
        val others = npcs.filter { it !== n && placeOf(it).place == currentPlace }
        if (others.isNotEmpty()) {
            val other = others[0]
            val gossipLine = n.gossipWith(other, clock.day)
            log(gossipLine, system = true)
            if (!repeated) {
                counters.gossips += 1
                ritual.record("gossip")
            }
        }
        checkAchievements()
        return line
    }

    fun helpNpc(npcName: String): Boolean {
        val n = npc(npcName) ?: return false
        val spot = placeOf(n)
        if (spot.place != currentPlace) {
            log("${n.identity.npcName} сейчас не здесь — загляни в «${Places.byId(spot.place)?.name ?: spot.place}».", system = true)
            return false
        }
        if (!player.spendEnergy(2)) {
            log("Нужно 2 силы, чтобы помочь. Отдохни немного.", system = true)
            return false
        }
        val reaction = n.reactToAction("помог по хозяйству", 1, clock.day)
        log("${npcName}: $reaction")
        counters.helps += 1
        gainXp(5)
        ritual.record("kind")
        checkAchievements()
        return true
    }

    fun gift(npcName: String, itemId: String): Boolean {
        val n = npc(npcName) ?: return false
        val item = Items.byId(itemId) ?: return false
        if (inventory.count(itemId) <= 0) return false
        val def = NpcDefs.byName(npcName) ?: return false
        inventory.removeItem(itemId, 1)

        val (trustGain, pool) = when {
            itemId in def.loved -> 0.25f to Lines.GIFT_LOVED
            itemId in def.liked -> 0.15f to Lines.GIFT_LIKED
            else -> 0.05f to Lines.GIFT_NEUTRAL
        }
        n.identity.trust = (n.identity.trust + trustGain).coerceIn(-1f, 1f)
        n.identity.mood = "happy"
        n.memory.addEvent("подарил(а) ${item.name}", 7, clock.day, aboutPlayer = true)
        log("${npcName}: ${rng.pick(pool).replace("%s", item.name)}")
        counters.gifts += 1
        gainXp(6)
        ritual.record("gift")
        checkAchievements()
        return true
    }

    fun craft(recipeId: String): Boolean {
        val r = Recipes.byId(recipeId) ?: return false
        for ((id, cnt) in r.inputs) {
            if (inventory.count(id) < cnt) {
                log("Не хватает ингредиентов для «${r.name}».", system = true)
                return false
            }
        }
        if (!player.spendEnergy(1)) {
            log("Нужна 1 сила, чтобы готовить.", system = true)
            return false
        }
        for ((id, cnt) in r.inputs) inventory.removeItem(id, cnt)
        inventory.addItem(r.result, 1)
        counters.crafts += 1
        gainXp(r.xp)
        val res = Items.byId(r.result)
        log("Приготовлено: ${r.emoji} ${r.name} (${res?.emoji} ${res?.name})", highlight = true)
        ritual.record("craft")
        checkAchievements()
        return true
    }

    fun fulfillRequest(index: Int): Boolean {
        if (index < 0 || index >= requests.size) return false
        val req = requests[index]
        if (inventory.count(req.item) < req.count) {
            log("Нужно больше «${Items.byId(req.item)?.name}».", system = true)
            return false
        }
        inventory.removeItem(req.item, req.count)
        var reward = req.coins.toFloat()
        if (eventId == "market_day") reward *= 1.5f
        if ("fair" in upgrades) reward *= 1.5f
        val coinsGain = reward.roundToInt()
        addCoins(coinsGain)
        npc(req.npc)?.let {
            it.identity.trust = (it.identity.trust + 0.08f).coerceIn(-1f, 1f)
            it.memory.addEvent("выполнил(а) просьбу: ${req.text}", 6, clock.day, aboutPlayer = true)
        }
        gainXp(req.xp)
        counters.requests += 1
        log("${req.npc}: Спасибо! Вот, держи $coinsGain монет.", highlight = true)
        requests.removeAt(index)
        if (firstRequestDay != clock.day) {
            firstRequestDay = clock.day
            addShards(1)
            log("+1 осколок мозаики за первый заказ дня.", system = true)
        }
        ritual.record("request")
        checkAchievements()
        return true
    }

    fun sellItem(itemId: String): Boolean {
        val item = Items.byId(itemId) ?: return false
        if (inventory.count(itemId) <= 0) return false
        inventory.removeItem(itemId, 1)
        val price = if ("sweet_shop" in upgrades) (item.sellPrice * 3) / 2 else item.sellPrice
        addCoins(price)
        log("Продано: ${item.emoji} ${item.name} за $price монет.", system = true)
        return true
    }

    fun buyFromMerchant(itemId: String): Boolean {
        if (eventId != "merchant") return false
        val price = when (itemId) {
            "pearl" -> { if (merchantPearlDay == clock.day) return false; 25 }
            "amber" -> { if (merchantAmberDay == clock.day) return false; 30 }
            else -> return false
        }
        if (coins < price) {
            log("У торговца это стоит $price монет — пока не хватает.", system = true)
            return false
        }
        coins -= price
        if (itemId == "pearl") merchantPearlDay = clock.day else merchantAmberDay = clock.day
        inventory.addItem(itemId, 1)
        log("Торговец ухмыляется: «${Items.byId(itemId)?.name}» теперь твой.", highlight = true)
        return true
    }

    fun petCat(): Boolean {
        if (eventId != "cat_visit" || pettedDay == clock.day) return false
        pettedDay = clock.day
        player.regen(1)
        counters.pets += 1
        log(Lines.PET_CAT, highlight = true)
        if (rng.chance(0.4f)) {
            inventory.addItem("shell", 1)
            log("Кот принёс тебе ракушку. 🐚", system = true)
        }
        checkAchievements()
        return true
    }

    fun rest(): Boolean {
        if (player.restedDay == clock.day) {
            log("На сегодня хватит отдыха — впереди дела.", system = true)
            return false
        }
        player.restedDay = clock.day
        val gain = if (currentPlace == "home") 6 else 4
        player.regen(gain)
        clock.advance(60f)
        log(rng.pick(Lines.REST_LINES) + " +$gain сил.", system = true)
        return true
    }

    fun buyUpgrade(id: String): Boolean {
        val def = Upgrades.byId(id) ?: return false
        if (id in upgrades) return false
        if (coins < def.price) {
            log("Для «${def.name}» нужно ${def.price} монет.", system = true)
            return false
        }
        coins -= def.price
        upgrades.add(id)
        if (id == "lanterns" || id == "lighthouse" || id == "townhall") player.maxEnergyBonus += 1
        log("Городок изменился: ${def.emoji} ${def.name}! ${def.description}", highlight = true)
        checkAchievements()
        return true
    }

    fun claimRitual(): Boolean {
        val reward = ritual.claim()
        if (reward <= 0) return false
        addCoins(reward)
        gainXp(10)
        log("Цели дня выполнены! +$reward монет, +10 опыта.", highlight = true)
        checkAchievements()
        return true
    }

    // ------------------------------------------------------------------ квесты
    fun questOf(npcName: String): QuestDef? = Quests.forNpc(npcName)

    fun questStepIndex(npcName: String): Int = questStep[npcName] ?: 0

    private fun questDone(npcName: String): Boolean {
        val q = Quests.forNpc(npcName) ?: return false
        return questStepIndex(npcName) >= q.steps.size
    }

    fun questCanAdvance(npcName: String): Boolean {
        val q = Quests.forNpc(npcName) ?: return false
        val idx = questStepIndex(npcName)
        if (idx >= q.steps.size) return false
        val step = q.steps[idx]
        val n = npc(npcName) ?: return false
        if (n.identity.trust < step.needTrust) return false
        if (step.requiresUpgrade != null && step.requiresUpgrade !in upgrades) return false
        for ((id, cnt) in step.needItems) if (inventory.count(id) < cnt) return false
        val base = questTalkBase[npcName] ?: 0
        if ((npcTalks[npcName] ?: 0) - base < step.needTalks) return false
        return true
    }

    fun advanceQuest(npcName: String): Boolean {
        val q = Quests.forNpc(npcName) ?: return false
        val idx = questStepIndex(npcName)
        if (idx >= q.steps.size) return false
        if (!questCanAdvance(npcName)) {
            log("Пока не всё готово для следующего шага истории.", system = true)
            return false
        }
        val step = q.steps[idx]
        val n = npc(npcName)!!
        for ((id, cnt) in step.needItems) inventory.removeItem(id, cnt)
        if (step.rewardCoins > 0) addCoins(step.rewardCoins)
        for ((id, cnt) in step.rewardItems) inventory.addItem(id, cnt)
        n.identity.trust = (n.identity.trust + step.rewardTrust).coerceIn(-1f, 1f)
        n.memory.addEvent("история «${q.title}»: шаг ${idx + 1}", 9, clock.day, aboutPlayer = true)
        addShards(step.rewardShards)
        gainXp(12)

        questStep[npcName] = idx + 1
        questTalkBase[npcName] = npcTalks[npcName] ?: 0
        log("📖 ${npcName} — «${q.title}», часть ${idx + 1}: ${step.story}", highlight = true)
        if (idx + 1 == q.steps.size) {
            counters.questsDone += 1
            log("История «${q.title}» завершена! $npcName этого не забудет.", highlight = true)
        }
        checkAchievements()
        return true
    }

    // ------------------------------------------------------------------ награды
    /** Опыт с учётом бонусов городка (библиотека даёт +25%). */
    private fun gainXp(amount: Int) {
        if (amount <= 0) return
        val boosted = if ("library" in upgrades) (amount * 5 + 2) / 4 else amount
        player.addXp(boosted)
    }

    fun addCoins(amount: Int) {
        if (amount <= 0) return
        coins = min(coins + amount, 999999)
    }

    fun addShards(amount: Int) {
        if (amount <= 0) return
        val before = shards
        shards = min(shards + amount, PUZZLE_TOTAL)
        if (before < PUZZLE_TOTAL && shards >= PUZZLE_TOTAL) {
            log("🧩 Мозаика городка собрана целиком! Город помнит тебя.", highlight = true)
            addCoins(500)
        }
        checkAchievements()
    }

    private fun checkAchievements() {
        for (def in Achievements.ALL) {
            if (def.id in achievements) continue
            if (achieved(def.id)) {
                achievements.add(def.id)
                addCoins(def.coins)
                addShards(def.shards)
                log("🏆 Достижение «${def.name}»! +${def.coins} монет.", highlight = true)
            }
        }
    }

    private fun achieved(id: String): Boolean = when (id) {
        "meet" -> counters.meets >= 1
        "talk10" -> counters.talks >= 10
        "talk50" -> counters.talks >= 50
        "help5" -> counters.helps >= 5
        "help20" -> counters.helps >= 20
        "gather20" -> counters.gathers >= 20
        "gather100" -> counters.gathers >= 100
        "craft5" -> counters.crafts >= 5
        "craft20" -> counters.crafts >= 20
        "gift5" -> counters.gifts >= 5
        "gift15" -> counters.gifts >= 15
        "gossip10" -> counters.gossips >= 10
        "request5" -> counters.requests >= 5
        "request20" -> counters.requests >= 20
        "quest1" -> counters.questsDone >= 1
        "quest6" -> counters.questsDone >= Quests.ALL.size
        "rich500" -> coins >= 500
        "level5" -> player.level >= 5
        "level10" -> player.level >= 10
        "upg3" -> upgrades.size >= 3
        "upg_all" -> upgrades.size >= Upgrades.ALL.size
        "rare1" -> counters.rares >= 1
        "friend1" -> npcs.any { it.identity.trust >= 0.8f }
        "streak3" -> player.streak >= 3
        "streak7" -> player.streak >= 7
        "days7" -> clock.day >= 7
        "shards30" -> shards >= 30
        "puzzle_full" -> shards >= PUZZLE_TOTAL
        else -> false
    }

    // ------------------------------------------------------------------ загрузка/оффлайн
    fun onLoaded(nowMs: Long) {
        // первый запуск: прошлой сессии нет — просто фиксируем точку отсчёта
        if (lastSeenMs <= 0L) {
            lastSeenMs = nowMs
            lastRegenAbsHour = clock.day * 24 + clock.hour()
            lastHour = clock.hour()
            return
        }
        val awaySec = ((nowMs - lastSeenMs) / 1000L).coerceIn(0L, 6 * 3600L)
        val awayHours = (awaySec / 3600L).toInt()
        if (awayHours >= 1) {
            player.regen(awayHours)
            val known = npcs.filter { it.memory.playerName.isNotEmpty() }
            if (known.isNotEmpty()) {
                val picks = known.shuffled(java.util.Random(rng.nextLong())).take(min(2, known.size))
                for (p in picks) {
                    log(rng.pick(Lines.AWAY_LINES).replace("%s", p.identity.npcName), recall = true)
                }
            }
            val friend = npcs.find { it.identity.trust >= 0.5f }
            if (friend != null && rng.chance(0.5f)) {
                val giftItem = if (rng.chance(0.5f)) "bread" else "flower"
                inventory.addItem(giftItem, 1)
                log("${friend.identity.npcName} оставил(а) для тебя: ${Items.byId(giftItem)?.emoji} ${Items.byId(giftItem)?.name}.", recall = true)
            }
        }
        lastRegenAbsHour = clock.day * 24 + clock.hour()
        lastHour = clock.hour()
        lastSeenMs = nowMs
    }

    fun newGame() {
        clock.day = 1
        clock.totalMinutes = 8 * 60
        spawnTown()
        coins = 0
        playerName = ""
        shards = 0
        upgrades.clear()
        achievements.clear()
        questStep.clear()
        questTalkBase.clear()
        npcTalks.clear()
        counters.meets = 0; counters.talks = 0; counters.helps = 0; counters.gifts = 0
        counters.crafts = 0; counters.gathers = 0; counters.gossips = 0; counters.requests = 0
        counters.rares = 0; counters.questsDone = 0; counters.pets = 0
        lastTalkMinute.clear()
        inventory.items.clear()
        journal.clear()
        currentPlace = "площадь"
        player.loadState(PlayerState())
        ritual.resetForDay(1, defaultGoals())
        requestsDay = 1
        rollRequests()
        eventId = "quiet"
        eventDay = 1
        lastSeenMs = 0L
        log("Ты приезжаешь в маленький городок у реки. Марта выглядывает из пекарни: «Привет! Как тебя зовут?»", highlight = true)
        log("Цели дня: " + ritual.goals.joinToString(" · ") { it.text }, system = true)
        log("Подсказка: тапни по жителю на карте, чтобы поговорить, а кнопка «Собрать» приносит ресурсы.", system = true)
        log("Жители помнят твои поступки и рассказывают о них друг другу. Начни с простого: поговори и помоги.", system = true)
    }

    private fun defaultGoals(): List<GoalDef> = DailyRitual.DEFAULT_GOALS

    // ------------------------------------------------------------------ views
    fun npcViews(): List<NpcView> = npcs.map { n ->
        val name = n.identity.npcName
        val def = NpcDefs.byName(name)
        val spot = placeOf(n)
        val q = Quests.forNpc(name)
        val idx = questStepIndex(name)
        val done = q != null && idx >= q.steps.size
        val stepText = if (q != null && idx < q.steps.size) q.steps[idx].text else ""
        NpcView(
            name = name,
            mood = n.identity.mood,
            moodText = when (n.identity.mood) {
                "happy" -> "в хорошем настроении"
                "angry" -> "не в духе"
                "sleepy" -> "сонный"
                else -> "спокоен"
            },
            trust = n.identity.trust,
            tier = TrustTiers.tier(n.identity.trust),
            tierName = TrustTiers.name(n.identity.trust),
            place = spot.place,
            placeName = Places.byId(spot.place)?.name ?: spot.place,
            activity = spot.activity,
            bio = def?.bio ?: "",
            portraitKey = def?.portraitKey ?: name.lowercase(),
            isHere = spot.place == currentPlace,
            questTitle = q?.title ?: "",
            questStepIndex = idx,
            questSteps = q?.steps?.size ?: 0,
            questText = stepText,
            questDone = done,
            questReady = questCanAdvance(name),
            lovesHint = if (TrustTiers.tier(n.identity.trust) >= 1 && def != null) {
                "Любит: " + def.loved.mapNotNull { Items.byId(it)?.name }.joinToString(", ")
            } else "Подружись ближе — и узнаешь, что этот житель любит.",
            lovedIds = if (TrustTiers.tier(n.identity.trust) >= 1 && def != null) def.loved else emptyList()
        )
    }

    fun requestViews(): List<RequestView> = requests.mapIndexed { i, r ->
        RequestView(
            index = i,
            npc = r.npc,
            text = r.text,
            itemEmoji = Items.byId(r.item)?.emoji ?: "",
            itemName = Items.byId(r.item)?.name ?: r.item,
            need = r.count,
            have = inventory.count(r.item),
            coins = r.coins,
            canDo = inventory.count(r.item) >= r.count
        )
    }

    fun recipeViews(): List<RecipeView> = Recipes.ALL.map { r ->
        RecipeView(
            id = r.id,
            name = r.name,
            emoji = r.emoji,
            inputsText = r.inputs.entries.joinToString(" + ") { (id, cnt) ->
                "${Items.byId(id)?.emoji ?: ""} ${Items.byId(id)?.name ?: id} ×$cnt"
            },
            canCraft = r.inputs.all { (id, cnt) -> inventory.count(id) >= cnt }
        )
    }

    fun itemViews(): List<ItemView> = inventory.items.entries
        .sortedByDescending { it.value }
        .mapNotNull { (id, cnt) ->
            Items.byId(id)?.let { d -> ItemView(d.id, d.name, d.emoji, cnt, d.sellPrice, d.rarity) }
        }

    fun upgradeViews(): List<UpgradeView> = Upgrades.ALL.map { u ->
        UpgradeView(u.id, u.name, u.emoji, u.price, u.description, u.id in upgrades, coins >= u.price)
    }

    fun achievementViews(): List<AchievementView> = Achievements.ALL.map { a ->
        AchievementView(a.id, a.name, a.desc, a.coins, a.id in achievements)
    }

    fun goalViews(): List<GoalView> = ritual.status().map { GoalView(it.id, it.text, it.current, it.target, it.done) }

    // ------------------------------------------------------------------ save
    fun toState(): EngineState = EngineState(
        version = 3,
        playerName = playerName,
        coins = coins,
        clock = clock.toState(),
        inventory = inventory.toState(),
        ritual = ritual.toState(),
        player = player.toState(),
        rng = rng.toState(),
        npcs = npcs.map { it.toState() },
        counters = counters,
        achievements = achievements.toList(),
        upgrades = upgrades.toList(),
        questStep = questStep.toMap(),
        questTalkBase = questTalkBase.toMap(),
        npcTalks = npcTalks.toMap(),
        requests = requests.toList(),
        requestsDay = requestsDay,
        shards = shards,
        currentPlace = currentPlace,
        eventId = eventId,
        eventDay = eventDay,
        dailyGoals = ritual.goals,
        pettedDay = pettedDay,
        merchantPearlDay = merchantPearlDay,
        merchantAmberDay = merchantAmberDay,
        firstRequestDay = firstRequestDay,
        musicEnabled = musicEnabled,
        lastSeenMs = lastSeenMs,
        journal = journal.takeLast(120),
        nextEntryId = nextEntryId
    )

    fun loadState(s: EngineState) {
        playerName = TextSafe.sanitizeName(s.playerName)
        coins = s.coins.coerceIn(0, 999999)
        clock.loadState(s.clock)
        inventory.loadState(s.inventory)
        ritual.loadState(s.ritual)
        player.loadState(s.player)
        rng.loadState(s.rng)
        for (i in npcs.indices) {
            val st = s.npcs.firstOrNull { it.npcName == npcs[i].identity.npcName }
            if (st != null) npcs[i].loadState(st)
        }
        counters.meets = max(0, s.counters.meets); counters.talks = max(0, s.counters.talks)
        counters.helps = max(0, s.counters.helps); counters.gifts = max(0, s.counters.gifts)
        counters.crafts = max(0, s.counters.crafts); counters.gathers = max(0, s.counters.gathers)
        counters.gossips = max(0, s.counters.gossips); counters.requests = max(0, s.counters.requests)
        counters.rares = max(0, s.counters.rares); counters.questsDone = max(0, s.counters.questsDone)
        counters.pets = max(0, s.counters.pets)
        achievements.clear(); achievements.addAll(s.achievements.filter { Achievements.byId(it) != null })
        upgrades.clear(); upgrades.addAll(s.upgrades.filter { Upgrades.byId(it) != null })
        questStep.clear(); s.questStep.forEach { (k, v) -> questStep[k.take(24)] = v.coerceIn(0, 8) }
        questTalkBase.clear(); s.questTalkBase.forEach { (k, v) -> questTalkBase[k.take(24)] = max(0, v) }
        npcTalks.clear(); s.npcTalks.forEach { (k, v) -> npcTalks[k.take(24)] = max(0, v) }
        requests.clear(); requests.addAll(s.requests.take(6))
        requestsDay = s.requestsDay.coerceIn(0, 9999)
        shards = s.shards.coerceIn(0, PUZZLE_TOTAL)
        currentPlace = if (Places.byId(s.currentPlace) != null) s.currentPlace else "площадь"
        eventId = s.eventId
        eventDay = s.eventDay.coerceIn(0, 9999)
        pettedDay = s.pettedDay.coerceIn(0, 9999)
        merchantPearlDay = s.merchantPearlDay.coerceIn(0, 9999)
        merchantAmberDay = s.merchantAmberDay.coerceIn(0, 9999)
        firstRequestDay = s.firstRequestDay.coerceIn(0, 9999)
        musicEnabled = s.musicEnabled
        lastSeenMs = s.lastSeenMs
        journal.clear(); journal.addAll(s.journal.takeLast(120))
        nextEntryId = max(s.nextEntryId, journal.lastOrNull()?.id ?: 0L)
        if (requests.isEmpty() || requestsDay != clock.day) rollRequests()
        lastRegenAbsHour = clock.day * 24 + clock.hour()
        lastHour = clock.hour()
    }

    companion object {
        const val MAX_JOURNAL = 300
        const val TALK_COOLDOWN_MINUTES = 15
    }
}

/** Склонение названий предметов для заказов («принеси мне рыбу»). */
fun ItemDef.accusative(): String = when (id) {
    "flour" -> "муку"
    "bread" -> "хлеб"
    "flower" -> "цветы"
    "honey" -> "мёд"
    "ribbon" -> "ленту"
    "plank" -> "доски"
    "nail" -> "гвозди"
    "fish" -> "рыбу"
    "shell" -> "ракушки"
    "berry" -> "ягоды"
    else -> name
}
