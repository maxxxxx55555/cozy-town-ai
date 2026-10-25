## 2025-02-17 - Custom Compose Canvas TalkBack Accessibility
**Learning:** Custom drawn Jetpack Compose Canvas elements (`Canvas`) are invisible to TalkBack screen readers unless `Modifier.semantics(mergeDescendants = true)` with a descriptive `contentDescription` is attached to the parent container.
**Action:** Always add `semantics(mergeDescendants = true)` with dynamic descriptive summary of interactive elements on any custom Canvas parent node.
