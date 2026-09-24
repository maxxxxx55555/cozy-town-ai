# ТЗ — «Пазл из Жизни» (Puzzle of Life), Android / Google Play
Версия: 1.0 (MVP). Волна 10. Полное продуктовое описание — в GDD.md.

## 1. Стек и архитектура
- Godot 4.7.stable, GDScript (без C#/GDExtension), mobile renderer, 2D.
- Офлайн-игра: no INTERNET, no аналитика, no сторонние SDK в MVP.
- Тесты: собственный headless-харнесс (SceneTree-скрипты), exit 0 = pass.

### Файлы
| Файл | Назначение |
|---|---|
| project.godot | конфиг: имя, иконка res://assets/icon.png, 1080×1920 портрет, mobile, ETC2/ASTC |
| scenes/main.tscn | главная сцена (Control, game.gd) |
| scripts/game.gd | ядро экрана, UI, хуки 0:30/5:00, автосейв, SFX, privacy-панель |
| scripts/npc.gd | NPC: память+характер+расписание+реакции+сплетни |
| scripts/npc_memory.gd | краткосрочная/долговременная память, консолидация, капы |
| scripts/npc_identity.gd | traits, mood, trust, отношения |
| scripts/schedule.gd | расписание дня (час → место/занятие) |
| scripts/gossip.gd | реплики сплетен |
| scripts/dialogue_composer.gd | процедурные реплики (память+характер+место+mood) |
| scripts/llm_dialogue.gd | опциональные LLM-диалоги (выключены; фильтры, payload) |
| scripts/save_game.gd | сейв JSON+SHA-256, версия, санитайз, whitelist анлоков |
| scripts/inventory.gd | инвентарь с защитой от ≤0 и дублей |
| scripts/daily_ritual.gd | дневные цели, награда, сброс |
| scripts/game_clock.gd | игровое время, кламп speedhack |
| scripts/report_service.gd | in-app report (≤50 записей, ≤1000 символов) |
| scripts/town_map.gd | карта городка: NPC по расписанию, цвет = mood |
| tests/test_all.gd | юнит-тесты (SceneTree) |
| tests/test_integration.gd | интеграция UI-хуков, сейв между сессиями, report, audio, privacy |
| tools/ | gen_sfx.ps1, gen_store_art.ps1, capture_screens.gd, fetch_templates.py, install_android_template.ps1, patch_local_secrets.ps1 |
| export_presets.cfg | Android preset: AAB, API 36, custom build, arm64+armeabi-v7a |
| .github/workflows/android-aab.yml | CI: импорт шаблонов, тесты, экспорт AAB, артефакт |

## 2. Команды (DoD-верификация)
```
godot --headless --path . --import
godot --headless --path . -s tests/test_all.gd          # 76+ PASS, exit 0
godot --headless --path . -s tests/test_integration.gd  # 20+ PASS, exit 0
godot --headless --path . --quit-after 3                # smoke: exit 0
godot --path . -s tools/capture_screens.gd --resolution 1080x1920
godot --headless --path . --export-release "Android" build/game.aab
```

## 3. Android / Google Play (требования)
- Target SDK = 36 (Android 16) — обязательно для новых приложений с 31.08.2026.
- Формат релиза: только .AAB (APK — локальные тесты).
- Use Custom Build = ON; ABI: arm64-v8a + armeabi-v7a.
- Play App Signing: включён в Console; upload keystore — release.keystore (не в git).
- Плагин GodotGooglePlayBilling v8.3.0+ — только если появится IAP (v1.1), Android v2 plugin system (AAR+Gradle).
- AI-generated content: сейчас декларация «не используется» (текст процедурный); при включении LLM — text generation + фильтры + report.
- Data Safety: «No data collected / No data shared» для MVP (офлайн, без сети).
- In-app report: кнопка «Сообщить» — реализована.
- Store listing: иконка 1024×1024, feature 1024×500, ≥6 скриншотов, описание с хуком (docs/ASO.md).

## 4. Безопасность и red team (покрыто тестами)
1. Tampered save (изменённый data при старом checksum) → отклонён.
2. Сейв из «будущей» версии → отклонён.
3. Монеты 1e9 → кап 999 999; отрицательные → 0.
4. Отрицательные/нулевые предметы → отброшены; вычитание больше остатка → запрещено.
5. Обход анлоков (неизвестные id, дубли) → whitelist.
6. trust=99 / relationship=42 / чужой mood → кламп |1.0| / дефолт neutral.
7. Speedhack/сон: 100 000 сек в кадре → кламп 600 сек, дни не перематываются.
8. Спам целей и сплетен → кулдаун 5 сек, дедуп сплетен за день, кап долгой памяти 200.
9. Потолок журнала UI 300 строк (анти-утечка памяти).

## 5. Производительность
- Прокси-бюджет: 200 NPC × 10 кадров (tick + карта) < 50 мс — фактически ~10–20 мс.
- Целевой FPS на mid-range: ≥ 30 (проверка на устройстве — в релизном чек-листе).

## 6. Definition of Done (релиз)
- [x] GDD/TZ/ASO/PRIVACY/PLAY_COMPLIANCE/MONETIZATION/TESTING актуальны
- [x] 76 unit + 20 integration тестов зелёные, smoke exit 0
- [x] Android preset: API 36, AAB, custom build, обе ABI
- [x] Keystore создан, секреты вне git, скрипт инъекции в preset
- [x] CI-workflow для сборки AAB
- [ ] AAB собран локально (блокер: нет export templates, 1279 МБ, сеть ~19 КБ/с)
- [ ] Загрузка в Play Console, Play App Signing, AI/Data Safety формы, релиз 100%
- [ ] Device QA: FPS ≥ 30, сворачивание/возврат, отсутствие INTERNET-разрешений

## 7. Известные ограничения
- Текстовая (кодовая) графика; нет музыки и анимаций.
- Один сейв-слот; нет облачных сохранений.
- Локализация только RU.