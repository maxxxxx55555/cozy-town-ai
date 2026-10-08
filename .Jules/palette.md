## 2025-05-18 - Compose Input Accessibility and Keyboard Submissions
**Learning:** Single-line text inputs (`OutlinedTextField`) without IME actions force users to dismiss the soft keyboard before tapping action buttons. Additionally, metric/status badges containing multiple text or icon elements are read as fragmented items by screen readers unless descendants are explicitly merged.
**Action:** Always pair single-line text fields with `KeyboardOptions(imeAction = ImeAction.Done)` and `KeyboardActions`, and use `.semantics(mergeDescendants = true)` on compact status badges.
