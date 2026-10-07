package com.aistudio.cozytown.model

import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
data class InventoryState(
    val items: Map<String, Int> = emptyMap()
)

class Inventory {
    companion object {
        const val MAX_ITEM_COUNT = 999
        const val MAX_ID_LENGTH = 32

        fun isSafeId(id: String): Boolean {
            if (id.isEmpty() || id.length > MAX_ID_LENGTH || id.contains("..") || id.contains("/") || id.contains("\\")) {
                return false
            }
            for (c in id) {
                val cl = c.lowercaseChar()
                val ok = (cl in 'a'..'z') || (c in '0'..'9') || c == '_' || c == '-' || c == ':'
                if (!ok) return false
            }
            return true
        }
    }

    val items: MutableMap<String, Int> = mutableMapOf()

    fun addItem(id: String, count: Int = 1): Boolean {
        if (!isSafeId(id) || count <= 0) return false
        val current = items[id] ?: 0
        val nextCount = min(current + count, MAX_ITEM_COUNT)
        if (nextCount <= 0) return false
        items[id] = nextCount
        return true
    }

    fun removeItem(id: String, count: Int = 1): Boolean {
        if (count <= 0 || !items.containsKey(id) || (items[id] ?: 0) < count) return false
        val next = (items[id] ?: 0) - count
        if (next == 0) {
            items.remove(id)
        } else {
            items[id] = next
        }
        return true
    }

    fun count(id: String): Int = items[id] ?: 0

    fun toState(): InventoryState = InventoryState(items.toMap())

    fun loadState(state: InventoryState) {
        items.clear()
        state.items.forEach { (k, v) ->
            val id = k.trim()
            if (isSafeId(id) && v > 0) {
                items[id] = min(v, MAX_ITEM_COUNT)
            }
        }
    }
}
