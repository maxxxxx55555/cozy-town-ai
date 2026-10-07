package com.aistudio.cozytown.storage

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ReportEntry(
    val reason: String,
    val text: String,
    val context: Map<String, String> = emptyMap(),
    val ts: Long = System.currentTimeMillis() / 1000
)

class ReportService(private val context: Context) {
    companion object {
        const val MAX_REPORTS = 50
        val REASONS = listOf("rude", "bug", "wrong_memory", "other")
        const val REPORT_FILENAME = "reports.json"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    private val reportFile: File
        get() = File(context.filesDir, REPORT_FILENAME)

    fun submit(reason: String, text: String, contextInfo: Map<String, String> = emptyMap()): Boolean {
        if (reason !in REASONS) return false
        val clean = text.trim().take(1000)
        if (clean.isEmpty()) return false

        val currentList = list().toMutableList()
        currentList.add(
            ReportEntry(
                reason = reason,
                text = clean,
                context = contextInfo,
                ts = System.currentTimeMillis() / 1000
            )
        )
        while (currentList.size > MAX_REPORTS) {
            currentList.removeAt(0)
        }
        return try {
            val content = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ReportEntry.serializer()), currentList)
            reportFile.writeText(content, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun list(): List<ReportEntry> {
        return try {
            if (!reportFile.exists()) return emptyList()
            val text = reportFile.readText(Charsets.UTF_8)
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(ReportEntry.serializer()), text)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun count(): Int = list().size
}
