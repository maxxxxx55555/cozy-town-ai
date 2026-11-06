# Palette's Journal - Critical UX Learnings

## 2025-05-18 - Semantic merging for status badges in Jetpack Compose
**Learning:** In Jetpack Compose, compact status badges consisting of an emoji/icon and adjacent number/text (like ⏰ 08:00 or ⭐ 50) are announced piecemeal or incompletely by TalkBack screen readers (e.g. "alarm clock, 08:00" or just "50" without context).
**Action:** Use `Modifier.semantics(mergeDescendants = true) { contentDescription = "..." }` on composite badges to provide clean, unified spoken context.
