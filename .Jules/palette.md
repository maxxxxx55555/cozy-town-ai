# Palette's Journal - Critical UX & Accessibility Learnings

## 2025-05-18 - Jetpack Compose Custom Canvas Accessibility Semantics
**Learning:** Custom Canvas elements in Jetpack Compose (such as interactive maps or custom-drawn diagrams) are completely opaque to TalkBack screen readers by default. Without explicit `semantics` modifiers, screen reader users receive no announcements or context about interactive canvas regions.
**Action:** Always wrap or attach `.semantics { contentDescription = "..."; role = Role.Button }` and use `semantics(mergeDescendants = true)` on custom Canvas elements and merged indicator widgets to guarantee screen reader accessibility.
