## 2025-02-16 - Compose Accessibility Semantics & Labels
**Learning:** Decorative or icon-heavy custom components (like custom Canvas maps or icon-only header indicators) require explicit `semantics { contentDescription = "..." }` and `contentDescription` on Material Icons in Compose so TalkBack and screen readers can announce their purpose accurately.
**Action:** Always verify decorative vs informative icons and canvas views in Compose to ensure proper content descriptions are provided.
