# TESTING — «Пазл из Жизни» 2.0

## Быстрый путь

```bash
gradle :app:testDebugUnitTest     # 30 JUnit-тестов ядра (один тест на кейс)
gradle :app:assembleDebug         # debug APK
```

## Путь без Android SDK (headless-ядро)

Логика игры не зависит от Android, поэтому её можно собрать и прогнать обычным Kotlin-компилятором:

```bash
bash tools/harness/build_core.sh
```

Скрипт:
1. компилирует `model/` + `core/` (вместе с плагином kotlinx.serialization);
2. компилирует тест-кейсы `app/src/test/.../EngineTestCases.kt` + раннер `tools/harness/HarnessMain.kt`;
3. печатает `PASS/FAIL` по каждому кейсу, итог и строку `BALANCE` (снимок баланса после симуляции 3 дней).

Пути к JDK/kotlinc ищутся в `tools/` (можно задать `JAVA_HOME` и `KOTLINC_HOME`).
Требуемые библиотеки: `kotlinx-serialization-core/json` в `tools/harness/lib/`.

## Что покрыто (30 кейсов)

| Блок | Кейсы |
|---|---|
| Старт | intro_name_memory, name_sanitized |
| Мир | move_gating, gather_energy_items, storm_blocks_pier |
| Экономика | craft_flow, upgrades_unlock, merchant_limits |
| Жители | gift_trust_tiers, talk_and_gossip, help_energy_trust, talk_cooldown_antispam |
| Петля дня | requests_fulfill, ritual_claim, day_rollover_streak, rest_once_per_day |
| Прогресс | quest_chain, achievements_and_shards, puzzle_cap, journal_cap |
| Ограничения мира | no_npc_in_locked_place, loved_gifts_revealed_by_trust |
| Пейсинг | realistic_week_pacing (неделя реалистичной игры + BALANCE-НЕДЕЛЯ) |
| Оффлайн-возврат | offline_return, first_launch_is_silent |
| Данные | save_roundtrip, save_tamper_checksum, legacy_v2_migration, rng_determinism, offline_return |
| Баланс | playthrough_three_days (симуляция трёх дней + BALANCE-отчёт) |

## Что проверяется руками (вне тестов)

- Запуск на устройстве: прокрутка вкладок, тапы по карте, клавиатура при вводе имени.
- Сворачивание/возврат: автосейв при `onPause`, музыка останавливается, при возврате продолжается.
- FPS на mid-range ≥ 30; отсутствие INTERNET-разрешений (`aapt dump permissions app-debug.apk`).
- Прогресс не теряется после перезапуска процесса (имя, монеты, день, доверие, апгрейды).

## Ручной чек-лист прогрессии (10 минут)

1. Ввести имя → во вкладке «Жители» у всех шести «Знакомец», в журнале приветствие.
2. Перейти на рынок в 13:00 → поговорить с Мартой и Аней → в журнале появляется сплетня.
3. Собрать 3 предмета → посмотреть энергию (⚡ уменьшается) → отдохнуть в доме.
4. Собрать «доски + гвозди» → скрафтить шкатулку → подарить Борису (+доверие).
5. Выполнить заказ в «Делах» → монеты и осколок мозаики.
6. Купить улучшение в «Городе» → открывается новое место на карте.
7. Продолжить историю Марты до 2-го шага → в журнале появляется «📖 …».
