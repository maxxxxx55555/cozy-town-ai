# AUDIT_STATE — «Пазл из Жизни»
Волна: 2 | Score: 0% → 70% (w1) → **80%** (8/10)

## GAP-матрица
1. GDD/TZ — ✅
2. project.godot + main scene — ✅
3. NPC: память/характер/эмоции — ✅
4. NPC: расписание/отношения/сплетни — ✅ (+gossip propagation w2)
5. Save: JSON+checksum, anti-tamper — ✅ (+unlock whitelist, inventory guard w2)
6. Тесты headless — ✅ 24/24 pass, exit 0 (w2)
7. Android preset: API 36, AAB, custom build, arm64+v7a — ✅
8. Build template + keystore + AAB — ⚠️ keystore ✅; шаблоны качаются (3.3/1100 МБ, ~13КБ/с) → BLOCKER
9. Play Console: signing, AI-decl, DataSafety, listing — ❌ (вне репо, ручной шаг)
10. ASO: описание + иконка + feature + 6 скриншотов — ✅ черновики (docs/ASO.md, assets/store/)

## Волна 3 (приоритет)
- Докачать шаблоны → Install Android Build Template → прописать keystore → AAB.
- Perf-профиль на mid-range, больше NPC/мест, монетизация-дизайн.
