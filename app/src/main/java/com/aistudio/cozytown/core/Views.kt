package com.aistudio.cozytown.core

/** Лёгкие неизменяемые снимки для UI — без ссылок на живые объекты движка. */

data class NpcView(
    val name: String,
    val mood: String,
    val moodText: String,
    val trust: Float,
    val tier: Int,
    val tierName: String,
    val place: String,
    val placeName: String,
    val activity: String,
    val bio: String,
    val portraitKey: String,
    val isHere: Boolean,
    val questTitle: String,
    val questStepIndex: Int,
    val questSteps: Int,
    val questText: String,
    val questDone: Boolean,
    val questReady: Boolean,
    val lovesHint: String,
    val lovedIds: List<String>
)

data class RequestView(
    val index: Int,
    val npc: String,
    val text: String,
    val itemEmoji: String,
    val itemName: String,
    val need: Int,
    val have: Int,
    val coins: Int,
    val canDo: Boolean
)

data class RecipeView(
    val id: String,
    val name: String,
    val emoji: String,
    val inputsText: String,
    val canCraft: Boolean
)

data class ItemView(
    val id: String,
    val name: String,
    val emoji: String,
    val count: Int,
    val sellPrice: Int,
    val rarity: Int
)

data class UpgradeView(
    val id: String,
    val name: String,
    val emoji: String,
    val price: Int,
    val description: String,
    val owned: Boolean,
    val affordable: Boolean
)

data class AchievementView(
    val id: String,
    val name: String,
    val desc: String,
    val coins: Int,
    val done: Boolean
)

data class GoalView(
    val id: String,
    val text: String,
    val current: Int,
    val target: Int,
    val done: Boolean
)
