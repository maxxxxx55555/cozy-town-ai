package com.aistudio.cozytown.storage

import android.content.Context
import com.aistudio.cozytown.model.DailyRitualState
import com.aistudio.cozytown.model.GameClockState
import com.aistudio.cozytown.model.InventoryState
import com.aistudio.cozytown.model.NPCState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

@Serializable
data class GameSaveState(
    val player_name: String = "",
    val coins: Int = 0,
    val clock: GameClockState = GameClockState(),
    val inventory: InventoryState = InventoryState(),
    val ritual: DailyRitualState = DailyRitualState(),
    val music_enabled: Boolean = true,
    val unlocks: List<String> = emptyList(),
    val npcs: List<NPCState> = emptyList()
)

@Serializable
data class SavePayload(
    val data: String,
    val checksum: String,
    val version: Int
)

class SaveGame(private val context: Context) {
    companion object {
        const val VERSION = 2
        const val MAX_COINS = 999999
        const val MAX_NAME = 24
        const val SAVE_FILENAME = "save.json"
        const val BACKUP_SUFFIX = ".bak"
        const val TEMP_SUFFIX = ".tmp"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun sanitizeName(name: String): String {
            var n = name.trim().replace("\n", "").replace("\r", "").replace("\t", "")
            // Remove BBCode / markup tags like [b]...[/b]
            n = n.replace(Regex("\\[[^\\]]*\\]"), "")
            return n.take(MAX_NAME)
        }

        fun checksum(data: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(data.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    private val saveFile: File
        get() = File(context.filesDir, SAVE_FILENAME)

    private val backupFile: File
        get() = File(context.filesDir, SAVE_FILENAME + BACKUP_SUFFIX)

    private val tempFile: File
        get() = File(context.filesDir, SAVE_FILENAME + TEMP_SUFFIX)

    fun saveState(state: GameSaveState): Boolean {
        return try {
            val sanitized = sanitize(state)
            val dataJson = json.encodeToString(GameSaveState.serializer(), sanitized)
            val payload = SavePayload(
                data = dataJson,
                checksum = checksum(dataJson),
                version = VERSION
            )
            val payloadJson = json.encodeToString(SavePayload.serializer(), payload)
            tempFile.writeText(payloadJson, Charsets.UTF_8)

            if (saveFile.exists()) {
                if (isValidSnapshot(saveFile)) {
                    if (backupFile.exists()) {
                        backupFile.delete()
                    }
                    saveFile.renameTo(backupFile)
                } else {
                    saveFile.delete()
                }
            }
            tempFile.renameTo(saveFile)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadState(): GameSaveState? {
        if (saveFile.exists()) {
            val loaded = loadFromFile(saveFile)
            if (loaded != null) {
                return loaded
            }
        }
        if (backupFile.exists()) {
            return loadFromFile(backupFile)
        }
        return null
    }

    fun resetProgress() {
        if (saveFile.exists()) saveFile.delete()
        if (backupFile.exists()) backupFile.delete()
        if (tempFile.exists()) tempFile.delete()
    }

    private fun isValidSnapshot(file: File): Boolean {
        return try {
            if (!file.exists()) return false
            val text = file.readText(Charsets.UTF_8)
            val payload = json.decodeFromString(SavePayload.serializer(), text)
            if (payload.version < 0 || payload.version > VERSION) return false
            if (payload.checksum != checksum(payload.data)) return false
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun loadFromFile(file: File): GameSaveState? {
        return try {
            if (!file.exists()) return null
            val text = file.readText(Charsets.UTF_8)
            val payload = json.decodeFromString(SavePayload.serializer(), text)
            if (payload.version < 0 || payload.version > VERSION) return null
            if (payload.checksum != checksum(payload.data)) return null
            val state = json.decodeFromString(GameSaveState.serializer(), payload.data)
            sanitize(state)
        } catch (e: Exception) {
            null
        }
    }

    private fun sanitize(state: GameSaveState): GameSaveState {
        val cleanName = sanitizeName(state.player_name)
        val cleanCoins = state.coins.coerceIn(0, MAX_COINS)
        val cleanClock = state.clock.copy(
            day = state.clock.day.coerceIn(1, 9999),
            totalMinutes = state.clock.totalMinutes.coerceIn(0, 24 * 60 - 1)
        )
        return state.copy(
            player_name = cleanName,
            coins = cleanCoins,
            clock = cleanClock
        )
    }
}
