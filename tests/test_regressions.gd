extends SceneTree
## Регрессионные проверки production-контура. Запуск: godot --headless --path . -s tests/test_regressions.gd

var failures := 0
var owned_nodes: Array[Node] = []

func check(cond: bool, name: String) -> void:
	if cond:
		print("PASS: ", name)
	else:
		failures += 1
		print("FAIL: ", name)

func own_npc() -> NPC:
	return own(NPC.new()) as NPC

func own(node: Node) -> Node:
	owned_nodes.append(node)
	return node

func _init() -> void:
	SaveGame.set_path("user://save_regressions.json")
	ReportService.set_path("user://reports_regressions.json")
	for suffix in ["", SaveGame.BACKUP_SUFFIX, SaveGame.TEMP_SUFFIX, ".badver"]:
		var cleanup_path: String = SaveGame.path() + str(suffix)
		if FileAccess.file_exists(cleanup_path):
			DirAccess.remove_absolute(ProjectSettings.globalize_path(cleanup_path))

	for suffix in ["", ReportService.BACKUP_SUFFIX, ReportService.TEMP_SUFFIX]:
		var report_cleanup_path: String = ReportService.path() + str(suffix)
		if FileAccess.file_exists(report_cleanup_path):
			DirAccess.remove_absolute(ProjectSettings.globalize_path(report_cleanup_path))
	check(ReportService.submit("other", "Первый отчёт", {}), "report first snapshot")
	check(ReportService.submit("bug", "Второй отчёт", {}), "report second snapshot")
	var damaged_report := FileAccess.open(ReportService.path(), FileAccess.WRITE)
	damaged_report.store_string("broken")
	damaged_report.close()
	check(ReportService.list().size() == 1 and ReportService.list()[0]["text"] == "Первый отчёт", "damaged report falls back to backup")

	# 1 секунда реального времени = 1 игровая минута (GDD/TZ).
	var clock := GameClock.new()
	var before := clock.total_minutes
	clock.advance(1.0)
	check(clock.total_minutes == before + 1, "one real second advances one game minute")
	var fractional := GameClock.new()
	for _i in range(120):
		fractional.advance(1.0 / 60.0)
	check(fractional.total_minutes == 8 * 60 + 2, "fractional frame deltas accumulate into game minutes")

	# Загрузка часов нормализует даже испорченный/старый сейв.
	var loaded_clock := GameClock.new()
	loaded_clock.from_dict({"day": "broken", "total_minutes": 999999})
	check(loaded_clock.day == 1 and loaded_clock.total_minutes >= 0 and loaded_clock.total_minutes < 24 * 60, "clock load normalizes malformed values")

	# Вложенные записи памяти не должны ронять загрузку сейва.
	var memory := NPCMemory.new()
	memory.from_dict({"short_term": [{"text": "valid", "importance": 9, "day": 2, "about_player": true}, "bad"], "long_term": [null, {"text": "kept", "importance": 8, "day": 1, "about_player": true}]})
	check(memory.short_term.size() == 1 and memory.long_term.size() == 1, "memory load sanitizes malformed records")
	check(memory.recall_about_player().size() == 2, "memory load retains valid player events")

	# После двух атомарных сохранений повреждение последнего файла не уничтожает предыдущий валидный снимок.
	check(SaveGame.save_state({"coins": 1, "player_name": "Первый"}), "regression save first snapshot")
	check(SaveGame.save_state({"coins": 2, "player_name": "Второй"}), "regression save second snapshot")
	var damaged := FileAccess.open(SaveGame.path(), FileAccess.WRITE)
	damaged.store_string("{\"data\":\"broken\"}")
	damaged.close()
	check(SaveGame.load_state().get("coins", 0) == 1, "damaged primary save falls back to backup")

	# Recovery-запись не должна вытеснять валидный backup повреждённым primary.
	check(SaveGame.save_state({"coins": 3}), "recovery save writes new primary")
	check(SaveGame._load_from(SaveGame.path() + SaveGame.BACKUP_SUFFIX, []).get("coins", 0) == 1, "recovery save keeps valid backup")

	# Валидный вторичный снимок всё ещё читается как основной.
	check(SaveGame.save_state({"coins": 4}), "regression save fourth snapshot")
	check(SaveGame.load_state().get("coins", 0) == 4, "valid primary save takes precedence")

	# Повреждённый тип версии не должен ронять парсер сейва.
	var bad_version_path := SaveGame.path() + ".badver"
	var bad_version := {"data": "{}", "checksum": SaveGame._checksum("{}"), "version": {"bad": true}}
	var bad_file := FileAccess.open(bad_version_path, FileAccess.WRITE)
	bad_file.store_string(JSON.stringify(bad_version))
	bad_file.close()
	check(SaveGame._load_from(bad_version_path, []).is_empty(), "malformed save version is rejected safely")
	var invalid_version_data := JSON.stringify({"day": 2, "coins": 7})
	var invalid_version_path := SaveGame.path() + ".invalidversion"
	var invalid_version := {"data": invalid_version_data, "checksum": SaveGame._checksum(invalid_version_data), "version": {"bad": true}}
	var invalid_version_file := FileAccess.open(invalid_version_path, FileAccess.WRITE)
	invalid_version_file.store_string(JSON.stringify(invalid_version))
	invalid_version_file.close()
	check(SaveGame._load_from(invalid_version_path, []).is_empty(), "invalid save version cannot load valid data")

	# Испорченные вложенные секции не должны приводить к runtime-ошибке при старте.
	var malformed := SaveGame.sanitize({"clock": "broken", "npcs": [{"relationships": "broken", "memory": "broken"}]})
	check(malformed["clock"] is Dictionary, "malformed clock replaced with safe dictionary")
	var malformed_npc := own_npc()
	malformed_npc.from_dict(malformed["npcs"][0])
	check(malformed_npc.identity.relationships.is_empty() and malformed_npc.memory.short_term.is_empty(), "malformed npc data is safely ignored")
	var malformed_trust := own_npc()
	malformed_trust.from_dict({"npc_name": "Марта", "trust": {"bad": true}})
	check(is_equal_approx(malformed_trust.identity.trust, 0.0), "malformed npc trust is safely ignored")
	var malformed_sections := {"clock": "broken", "inventory": "broken", "ritual": "broken", "npcs": ["broken"]}
	var normalized_sections := SaveGame.sanitize(malformed_sections)
	check(normalized_sections["clock"] is Dictionary and normalized_sections["inventory"] is Dictionary, "malformed save sections normalized")

	# Инвентарь не должен принимать бесконечные/неизвестные значения из сейва.
	var guarded_inventory := Inventory.new()
	guarded_inventory.from_dict({"apple": 999999, "../../secret": 2, "": 1, "valid_item": 2})
	check(guarded_inventory.count("apple") == Inventory.MAX_ITEM_COUNT, "inventory count capped")
	check(not guarded_inventory.items.has("../../secret") and not guarded_inventory.items.has(""), "inventory ids sanitized")
	check(guarded_inventory.count("valid_item") == 2, "inventory valid item retained")

	# Сериализация дробного игрового времени не теряет остаток.
	var clock_save := GameClock.new()
	clock_save.advance(0.5)
	var clock_restored := GameClock.new()
	clock_restored.from_dict(clock_save.to_dict())
	check(is_equal_approx(clock_restored.to_dict()["pending_seconds"], 0.5), "fractional clock remainder survives save")

	for node in owned_nodes:
		if is_instance_valid(node):
			node.free()

	if failures == 0:
		print("ALL REGRESSION TESTS PASSED")
	else:
		print("REGRESSION TESTS FAILED: %d" % failures)
	quit(1 if failures > 0 else 0)
