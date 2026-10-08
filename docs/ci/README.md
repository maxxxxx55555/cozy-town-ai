# CI для Android (временное расположение)

Файл `android.yml` — готовый workflow: юнит-тесты ядра и сборка debug APK.

**Почему он лежит здесь:** git-токен этой сессии не имеет права `workflows`, поэтому GitHub
не принимает push, изменяющий `.github/workflows/*`. Чтобы включить CI:

1. Перемести файл: `git mv docs/ci/android.yml .github/workflows/android.yml`
2. Удали устаревший прототип-воркфлоу: `git rm .github/workflows/android-aab.yml`
3. Закоммить и запушь из аккаунта с правом на workflows (или включи права приложения в настройках репозитория).

Пока CI не включён, тесты и сборку можно запускать локально:

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
bash tools/harness/build_core.sh   # ядро без Android SDK
```

Старый `android-aab.yml` ссылается на Godot-экспорт — он не относится к текущей Android-версии и подлежит удалению.
