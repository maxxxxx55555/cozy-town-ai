package com.aistudio.cozytown.harness

import com.aistudio.cozytown.core.EngineTestCases

/** Headless-прогон тест-кейсов ядра (без Android/Gradle). */
fun main() {
    var pass = 0
    val fails = mutableListOf<String>()
    for ((name, fn) in EngineTestCases.all()) {
        try {
            fn()
            pass += 1
            println("PASS $name")
        } catch (t: Throwable) {
            fails.add("$name: ${t.message}")
            println("FAIL $name -> ${t.message}")
        }
    }
    println("----")
    println("TOTAL ${pass + fails.size}, PASS $pass, FAIL ${fails.size}")
    if (fails.isNotEmpty()) {
        kotlin.system.exitProcess(1)
    }
}
