package com.aistudio.cozytown.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class NPCIdentityState(
    val npcName: String = "",
    val traits: List<String> = emptyList(),
    val mood: String = "neutral",
    val moodIntensity: Float = 0.5f,
    val trust: Float = 0.0f,
    val relationships: Map<String, Float> = emptyMap()
)

class NPCIdentity(
    var npcName: String = "",
    var traits: List<String> = emptyList(),
    var mood: String = "neutral",
    var moodIntensity: Float = 0.5f,
    var trust: Float = 0.0f,
    val relationships: MutableMap<String, Float> = mutableMapOf()
) {
    fun applyKarma(k: Int) {
        trust = (trust + k * 0.1f).coerceIn(-1.0f, 1.0f)
        if (k > 0) {
            mood = "happy"
        } else if (k < 0) {
            mood = "angry"
        }
        moodIntensity = (moodIntensity + abs(k) * 0.1f).coerceIn(0.0f, 1.0f)
    }

    fun tone(): String {
        return when {
            trust > 0.3f -> "warm"
            trust < -0.3f -> "cold"
            else -> "neutral"
        }
    }

    fun toState(): NPCIdentityState {
        return NPCIdentityState(
            npcName = npcName,
            traits = traits,
            mood = mood,
            moodIntensity = moodIntensity,
            trust = trust,
            relationships = relationships.toMap()
        )
    }

    fun loadState(state: NPCIdentityState) {
        npcName = state.npcName.take(24)
        trust = state.trust.coerceIn(-1.0f, 1.0f)
        val validMoods = setOf("neutral", "happy", "angry", "sleepy")
        mood = if (state.mood in validMoods) state.mood else "neutral"
        moodIntensity = state.moodIntensity.coerceIn(0.0f, 1.0f)
        relationships.clear()
        state.relationships.forEach { (name, rel) ->
            relationships[name.take(24)] = rel.coerceIn(-1.0f, 1.0f)
        }
    }
}
