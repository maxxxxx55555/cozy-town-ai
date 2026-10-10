## 2025-05-18 - Jetpack Compose Accessibility Semantics
**Learning:** In Compose, raw text/icon chips (like status bars with emojis) and custom Canvas elements lack clear screen reader context unless explicitly assigned `contentDescription` via `clearAndSetSemantics` or `semantics`.
**Action:** Use `clearAndSetSemantics { contentDescription = "..." }` on compound status chips and `semantics { contentDescription = "..." }` on custom interactive layout containers like Canvas elements.
