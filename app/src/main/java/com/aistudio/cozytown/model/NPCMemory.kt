package com.aistudio.cozytown.model

import kotlinx.serialization.Serializable

@Serializable
data class MemoryEvent(
    val text: String,
    val importance: Int,
    val day: Int,
    val aboutPlayer: Boolean = false
)

@Serializable
data class NPCMemoryState(
    val shortTerm: List<MemoryEvent> = emptyList(),
    val longTerm: List<MemoryEvent> = emptyList(),
    val playerName: String = ""
)

class NPCMemory(
    val shortTerm: MutableList<MemoryEvent> = mutableListOf(),
    val longTerm: MutableList<MemoryEvent> = mutableListOf(),
    var playerName: String = ""
) {
    companion object {
        const val SHORT_TERM_MAX = 10
        const val LONG_TERM_IMPORTANCE = 7
        const val MAX_LONG_TERM = 200
    }

    fun addEvent(text: String, importance: Int, day: Int, aboutPlayer: Boolean = false) {
        val cleanText = text.trim().take(240)
        if (cleanText.isEmpty()) return
        val clampedImportance = importance.coerceIn(0, 20)
        val clampedDay = day.coerceIn(1, 9999)

        shortTerm.add(MemoryEvent(cleanText, clampedImportance, clampedDay, aboutPlayer))
        if (shortTerm.size > SHORT_TERM_MAX) {
            consolidate()
        }
        if (longTerm.size > MAX_LONG_TERM) {
            pruneLongTerm()
        }
    }

    private fun consolidate() {
        val kept = mutableListOf<MemoryEvent>()
        for (event in shortTerm) {
            if (event.importance >= LONG_TERM_IMPORTANCE || event.aboutPlayer) {
                longTerm.add(event)
            } else {
                kept.add(event)
            }
        }
        shortTerm.clear()
        if (kept.size > SHORT_TERM_MAX) {
            shortTerm.addAll(kept.takeLast(SHORT_TERM_MAX))
        } else {
            shortTerm.addAll(kept)
        }
    }

    private fun pruneLongTerm() {
        while (longTerm.size > MAX_LONG_TERM) {
            var worstIndex = 0
            for (i in 1 until longTerm.size) {
                if (longTerm[i].importance < longTerm[worstIndex].importance) {
                    worstIndex = i
                }
            }
            longTerm.removeAt(worstIndex)
        }
    }

    fun recallAboutPlayer(): List<MemoryEvent> {
        val combined = (shortTerm + longTerm).filter { it.aboutPlayer }
        return combined.sortedByDescending { it.importance }
    }

    fun toState(): NPCMemoryState {
        return NPCMemoryState(
            shortTerm = shortTerm.toList(),
            longTerm = longTerm.toList(),
            playerName = playerName
        )
    }

    fun loadState(state: NPCMemoryState) {
        playerName = state.playerName.trim().take(64)
        shortTerm.clear()
        longTerm.clear()
        state.shortTerm.forEach { ev ->
            if (ev.text.isNotBlank()) {
                shortTerm.add(
                    MemoryEvent(
                        ev.text.trim().take(240),
                        ev.importance.coerceIn(0, 20),
                        ev.day.coerceIn(1, 9999),
                        ev.aboutPlayer
                    )
                )
            }
        }
        state.longTerm.forEach { ev ->
            if (ev.text.isNotBlank()) {
                longTerm.add(
                    MemoryEvent(
                        ev.text.trim().take(240),
                        ev.importance.coerceIn(0, 20),
                        ev.day.coerceIn(1, 9999),
                        ev.aboutPlayer
                    )
                )
            }
        }
        while (shortTerm.size > SHORT_TERM_MAX) {
            shortTerm.removeAt(0)
        }
        while (longTerm.size > MAX_LONG_TERM) {
            longTerm.removeAt(0)
        }
    }
}
