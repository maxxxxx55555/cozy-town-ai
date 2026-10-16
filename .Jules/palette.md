## 2025-02-18 - Compose Form Fields & Canvas Accessibility Semantics
**Learning:** OutlinedTextField components without explicit labels relying on placeholders lose accessibility context once input is entered or when spoken by screen readers. Custom Compose Canvas elements need explicit semantics(contentDescription) to communicate purpose.
**Action:** Always provide explicit `label = { Text(...) }` on OutlinedTextField components and add `semantics { contentDescription = ... }` on interactive Canvas or icon badge containers.
