# AUDIT_STATE — «Пазл из Жизни»
Волна: 5 | Score: 0% → 70% (w1) → 80% (w2) → 85% (w3) → 90% (w4) → **90%** (9/10; рост не закрыт: сеть)

## GAP-матрица
1. GDD/TZ — ✅ 2. project+scene — ✅ (иконка res://assets/icon.png)
3. NPC память/характер/эмоции (mood по контексту дня) — ✅
4. NPC расписание/отношения/сплетни — ✅
5. Save JSON+checksum+anti-cheat — ✅ 6. Юнит-тесты 41/41 exit 0 — ✅
7. Android preset API36/AAB/custom build/arm64+v7a — ✅
8. Keystore ✅ / templates+AAB ❌ (сеть) — ⚠️½ (CI-workflow собирает AAB на стороне GitHub)
9. AI-декларация+Data Safety+in-app report — ⚠️½ (в репо готово; шаги Console вручную)
10. ASO (описание/иконка/feature/6 скринов) — ✅

## Волна 5
- tests/test_integration.gd: UI-хуки 0:30/5:00, новая сессия «Я помню», report — 8/8 exit 0.
- docs/MONETIZATION.md (IAP только косметика, GodotGooglePlayBilling v8.3+), docs/TESTING.md, mood-механика.

## Волна 6
- Сеть: сменить провайдер/ VPN или запустить CI на внешнем раннере → AAB.
- Перенос в GitHub-remote, чтобы CI собрал AAB автоматически.
