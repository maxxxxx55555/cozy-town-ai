# PLAY_COMPLIANCE — «Пазл из Жизни»

## Блокеры/шаги Play Console (ручные, вне репо)
1. Target SDK = **36** — задан в export_presets.cfg (`gradle_build/target_sdk="36"`). Обязательно для новых приложений с 31.08.2026. ✅ в репо
2. Формат релиза: **.AAB** (`gradle_build/export_format=1`). APK — только для локальных тестов. ✅ в репо
3. **Play App Signing** — включён по умолчанию при первой загрузке AAB в Console (шаг Console). Upload keystore: release.keystore (в git НЕ коммитится).
4. **App content → AI-generated content**: сейчас диалоги NPC процедурные (DialogueComposer, без LLM) → декларация «AI не используется для генерации контента». Если включим LLM-диалоги — отметить *text generation* + правила (report-кнопка, фильтры).
5. **In-app report** — кнопка «Сообщить» (ReportService, user://reports.json, ≤50 записей). ✅ в репо. Требование Play для AI-контента выполнено на уровне UI.
6. **Data Safety**: память NPC и сейв хранятся **локально** (user://save.json, user://reports.json), не передаются на сервер → «No data collected» при текущей архитектуре. При добавлении аналитики/IAP обновить форму.
7. Permissions: интернет не запрашивается (`permissions/internet=false`) — соответствует Data Safety «no data shared».
8. Min SDK: по умолчанию Godot (24) — допустимо для API 36.
9. Store listing: иконка 1024×1024, feature graphic 1024×500, ≥6 скриншотов (assets/store/), описание с хуком (docs/ASO.md).
10. Возрастной рейтинг: 3+ / Everyone (нет насилия, нет покупок в MVP).

## Сборка релиза
- Локально: `tools/patch_local_secrets.ps1` → `godot --headless --path . --export-release "Android" build/game.aab` → `git checkout export_presets.cfg`.
- CI: `.github/workflows/android-aab.yml` (keystore из GitHub Secrets: ANDROID_KEYSTORE_B64 / _USER / _PASSWORD).
