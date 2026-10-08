package com.aistudio.cozytown.core

import kotlinx.serialization.Serializable

/**
 * Детерминированный ГПСЧ (LCG) с сериализуемым состоянием —
 * одинаковые seed дают одинаковую последовательность, поэтому
 * «случайность» городка воспроизводима и переживает сохранения.
 */
@Serializable
data class RngState(val seed: Long = 0L)

class Rng(seed: Long) {
    private var state: Long = if (seed == 0L) 0x9E3779B97F4A7C15UL.toLong() else seed

    fun toState(): RngState = RngState(state)

    fun loadState(s: RngState) {
        state = if (s.seed == 0L) 0x2545F4914F6CDD1DL else s.seed
    }

    /** Следующее значение в [0, Long.MAX_VALUE]. */
    fun nextLong(): Long {
        // xorshift64*
        var x = state
        x = x xor (x ushr 12)
        x = x xor (x shl 25)
        x = x xor (x ushr 27)
        state = x
        val r = x * 0x2545F4914F6CDD1DL
        return if (r < 0) -r else r
    }

    /** Целое в [0, bound). */
    fun nextInt(bound: Int): Int {
        if (bound <= 0) return 0
        return (nextLong() % bound).toInt()
    }

    /** Float в [0, 1). */
    fun nextFloat(): Float = (nextLong() % 10000).toInt() / 10000f

    fun chance(p: Float): Boolean = nextFloat() < p

    fun pick(list: List<String>): String = list[nextInt(list.size)]

    fun <T> pickItem(list: List<T>): T = list[nextInt(list.size)]
}
