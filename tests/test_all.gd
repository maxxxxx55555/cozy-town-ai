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

	var f := FileAccess.open(SaveGame.SAVE_PATH, FileAccess.READ)
	var raw = JSON.parse_string(f.get_as_text())
	f.close()
	raw["data"] = JSON.stringify({"coins": 999999, "day": 3})
	var f2 := FileAccess.open(SaveGame.SAVE_PATH, FileAccess.WRITE)
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
	gc.advance(60 * 17) # +17ч → следующий день 01:00
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

	if failures == 0:
		print("ALL TESTS PASSED")
	else:
		print("TESTS FAILED: %d" % failures)
	quit(1 if failures > 0 else 0)
