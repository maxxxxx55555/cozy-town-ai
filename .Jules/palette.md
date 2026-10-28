# Palette's Journal - Critical UX & Accessibility Learnings

## 2025-05-15 - Custom Compose Canvas Accessibility
**Learning:** Custom drawn Jetpack Compose Canvas elements (such as interactive maps or custom graphics) are invisible to screen readers like TalkBack unless accessibility semantics are explicitly attached to their parent container.
**Action:** Always add `Modifier.semantics(mergeDescendants = true) { contentDescription = "..." }` on the parent node containing custom Canvas views so screen readers can convey the presence and purpose of the element.
