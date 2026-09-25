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
	SaveGame.set_path("user://save_integration_%d.json" % Time.get_ticks_usec())
	ReportService.set_path("user://reports_integration_%d.json" % Time.get_ticks_usec())
	if FileAccess.file_exists(SaveGame.path()):
		DirAccess.remove_absolute(ProjectSettings.globalize_path(SaveGame.path()))
	if FileAccess.file_exists(SaveGame.path() + SaveGame.BACKUP_SUFFIX):
		DirAccess.remove_absolute(ProjectSettings.globalize_path(SaveGame.path() + SaveGame.BACKUP_SUFFIX))
	if FileAccess.file_exists(SaveGame.path() + SaveGame.TEMP_SUFFIX):
		DirAccess.remove_absolute(ProjectSettings.globalize_path(SaveGame.path() + SaveGame.TEMP_SUFFIX))

	for suffix in ["", ReportService.BACKUP_SUFFIX, ReportService.TEMP_SUFFIX]:
		var report_path: String = ReportService.path() + str(suffix)
		if FileAccess.file_exists(report_path):
			DirAccess.remove_absolute(ProjectSettings.globalize_path(report_path))
	var scene: PackedScene = load("res://scenes/main.tscn")
	var game = scene.instantiate()
	root.add_child(game)
	await process_frame
	await process_frame

	check(game.npcs.size() == 4, "town spawns 4 npcs")
	check(game.find_child("LogLabel", true, false) != null, "ui has journal")
	var hint := game.find_child("JournalHint", true, false) as Label
	check(hint != null and hint.text.contains("Подсказка"), "journal has contextual hint")
	check(game.log_lines <= 2, "initial journal stays compact")
	var initial_log := (game.find_child("LogLabel", true, false) as RichTextLabel).get_parsed_text()
	var journal_view := game.find_child("LogLabel", true, false) as RichTextLabel
	check(journal_view.visible and initial_log.contains("Цели дня") and initial_log.contains("Марта"), "journal renders gameplay text")
	var empty_hint := game.find_child("EmptyJournalHint", true, false) as Label
	check(empty_hint != null and empty_hint.visible, "empty journal has a centered story hint")
	check(empty_hint.text.contains("История уже началась"), "journal hint reflects existing story state")

	# Хук 0:30 — NPC запоминает имя
	game.find_child("NameInput", true, false).text = "Макс"
	game.find_child("NameButton", true, false).emit_signal("pressed")
	check(game.player_name == "Макс", "name hook: player_name set")
	var name_input_after := game.find_child("NameInput", true, false) as LineEdit
	check(name_input_after.text == "Макс" and not name_input_after.editable, "submitted name stays visible and locked")
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

	# Дневные цели: 2 разговора + помощь + сплетня (рынок, 14:00)
	game.clock.total_minutes = 14 * 60
	game.session_time += 6.0
	game.find_child("TalkButton", true, false).emit_signal("pressed")
	game.session_time += 6.0 # кулдаун: цель не засчитывается чаще 5 сек
	game.find_child("TalkButton", true, false).emit_signal("pressed")
	game.find_child("HelpButton", true, false).emit_signal("pressed")
	check(game.ritual.all_done(), "daily goals completed via UI")
	check(game.coins == DailyRitual.REWARD, "daily goals reward granted once")

	# Новая «сессия»: сейв → воспоминание
	game._save()
	var game2 = scene.instantiate()
	root.add_child(game2)
	await process_frame
	check(game2.player_name == "Макс", "new session restores name")
	check((game2.find_child("NameInput", true, false) as LineEdit).text == "Макс", "new session restores name input")
	check(not (game2.find_child("NameInput", true, false) as LineEdit).editable, "restored name field stays locked")
	check(game2.coins == DailyRitual.REWARD, "new session restores coins")
	check((game2.find_child("CoinsLabel", true, false) as Label).text.contains("25"), "coins label shows balance")
	var l3 := (game2.find_child("LogLabel", true, false) as RichTextLabel).get_parsed_text()
	check(l3.contains("Я помню") or l3.contains("Помню"), "new session npc recalls past action")

	# In-app report: открыть диалог, ввести текст и подтвердить.
	game2.find_child("ReportButton", true, false).emit_signal("pressed")
	game2.report_text.text = "Ошибка в реплике NPC"
	game2._submit_report()
	check(ReportService.count() >= 1, "report dialog stores user report")
	check(ReportService.list()[-1]["text"] == "Ошибка в реплике NPC", "report dialog preserves user text")

	# Reset progress removes all local gameplay data and returns to a clean first-run state.
	game2._reset_progress()
	check(game2.player_name == "" and game2.coins == 0 and not FileAccess.file_exists(SaveGame.path()), "reset progress clears local gameplay state")
	check(game2.log_lines <= 4 and not (game2.log_label.get_parsed_text().contains("Макс")), "reset progress clears old journal")
	# Пустая память не должна превращать молчание NPC в выполненную цель «сплетня».
	game2.clock.total_minutes = 14 * 60
	game2.find_child("TalkButton", true, false).emit_signal("pressed")
	var gossip_status: Dictionary = game2.ritual.status().filter(func(s): return s["id"] == "gossip")[0]
	check(int(gossip_status["current"]) == 0, "empty memory does not complete gossip goal")
	# После сброса допустимы только служебные строки нового знакомства и целей.

	# In-app privacy / data info (wave 8)
	game.find_child("PrivacyButton", true, false).emit_signal("pressed")
	var priv = game.find_child("PrivacyPanel", true, false)
	check(priv != null, "privacy panel opens")
	var priv_text := (priv.find_child("PrivacyText", true, false) as RichTextLabel).get_parsed_text() if priv and priv.find_child("PrivacyText", true, false) != null else ""
	check(priv_text.contains("устройстве") and priv_text.contains("Память NPC"), "privacy panel explains local npc memory")
	game.find_child("PrivacyButton", true, false).emit_signal("pressed")
	await process_frame
	check(game.find_child("PrivacyPanel", true, false) == null, "privacy button toggles panel closed")

	# Audio
	check(game.sfx.size() == 3, "sfx players initialized")
	check(game.find_child("MusicButton", true, false) != null, "music toggle exists")
	var actions_grid := game.find_child("MusicButton", true, false).get_parent() as GridContainer
	check(actions_grid != null and actions_grid.columns == 3, "action grid balances five buttons")
	var music_button = game.find_child("MusicButton", true, false) as Button
	music_button.emit_signal("pressed")
	check(game.music_enabled == false and music_button.text.contains("выкл"), "music toggle mutes background")
	game._save()
	var game3 = scene.instantiate()
	root.add_child(game3)
	await process_frame
	check(game3.music_enabled == false and game3.music_button.text.contains("выкл"), "music preference survives new session")
	game3._stop_audio()
	game3.free()
	check(game.music_player.stream != null, "music stream loaded")
	var music_stream = game.music_player.stream
	if music_stream is AudioStreamWAV:
		check(music_stream.loop_mode == AudioStreamWAV.LOOP_FORWARD, "music stream has forward loop mode")
		check(music_stream.loop_end == 1058400, "music loop covers the full 48-second PCM track")
	check(game.town_map.place_textures.size() == 7, "town map preloads place textures")
	check(game.town_map.npc_textures.size() == 4, "town map preloads npc textures")
	check((game.find_child("LogLabel", true, false) as RichTextLabel).get_theme_color("default_color").to_html(false) == "3b3027", "journal uses dark readable text")
	check(game.town_map.custom_minimum_size.y <= 350.0, "map leaves readable journal space")
	check(game.find_child("coin", true, false) != null, "coin sfx player exists")
	var coin_stream = (game.find_child("coin", true, false) as AudioStreamPlayer).stream
	check(coin_stream != null, "coin sfx stream loaded")

	# Журнал не растёт бесконечно (wave 10)
	for i in range(400):
		game._log("строка %d" % i)
	check(game.log_lines <= game.MAX_LOG_LINES, "log journal capped")

	for audio_game in [game, game2, game3]:
		if is_instance_valid(audio_game):
			audio_game._stop_audio()
	# Release local resource references before the SceneTree exits.
	music_stream = null
	coin_stream = null
	game.free()
	game2.free()
	for _i in range(30):
		await process_frame
	if failures == 0:
		print("ALL INTEGRATION TESTS PASSED")
	else:
		print("INTEGRATION TESTS FAILED: %d" % failures)
	quit(1 if failures > 0 else 0)

