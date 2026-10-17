package com.aistudio.cozytown

import com.aistudio.cozytown.model.DailyRitual
import com.aistudio.cozytown.model.GameClock
import com.aistudio.cozytown.storage.SaveGame
import com.aistudio.cozytown.ui.GameUiState
import com.aistudio.cozytown.ui.JournalEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicTest {

    @Test
    fun testSanitizeName() {
        val input = "  [b]Alex[/b]  \n"
        val clean = SaveGame.sanitizeName(input)
        assertEquals("Alex", clean)
    }

    @Test
    fun testDailyRitualProgression() {
        val ritual = DailyRitual()
        ritual.resetForDay(1)

        assertFalse(ritual.allDone())
        assertEquals(0, ritual.claim())

        ritual.record("talk")
        ritual.record("talk")
        ritual.record("kind")
        ritual.record("gossip")

        assertTrue(ritual.allDone())
        assertEquals(DailyRitual.REWARD, ritual.claim())
        assertEquals("Claimed ritual reward should return 0 on second attempt", 0, ritual.claim())
    }

    @Test
    fun testGameClockAdvance() {
        val clock = GameClock()
        clock.day = 1
        clock.totalMinutes = 8 * 60 // 08:00

        assertEquals(1, clock.day)
        assertEquals(8, clock.hour())
        assertEquals("День 1, 08:00", clock.timeString())

        clock.advance(60f) // advance 60 minutes
        assertEquals(9, clock.hour())
        assertEquals("День 1, 09:00", clock.timeString())
    }

    @Test
    fun testGameUiStateDefaults() {
        val state = GameUiState()
        assertEquals("", state.playerName)
        assertFalse(state.isNameSubmitted)
        assertTrue(state.isMusicEnabled)
    }
}
