# AUDIT_STATE — «Пазл из Жизни»
Волна: 3 | Score: 0% → 70% (w1) → 80% (w2) → **85%** (8.5/10)

## GAP-матрица
1. GDD/TZ — ✅
2. project.godot + main scene — ✅
3. NPC: память/характер/эмоции — ✅
4. NPC: расписание/отношения/сплетни — ✅ (4 NPC, места, friendship-бонды w3)
5. Save: JSON+checksum, anti-tamper — ✅ (unlock whitelist, inventory guard)
6. Тесты headless — ✅ 30/30 pass, exit 0 (вкл. perf-бюджет 2000 оп < 200мс)
7. Android preset: API 36, AAB, custom build, arm64+v7a — ✅
8. Build template + keystore + AAB — ⚠️½: keystore ✅; tools/fetch_templates.py (range-выкачка entry) ✅; сеть к GitHub ~0.7-1.5 КБ/с, range-запросы не отдают данные → шаблоны не скачаны → BLOCKER (фоновая докачка идёт)
9. Play Console: signing, AI-decl, DataSafety, listing — ❌ (вне репо, ручной шаг)
10. ASO — ✅ черновики (иконка, feature, 6 скриншотов, описание)

## Артефакты волны 3
- push в origin (bare ../pazly-remote.git) ✅
- tools/patch_local_secrets.ps1 — keystore-инъекция без коммита секретов
- tpl.tpz фоновая докачка (3.7 МБ / 1279 МБ)

## Волна 4
- Докачать шаблоны (или сменить канал) → android/build → AAB → keystore в preset.
- Экранные хуки на арте, perf на устройстве, монетизация-дизайн.
