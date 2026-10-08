package com.aistudio.cozytown.storage

import android.content.Context
import com.aistudio.cozytown.core.SaveCodec
import com.aistudio.cozytown.core.TextSafe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SavePayload(
    val data: String,
    val checksum: String,
    val version: Int
)

/**
 * Локальное сохранение: JSON + SHA-256 checksum, temp + .bak,
 * восстановление при повреждении, отказ от «будущих» версий.
 * Формат данных — v3 (EngineState), миграция v1/v2 в SaveCodec.
 */
class SaveGame(private val context: Context) {
    companion object {
        const val VERSION = SaveCodec.CURRENT_VERSION
        const val SAVE_FILENAME = "save.json"
        const val BACKUP_SUFFIX = ".bak"
        const val TEMP_SUFFIX = ".tmp"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun sanitizeName(name: String): String = TextSafe.sanitizeName(name)

        fun checksum(data: String): String = TextSafe.checksum(data)
    }

    private val saveFile: File
        get() = File(context.filesDir, SAVE_FILENAME)

    private val backupFile: File
        get() = File(context.filesDir, SAVE_FILENAME + BACKUP_SUFFIX)

    private val tempFile: File
        get() = File(context.filesDir, SAVE_FILENAME + TEMP_SUFFIX)

    fun saveRaw(dataJson: String): Boolean {
        return try {
            val payload = SavePayload(
                data = dataJson,
                checksum = TextSafe.checksum(dataJson),
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

    /** Возвращает пару (dataJson, version) из валидного снапшота или бэкапа. */
    fun loadRaw(): Pair<String, Int>? {
        if (saveFile.exists()) {
            readValid(saveFile)?.let { return it }
        }
        if (backupFile.exists()) {
            readValid(backupFile)?.let { return it }
        }
        return null
    }

    fun resetProgress() {
        if (saveFile.exists()) saveFile.delete()
        if (backupFile.exists()) backupFile.delete()
        if (tempFile.exists()) tempFile.delete()
    }

    private fun isValidSnapshot(file: File): Boolean = readValid(file) != null

    private fun readValid(file: File): Pair<String, Int>? {
        return try {
            if (!file.exists()) return null
            val text = file.readText(Charsets.UTF_8)
            val payload = json.decodeFromString(SavePayload.serializer(), text)
            if (payload.version < 0 || payload.version > VERSION) return null
            if (payload.checksum != TextSafe.checksum(payload.data)) return null
            payload.data to payload.version
        } catch (e: Exception) {
            null
        }
    }
}
