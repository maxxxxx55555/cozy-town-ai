package com.aistudio.cozytown

import com.aistudio.cozytown.model.GameClock
import com.aistudio.cozytown.ui.GameUiState
import com.aistudio.cozytown.ui.JournalEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUiStateTest {

    @Test
    fun defaultGameUiState_initializesWithExpectedDefaults() {
        val state = GameUiState()

        assertEquals(1, state.day)
        assertEquals("День 1, 08:00", state.timeString)
        assertEquals(8, state.hour)
        assertEquals(0, state.coins)
        assertEquals("", state.playerName)
        assertFalse(state.isNameSubmitted)
        assertTrue(state.isMusicEnabled)
        assertTrue(state.journalLines.isEmpty())
    }

    @Test
    fun journalEntry_propertiesAreSetCorrectly() {
        val entry = JournalEntry(
            id = 1L,
            text = "Test message",
            isSystem = true,
            isHighlight = false,
            isRecall = false
        )

        assertEquals(1L, entry.id)
        assertEquals("Test message", entry.text)
        assertTrue(entry.isSystem)
        assertFalse(entry.isHighlight)
        assertFalse(entry.isRecall)
    }

    @Test
    fun gameClock_advanceTimeCorrectly() {
        val clock = GameClock()
        assertEquals(1, clock.day)
        assertEquals(8, clock.hour())

        // Advance by 60 minutes
        clock.advance(60f)
        assertEquals(9, clock.hour())
        assertEquals(1, clock.day)
    }
}
