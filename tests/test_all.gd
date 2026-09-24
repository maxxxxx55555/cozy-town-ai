extends SceneTree
## Headless-тесты: godot --headless -s tests/test_all.gd → exit 0 = pass

var failures := 0

func check(cond: bool, name: String) -> void:
	if cond:
		print("PASS: ", name)
	else:
		failures += 1
		print("FAIL: ", name)

func _init() -> void:
	SaveGame.set_path("user://save_unit.json")
	# --- Memory ---
	var mem := NPCMemory.new()
	mem.player_name = "Макс"
	for i in range(15):
		mem.add_event("событие %d" % i, 1, 1)
	check(mem.short_term.size() <= NPCMemory.SHORT_TERM_MAX, "memory short-term cap")
	mem.add_event("игрок подарил цветы", 9, 2, true)
	for i in range(5):
		mem.add_event("шум %d" % i, 1, 2)
	var recalled := mem.recall_about_player()
	check(recalled.size() > 0 and recalled[0]["text"] == "игрок подарил цветы", "memory recalls player event")

	# --- Identity / reactions ---
	var npc := NPC.new()
	npc.identity.npc_name = "Марта"
	npc.react_to_action("помог в саду", 5, 1)
	check(npc.identity.trust > 0.0, "trust rises on good action")
	npc.memory.player_name = "Макс"
	npc.react_to_action("разбил окно", -10, 1)
	check(npc.identity.tone() == "cold", "tone becomes cold after bad action")
	check(npc.greet(1).contains("Я помню"), "greet recalls past action")

	# --- Schedule ---
	var sched := DaySchedule.new()
	sched.add_slot(9, "мастерская", "работа")
	sched.add_slot(18, "площадь", "отдых")
	check(sched.place_at(10)["place"] == "мастерская", "schedule midday")
	check(sched.place_at(20)["place"] == "площадь", "schedule evening")
	check(sched.place_at(3)["place"] == "home", "schedule default night")

	# --- Gossip ---
	var a := NPC.new()
	a.identity.npc_name = "Марта"
	a.memory.player_name = "Макс"
	a.memory.add_event("игрок помог в саду", 8, 1, true)
	var b := NPC.new()
	b.identity.npc_name = "Борис"
	var g := a.gossip_with(b)
	check(g.length() > 0 and g.contains("помог"), "gossip mentions event")

	# --- Save roundtrip + anti-tamper + negative values ---
	var state := {"coins": 100, "day": 3, "player_name": "Макс"}
	check(SaveGame.save_state(state), "save writes")
	var loaded := SaveGame.load_state()
	check(loaded.get("coins", -1) == 100, "save roundtrip")

	var f := FileAccess.open(SaveGame.path(), FileAccess.READ)
	var raw = JSON.parse_string(f.get_as_text())
	f.close()
	raw["data"] = JSON.stringify({"coins": 999999, "day": 3})
	var f2 := FileAccess.open(SaveGame.path(), FileAccess.WRITE)
	f2.store_string(JSON.stringify(raw))
	f2.close()
	check(SaveGame.load_state().is_empty(), "tampered save rejected")

	var bad := {"coins": -50}
	check(SaveGame.sanitize(bad)["coins"] == 0, "negative coins sanitized")

	# --- NPC serialize roundtrip ---
	var n1 := NPC.new()
	n1.identity.npc_name = "Марта"
	n1.identity.trust = 0.5
	n1.memory.player_name = "Макс"
	n1.memory.add_event("игрок: чай", 8, 1, true)
	var n2 := NPC.new()
	n2.from_dict(n1.to_dict())
	check(n2.memory.player_name == "Макс" and n2.identity.trust == 0.5, "npc serialize roundtrip")

	# --- GameClock ---
	var gc := GameClock.new()
	check(gc.hour() == 8, "clock starts 08:00")
	gc.advance(60.0)
	for i in range(16): # +17ч по минутам: смена суток
		gc.advance(60.0)
	check(gc.day == 2 and gc.hour() == 1, "clock rolls past midnight")
	var gc2 := GameClock.new()
	gc2.from_dict(gc.to_dict())
	check(gc2.day == 2 and gc2.total_minutes == gc.total_minutes, "clock serialize roundtrip")

	# --- Inventory anti-cheat ---
	var inv := Inventory.new()
	inv.add_item("apple", 3)
	check(inv.count("apple") == 3, "inventory add")
	check(not inv.add_item("apple", -5), "inventory rejects negative add")
	check(not inv.remove_item("apple", 99), "inventory rejects over-remove")
	inv.from_dict({"apple": 2, "hacked": -10, "junk": 0})
	check(inv.count("apple") == 2 and inv.count("hacked") == 0 and inv.count("junk") == 0, "inventory from_dict drops non-positive")

	# --- Gossip propagation ---
	var g1 := NPC.new()
	g1.identity.npc_name = "Марта"
	g1.memory.player_name = "Макс"
	g1.memory.add_event("игрок спас кота", 9, 1, true)
	var g2 := NPC.new()
	g2.identity.npc_name = "Борис"
	g1.gossip_with(g2, 2)
	var b_recall := g2.memory.recall_about_player()
	check(b_recall.size() > 0 and b_recall[0]["text"] == "игрок спас кота", "gossip propagates memory to other npc")
	check(b_recall[0]["importance"] == 8, "gossip weakens importance by 1")

	# --- Unlock whitelist (bypass guard) ---
	var hacked := {"unlocks": ["garden", "premium_hack", "garden"]}
	var clean := SaveGame.sanitize(hacked, ["garden", "festival"])
	check(clean["unlocks"] == ["garden"], "unlock whitelist drops unknown + dupes")

	# --- Relationships (wave 3) ---
	var r1 := NPC.new()
	r1.identity.npc_name = "Лука"
	r1.memory.player_name = "Макс"
	r1.memory.add_event("игрок принёс рыбу", 7, 1, true)
	var r2 := NPC.new()
	r2.identity.npc_name = "Аня"
	r1.gossip_with(r2, 3)
	check(r1.identity.relationships.get("Аня", 0.0) > 0.0, "gossip builds bond a→b")
	check(r2.identity.relationships.get("Лука", 0.0) > 0.0, "gossip builds bond b→a")
	r1.meet(r2, 0.2)
	check(is_equal_approx(r1.identity.relationships["Аня"], 0.3), "meet clamps/bumps relationship")
	var r3 := NPC.new()
	r3.from_dict(r1.to_dict())
	check(r3.identity.relationships.get("Аня", 0.0) == r1.identity.relationships["Аня"], "relationships serialize roundtrip")

	# --- Perf budget (headless simulation) ---
	var t0 := Time.get_ticks_msec()
	var perf_npc := NPC.new()
	for i in range(2000):
		perf_npc.memory.add_event("событие %d" % i, i % 10, 1, i % 3 == 0)
	perf_npc.memory.recall_about_player()
	var dt := Time.get_ticks_msec() - t0
	check(dt < 200, "perf: 2000 memory ops + recall under 200ms (%d ms)" % dt)

	# --- DialogueComposer (wave 4) ---
	var dc_npc := NPC.new()
	dc_npc.identity.npc_name = "Аня"
	dc_npc.identity.traits = PackedStringArray(["энергичная"])
	dc_npc.schedule.add_slot(7, "рынок", "торгует цветами")
	dc_npc.memory.player_name = "Макс"
	dc_npc.memory.add_event("игрок помог с ящиками", 8, 1, true)
	var line := DialogueComposer.compose(dc_npc, 1, 8)
	check(line.contains("Макс"), "composer greets by name")
	check(line.contains("рынок") or line.contains("торгует"), "composer mentions current activity")
	check(line.contains("игрок помог с ящиками"), "composer cites memory")
	var no_name := NPC.new()
	no_name.identity.npc_name = "Борис"
	check(DialogueComposer.compose(no_name, 1, 9) == "Привет! Как тебя зовут?", "composer asks name when unknown")

	# --- ReportService (Play AI-content requirement) ---
	if FileAccess.file_exists(ReportService.REPORT_PATH):
		DirAccess.remove_absolute(ProjectSettings.globalize_path(ReportService.REPORT_PATH))
	check(ReportService.submit("other", "Найдена ошибка в реплике", {"day": 1}), "report accepted")
	check(ReportService.count() == 1, "report stored")
	check(not ReportService.submit("spam_reason", "x", {}), "report rejects unknown reason")
	check(not ReportService.submit("bug", "   ", {}), "report rejects empty text")
	check(not ReportService.submit("bug", "a".repeat(1001), {}), "report rejects oversized text")

	# --- Mood by day context (wave 5) ---
	var mt := NPC.new()
	mt.identity.npc_name = "Марта"
	mt.schedule.add_slot(8, "пекарня", "печёт хлеб")
	mt.tick(9)
	check(mt.identity.mood == "happy", "baker happy in morning")
	mt.tick(23)
	check(mt.identity.mood == "sleepy", "everyone sleepy at night")
	mt.identity.apply_karma(-10)
	mt.tick(9)
	check(mt.identity.mood == "angry", "anger not erased by time")

	# --- TownMap (wave 6) ---
	var tm := TownMap.new()
	var tm_a := NPC.new()
	tm_a.identity.npc_name = "Марта"
	tm_a.schedule.add_slot(8, "пекарня", "печёт хлеб")
	var tm_b := NPC.new()
	tm_b.identity.npc_name = "Лука"
	tm_b.schedule.add_slot(9, "причал", "ловит рыбу")
	tm.npcs = [tm_a, tm_b]
	var pa := tm.pos_for(tm_a, 8)
	var pb := tm.pos_for(tm_b, 9)
	check(pa != pb and pa.x >= 0.0 and pb.x >= 0.0, "townmap places npcs at different spots")
	var pm := tm.pos_for(tm_a, 23)
	check(pm == tm.pos_for(tm_a, 23), "townmap position stable for same hour")
	tm.free()

	# --- LLMDialogue opt-in (wave 6) ---
	check(not LLMDialogue.ENABLED, "llm dialogue disabled by default")
	var llm_npc := NPC.new()
	llm_npc.identity.npc_name = "Марта"
	llm_npc.identity.traits = PackedStringArray(["добрая", "болтливая"])
	llm_npc.identity.trust = 0.8
	llm_npc.memory.player_name = "Макс"
	llm_npc.memory.add_event("игрок помог с хлебом", 8, 1, true)
	var req := LLMDialogue.build_request(llm_npc, "  привет  ", "https://api.example/v1/chat", "k")
	check(req["body"].contains("Марта"), "llm request carries npc name")
	check(req["body"].contains("помог с хлебом"), "llm request carries memory")
	check(req["body"].contains("привет"), "llm request sanitizes input")
	check((req["headers"] as Array).size() == 2, "llm request has auth+content-type headers")
	check(LLMDialogue.sanitize_input("x".repeat(600)).length() == 500, "llm input truncated to 500")
	check(LLMDialogue.filter_output("y".repeat(500)).length() == LLMDialogue.MAX_OUTPUT, "llm output capped")
	check(not "\n\n" in LLMDialogue.filter_output("a\n\n\nb"), "llm output collapses newlines")

	# --- DailyRitual (wave 7) ---
	var dr := DailyRitual.new()
	check(dr.reset_for_day(1), "ritual inits for day 1")
	check(not dr.reset_for_day(1), "ritual no reset same day")
	check(dr.status().size() == 3, "ritual has 3 goals")
	dr.record("talk")
	dr.record("talk")
	dr.record("kind")
	dr.record("gossip")
	check(dr.all_done(), "ritual all done")
	check(dr.claim() == DailyRitual.REWARD, "ritual reward granted")
	check(dr.claim() == 0, "ritual reward only once")
	var dr2 := DailyRitual.new()
	dr2.from_dict({"day": 1, "progress": {"talk": 5, "hack": 99, "kind": -3}, "claimed": false})
	check(dr2.status().size() == 3 and dr2.claim() == 0, "ritual ignores unknown/negative progress")
	dr2.reset_for_day(2)
	check(not dr2.all_done(), "ritual resets next day")
	var dr3 := DailyRitual.new()
	dr3.from_dict(dr.to_dict())
	check(dr3.day == 1 and dr3.claimed, "ritual serialize roundtrip")

	# --- Save hardening / red team (wave 7) ---
	var hacked2 := {"coins": 10 ** 9, "player_name": "x".repeat(50) + "\n", "clock": {"day": -3, "total_minutes": -99}}
	var clean2 := SaveGame.sanitize(hacked2)
	check(clean2["coins"] == SaveGame.MAX_COINS, "coins capped at max")
	check((clean2["player_name"] as String).length() == SaveGame.MAX_NAME and "\n" not in clean2["player_name"], "player_name sanitized/capped")
	check(clean2["clock"]["day"] == 1 and clean2["clock"]["total_minutes"] == 0, "clock clamped")
	var evil_npc := NPC.new()
	evil_npc.from_dict({"npc_name": "z".repeat(40), "trust": 99.0, "mood": "hacked", "relationships": {"X": 42.0}})
	check(evil_npc.identity.trust == 1.0, "trust clamped on load")
	check(evil_npc.identity.mood == "neutral", "unknown mood rejected")
	check(evil_npc.identity.relationships["X"] == 1.0, "relationship clamped on load")

	# --- Perf proxy: 200 NPC update loop < 50ms (headless proxy for 30+ FPS) ---
	var crowd: Array = []
	for i in range(200):
		var c := NPC.new()
		c.identity.npc_name = "NPC%d" % i
		c.schedule.add_slot(8, "площадь", "гуляет")
		c.memory.add_event("e%d" % i, 5, 1, i % 2 == 0)
		crowd.append(c)
	var tmap := TownMap.new()
	tmap.npcs = crowd
	var tp0 := Time.get_ticks_usec()
	for frame in range(10):
		for c in crowd:
			c.tick(8)
		for c in crowd:
			tmap.pos_for(c, 8)
	var tp_ms := (Time.get_ticks_usec() - tp0) / 10000.0
	check(tp_ms < 50.0, "perf: 200 npc × 10 frames update+map < 50ms (%.1f ms)" % tp_ms)
	tmap.free()

	# --- Schedule overlaps for gossip (wave 7) ---
	var so_a := NPC.new()
	so_a.schedule.add_slot(8, "пекарня", "печёт хлеб")
	so_a.schedule.add_slot(13, "рынок", "покупает цветы")
	so_a.schedule.add_slot(18, "площадь", "гуляет")
	var so_b := NPC.new()
	so_b.schedule.add_slot(7, "рынок", "торгует цветами")
	so_b.schedule.add_slot(20, "площадь", "танцует")
	check(so_a.schedule.place_at(14)["place"] == so_b.schedule.place_at(14)["place"], "npcs overlap at market 14:00")
	check(so_a.schedule.place_at(21)["place"] == so_b.schedule.place_at(21)["place"], "npcs overlap at square 21:00")
	check(so_a.schedule.place_at(9)["place"] != so_b.schedule.place_at(9)["place"], "npcs separate in morning")

	# --- Bugfix wave 9 ---
	var big_mem := NPCMemory.new()
	for i in range(300):
		big_mem.add_event("крупное событие %d" % i, 9, i / 30 + 1, true)
	check(big_mem.long_term.size() <= NPCMemory.MAX_LONG_TERM, "long-term memory capped (anti-dos)")
	check(big_mem.recall_about_player().size() > 0, "capped memory still recalls")

	var gs1 := NPC.new()
	gs1.identity.npc_name = "Марта"
	gs1.memory.add_event("игрок купил хлеб", 8, 5, true)
	var gs2 := NPC.new()
	gs2.identity.npc_name = "Борис"
	gs1.gossip_with(gs2, 5)
	gs1.gossip_with(gs2, 5)
	var gs_recall := gs2.memory.recall_about_player()
	check(gs_recall.size() == 1, "gossip not duplicated same day")

	var sh_clock := GameClock.new()
	sh_clock.advance(100000.0)
	check(sh_clock.total_minutes == 8 * 60 + int(GameClock.MAX_STEP) and sh_clock.day == 1, "clock speedhack clamped (no day jump)")
	var sh_clock2 := GameClock.new()
	sh_clock2.advance(16 * 3600.0)
	check(sh_clock2.total_minutes == 8 * 60 + int(GameClock.MAX_STEP) and sh_clock2.day == 1, "background sleep cannot fast-forward days")

	var future := '{"data":"{}","checksum":"%s","version":999}' % SaveGame._checksum("{}")
	var ff := FileAccess.open(SaveGame.path(), FileAccess.WRITE)
	ff.store_string(future)
	ff.close()
	check(SaveGame.load_state().is_empty(), "save from newer game version rejected")

	if failures == 0:
		print("ALL TESTS PASSED")
	else:
		print("TESTS FAILED: %d" % failures)
	quit(1 if failures > 0 else 0)
