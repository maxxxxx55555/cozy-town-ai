# PLAY_COMPLIANCE — «Пазл из Жизни» 2.0

## Что уже соответствует в репозитории

| Требование | Где в репо | Статус |
|---|---|---|
| Target SDK 36 | `app/build.gradle.kts` (`targetSdk = 36`) | ✅ |
| Формат релиза AAB | `gradle :app:bundleRelease` | ✅ (шаг сборки) |
| Отсутствие интернет-разрешений | `AndroidManifest.xml` (нет `uses-permission`) | ✅ |
| In-app report | «Сообщить» (`ReportService`, ≤50 записей, ≤1000 символов) | ✅ |
| Панель данных | «Данные» (локальное хранение, сброс прогресса) | ✅ |
| AI-generated content | диалоги процедурные (без облачных LLM) → декларация «не используется» | ✅ |
| Иконка приложения | `res/mipmap-*` + store-иконка | ✅ |
| Портретная ориентация | `android:screenOrientation="portrait"` | ✅ |
| Min SDK 26 | `minSdk = 26` | ✅ |
| Рейтинг | 3+/Everyone: без насилия, без покупок, без рекламы | ✅ |

## Data Safety

- Собираемые данные: **нет**. Передаваемые данные: **нет**.
- Локально хранится: имя игрока, прогресс, память NPC, локальные отчёты (`filesDir/save.json`, `filesDir/reports.json`).
- Удаление данных: удаление приложения или кнопка «Сброс прогресса» в разделе «Данные».
- Разрешения: не запрашиваются (нет INTERNET, нет доступа к файлам/контактам/геолокации).

## Шаги вне репозитория (ручные)

1. Upload keystore + Play App Signing: ключ создаётся командой
   `keytool -genkeypair -keystore release.keystore -alias upload ...`; секреты — в переменных окружения/CI,
   **не в git**. Для локальной сборки прописать в `app/build.gradle.kts` `signingConfigs.release`.
2. `gradle :app:bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
3. Play Console: создать приложение, заполнить Data Safety (см. выше), App content (AI: не используется),
   возрастной рейтинг, загрузить AAB, описания и графику (docs/ASO.md).
4. Внутреннее тестирование → production.

## Проверки перед загрузкой

```bash
gradle :app:testDebugUnitTest           # ядро: 28 тестов
gradle :app:assembleDebug               # APK
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk   # список разрешений (ожидается пусто)
```
