package com.aistudio.cozytown.core

import com.aistudio.cozytown.model.DailyRitualState
import com.aistudio.cozytown.model.GameClockState
import com.aistudio.cozytown.model.InventoryState
import com.aistudio.cozytown.model.NPCState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/** Чистые сейв-утилиты: санитизация, checksum, парсинг/миграция. Без Android. */
object TextSafe {
    const val MAX_NAME = 24

    fun sanitizeName(name: String): String {
        var n = name.trim().replace("\n", "").replace("\r", "").replace("\t", "")
        // Убираем BBCode / markup-теги вида [b]...[/b]
        n = n.replace(Regex("\\[[^\\]]*\\]"), "")
        return n.take(MAX_NAME)
    }

    fun checksum(data: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(data.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/** Старый формат сейва (v1/v2) — для миграции. */
@Serializable
data class LegacySaveState(
    val player_name: String = "",
    val coins: Int = 0,
    val clock: GameClockState = GameClockState(),
    val inventory: InventoryState = InventoryState(),
    val ritual: DailyRitualState = DailyRitualState(),
    val music_enabled: Boolean = true,
    val unlocks: List<String> = emptyList(),
    val npcs: List<NPCState> = emptyList()
)

object SaveCodec {
    const val CURRENT_VERSION = 3

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun encode(state: EngineState): String = json.encodeToString(EngineState.serializer(), state)

    /** Разбирает данные сейва любой поддерживаемой версии в EngineState. */
    fun parse(data: String, version: Int): EngineState? {
        return try {
            if (version >= CURRENT_VERSION) {
                json.decodeFromString(EngineState.serializer(), data)
            } else {
                migrateLegacy(data)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun migrateLegacy(data: String): EngineState {
        val legacy = json.decodeFromString(LegacySaveState.serializer(), data)
        return EngineState(
            version = CURRENT_VERSION,
            playerName = TextSafe.sanitizeName(legacy.player_name),
            coins = legacy.coins.coerceIn(0, 999999),
            clock = legacy.clock,
            inventory = legacy.inventory,
            ritual = legacy.ritual,
            musicEnabled = legacy.music_enabled,
            npcs = legacy.npcs
        )
    }
}
