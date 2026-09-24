extends SceneTree
## Интеграционный тест UI-хуков и «прошлой сессии»:
## godot --headless --path . -s tests/test_integration.gd

var failures := 0

func check(cond: bool, name: String) -> void:
	if cond:
		print("PASS: ", name)
	else:
		failures += 1
		print("FAIL: ", name)

func _initialize() -> void:
	await process_frame
	if FileAccess.file_exists(SaveGame.SAVE_PATH):
		DirAccess.remove_absolute(ProjectSettings.globalize_path(SaveGame.SAVE_PATH))

	var scene: PackedScene = load("res://scenes/main.tscn")
	var game = scene.instantiate()
	root.add_child(game)
	await process_frame
	await process_frame

	check(game.npcs.size() == 4, "town spawns 4 npcs")
	check(game.find_child("NameInput", true, false) != null, "ui has name input")

	# Хук 0:30 — NPC запоминает имя
	game.find_child("NameInput", true, false).text = "Макс"
	game.find_child("NameButton", true, false).emit_signal("pressed")
	check(game.player_name == "Макс", "name hook: player_name set")
	var all_know := true
	for n in game.npcs:
		if n.memory.player_name != "Макс":
			all_know = false
	check(all_know, "name hook: all npcs remember name")

	# Разговор + цитата памяти
	game.npcs[0].react_to_action("помог с хлебом", 6, game.clock.day)
	game.find_child("TalkButton", true, false).emit_signal("pressed")
	check((game.find_child("LogLabel", true, false) as RichTextLabel).get_parsed_text().contains("Макс"), "talk greets by name")

	# Хук 5:00 — NPC вспоминает
	game.session_time = 301.0
	await process_frame
	check((game.find_child("LogLabel", true, false) as RichTextLabel).get_parsed_text().contains("вспоминает"), "recall hook at 5:00")

	# Новая «сессия»: сейв → воспоминание
	game._save()
	var game2 = scene.instantiate()
	root.add_child(game2)
	await process_frame
	check(game2.player_name == "Макс", "new session restores name")
	var l3 := (game2.find_child("LogLabel", true, false) as RichTextLabel).get_parsed_text()
	check(l3.contains("Я помню") or l3.contains("Помню"), "new session npc recalls past action")

	# In-app report
	game2.find_child("ReportButton", true, false).emit_signal("pressed")
	check(ReportService.count() >= 1, "report button stores report")

	game.queue_free()
	game2.queue_free()
	await process_frame
	if failures == 0:
		print("ALL INTEGRATION TESTS PASSED")
	else:
		print("INTEGRATION TESTS FAILED: %d" % failures)
	quit(1 if failures > 0 else 0)

