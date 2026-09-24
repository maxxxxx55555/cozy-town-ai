# task_plan.md — «Пазл из Жизни»

## Goal
Довести Android-игру (Godot 4.7) до состояния «готово к загрузке в Google Play» и передать владельцу
финальные GDD/TZ. Ограничение: сборка AAB заблокирована сетью на машине разработки.

## Waves
| # | Фокус | Статус |
|---|-------|--------|
| 1 | GDD/TZ, NPC-ядро (память/характер/расписание/сплетни), save+checksum, Android preset | complete |
| 2 | Игровой цикл, хуки 0:30/5:00, часы, инвентарь, ASO-черновики | complete |
| 3 | Отношения/бонды, 4 NPC, perf-бюджет, инструменты шаблонов | complete |
| 4 | In-app report, DialogueComposer, compliance-док, CI AAB | complete |
| 5 | Integration-тесты, mood-механика, иконка, монетизация/тестинг доки | complete |
| 6 | Карта города, opt-in LLM-провайдер, реальные скриншоты | complete |
| 7 | Дневные цели, hardening сейва, пересечения расписаний, red-team тесты | complete |
| 8 | SFX офлайн, privacy-панель и политика | complete |
| 9 | Баг-фикс: Android-пауза, капы памяти, дедуп сплетен, speedhack, версия сейва | complete |
| 10 | Финальные GDD/TZ, чек-лист релиза, кап журнала, финальная верификация | in_progress |

## Next Step
Финальный прогон тестов + commit/push, затем выдать владельцу GDD/TZ и список внешних шагов
(AAB через нормальную сеть или GitHub Actions; Play Console: signing, AI/Data Safety, листинг).

## Errors Encountered
| Error | Resolution |
|---|---|
| `Crypto.sha256_buffer` не существует в 4.7 | заменено на HashingContext SHA-256 |
| `find_child` не находил код-ноды | третий аргумент `owned=false` |
| `for pair in dict` даёт ключи, не пары | итерация по `keys()` |
| Тесты делили save.json → ложные падения | `SaveGame.set_path()` + изолированные пути |
| Клампинг часов сломал старые тесты | тесты клока переписаны на пошаговое время |
| GitHub: 1279 МБ шаблонов при ~19 КБ/с | блокер сети, обход через CI/другую сеть |