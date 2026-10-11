## 2025-05-18 - Compose TextFields and Header Badges Accessibility
**Learning:** In Jetpack Compose, `OutlinedTextField` relying solely on `placeholder` lacks a permanent accessible label when filled or focused. Additionally, disabled submit buttons without supporting/validation text leave users unaware of character length constraints.
**Action:** Always provide explicit `label` and `supportingText` for input validation in Jetpack Compose forms, and group icon+text badges with semantic `contentDescription`.
