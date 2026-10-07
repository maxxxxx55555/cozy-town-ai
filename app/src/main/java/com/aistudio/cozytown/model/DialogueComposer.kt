package com.aistudio.cozytown.model

object DialogueComposer {
    private val OPENERS = mapOf(
        "warm" to listOf("Ох, %s! Как я рада тебя видеть.", "Привет, %s! Я тебя ждала.", "%s! Садись рядом."),
        "cold" to listOf("Опять ты, %s.", "Чего тебе, %s?", "%s. Я не забыла."),
        "neutral" to listOf("Привет, %s.", "А, это ты, %s.", "Доброго дня, %s.")
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
        "любопытная" to "Расскажи, что у тебя нового!"
    )

    fun compose(npc: NPC, day: Int, hour: Int = 9): String {
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

        val evs = npc.memory.recallAboutPlayer()
        if (evs.isNotEmpty()) {
            lines.add("Помню: ${evs[0].text}.")
        }

        when (npc.identity.mood) {
            "happy" -> lines.add("Настроение хорошее.")
            "angry" -> lines.add("Настроение, сам понимаешь, не очень.")
        }

        return lines.joinToString(" ")
    }
}
