# AUDIT_STATE — «Пазл из Жизни»
Волна: hardening после wave 10, wave 12 visual/robustness. Это не финальный сертификат Google Play: Android export и device QA должны быть подтверждены отдельно.

## Что реально проверено в текущем workspace
1. Godot 4.7 import/parse/editor scan проходит без ошибок.
2. Unit, integration и regression headless-наборы проходят; smoke главной сцены проходит.
3. Сейв: checksum, temp + `.bak`, fallback при битом primary, санитизация вложенных данных.
4. Время: дробные `delta` накапливаются и 1 реальная секунда = 1 игровая минута.
5. UI: адаптивная сетка, 3-колоночная панель действий, collision-safe подписи карты, контекстная/пустая подсказка журнала, 1080×1920 store-кадры, подключённые спрайты, 48-секундная музыка с явным loop range, mute/unmute с сохранением настройки, отчёт с пользовательским текстом, сброс прогресса.
6. Локальная верификация: 87 unit PASS, 43 integration PASS, 25 regression PASS, headless smoke exit 0, capture exit 0; `git diff --check` чистый.
7. Android preset структурно AAB/target SDK 36/arm64+armeabi-v7a/no INTERNET; CI запускает unit+integration+regression.

## Security history audit (wave 13)
- Текущий tree чист: `git grep` по staged/working tree не находит plaintext-паролей, `puzzle123`, `secrets.local.md` или `ANDROID_KEYSTORE_B64`.
- `tools/patch_local_secrets.ps1` теперь берёт `ANDROID_KEYSTORE_USER` и `ANDROID_KEYSTORE_PASSWORD` из env, не печатает пароль.
- **Критично для релиза:** в истории репозитория (старые коммиты `0f2a383`, `73f09aa`, `071bebb`, `fa8d0a3`, `efa416b`, `dc3056b`) plaintext-пароль `puzzle123` хранится в `tools/patch_local_secrets.ps1`. Он остаётся в git history даже после удаления из текущего коммита.
- **Обязательно перед публикацией:** создай новый release keystore, задай новый alias/password, обнови GitHub Secrets, отозвай старый keystore, удали старые коммиты с паролем (BFG/filter-branch) или примите, что история остаётся открытой.

9. **Контент для стримеров/ютуберов:** добавлены `docs/STREAMER_GUIDE.md` (сцены, интерактив для чата, кайды), `docs/WALKTHROUGH.md` (пошаговый сценарий от Дня 1 до Дня 7), ссылки в `docs/RELEASE_CHECKLIST.md`.

## Честные оставшиеся блокеры
- Локально отсутствует `android_source.zip`/полный export template, поэтому AAB на этой машине не собран.
- Успешный GitHub Actions artifact для текущих изменений и загрузка в Google Play Console не подтверждены.
- Нужна ротация release keystore/пароля: старые реквизиты уже были опубликованы в предыдущем коммите/переписке и не считаются безопасными.
- Нужен physical Android device QA: FPS, сворачивание/возврат, звук, тач-таргеты, размер APK/AAB.
- Это офлайн-игра: локальный checksum/лимиты защищают от случайной и простой ручной порчи, но не являются серверным anti-cheat.
