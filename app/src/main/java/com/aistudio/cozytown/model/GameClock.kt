package com.aistudio.cozytown.model

import kotlinx.serialization.Serializable
import kotlin.math.floor

@Serializable
data class GameClockState(
    val day: Int = 1,
    val totalMinutes: Int = 8 * 60,
    val pendingSeconds: Float = 0f
)

class GameClock(
    var day: Int = 1,
    var totalMinutes: Int = 8 * 60
) {
    private var pendingSeconds: Float = 0f

    companion object {
        const val MAX_STEP: Float = 600f
    }

    fun advance(realSeconds: Float) {
        val seconds = realSeconds.coerceIn(0f, MAX_STEP)
        pendingSeconds += seconds
        val minutes = floor(pendingSeconds).toInt()
        pendingSeconds -= minutes.toFloat()
        totalMinutes += minutes
        while (totalMinutes >= 24 * 60) {
            totalMinutes -= 24 * 60
            day += 1
        }
    }

    fun hour(): Int = totalMinutes / 60

    fun minute(): Int = totalMinutes % 60

    fun timeString(): String {
        return "День $day, ${hour().toString().padStart(2, '0')}:${minute().toString().padStart(2, '0')}"
    }

    fun toState(): GameClockState {
        return GameClockState(day = day, totalMinutes = totalMinutes, pendingSeconds = pendingSeconds)
    }

    fun loadState(state: GameClockState) {
        day = state.day.coerceIn(1, 9999)
        totalMinutes = state.totalMinutes.coerceIn(0, 24 * 60 - 1)
        pendingSeconds = state.pendingSeconds.coerceIn(0f, 1f)
    }
}
