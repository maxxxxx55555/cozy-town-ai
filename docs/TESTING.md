# TESTING — «Пазл из Жизни»

## Команды (локально, из корня)
1. `godot --headless --path . --import` — обновить кэш классов (после добавления `class_name`).
2. `godot --headless --path . -s tests/test_all.gd` — юнит-тесты систем (exit 0 = pass).
3. `godot --headless --path . -s tests/test_integration.gd` — интеграция: UI-хуки, сейв между «сессиями», report.
4. `godot --headless --path . --quit-after 3` — smoke-запуск главной сцены.

## Покрытие
- Память: консолидация, cap краткосрочной, recall по важности.
- Отношения/сплетни: propagation, ослабление важности, бонсы, serialize roundtrip.
- Хуки: 0:30 (имя → все NPC), 5:00 (recall), новая сессия (greet с «Я помню»).
- Безопасность: tampered save, отрицательные монеты, unlock-whitelist, inventory ≤0, отчёт-валидация.
- Расписание/часы/эмоции: смена суток, hour rollover, mood по контексту, anger не стирается.
- Perf-бюджет: 2000 операций памяти + recall < 200 мс (headless).
- Монетизация/anti-cheat: дубликаты предметов, обход анлоков, валюта.

## Android (вручную/на устройстве)
- `adb install` debug APK; профиль: Godot Profiler → FPS ≥ 30 на mid-range ( Snapdragon 6-series / A15 ).
- Проверить: смена ориентации, сворачивание/возврат (NOTIFICATION_WM_CLOSE_REQUEST → save), отсутствие интернет-разрешений.
- Release: AAB подписан release-keystore, target SDK 36, обе ABI (arm64-v8a + armeabi-v7a).
