# findings.md — факты, проверенные в этой машине/сессии

## Окружение песочницы (итерация 2.0)
- **Нет Java, Gradle, Android SDK.** Внешняя сеть открыта только к: github.com, codeload.github.com,
  api.github.com, registry.npmjs.org, pypi.org, files.pythonhosted.org.
  `dl.google.com`, `repo1.maven.org`, `services.gradle.org`, `objects.githubusercontent.com` — заблокированы,
  поэтому собрать APK здесь нельзя.
- Обход для проверки логики:
  - JDK: `pip install jdk4py` → `JAVA_HOME=<venv>/lib/python3.11/site-packages/jdk4py/java-runtime`
    (только runtime: есть `java`/`keytool`, но **нет `javac`**).
  - Kotlin-компилятор: npm-пакет `kotlin-compiler` (tarball ~90 МБ) → `tools/kotlinc/bin/kotlinc`;
    внутри есть `kotlin-stdlib.jar`, `kotlinx-coroutines-core-jvm.jar` и плагин `kotlinx-serialization-compiler-plugin.jar`.
  - kotlinx.serialization (runtime-библиотеки): PyPI-пакет `kotlin-jupyter-kernel`
    → внутри wheel лежат `kotlinx-serialization-core-jvm-1.9.0.jar` и `kotlinx-serialization-json-jvm-1.9.0.jar`
    (скопированы в `tools/harness/lib/`).
  - Итог: `bash tools/harness/build_core.sh` компилирует `model/` + `core/` (с плагином serialization) и
    прогоняет 30 тестов ядра — **без Android SDK и Gradle**.
- Кириллица в stdout JVM: нужен `-Dstdout.encoding=UTF-8`, иначе в выводе `?`.
- `kotlinc` из npm требует, чтобы `java` был в PATH (обёртка вызывает `java`), сам по себе `JAVA_HOME` не спасает.

## Проверенные факты Kotlin 2.4 / AGP
- `@Serializable`-класс, объявленный внутри `companion object`, не резолвится плагином сериализации
  (ошибка «serializer was not found for type … Unresolved qualified name») → такие классы нужно выносить
  в top-level (так появился `model/GoalDef`).
- Компиляция Compose-кода без AndroidX-классов даёт каскад «unresolved reference», но **синтаксические**
  ошибки всё равно видны отдельно (`expecting …`, `unexpected token`) — этим можно проверять парсинг UI-кода.
- Полезный приём для UI-ревью без AndroidX: фильтровать ошибки вида
  `no value passed for parameter`, `no parameter with name`, `too many arguments` — они указывают на реальные
  ошибки вызовов в собственном коде (так был найден `EventBanner(state=)`/`uiState=`).

## Игровые данные (баланс, итерация 2.0)
- BALANCE-прогон (интенсивная симуляция, 3 игровых дня): день 4, 246 монет, уровень 5, 12 достижений,
  46 осколков, 5 улучшений, 30 разговоров, 150 сборов, 42 сплетни, 2 друга, 1 пройденная арка.
- Полный прогон 30 тестов + 3 дня симуляции: ~18 с на JVM (компиляция kotlinc занимает ~17 с из этого времени).
- Экономика: продажа 2–28 монет, апгрейды 60–250, заказ 15–24 (×1.5 в базарный день и с ярмаркой),
  цели дня 25 + 10 xp, серия дней 10×дней (до 70 в день), финал мозаики +500.

## Прошлые находки (Godot-прототип, итерация 1.x)
- Godot 4.7 export templates: 1279 МБ asset отдавался на ~19 КБ/с — сборка AAB в тех условиях была невозможна.
- `Crypto.sha256_buffer` в 4.7 отсутствует (использовался `HashingContext`).
- Godot-код в итерации 2.0 заменён Android/Kotlin-версией; эти факты оставлены как исторический контекст.
