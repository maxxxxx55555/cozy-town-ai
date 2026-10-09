package com.aistudio.cozytown

import com.aistudio.cozytown.storage.SaveGame
import com.aistudio.cozytown.ui.GameUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUiStateTest {

    @Test
    fun testNameSanitization() {
        val input = "  [b]Алексей[/b]\n "
        val sanitized = SaveGame.sanitizeName(input)
        assertEquals("Алексей", sanitized)
    }

    @Test
    fun testNameSubmissionState() {
        val initialState = GameUiState()
        assertFalse(initialState.isNameSubmitted)
        assertEquals("", initialState.playerName)

        val submittedName = SaveGame.sanitizeName("Алексей")
        val submittedState = initialState.copy(
            playerName = submittedName,
            nameInputText = submittedName,
            isNameSubmitted = true
        )

        assertTrue(submittedState.isNameSubmitted)
        assertEquals("Алексей", submittedState.playerName)
    }
}
