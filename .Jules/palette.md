## 2025-05-18 - Labeled Buttons and Content Descriptions
**Learning:** Decorative icons inside buttons that already have visible text labels should have `contentDescription = null` to prevent screen readers (e.g., TalkBack) from reading the button label twice.
**Action:** Only add non-null `contentDescription` to icon-only buttons or standalone visual indicators without text labels.
