package com.aistudio.cozytown

import com.aistudio.cozytown.model.DaySchedule
import com.aistudio.cozytown.model.NPC
import com.aistudio.cozytown.model.NPCIdentity
import com.aistudio.cozytown.model.NPCMemory
import com.aistudio.cozytown.storage.SaveGame
import com.aistudio.cozytown.ui.GameUiState
import com.aistudio.cozytown.ui.JournalEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUiStateTest {

    @Test
    fun testNameSanitization() {
        val rawName = "  [b]Анна[/b]\n "
        val cleanName = SaveGame.sanitizeName(rawName)
        assertEquals("Анна", cleanName)
    }

    @Test
    fun testInitialGameUiState() {
        val state = GameUiState()
        assertFalse(state.isNameSubmitted)
        assertEquals("", state.playerName)
        assertTrue(state.isMusicEnabled)
    }

    @Test
    fun testNpcMemoryPlayerIntro() {
        val npc = NPC(
            identity = NPCIdentity(npcName = "Марта", traits = listOf("добрая")),
            memory = NPCMemory(),
            schedule = DaySchedule()
        )

        npc.memory.playerName = "Алекс"
        npc.memory.addEvent("познакомился(ась) с игроком Алекс", 8, 1, aboutPlayer = true)

        assertEquals("Алекс", npc.memory.playerName)
        val recalls = npc.memory.recallAboutPlayer()
        assertFalse(recalls.isEmpty())
        assertTrue(recalls[0].text.contains("Алекс"))
    }

    @Test
    fun testJournalEntryHighlight() {
        val entry = JournalEntry(
            id = 1,
            text = "Марта запомнила ваше имя!",
            isHighlight = true
        )
        assertTrue(entry.isHighlight)
        assertFalse(entry.isSystem)
    }
}
