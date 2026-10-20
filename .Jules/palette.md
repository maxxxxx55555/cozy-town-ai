## 2025-03-08 - Icons with Visible Text Labels
**Learning:** Icons inside buttons alongside visible text labels should have `contentDescription = null` to avoid duplicate screen reader announcements. Icons without text (like stat indicators without clear text labels) should have localized `contentDescription`s.
**Action:** Keep leading icons in text buttons decorative (`contentDescription = null`), but add descriptions to standalone or metric icons (e.g. currency star icon).
