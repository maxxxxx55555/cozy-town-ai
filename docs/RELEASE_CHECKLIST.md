# RELEASE CHECKLIST — «Пазл из Жизни» (шаги, которые нужны от владельца)

## A. Сборка AAB (единственный технический блокер)
На машине разработки нет export templates: GitHub отдаёт ~19 КБ/с, Range-запросы не работают,
зеркала (tuna/ustc/ghproxy*/ddlc/ghfast/llkk) недоступны → 1279 МБ скачать нельзя.

Вариант 1 — хорошая сеть/VPN:
```
curl -L -o tpl.tpz https://github.com/godotengine/godot/releases/download/4.7-stable/Godot_v4.7-stable_export_templates.tpz
python tools/fetch_templates.py extract     # или распаковать вручную
powershell -File tools/install_android_template.ps1
powershell -File tools/patch_local_secrets.ps1
godot --headless --path . --export-release "Android" build/game.aab
git checkout export_presets.cfg             # убрать секреты из рабочего файла
```
Вариант 2 — GitHub Actions: загрузить репозиторий на GitHub, положить секреты
`ANDROID_KEYSTORE_B64` (base64 release.keystore), `ANDROID_KEYSTORE_USER`, `ANDROID_KEYSTORE_PASSWORD`
→ workflow `.github/workflows/android-aab.yml` соберёт AAB и выдаст артефакт `game-aab`.

Вариант 3 — любой компьютер с нормальной сетью: `godot --headless --path . --export-release "Android" build/game.aab`.

## B. Play Console (ручные шаги)
1. Создать приложение «Пазл из Жизни», язык RU, тип Game, бесплатное.
2. App content:
   - Privacy policy → разместить docs/PRIVACY.md по URL (напр. GitHub Pages) и вставить ссылку.
   - Ads: нет рекламы.
   - AI-generated content: «No» (текст процедурный). После включения LLM — «Yes → text generation».
   - Data safety: No data collected / No data shared.
   - Target audience: 13+ или 3+ (в FAQ указать отсутствие сбора данных).
3. Store listing: название, короткое/полное описание (docs/ASO.md), иконка 1024×1024,
   feature graphic 1024×500, 6 скриншотов (assets/store/real_*.png).
4. Upload AAB → Play App Signing (по умолчанию) → internal testing track → device QA.
5. QA на устройстве: FPS ≥ 30 (Profiler), сворачивание/возврат (сохранение на паузе),
   отсутствие INTERNET-разрешений, размер и время запуска.
6. Production rollout: 20% → 100%; следить за vitals (ANR/crash-free ≥ 99%).

## C. Что уже сделано и проверено в репозитории
- Код, unit/integration/regression автотесты, smoke, скриншоты, ASO-ассеты, спрайты NPC/мест, фоновая музыка, privacy-панель, пользовательский отчёт, сброс прогресса, сейв с checksum/backup, дневные цели, карта, SFX, CI-workflow и скрипты сборки.
- Exact test count: запускать `tests/test_all.gd`, `tests/test_integration.gd`, `tests/test_regressions.gd`; текущий локальный pipeline завершается успешно.