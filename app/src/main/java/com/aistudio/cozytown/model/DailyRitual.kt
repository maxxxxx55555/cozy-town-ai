package com.aistudio.cozytown.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
data class GoalStatus(
    val id: String,
    val text: String,
    val current: Int,
    val target: Int,
    val done: Boolean
)

@Serializable
data class DailyRitualState(
    val day: Int = 0,
    val progress: Map<String, Int> = emptyMap(),
    val claimed: Boolean = false
)

class DailyRitual {
    companion object {
        data class GoalDef(val id: String, val text: String, val target: Int)
        val GOALS = listOf(
            GoalDef("talk", "Поговори с жителями", 2),
            GoalDef("kind", "Сделай добрый поступок", 1),
            GoalDef("gossip", "Узнай сплетню", 1)
        )
        const val REWARD = 25
    }

    var day: Int = 0
    val progress: MutableMap<String, Int> = mutableMapOf()
    var claimed: Boolean = false

    fun resetForDay(d: Int): Boolean {
        if (day == d && progress.isNotEmpty()) {
            return false
        }
        day = d
        progress.clear()
        claimed = false
        for (g in GOALS) {
            progress[g.id] = 0
        }
        return true
    }

    fun record(eventId: String) {
        if (progress.containsKey(eventId)) {
            progress[eventId] = (progress[eventId] ?: 0) + 1
        }
    }

    fun status(): List<GoalStatus> {
        return GOALS.map { g ->
            val cur = progress[g.id] ?: 0
            GoalStatus(
                id = g.id,
                text = g.text,
                current = min(cur, g.target),
                target = g.target,
                done = cur >= g.target
            )
        }
    }

    fun allDone(): Boolean {
        return status().all { it.done }
    }

    fun claim(): Int {
        if (claimed || !allDone()) {
            return 0
        }
        claimed = true
        return REWARD
    }

    fun toState(): DailyRitualState {
        return DailyRitualState(
            day = day,
            progress = progress.toMap(),
            claimed = claimed
        )
    }

    fun loadState(state: DailyRitualState) {
        day = state.day.coerceIn(0, 9999)
        claimed = state.claimed
        progress.clear()
        for (g in GOALS) {
            val v = state.progress[g.id] ?: 0
            progress[g.id] = max(v, 0)
        }
    }
}
