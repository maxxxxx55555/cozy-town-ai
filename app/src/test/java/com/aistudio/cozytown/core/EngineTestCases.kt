package com.aistudio.cozytown.core

import com.aistudio.cozytown.model.GameClockState
import com.aistudio.cozytown.model.InventoryState
import com.aistudio.cozytown.model.NPCMemoryState

/**
 * Общие тест-кейсы ядра. Запускаются и в Gradle (JUnit-обёртка EngineTest),
 * и headless (tools/harness/HarnessMain.kt) — одна и та же логика.
 */
object EngineTestCases {

    class Fail(msg: String) : AssertionError(msg)

    fun expect(cond: Boolean, msg: String) {
        if (!cond) throw Fail(msg)
    }

    fun expectEq(a: Any?, b: Any?, msg: String) {
        if (a != b) throw Fail("$msg: ожидалось <$b>, получено <$a>")
    }

    private fun engine(): GameEngine = GameEngine(seed = 1234L)

    fun all(): List<Pair<String, () -> Unit>> = listOf(
        "intro_name_memory" to ::introNameMemory,
        "name_sanitized" to ::nameSanitized,
        "move_gating" to ::moveGating,
        "gather_energy_items" to ::gatherEnergyItems,
        "storm_blocks_pier" to ::stormBlocksPier,
        "craft_flow" to ::craftFlow,
        "gift_trust_tiers" to ::giftTrustTiers,
        "talk_and_gossip" to ::talkAndGossip,
        "help_energy_trust" to ::helpEnergyTrust,
        "requests_fulfill" to ::requestsFulfill,
        "ritual_claim" to ::ritualClaim,
        "quest_chain" to ::questChain,
        "upgrades_unlock" to ::upgradesUnlock,
        "achievements_and_shards" to ::achievementsAndShards,
        "day_rollover_streak" to ::dayRolloverStreak,
        "rest_once_per_day" to ::restOncePerDay,
        "merchant_limits" to ::merchantLimits,
        "save_roundtrip" to ::saveRoundtrip,
        "save_tamper_checksum" to ::saveTamperChecksum,
        "legacy_v2_migration" to ::legacyV2Migration,
        "rng_determinism" to ::rngDeterminism,
        "offline_return" to ::offlineReturn,
        "puzzle_cap" to ::puzzleCap,
        "journal_cap" to ::journalCap,
        "playthrough_three_days" to ::playthroughThreeDays,
        "talk_cooldown_antispam" to ::talkCooldownAntispam,
        "no_npc_in_locked_place" to ::noNpcInLockedPlace,
        "loved_gifts_revealed_by_trust" to ::lovedGiftsRevealedByTrust,
        "realistic_week_pacing" to ::realisticWeekPacing,
        "first_launch_is_silent" to ::firstLaunchIsSilent
    )

    fun introNameMemory() {
        val e = engine()
        expect(!e.setName("А"), "короткое имя отклоняется")
        expect(e.setName("Максим"), "имя принимается")
        expect(!e.setName("Другое"), "второе имя не принимается")
        expectEq(e.playerName, "Максим", "имя игрока")
        expect(e.npcs.all { it.memory.playerName == "Максим" }, "все NPC запомнили имя")
        expect(e.npcs.all { it.memory.recallAboutPlayer().isNotEmpty() }, "все NPC помнят встречу")
        expect(e.counters.meets == 1, "счётчик знакомств")
        expect("meet" in e.achievements, "достижение знакомства")
    }

    fun nameSanitized() {
        val e = engine()
        expect(e.setName("  [b]Иван[/b]\nИванов  ".take(60)), "имя с разметкой")
        expectEq(e.playerName, "ИванИванов", "BBCode и переводы строк убраны")
    }

    fun moveGating() {
        val e = engine()
        expect(e.moveTo("пекарня"), "переход в пекарню")
        expectEq(e.currentPlace, "пекарня", "текущее место")
        expect(!e.moveTo("сад"), "сад закрыт без апгрейда")
        expect(!e.moveTo("неизвестное"), "неизвестное место отклоняется")
        e.coins = 500
        expect(e.buyUpgrade("garden"), "покупка сада")
        expect(e.moveTo("сад"), "сад открыт после апгрейда")
    }

    fun gatherEnergyItems() {
        val e = engine()
        e.moveTo("пекарня")
        val energyBefore = e.player.energy
        expect(e.gather(), "сбор в пекарне")
        expectEq(e.player.energy, energyBefore - 1, "энергия тратится")
        val total = e.inventory.items.values.sum()
        expect(total >= 1, "предмет собран")
        expect(e.counters.gathers >= 1, "счётчик сбора")
        // энергии не хватит
        e.player.energy = 0
        expect(!e.gather(), "без сил сбор не работает")
    }

    fun stormBlocksPier() {
        val e = engine()
        e.eventId = "storm"
        e.moveTo("причал")
        val energy = e.player.energy
        expect(!e.gather(), "шторм блокирует причал")
        expectEq(e.player.energy, energy, "энергия не тратится при шторме")
    }

    fun craftFlow() {
        val e = engine()
        expect(!e.craft("pie"), "без ингредиентов крафт не работает")
        e.inventory.addItem("bread", 1)
        e.inventory.addItem("honey", 1)
        expect(e.craft("pie"), "крафт пирога")
        expectEq(e.inventory.count("pie"), 1, "пирог в инвентаре")
        expectEq(e.inventory.count("bread"), 0, "хлеб потрачен")
        expectEq(e.inventory.count("honey"), 0, "мёд потрачен")
        expect(e.counters.crafts == 1, "счётчик крафта")
    }

    fun giftTrustTiers() {
        val e = engine()
        e.setName("Тест")
        val marta = e.npc("Марта")!!
        e.inventory.addItem("pie", 2)
        e.inventory.addItem("bread", 2)
        val t0 = marta.identity.trust
        expect(e.gift("Марта", "pie"), "подарок любимым")
        val t1 = marta.identity.trust
        expect(t1 - t0 >= 0.24f, "любимый подарок даёт +0.25")
        expect(e.gift("Марта", "bread"), "подарок понравившимся")
        val t2 = marta.identity.trust
        expect(t2 - t1 >= 0.14f && t2 - t1 <= 0.16f, "понравившийся +0.15")
        expect(marta.memory.recallAboutPlayer().any { it.text.contains("пирог", true) || it.text.contains("Пирог") }, "NPC помнит подарок")
    }

    fun talkAndGossip() {
        val e = engine()
        e.setName("Тест")
        e.clock.totalMinutes = 13 * 60 // Марта и Аня на рынке
        e.lastSeenMs = 0
        e.moveTo("рынок")
        val line = e.talkTo("Марта")
        expect(line != null && line.contains("Тест"), "диалог содержит имя")
        expect(e.counters.talks == 1, "счётчик разговоров")
        expect(e.counters.gossips >= 1, "сплетня с Аней на рынке")
        e.moveTo("площадь")
        expect(e.talkTo("Марта") == null, "вне локации NPC разговор не идёт")
    }

    fun helpEnergyTrust() {
        val e = engine()
        e.setName("Тест")
        e.clock.totalMinutes = 9 * 60
        e.moveTo("пекарня")
        val marta = e.npc("Марта")!!
        val t0 = marta.identity.trust
        val en = e.player.energy
        expect(e.helpNpc("Марта"), "помощь в локации")
        expectEq(e.player.energy, en - 2, "помощь стоит 2 силы")
        expect(marta.identity.trust > t0, "доверие растёт от помощи")
        e.player.energy = 1
        expect(!e.helpNpc("Марта"), "мало сил — не помочь")
    }

    fun requestsFulfill() {
        val e = engine()
        e.setName("Тест")
        expect(e.requests.size == 3, "три заказа на старте")
        e.requestViews() // view для UI
        val req = e.requests[0]
        e.inventory.addItem(req.item, req.count)
        val coins0 = e.coins
        expect(e.fulfillRequest(0), "заказ выполняется")
        expect(e.coins > coins0, "монеты за заказ")
        expectEq(e.requests.size, 2, "заказ убран")
        expect(e.shards >= 1, "осколок за первый заказ дня")
    }

    fun ritualClaim() {
        val e = engine()
        e.ritual.record("talk"); e.ritual.record("talk")
        e.ritual.record("kind")
        e.ritual.record("gossip")
        expect(e.ritual.allDone(), "цели выполнены")
        val coins0 = e.coins
        expect(e.claimRitual(), "награда получена")
        expectEq(e.coins, coins0 + 25, "+25 монет")
        expect(!e.claimRitual(), "повторно не получить")
    }

    fun questChain() {
        val e = engine()
        e.setName("Тест")
        val marta = e.npc("Марта")!!
        expect(!e.advanceQuest("Марта"), "шаг 0 требует разговора")
        e.clock.totalMinutes = 9 * 60
        e.moveTo("пекарня")
        e.talkTo("Марта")
        expect(e.questCanAdvance("Марта"), "после разговора шаг доступен")
        expect(e.advanceQuest("Марта"), "шаг 1 пройден")
        expectEq(e.questStepIndex("Марта"), 1, "индекс шага")
        expect(!e.advanceQuest("Марта"), "шаг 2 требует муку")
        e.inventory.addItem("flour", 2)
        expect(e.advanceQuest("Марта"), "шаг 2 с мукой")
        expectEq(e.inventory.count("bread"), 3, "награда хлебом")
        expect(!e.advanceQuest("Марта"), "шаг 3 требует доверие 0.4")
        marta.identity.trust = 0.5f
        e.clock.totalMinutes += GameEngine.TALK_COOLDOWN_MINUTES + 1
        e.talkTo("Марта")
        expect(e.advanceQuest("Марта"), "шаг 3")
        e.inventory.addItem("honey", 1)
        expect(e.advanceQuest("Марта"), "финал арки")
        expectEq(e.questStepIndex("Марта"), 4, "арка завершена")
        expect(e.counters.questsDone == 1, "счётчик арок")
        expect("quest1" in e.achievements, "достижение первой арки")
    }

    fun upgradesUnlock() {
        val e = engine()
        expect(!e.buyUpgrade("lanterns"), "без монет не купить")
        e.coins = 100
        expect(e.buyUpgrade("lanterns"), "фонарики куплены")
        expect("lanterns" in e.upgrades, "в списке апгрейдов")
        expectEq(e.player.maxEnergyBonus, 1, "бонус к силам")
        expect(!e.buyUpgrade("lanterns"), "повторно не купить")
        expectEq(e.coins, 40, "монеты списаны")
    }

    fun achievementsAndShards() {
        val e = engine()
        e.addShards(30)
        expect("shards30" in e.achievements, "достижение 30 осколков")
        expect(e.shards >= 30, "осколки на месте (награда достижения добавляет сверху)")
    }

    fun dayRolloverStreak() {
        val e = engine()
        e.player.lastActiveDay = 1
        e.clock.totalMinutes = 23 * 60 + 55
        e.tick(600f) // пересечение полуночи
        expectEq(e.clock.day, 2, "новый день")
        expectEq(e.player.streak, 1, "серия 1")
        e.clock.totalMinutes = 23 * 60 + 55
        e.tick(600f)
        expectEq(e.clock.day, 3, "ещё день")
        expectEq(e.player.streak, 2, "серия 2")
        expect(e.requests.size == 3, "заказы обновились")
        expect(e.goalViews().size == 3, "цели обновились")
    }

    fun restOncePerDay() {
        val e = engine()
        e.player.energy = 0
        val en = e.player.energy
        expect(e.rest(), "отдых сработал")
        expect(e.player.energy > en, "силы добавились")
        expect(!e.rest(), "второй отдых за день недоступен")
    }

    fun merchantLimits() {
        val e = engine()
        e.eventId = "merchant"
        e.coins = 100
        expect(e.buyFromMerchant("pearl"), "жемчуг куплен")
        expect(!e.buyFromMerchant("pearl"), "второй жемчуг за день нельзя")
        expect(e.buyFromMerchant("amber"), "янтарь куплен")
        e.eventId = "quiet"
        expect(!e.buyFromMerchant("bread"), "без торговца не купить")
    }

    fun saveRoundtrip() {
        val e = engine()
        e.setName("Сохранялка")
        e.coins = 777
        e.inventory.addItem("pie", 3)
        e.clock.totalMinutes = 15 * 60
        val state = e.toState()
        val jsonStr = SaveCodec.encode(state)

        val e2 = GameEngine(seed = 99L)
        val parsed = SaveCodec.parse(jsonStr, 3) ?: throw Fail("parse вернул null")
        e2.loadState(parsed)
        expectEq(e2.playerName, "Сохранялка", "имя после загрузки")
        expectEq(e2.coins, 777, "монеты после загрузки")
        expectEq(e2.inventory.count("pie"), 3, "инвентарь после загрузки")
        expectEq(e2.clock.hour(), 15, "время после загрузки")
        expectEq(e2.journal.size, e.journal.size, "журнал после загрузки")
    }

    fun saveTamperChecksum() {
        val e = engine()
        e.coins = 10
        val jsonStr = SaveCodec.encode(e.toState())
        val tampered = jsonStr.replace("\"coins\":10", "\"coins\":999999")
        expect(TextSafe.checksum(tampered) != TextSafe.checksum(jsonStr), "checksum меняется при подмене")
        // парсинг без валидации checksum — валидация в SaveGame; здесь проверяем, что данные не совпадают
        val p = SaveCodec.parse(tampered, 3)!!
        expect(p.coins == 999999, "подмена видна до валидации — валидация checksum обязательна на слое SaveGame")
    }

    fun legacyV2Migration() {
        val legacy = LegacySaveState(
            player_name = "Старый Сейв",
            coins = 42,
            clock = GameClockState(day = 5, totalMinutes = 10 * 60),
            inventory = InventoryState(mapOf("bread" to 2)),
            music_enabled = false
        )
        val data = SaveCodec.json.encodeToString(LegacySaveState.serializer(), legacy)
        val st = SaveCodec.parse(data, 2) ?: throw Fail("миграция v2 не сработала")
        expectEq(st.playerName, "Старый Сейв", "имя из v2")
        expectEq(st.coins, 42, "монеты из v2")
        expectEq(st.clock.day, 5, "день из v2")
        expect(!st.musicEnabled, "музыка из v2")
        val e = GameEngine()
        e.loadState(st)
        expectEq(e.playerName, "Старый Сейв", "движок принял миграцию")
    }

    fun rngDeterminism() {
        val a = Rng(777L)
        val b = Rng(777L)
        repeat(50) {
            expectEq(a.nextLong(), b.nextLong(), "rng детерминирован")
        }
        val c = Rng(778L)
        val d = Rng(777L)
        expect(c.nextLong() != d.nextLong(), "разные seed — разные последовательности")
    }

    fun offlineReturn() {
        val e = engine()
        e.setName("Возвращенец")
        val marta = e.npc("Марта")!!
        marta.identity.trust = 0.6f
        e.player.energy = 0
        e.lastSeenMs = 1_000_000L
        e.onLoaded(1_000_000L + 3 * 3600_000L) // 3 часа спустя
        expect(e.player.energy >= 3, "силы восстановились за 3 часа")
        expect(e.journal.any { it.recall }, "есть строки «пока тебя не было»")
    }

    fun puzzleCap() {
        val e = engine()
        e.addShards(1000)
        expectEq(e.shards, PUZZLE_TOTAL, "осколки ограничены")
        expect("puzzle_full" in e.achievements, "достижение полной мозаики")
    }

    fun journalCap() {
        val e = engine()
        repeat(400) { e.log("строка $it") }
        expect(e.journal.size <= GameEngine.MAX_JOURNAL, "журнал ограничен")
    }

    /** Симуляция трёх дней реальной игры: проверяет, что прогресс идёт, а баланс не ломается. */
    fun playthroughThreeDays() {
        val e = GameEngine(seed = 2026L)
        e.setName("Максим")
        val playable = listOf("пекарня", "рынок", "мастерская", "причал", "площадь", "таверна", "сад", "маяк")
        for (day in 1..3) {
            for (session in 0 until 4) {
                for (place in playable) {
                    if (e.moveTo(place)) repeat(4) { e.gather() }
                }
                for (npc in e.npcs.map { it.identity.npcName }) {
                    val n = e.npc(npc) ?: continue
                    val spot = n.schedule.placeAt(e.clock.hour()).place
                    if (spot != "home" && e.moveTo(spot)) {
                        e.talkTo(npc)
                        if (e.player.energy >= 2) e.helpNpc(npc)
                    }
                }
                for (npc in e.npcs.map { it.identity.npcName }) {
                    if (e.questCanAdvance(npc)) e.advanceQuest(npc)
                }
                val craftable = e.recipeViews().filter { it.canCraft }
                for (r in craftable) e.craft(r.id)
                for (i in e.requests.indices.reversed()) e.fulfillRequest(i)
                val firstItem = e.itemViews().firstOrNull()
                if (firstItem != null) e.gift("Марта", firstItem.id)
                if (e.ritual.allDone()) e.claimRitual()
                for (u in e.upgradeViews()) {
                    if (!u.owned && u.affordable) { e.buyUpgrade(u.id); break }
                }
                e.rest()
                e.player.regen(99)
            }
            // проживаем остаток суток: 1440 игровых минут по 10-минутным шагам
            repeat(24 * 60 / 10) { e.tick(10f) }
        }
        println(
            "BALANCE день=${e.clock.day} монеты=${e.coins} ур=${e.player.level} xp=${e.player.xp} " +
                "осколки=${e.shards} достижения=${e.achievements.size} арки=${e.counters.questsDone} " +
                "предметы=${e.inventory.items.values.sum()} апгрейды=${e.upgrades.size} " +
                "разговоры=${e.counters.talks} сбор=${e.counters.gathers} крафт=${e.counters.crafts} " +
                "заказы=${e.counters.requests} подарки=${e.counters.gifts} сплетни=${e.counters.gossips} " +
                "друзья=${e.npcs.count { it.identity.trust >= 0.5f }}"
        )
        expect(e.coins > 100, "за три дня игрок зарабатывает монеты")
        expect(e.player.level >= 2, "уровень растёт")
        expect(e.achievements.size >= 3, "достижения открываются")
        expect(e.counters.talks >= 10, "разговоров достаточно")
        expect(e.shards > 0, "осколки мозаики капают")
        expect(e.journal.isNotEmpty(), "журнал наполнен")
        expect(e.upgrades.isNotEmpty(), "хотя бы одно улучшение куплено")
    }

    /** Спам-разговоры не должны давать опыт и счётчики. */
    fun talkCooldownAntispam() {
        val e = engine()
        e.setName("Тест")
        e.clock.totalMinutes = 13 * 60
        e.moveTo("рынок")
        e.talkTo("Марта")
        expectEq(e.counters.talks, 1, "первый разговор засчитан")
        val xpAfterFirst = e.player.xp
        e.talkTo("Марта")
        e.talkTo("Марта")
        expectEq(e.counters.talks, 1, "повторы в течение 15 минут не считаются")
        expectEq(e.player.xp, xpAfterFirst, "повторы не дают опыт")
        e.clock.totalMinutes += GameEngine.TALK_COOLDOWN_MINUTES
        e.talkTo("Марта")
        expectEq(e.counters.talks, 2, "после кулдауна разговор снова засчитан")
    }

    /** Житель не может «сидеть» в закрытом месте — игрок должен иметь возможность до него дойти. */
    fun noNpcInLockedPlace() {
        val e = engine()
        e.setName("Тест")
        // без апгрейдов: сад и маяк закрыты
        for (hour in listOf(7, 9, 10, 14, 17, 18, 20, 23)) {
            e.clock.totalMinutes = hour * 60
            for (n in e.npcs) {
                val place = e.placeOf(n).place
                val def = Places.byId(place) ?: continue
                if (def.requiresUpgrade != null) {
                    throw Fail("житель ${n.identity.npcName} в закрытом месте «${def.name}» в $hour:00")
                }
            }
            for (view in e.npcViews()) {
                val def = Places.byId(view.place) ?: continue
                if (def.requiresUpgrade != null && def.requiresUpgrade !in e.upgrades) {
                    throw Fail("в списке жителей ${view.name} указан в закрытом месте «${def.name}»")
                }
            }
        }
        // после открытия сада Соня снова может туда ходить
        e.coins = 500
        e.buyUpgrade("garden")
        e.clock.totalMinutes = 18 * 60
        val sonya = e.npc("Соня")!!
        expectEq(e.placeOf(sonya).place, "сад", "после апгрейда Соня снова в саду")
    }

    /** Вкусы жителя открываются по мере дружбы и используются в диалоге подарка. */
    fun lovedGiftsRevealedByTrust() {
        val e = engine()
        e.setName("Тест")
        val stranger = e.npcViews().first { it.name == "Борис" }
        expect(stranger.lovedIds.isEmpty(), "у знакомца вкусы ещё не открыты")
        expect(stranger.lovesHint.contains("Подружись"), "подсказка зовёт подружиться")
        val boris = e.npc("Борис")!!
        boris.identity.trust = 0.3f
        val friend = e.npcViews().first { it.name == "Борис" }
        expect(friend.lovedIds.contains("box"), "с «Приятелем» видно любимый предмет")
        expect(friend.lovesHint.contains("Шкатулка"), "в подсказке есть название предмета")
    }

    /**
     * Реалистичная неделя: энергия тратится только на действия и восстанавливается регенерацией,
     * отдыхом и сном. Проверяем, что за 7 дней игрок видит прогресс, но не «проходит игру».
     */
    fun realisticWeekPacing() {
        val e = GameEngine(seed = 7L)
        e.setName("Настя")
        val playable = listOf("пекарня", "рынок", "мастерская", "причал", "площадь", "таверна")
        val log = mutableListOf<String>()
        for (day in 1..7) {
            // три «захода» в день; естественная регенерация идёт за счёт тиков времени
            for (session in 0 until 3) {
                for (place in playable) {
                    if (e.moveTo(place)) repeat(2) { e.gather() }
                }
                for (npc in e.npcs.map { it.identity.npcName }) {
                    val n = e.npc(npc) ?: continue
                    val spot = e.placeOf(n).place
                    if (spot != "home" && e.moveTo(spot)) {
                        e.talkTo(npc)
                        if (e.player.energy >= 2) e.helpNpc(npc)
                    }
                }
                for (npc in e.npcs.map { it.identity.npcName }) if (e.questCanAdvance(npc)) e.advanceQuest(npc)
                for (r in e.recipeViews()) if (r.canCraft) e.craft(r.id)
                for (i in e.requests.indices.reversed()) e.fulfillRequest(i)
                val gift = e.itemViews().firstOrNull { it.count >= 2 }
                if (gift != null) e.gift(e.npcViews().first().name, gift.id)
                if (e.ritual.allDone()) e.claimRitual()
                // покупки: сначала самое доступное улучшение
                e.upgradeViews().filter { !it.owned && it.affordable }.minByOrNull { it.price }
                    ?.let { e.buyUpgrade(it.id) }
                if (session == 0) e.rest()
                // «проходит время»: 6 игровых часов между заходами
                repeat(36) { e.tick(10f) }
            }
            // ночь: тикаем до утра (энергия восстанавливается)
            repeat(24 * 60 / 10) { e.tick(10f) }
            log += "день $day: монет ${e.coins}, ур ${e.player.level}, осколков ${e.shards}, " +
                "апгрейдов ${e.upgrades.size}, арок ${e.counters.questsDone}, друзей ${e.npcs.count { it.identity.trust >= 0.5f }}"
        }
        println("BALANCE-НЕДЕЛЯ " + log.joinToString(" | "))
        expect(e.coins >= 0, "монеты не уходят в минус, даже когда всё потрачено на городок")
        expect(e.counters.requests >= 3, "заказы выполняются")
        expect(e.counters.talks >= 20, "жители охотно разговаривают")
        expect(e.player.level >= 3, "уровень растёт за неделю")
        expect(e.upgrades.size >= 2, "за неделю открываются улучшения")
        expect(e.shards >= 8, "осколки копятся")
        expect(e.coins < 4000, "экономика не разгоняется в бесконечность")
        expect(e.shards < PUZZLE_TOTAL, "полная мозаика не собирается за неделю — есть долгая цель")
    }

    /** Первый запуск не должен выдавать «пока тебя не было» и подарки за несуществующую сессию. */
    fun firstLaunchIsSilent() {
        val e = GameEngine(seed = 5L)
        e.setName("Новичок")
        val before = e.journal.size
        val energyBefore = e.player.energy
        e.onLoaded(System.currentTimeMillis())
        expectEq(e.journal.size, before, "первый запуск не пишет оффлайн-строк")
        expectEq(e.player.energy, energyBefore, "первый запуск не даёт бонусных сил")

        // а вот реальный возврат через 3 часа — даёт силы и приветствие
        e.lastSeenMs = 1_000_000L
        e.player.energy = 0
        e.onLoaded(1_000_000L + 3 * 3600_000L)
        expect(e.player.energy >= 3, "после отсутствия силы восстановились")
        expect(e.journal.size > before, "есть строки «пока тебя не было»")
    }
}
