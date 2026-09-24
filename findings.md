# findings.md — факты, проверенные в этой машине/сессии

## Окружение
- Godot 4.7.stable.official.5b4e0cb0f доступен в PATH; Java 17 (keytool) есть; Python 3 есть.
- Export templates НЕ установлены: `%APPDATA%/Godot/export_templates/4.7.stable/` пуст.
- Локальный remote для push: `../pazly-remote.git` (bare), origin настроен.

## Сеть (блокер AAB)
- GitHub release asset (export_templates.tpz, 1279.2 МБ): ~0.7–19 КБ/с, `Range` не поддерживается
  (повторный `curl -C -` перезапускает загрузку), HEAD отвечает ~15 с.
- Cloudflare speed test: 2 МБ скачаны на 18 985 Б/с → проблема не только в GitHub.
- Мёртвые/недоступные зеркала: tuna, ustc, tpl/downloads.tuxfamily, ghproxy.net/cc/cn, gh-proxy.net,
  ghfast.top, gh.llkk.cc, gh.ddlc.top, gitcode.
→ Вывод: AAB невозможен на этой машине без смены канала; подготовлены CI-workflow и скрипты.

## Технические факты Godot 4.7 (проверено кодом)
- `Crypto.sha256_buffer` отсутствует → `HashingContext` (SHA-256) работает.
- `Node.find_child(pattern, recursive, owned=true)` — код-ноды требуют `owned=false`.
- `for x in dict` перебирает ключи; пары — через `dict.keys()`.
- `--headless` + `SceneTree`-скрипт через `_init`/`_initialize` — рабочий паттерн авто-тестов.
- `root.get_texture().get_image()` в оконном режиме даёт реальные скриншоты (headless — нет).
- Android-пауза: `MainLoop.NOTIFICATION_APPLICATION_PAUSED` (WM_CLOSE_REQUEST — только десктоп).

## Игровые данные (для баланса)
- 200 NPC × 10 кадров (tick + pos_for) ≈ 10–20 мс → запас под 30+ FPS на mid-range.
- Память: 2000 операций + recall ≈ 17 мс.