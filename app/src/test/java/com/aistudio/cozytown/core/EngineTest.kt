package com.aistudio.cozytown.core

import org.junit.Assert.fail
import org.junit.Test

/**
 * JUnit-обёртка над общими тест-кейсами ядра (EngineTestCases).
 * Каждый кейс — отдельный тест, чтобы отчёт был читаемым.
 * Запуск: ./gradlew :app:testDebugUnitTest
 * (тот же набор гоняется headless: tools/harness/build_core.sh)
 */
class EngineTest {

    private fun run(name: String) {
        val cases = EngineTestCases.all()
        val case = cases.firstOrNull { it.first == name }
            ?: fail("тест-кейс «$name» не найден и должен быть в EngineTestCases.all()")
        try {
            case.second.invoke()
        } catch (t: Throwable) {
            fail("$name: ${t.message}")
        }
    }

    @Test fun intro_name_memory() = run("intro_name_memory")
    @Test fun name_sanitized() = run("name_sanitized")
    @Test fun move_gating() = run("move_gating")
    @Test fun gather_energy_items() = run("gather_energy_items")
    @Test fun storm_blocks_pier() = run("storm_blocks_pier")
    @Test fun craft_flow() = run("craft_flow")
    @Test fun gift_trust_tiers() = run("gift_trust_tiers")
    @Test fun talk_and_gossip() = run("talk_and_gossip")
    @Test fun help_energy_trust() = run("help_energy_trust")
    @Test fun requests_fulfill() = run("requests_fulfill")
    @Test fun ritual_claim() = run("ritual_claim")
    @Test fun quest_chain() = run("quest_chain")
    @Test fun upgrades_unlock() = run("upgrades_unlock")
    @Test fun achievements_and_shards() = run("achievements_and_shards")
    @Test fun day_rollover_streak() = run("day_rollover_streak")
    @Test fun rest_once_per_day() = run("rest_once_per_day")
    @Test fun merchant_limits() = run("merchant_limits")
    @Test fun save_roundtrip() = run("save_roundtrip")
    @Test fun save_tamper_checksum() = run("save_tamper_checksum")
    @Test fun legacy_v2_migration() = run("legacy_v2_migration")
    @Test fun rng_determinism() = run("rng_determinism")
    @Test fun offline_return() = run("offline_return")
    @Test fun puzzle_cap() = run("puzzle_cap")
    @Test fun journal_cap() = run("journal_cap")
    @Test fun playthrough_three_days() = run("playthrough_three_days")
    @Test fun talk_cooldown_antispam() = run("talk_cooldown_antispam")
    @Test fun no_npc_in_locked_place() = run("no_npc_in_locked_place")
    @Test fun loved_gifts_revealed_by_trust() = run("loved_gifts_revealed_by_trust")
    @Test fun realistic_week_pacing() = run("realistic_week_pacing")
    @Test fun first_launch_is_silent() = run("first_launch_is_silent")
}
