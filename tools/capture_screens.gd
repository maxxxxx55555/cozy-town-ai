extends SceneTree
## Реальные скриншоты стора из живого билда в целевом portrait-размере:
## godot --path . -s tools/capture_screens.gd --resolution 1080x1920

func _initialize() -> void:
	if DisplayServer.get_name() == "headless":
		push_error("capture_screens.gd requires a rendering display; run without --headless")
		quit(1)
		return
	await process_frame
	# Store screenshots use the game's portrait target, independent of the desktop window size.
	root.size = Vector2i(1080, 1920)
	await process_frame
	var capture_id := Time.get_ticks_usec()
	SaveGame.set_path("user://capture_screens_%d.json" % capture_id)
	ReportService.set_path("user://capture_reports_%d.json" % capture_id)
	for suffix in ["", SaveGame.BACKUP_SUFFIX, SaveGame.TEMP_SUFFIX]:
		var save_path: String = SaveGame.path() + str(suffix)
		if FileAccess.file_exists(save_path):
			DirAccess.remove_absolute(ProjectSettings.globalize_path(save_path))
	for suffix in ["", ReportService.BACKUP_SUFFIX, ReportService.TEMP_SUFFIX]:
		var report_path: String = ReportService.path() + str(suffix)
		if FileAccess.file_exists(report_path):
			DirAccess.remove_absolute(ProjectSettings.globalize_path(report_path))
	DirAccess.make_dir_recursive_absolute(ProjectSettings.globalize_path("assets/store"))
	var scene: PackedScene = load("res://scenes/main.tscn")
	var game = scene.instantiate()
	root.add_child(game)
	await process_frame
	await process_frame
	await _shot("assets/store/real_1_intro.png")

	game.find_child("NameInput", true, false).text = "Макс"
	game.find_child("NameButton", true, false).emit_signal("pressed")
	await process_frame
	await _shot("assets/store/real_2_first_name.png")

	game.npcs[0].react_to_action("помог с хлебом", 6, game.clock.day)
	game.find_child("TalkButton", true, false).emit_signal("pressed")
	await process_frame
	await _shot("assets/store/real_3_memory.png")

	game.session_time = 301.0
	await process_frame
	await _shot("assets/store/real_4_recall.png")

	game.npcs[0].gossip_with(game.npcs[1], game.clock.day)
	game.find_child("TalkButton", true, false).emit_signal("pressed")
	await process_frame
	await _shot("assets/store/real_5_gossip.png")

	game.clock.total_minutes = 19 * 60
	await process_frame
	await process_frame
	await _shot("assets/store/real_6_evening.png")
	game._stop_audio()
	game.free()
	for _i in range(30):
		await process_frame
	for suffix in ["", SaveGame.BACKUP_SUFFIX, SaveGame.TEMP_SUFFIX]:
		var save_path: String = SaveGame.path() + str(suffix)
		if FileAccess.file_exists(save_path):
			DirAccess.remove_absolute(ProjectSettings.globalize_path(save_path))
	for suffix in ["", ReportService.BACKUP_SUFFIX, ReportService.TEMP_SUFFIX]:
		var report_path: String = ReportService.path() + str(suffix)
		if FileAccess.file_exists(report_path):
			DirAccess.remove_absolute(ProjectSettings.globalize_path(report_path))
	print("SCREENSHOTS DONE")
	quit(0)

func _shot(path: String) -> void:
	await process_frame
	var img := root.get_texture().get_image()
	img.save_png(path)
