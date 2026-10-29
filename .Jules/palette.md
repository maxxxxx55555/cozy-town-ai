## 2025-05-10 - Jetpack Compose Canvas Accessibility Semantics
**Learning:** Custom Canvas components in Jetpack Compose are ignored by screen readers like TalkBack unless explicitly merged and annotated with semantics.
**Action:** Always add `Modifier.semantics(mergeDescendants = true) { contentDescription = "..." }` to the parent container of custom Compose Canvas elements.
