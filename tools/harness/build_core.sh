#!/usr/bin/env bash
# Headless-сборка чистого ядра игры (model + core) и тестов без Android/Gradle.
# Нужен JDK (или jdk4py) и kotlinc. Пути можно задать через env:
#   JAVA_HOME, KOTLINC_HOME (папка с bin/kotlinc), либо используются дефолты.
set -e
cd "$(dirname "$0")/../.."
ROOT="$(pwd)"

if [ -z "$JAVA_HOME" ]; then
  if [ -d "$ROOT/tools/.venv/lib/python3.11/site-packages/jdk4py/java-runtime" ]; then
    JAVA_HOME="$ROOT/tools/.venv/lib/python3.11/site-packages/jdk4py/java-runtime"
  elif [ -d "/home/user/tools/.venv/lib/python3.11/site-packages/jdk4py/java-runtime" ]; then
    JAVA_HOME="/home/user/tools/.venv/lib/python3.11/site-packages/jdk4py/java-runtime"
  fi
fi
if [ -z "$KOTLINC_HOME" ]; then
  if [ -x "$ROOT/tools/kotlinc/bin/kotlinc" ]; then
    KOTLINC_HOME="$ROOT/tools/kotlinc"
  elif [ -x "/home/user/tools/kotlinc/bin/kotlinc" ]; then
    KOTLINC_HOME="/home/user/tools/kotlinc"
  fi
fi
[ -n "$JAVA_HOME" ] || { echo "JAVA_HOME не найден (установи jdk4py: pip install jdk4py)"; exit 1; }
[ -n "$KOTLINC_HOME" ] || { echo "kotlinc не найден (npm pack kotlin-compiler)"; exit 1; }
export PATH="$JAVA_HOME/bin:$KOTLINC_HOME/bin:$PATH"

KLIB="$KOTLINC_HOME/lib"
SLIB="$ROOT/tools/harness/lib"
CP="$KLIB/kotlin-stdlib.jar:$SLIB/kotlinx-serialization-core-jvm-1.9.0.jar:$SLIB/kotlinx-serialization-json-jvm-1.9.0.jar"
SRC=app/src/main/java/com/aistudio/cozytown

mkdir -p tools/harness/out
kotlinc -nowarn -classpath "$CP" -Xplugin="$KLIB/kotlinx-serialization-compiler-plugin.jar" \
  $SRC/model/*.kt $SRC/core/*.kt -d tools/harness/out/core.jar

kotlinc -nowarn -classpath "$CP:tools/harness/out/core.jar" \
  app/src/test/java/com/aistudio/cozytown/core/EngineTestCases.kt \
  tools/harness/HarnessMain.kt -d tools/harness/out/tests.jar

java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
  -cp "$CP:tools/harness/out/core.jar:tools/harness/out/tests.jar" \
  com.aistudio.cozytown.harness.HarnessMainKt
