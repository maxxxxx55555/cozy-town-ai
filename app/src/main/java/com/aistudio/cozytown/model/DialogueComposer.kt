package com.aistudio.cozytown.model

object DialogueComposer {
    private val OPENERS = mapOf(
        "warm" to listOf("Ох, %s! Как я рада тебя видеть.", "Привет, %s! Я тебя ждала.", "%s! Садись рядом."),
        "cold" to listOf("Опять ты, %s.", "Чего тебе, %s?", "%s. Я не забыла."),
        "neutral" to listOf("Привет, %s.", "А, это ты, %s.", "Доброго дня, %s.")
    )

    private val TIER_LINES = listOf(
        listOf("Мы ведь недавно познакомились, но мне кажется, я знаю тебя сто лет."),
        listOf("Ты уже почти свой человек в городке.", "С тобой приятно болтать — заходи чаще."),
        listOf("Ты — мой друг. Я таких по пальцам пересчитаю.", "Знаешь, с тобой городок стал теплее."),
        listOf("Ты — душа города, и я говорю это не каждому.", "О таких, как ты, здесь будут помнить долго.")
    )

    private val ACTIVITY_LINES = listOf(
        "Я сейчас в %s — %s.",
        "Как видишь, я в %s: %s."
    )

    private val TRAIT_LINES = mapOf(
        "добрая" to "Если что — я всегда помогу.",
        "болтливая" to "Столько новостей, не расскажешь и за день!",
        "ворчливый" to "Молодёжь совсем распустилась.",
        "мечтательный" to "Сегодня вода была как небо.",
        "тихий" to "…Я просто посижу рядом.",
        "энергичная" to "Побежали на площадь?",
        "любопытная" to "Расскажи, что у тебя нового!",
        "бывалый" to "Море сегодня дышит ровно. Добрый знак.",
        "немногословный" to "Ммм. Хороший день.",
        "озорная" to "Спойдём что-нибудь запретное? Например, про Бориса!",
        "любознательная" to "А ты знаешь, что под причалом живёт краб-вор?"
    )

    private val EVENT_LINES = mapOf(
        "rain" to "Дождь — самое то, чтобы сидеть и мечтать.",
        "market_day" to "Базарный день! Цены пляшут, и я тоже.",
        "festival" to "Праздник фонариков — мой любимый. Не говори другим.",
        "merchant" to "Торговец привёз редкости, глянь на площади.",
        "storm" to "Море шумит так, что в ушах звенит. Осторожнее у воды.",
        "cat_visit" to "Видел(а) рыжего кота? Он приходит не просто так.",
        "quiet" to "Тихий день. Даже Борис сегодня не ворчит."
    )

    fun compose(npc: NPC, day: Int, hour: Int = 9, tier: Int = 0, eventId: String? = null): String {
        val pname = npc.memory.playerName
        if (pname.isEmpty()) {
            return "Привет! Как тебя зовут?"
        }
        val lines = mutableListOf<String>()
        val tone = npc.identity.tone()
        val openers = OPENERS[tone] ?: OPENERS["neutral"]!!
        val hashVal = (npc.identity.npcName.hashCode() + day).let { if (it < 0) -it else it }
        val openerFormat = openers[hashVal % openers.size]
        lines.add(openerFormat.replace("%s", pname))

        val spot = npc.schedule.placeAt(hour)
        val activityIndex = (if (day < 0) -day else day) % ACTIVITY_LINES.size
        lines.add(String.format(ACTIVITY_LINES[activityIndex], spot.place, spot.activity))

        for (t in npc.identity.traits) {
            val traitLine = TRAIT_LINES[t]
            if (traitLine != null) {
                lines.add(traitLine)
                break
            }
        }

        val safeTier = tier.coerceIn(0, TIER_LINES.size - 1)
        val tierPool = TIER_LINES[safeTier]
        lines.add(tierPool[(hashVal / 7) % tierPool.size])

        if (eventId != null) {
            EVENT_LINES[eventId]?.let { lines.add(it) }
        }

        val evs = npc.memory.recallAboutPlayer()
        if (evs.isNotEmpty()) {
            lines.add("Помню: ${evs[0].text}.")
        }

        when (npc.identity.mood) {
            "happy" -> lines.add("Настроение хорошее.")
            "angry" -> lines.add("Настроение, сам понимаешь, не очень.")
            "sleepy" -> lines.add("Ох, клонит в сон…")
        }

        return lines.joinToString(" ")
    }
}
