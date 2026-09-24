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

	if failures == 0:
		print("ALL TESTS PASSED")
	else:
		print("TESTS FAILED: %d" % failures)
	quit(1 if failures > 0 else 0)
