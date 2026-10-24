# Palette's Journal - Critical Learnings

## 2026-10-24 - Canvas Accessibility in Jetpack Compose
**Learning:** Custom drawn Compose Canvas elements (like maps, custom charts, avatars) are invisible to screen readers unless explicitly wrapped or annotated with `Modifier.semantics(mergeDescendants = true) { contentDescription = ... }`.
**Action:** Always provide rich `contentDescription` on the canvas wrapper Box or semantics node summarizing dynamic state for TalkBack users.
