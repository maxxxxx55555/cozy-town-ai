# progress.md — журнал сессии

## Волна 12 (визуальный полиш и robustness)
- Исправлены визуальные дефекты: кэш текстур карты, collision-safe подписи, 6 маршрутов, journal empty-state, 3-колоночные действия, read-only имя, store-кадры 1080×1920.
- Добавлены тесты для malformed отчётов, неизвестной версии сейва, BBCode-имени, ночного расписания, gossip-цели, подсказок журнала и label-layout.
- Исправлен реальный баг игрового времени: дробные `delta` теперь накапливаются до целой игровой минуты.
- Сейв и отчёты получили temp + `.bak`, fallback при повреждённом primary и безопасный JSON-парсер.
- Добавлены regression-тесты для recovery сейва, вложенных данных, trust, инвентаря, дробного времени и отчётов.
- Подключены music/sprite assets, адаптивный UI, реальный диалог отчёта и сброс прогресса.
- Локальный helper переведён на `ANDROID_KEYSTORE_USER`/`ANDROID_KEYSTORE_PASSWORD`; plaintext-пароль удалён из tracked-файлов.
- Итог последней локальной проверки: 87 unit + 43 integration + 25 regression PASS, headless smoke exit 0, capture exit 0.

## Артефакты репозитория
- Код: 14 скриптов (+тесты), main.tscn, Android preset (API 36, AAB, arm64+armeabi-v7a).
- Ассеты: иконка, feature graphic, 6 черновых и 6 реальных скриншотов, 3 SFX, спрайты NPC/мест, 48-секундная фоновая тема.
- Аудио-генератор: `tools/gen_music.py`; процедурный оригинальный трек без внешних сэмплов.
- Инструменты: gen_sfx.ps1, gen_store_art.ps1, capture_screens.gd, fetch_templates.py,
  install_android_template.ps1, patch_local_secrets.ps1; CI: android-aab.yml.
- Документы: GDD, TZ, AUDIT_STATE, ASO, PLAY_COMPLIANCE, PRIVACY, MONETIZATION, TESTING, RELEASE_CHECKLIST.

## Открытые пункты (вне репозитория)
1. AAB: нужен канал сети ≥ 1 ГБ или GitHub Actions (workflow готов).
2. Play Console: аккаунт, signing, AI/Data Safety формы, листинг, загрузка AAB.
3. Device QA: FPS ≥ 30, пауза/возврат, размер APK/AAB.