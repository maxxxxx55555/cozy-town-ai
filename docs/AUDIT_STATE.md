# AUDIT_STATE — «Пазл из Жизни»
Волна: 8 | План: **10 волн всего** (1-8 сделаны, 9-10: AAB+релиз) | Score: **90%** (9/10)

## Волна 8
- SFX сгенерированы офлайн (tools/gen_sfx.ps1): click/coin/success.wav, AudioStreamPlayer на действия.
- Кнопка «Данные»: in-app privacy summary о памяти NPC (требование Data Safety/Play).
- docs/PRIVACY.md: политика конфиденциальности + честная форма Data Safety (No data collected, offline).
- Тесты: 70 unit + 18 integration, exit 0.

## Осталось (волны 9-10)
- Wave 9: AAB (нужен внешний сетевой канал для 1279 МБ шаблонов) → загрузка в Play Console.
- Wave 10: релиз/раскатка, device QA (FPS≥30 на mid-range), первый отзыв/итерация.
