# TZ — «Пазл из Жизни» (Puzzle of Life)
## Android, Godot 4.7, release в Google Play

Мета-статус: **WAVE 1 — ACTIVE**. Дата: 23.09.2026.

## 1. Стек
- Godot 4.7.stable, GDScript, сцены .tscn, тесты: headless harness `tests/test_all.gd`.
- Рендер: Mobile renderer (2D), FPS ≥ 30 на mid-range.
- Сейвы: JSON + SHA-256 checksum (tamper-reject), путь `user://save.json`.

## 2. Android / Google Play
- Target SDK = 36 (Android 16) — обязательно для новых приложений с 31.08.2026.
- Формат релиза: только .AAB (APK — только локальные тесты).
- Use Custom Build (Gradle) = ON; Architectures: arm64-v8a + armeabi-v7a.
- Play App Signing — включён в Console; upload keystore отдельно, не в git.
- Export preset: `Android` в export_presets.cfg, `gradle_build/export_format=1`.
- AI-generated content декларация: text generation (если LLM-диалоги) + in-app report кнопка.
- Data Safety: честно про память NPC (локальное хранение, без передачи — если сервера нет).
- Store listing: иконка 1024×1024, feature graphic 1024×500, ≥6 скриншотов, описание с хуком.

## 3. Архитектура кода
- `scripts/npc_memory.gd` — краткосрочная (≤10 событий) + долгосрочная память, консолидация по importance.
- `scripts/npc_identity.gd` — имя, характер (traits), эмоция (mood+intensity), trust к игроку, отношения между NPC.
- `scripts/npc.gd` — узел NPC: приветствие, реакция на действие (меняет trust/mood), gossip.
- `scripts/schedule.gd` — дневное расписание (час → место/активность).
- `scripts/gossip.gd` — генерация реплик-сплетен между NPC.
- `scripts/save_game.gd` — save/load JSON+checksum, отклонение tampered-сейва, защита от отрицательных значений.
- `scripts/game.gd` + `scenes/main.tscn` — точка входа; хук: 0:30 — NPC запоминает имя; 5:00 — вспоминает действие прошлой сессии.

## 4. Качество
- Тесты: `godot --headless -s tests/test_all.gd` → exit 0 = pass.
- CI-экспорт: `godot --headless --export-release "Android" build/game.aab`.
- Red team: tampered save, infinite money, unlock bypass, duplicate item, negative values.
- Merge-правило: фича-ветки → <ARENA_BRANCH>, полный регресс перед merge.

## 5. GAP-матрица (будет в docs/AUDIT_STATE.md)
NPC: memory✅/identity/schedule/gossip/save; Android: preset/template/keystore/AAB; Play: signing/AI-decl/DataSafety/listing; Security: checksum/anti-cheat; ASO: имя/иконка/описание.
