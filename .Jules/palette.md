## 2025-05-18 - Jetpack Compose Canvas and Status Indicator Accessibility
**Learning:** Custom Canvas components and composite status pills (like icon + text counters) in Jetpack Compose are ignored or spoken disjointedly by screen readers (TalkBack) unless explicit `semantics(mergeDescendants = true)` and `contentDescription` properties are attached.
**Action:** Always add `semantics(mergeDescendants = true)` with clear `contentDescription` on status pills and custom interactive canvas containers.
