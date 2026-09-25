# SECURITY — «Пазл из Жизни» (Puzzle of Life)

## Текущая защита (уже реализовано)
- Локальный сейв: checksum + temp-файл + `.bak` ротация + fallback при повреждённом primary.
- Версионирование сейва: неизвестные/повреждённые верции отбрасываются.
- Sanitization: вложенные NPC, trust, schedule, memory, daily goals, inventory, item names, player name.
- BBCode: имена игроков не пропускают `[b]`, `[i]`, `[url]` и т.п.
- Report payload: длина ограничена, пустые отчёты не сохраняются.
- Anti-cheat (офлайн): лимиты монет в день, лимиты количества предметов, лимиты количества отчётов, лимиты частоты действий.
- CI: unit + integration + regression запускаются на каждом push/PR.
- Секреты: `tools/patch_local_secrets.ps1` берёт `ANDROID_KEYSTORE_USER` и `ANDROID_KEYSTORE_PASSWORD` из env, не печатает пароль.

## Известные уязвимости (должны быть устранены перед релизом)
1. **Git history содержит plaintext-пароль.**
   - Старые коммиты хранят `puzzle123` в `tools/patch_local_secrets.ps1`.
   - Коммиты: `0f2a383`, `73f09aa`, `071bebb`, `fa8d0a3`, `efa416b`, `dc3056b` и другие.
   - Даже после удаления из текущего коммита пароль остаётся в истории.
   - **Ремедиация:** создай новый keystore, обнови GitHub Secrets, отозвай старый keystore, удали пароль из истории (BFG Repo-Cleaner / `git filter-branch`).
2. **Локальный AAB не подписан новым keystore.**
   - Текущий `release.keystore` может быть старым/тестовым.
   - **Ремедиация:** пересоздай keystore перед публикацией.
3. **Офлайн-игра без серверной проверки.**
   - Локальные чеки/лимиты защищают от случайной и простой ручной порчи, но не от целенаправленного взлома.
   - **Ремедиация:** для production-версии рассмотреть серверную валидацию прогресса или anti-tamper-проверку.

## Чек-лист перед публикацией в Google Play
- [ ] Создан новый release keystore (не используется старый).
- [ ] Новый alias и пароль не совпадают с `puzzle123` / `puzzleoflife`.
- [ ] `ANDROID_KEYSTORE_USER`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEYSTORE_B64` обновлены в GitHub Secrets.
- [ ] Удалён старый keystore (если он использовался для уже опубликованных версий).
- [ ] Удалён plaintext-пароль из git history (BFG/filter-branch).
- [ ] В Google Play Console включён Play App Signing.
- [ ] Загружаемый AAB подписан новым keystore.
- [ ] Проверен Privacy Policy URL и Data Safety section.
- [ ] Проверены store listing: иконка, скриншоты, описание.
- [ ] Проведено внутреннее тестирование (internal testing track).
- [ ] Проведён physical Android device QA.
- [ ] Проверена ориентация (portrait), звук, тач-таргеты, размер APK/AAB.