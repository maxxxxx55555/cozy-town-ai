# Palette Journal - Critical Learnings

## 2025-05-18 - Compose Canvas TalkBack Semantics
**Learning:** Custom drawn Compose Canvas elements require `Modifier.semantics(mergeDescendants = true)` with a descriptive `contentDescription` on the parent node for TalkBack screen reader accessibility.
**Action:** When working on Compose Canvas components, add semantic properties describing the canvas contents for accessibility.
