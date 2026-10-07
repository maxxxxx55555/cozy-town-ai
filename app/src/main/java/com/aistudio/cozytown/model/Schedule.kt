package com.aistudio.cozytown.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleSlot(
    val hour: Int,
    val place: String,
    val activity: String
)

class DaySchedule {
    val slots = mutableListOf<ScheduleSlot>()

    fun addSlot(hour: Int, place: String, activity: String) {
        slots.add(ScheduleSlot(hour, place, activity))
        slots.sortBy { it.hour }
    }

    fun placeAt(hour: Int): ScheduleSlot {
        // Night logic: town is quiet from 22:00 to 06:00, all return home
        if (hour >= 22 || hour < 6) {
            return ScheduleSlot(hour, "home", "sleep")
        }
        var result = ScheduleSlot(0, "home", "sleep")
        for (slot in slots) {
            if (slot.hour <= hour) {
                result = slot
            }
        }
        return result
    }
}
