package com.aistudio.cozytown.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueComposerTest {

    @Test
    fun testComposeWithoutPlayerName() {
        val npc = NPC(
            identity = NPCIdentity("Марта", listOf("добрая")),
            memory = NPCMemory(),
            schedule = DaySchedule()
        )
        val line = DialogueComposer.compose(npc, day = 1, hour = 9)
        assertEquals("Привет! Как тебя зовут?", line)
    }

    @Test
    fun testComposeWithPlayerName() {
        val npc = NPC(
            identity = NPCIdentity("Марта", listOf("добрая")),
            memory = NPCMemory().apply { playerName = "Алекс" },
            schedule = DaySchedule().apply { addSlot(9, "пекарня", "печёт хлеб") }
        )
        val line = DialogueComposer.compose(npc, day = 1, hour = 9)
        assertTrue(line.contains("Алекс"))
        assertTrue(line.contains("пекарня"))
    }
}
