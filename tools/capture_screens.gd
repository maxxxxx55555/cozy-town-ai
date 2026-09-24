extends SceneTree
## Реальные скриншоты стора из живого билда:
## godot --path . -s tools/capture_screens.gd --resolution 540x960

func _initialize() -> void:
	await process_frame
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
	print("SCREENSHOTS DONE")
	quit(0)

func _shot(path: String) -> void:
	await process_frame
	var img := root.get_texture().get_image()
	img.save_png(path)
