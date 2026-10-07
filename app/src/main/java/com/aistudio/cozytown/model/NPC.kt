package com.aistudio.cozytown.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

@Serializable
data class NPCState(
    val npcName: String,
    val trust: Float,
    val mood: String,
    val moodIntensity: Float = 0.5f,
    val relationships: Map<String, Float>,
    val memory: NPCMemoryState
)

class NPC(
    val identity: NPCIdentity = NPCIdentity(),
    val memory: NPCMemory = NPCMemory(),
    val schedule: DaySchedule = DaySchedule()
) {
    fun greet(day: Int): String {
        val pname = memory.playerName
        if (pname.isEmpty()) {
            return "Привет! Как тебя зовут?"
        }
        var recalled = ""
        val evs = memory.recallAboutPlayer()
        if (evs.isNotEmpty()) {
            recalled = " Я помню: " + evs[0].text
        }
        return when (identity.tone()) {
            "warm" -> "Рада тебя видеть, $pname!$recalled"
            "cold" -> "Опять ты, $pname.$recalled"
            else -> "Привет, $pname.$recalled"
        }
    }

    fun reactToAction(actionText: String, karma: Int, day: Int): String {
        identity.applyKarma(karma)
        memory.addEvent("игрок: $actionText", abs(karma) + 5, day, aboutPlayer = true)
        return when (identity.tone()) {
            "warm" -> "Спасибо! Я это запомню."
            "cold" -> "Я это запомню…"
            else -> "Понял(а)."
        }
    }

    fun tick(hour: Int) {
        val spot = schedule.placeAt(hour)
        if (identity.mood == "angry") {
            return
        }
        if (hour >= 22) {
            identity.mood = "sleepy"
        } else if (hour == 14) {
            identity.mood = "neutral"
        } else if (spot.activity == "печёт хлеб" && hour in 8..11) {
            identity.mood = "happy"
        }
    }

    fun meet(other: NPC, warmth: Float = 0.2f) {
        val cur = identity.relationships[other.identity.npcName] ?: 0.0f
        identity.relationships[other.identity.npcName] = (cur + warmth).coerceIn(-1.0f, 1.0f)
    }

    fun gossipWith(partner: NPC, day: Int = 1): String {
        val evs = memory.recallAboutPlayer()
        if (evs.isEmpty()) {
            return "${identity.npcName} и ${partner.identity.npcName} молча кивают друг другу."
        }
        val ev = evs[0]
        // Check if already gossiped about this today
        val partnerAll = partner.memory.shortTerm + partner.memory.longTerm
        for (e in partnerAll) {
            if (e.text == ev.text && e.day == day) {
                return Gossip.line(identity.npcName, partner.identity.npcName, ev.text)
            }
        }
        partner.memory.addEvent(ev.text, max(ev.importance - 1, 1), day, ev.aboutPlayer)
        meet(partner, 0.1f)
        partner.meet(this, 0.1f)
        return Gossip.line(identity.npcName, partner.identity.npcName, ev.text)
    }

    fun toState(): NPCState {
        return NPCState(
            npcName = identity.npcName,
            trust = identity.trust,
            mood = identity.mood,
            moodIntensity = identity.moodIntensity,
            relationships = identity.relationships.toMap(),
            memory = memory.toState()
        )
    }

    fun loadState(state: NPCState) {
        identity.loadState(
            NPCIdentityState(
                npcName = state.npcName,
                traits = identity.traits,
                mood = state.mood,
                moodIntensity = state.moodIntensity,
                trust = state.trust,
                relationships = state.relationships
            )
        )
        memory.loadState(state.memory)
    }
}
