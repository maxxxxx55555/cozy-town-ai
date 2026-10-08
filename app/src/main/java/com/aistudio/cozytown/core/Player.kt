package com.aistudio.cozytown.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
data class PlayerState(
    val level: Int = 1,
    val xp: Int = 0,
    val energy: Int = 10,
    val maxEnergyBonus: Int = 0,
    val streak: Int = 0,
    val lastActiveDay: Int = 0,
    val streakClaimedDay: Int = 0,
    val restedDay: Int = 0
)

/**
 * Прогресс игрока: уровень/опыт, силы (энергия действий),
 * серия возвращений (streak).
 */
class Player {
    var level: Int = 1
        private set
    var xp: Int = 0
        private set
    var energy: Int = 10
    var maxEnergyBonus: Int = 0
    var streak: Int = 0
    var lastActiveDay: Int = 0
    var streakClaimedDay: Int = 0
    var restedDay: Int = 0

    /** События для журнала/звуков, которые движок обрабатывает сам. */
    val levelUps = mutableListOf<Int>()

    fun xpNeed(): Int = 60 + (level - 1) * 40

    fun maxEnergy(): Int = 10 + (level - 1) / 2 + maxEnergyBonus

    fun addXp(amount: Int) {
        if (amount <= 0) return
        xp += amount
        while (xp >= xpNeed() && level < 99) {
            xp -= xpNeed()
            level += 1
            energy = maxEnergy()
            levelUps.add(level)
        }
        xp = min(xp, xpNeed())
    }

    fun spendEnergy(cost: Int): Boolean {
        if (cost <= 0) return true
        if (energy < cost) return false
        energy -= cost
        return true
    }

    fun regen(amount: Int) {
        energy = min(energy + amount, maxEnergy())
    }

    fun toState(): PlayerState = PlayerState(
        level = level, xp = xp, energy = energy, maxEnergyBonus = maxEnergyBonus,
        streak = streak, lastActiveDay = lastActiveDay,
        streakClaimedDay = streakClaimedDay, restedDay = restedDay
    )

    fun loadState(s: PlayerState) {
        level = s.level.coerceIn(1, 99)
        xp = max(0, s.xp)
        maxEnergyBonus = s.maxEnergyBonus.coerceIn(0, 10)
        energy = s.energy.coerceIn(0, maxEnergy())
        streak = s.streak.coerceIn(0, 3650)
        lastActiveDay = s.lastActiveDay.coerceIn(0, 9999)
        streakClaimedDay = s.streakClaimedDay.coerceIn(0, 9999)
        restedDay = s.restedDay.coerceIn(0, 9999)
    }
}
