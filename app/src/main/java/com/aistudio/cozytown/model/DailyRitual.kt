package com.aistudio.cozytown.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
data class GoalDef(val id: String, val text: String, val target: Int)

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
    val goals: List<GoalDef> = emptyList(),
    val progress: Map<String, Int> = emptyMap(),
    val claimed: Boolean = false
)

/**
 * Дневные цели. Набор целей меняется каждый день (ротация из пула),
 * награда — монеты и опыт.
 */
class DailyRitual {
    companion object {
        val DEFAULT_GOALS = listOf(
            GoalDef("talk", "Поговори с жителями", 2),
            GoalDef("kind", "Сделай добрый поступок", 1),
            GoalDef("gossip", "Узнай сплетню", 1)
        )
        const val REWARD = 25
    }

    var day: Int = 0
    var goals: List<GoalDef> = DEFAULT_GOALS
    val progress: MutableMap<String, Int> = mutableMapOf()
    var claimed: Boolean = false

    fun resetForDay(d: Int, newGoals: List<GoalDef> = DEFAULT_GOALS): Boolean {
        if (day == d && progress.isNotEmpty()) {
            return false
        }
        day = d
        goals = if (newGoals.isEmpty()) DEFAULT_GOALS else newGoals
        progress.clear()
        claimed = false
        for (g in goals) {
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
        return goals.map { g ->
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
            goals = goals,
            progress = progress.toMap(),
            claimed = claimed
        )
    }

    fun loadState(state: DailyRitualState) {
        day = state.day.coerceIn(0, 9999)
        claimed = state.claimed
        goals = if (state.goals.isEmpty()) DEFAULT_GOALS else state.goals.map {
            GoalDef(it.id.take(24), it.text.take(80), it.target.coerceIn(1, 20))
        }
        progress.clear()
        for (g in goals) {
            val v = state.progress[g.id] ?: 0
            progress[g.id] = max(v, 0)
        }
    }
}
