extends Control
## Точка входа. Хуки: 0:30 — NPC запоминает имя; 5:00 — вспоминает прошлое.

const RECALL_DELAY_SEC := 300.0
const AUTOSAVE_SEC := 30.0
const ALLOWED_UNLOCKS: Array = ["garden", "festival", "workshop_plus"]

var npcs: Array[NPC] = []
var clock := GameClock.new()
var inventory := Inventory.new()
var ritual := DailyRitual.new()
var player_name := ""
var coins := 0
var session_time := 0.0
var autosave_time := 0.0
var recall_shown := false

var log_label: RichTextLabel
var journal_hint: Label
var empty_journal_hint: Label
var town_map: TownMap
var coins_label: Label
var help_button: Button
var privacy_button: Button
const TALK_COOLDOWN := 5.0 # анти-спам дневных целей
const MAX_LOG_LINES := 300 # потолок журнала: долгая сессия не растёт в памяти
const COLOR_PAPER := Color("#f6ebd7")
const COLOR_INK := Color("#3b3027")
const COLOR_TERRA := Color("#b85c45")
const COLOR_SAND := Color("#d9a441")
const COLOR_MINT := Color("#5f9b78")
const COLOR_RIVER := Color("#72b4d2")
var log_lines := 0
var last_talk_time := -999.0
var last_ritual_log := ""
var sfx := {}
var music_player: AudioStreamPlayer
var music_button: Button
var music_enabled := true
var time_label: Label
var name_input: LineEdit
var name_button: Button
var talk_button: Button
var report_button: Button
var report_dialog: AcceptDialog
var report_text: TextEdit

func _ready() -> void:
	_init_audio()
	_build_ui()
	_spawn_town()
	town_map.npcs = npcs
	_load()
	_apply_music_setting()
	if ritual.reset_for_day(clock.day):
		_log_ritual()
	_update_coins()
	_intro()
	_update_journal_hint()

func _process(delta: float) -> void:
	session_time += delta
	autosave_time += delta
	clock.advance(delta)
	time_label.text = clock.time_string()
	town_map.hour = clock.hour()
	town_map.queue_redraw()
	for npc in npcs:
		npc.tick(clock.hour())
	if ritual.reset_for_day(clock.day):
		last_ritual_log = ""
		_log("День %d. Цели обновлены." % clock.day)
		_log_ritual()
		_update_journal_hint()
	if not recall_shown and session_time >= RECALL_DELAY_SEC:
		_show_recall()
	if autosave_time >= AUTOSAVE_SEC:
		autosave_time = 0.0
		_save()

func _notification(what: int) -> void:
	# Android: приложение уходит в фон — сейв обязателен (WM_CLOSE только на десктопе).
	if what == NOTIFICATION_WM_CLOSE_REQUEST or what == MainLoop.NOTIFICATION_APPLICATION_PAUSED:
		_save()

func _build_ui() -> void:
	var background := PanelContainer.new()
	background.name = "Background"
	background.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	var background_style := StyleBoxFlat.new()
	background_style.bg_color = COLOR_PAPER
	background_style.border_color = Color("#d9b98c")
	background_style.set_border_width_all(2)
	background.add_theme_stylebox_override("panel", background_style)
	add_child(background)

	var margin := MarginContainer.new()
	margin.add_theme_constant_override("margin_left", 18)
	margin.add_theme_constant_override("margin_right", 18)
	margin.add_theme_constant_override("margin_top", 16)
	margin.add_theme_constant_override("margin_bottom", 16)
	background.add_child(margin)
	var vb := VBoxContainer.new()
	vb.add_theme_constant_override("separation", 10)
	margin.add_child(vb)

	var header := HBoxContainer.new()
	header.add_theme_constant_override("separation", 12)
	vb.add_child(header)
	var title := Label.new()
	title.text = "ПАЗУЛ ИЗ ЖИЗНИ"
	title.add_theme_font_size_override("font_size", 27)
	title.add_theme_color_override("font_color", COLOR_TERRA)
	header.add_child(title)
	time_label = Label.new()
	time_label.name = "TimeLabel"
	time_label.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	time_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	time_label.add_theme_font_size_override("font_size", 18)
	time_label.add_theme_color_override("font_color", COLOR_INK)
	header.add_child(time_label)
	coins_label = Label.new()
	coins_label.name = "CoinsLabel"
	coins_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
	coins_label.add_theme_font_size_override("font_size", 18)
	coins_label.add_theme_color_override("font_color", COLOR_SAND.darkened(0.2))
	header.add_child(coins_label)

	town_map = TownMap.new()
	town_map.name = "TownMap"
	town_map.custom_minimum_size = Vector2(0, 350)
	vb.add_child(town_map)

	var log_panel := PanelContainer.new()
	log_panel.size_flags_vertical = Control.SIZE_EXPAND_FILL
	var log_style := StyleBoxFlat.new()
	log_style.bg_color = Color("#fffaf0")
	log_style.set_corner_radius_all(14)
	log_style.set_content_margin_all(14)
	log_style.border_color = Color("#e2c79c")
	log_style.set_border_width_all(1)
	log_panel.add_theme_stylebox_override("panel", log_style)
	var log_layout := VBoxContainer.new()
	log_layout.add_theme_constant_override("separation", 8)
	log_panel.add_child(log_layout)
	vb.add_child(log_panel)
	log_label = RichTextLabel.new()
	log_label.name = "LogLabel"
	log_label.size_flags_vertical = Control.SIZE_EXPAND_FILL
	log_label.bbcode_enabled = true
	log_label.scroll_following = false
	log_label.add_theme_font_size_override("normal_font_size", 18)
	log_label.add_theme_color_override("default_color", COLOR_INK)
	log_layout.add_child(log_label)
	empty_journal_hint = Label.new()
	empty_journal_hint.name = "EmptyJournalHint"
	empty_journal_hint.text = "Здесь появится история городка.\nПознакомься с жителями — и начнётся."
	empty_journal_hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	empty_journal_hint.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	empty_journal_hint.mouse_filter = Control.MOUSE_FILTER_IGNORE
	empty_journal_hint.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	empty_journal_hint.add_theme_font_size_override("font_size", 16)
	empty_journal_hint.add_theme_color_override("font_color", COLOR_INK.lightened(0.22))
	log_label.add_child(empty_journal_hint)
	journal_hint = Label.new()
	journal_hint.name = "JournalHint"
	journal_hint.add_theme_font_size_override("font_size", 15)
	journal_hint.add_theme_color_override("font_color", COLOR_TERRA.darkened(0.12))
	journal_hint.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	log_layout.add_child(journal_hint)

	var name_row := HBoxContainer.new()
	name_row.add_theme_constant_override("separation", 8)
	vb.add_child(name_row)
	name_input = LineEdit.new()
	name_input.name = "NameInput"
	name_input.placeholder_text = "Твоё имя…"
	name_input.max_length = SaveGame.MAX_NAME
	name_input.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	name_input.custom_minimum_size = Vector2(0, 52)
	name_input.add_theme_font_size_override("font_size", 18)
	name_input.add_theme_stylebox_override("normal", _input_style(Color("#fffaf0"), Color("#d9b98c")))
	name_input.add_theme_stylebox_override("focus", _input_style(Color("#fffdf8"), COLOR_TERRA))
	name_row.add_child(name_input)
	name_button = Button.new()
	name_button.name = "NameButton"
	name_button.text = "Представиться"
	name_button.custom_minimum_size = Vector2(180, 52)
	_style_button(name_button, COLOR_TERRA, Color("#fffaf0"))
	name_button.pressed.connect(_on_name_submitted)
	name_row.add_child(name_button)

	var actions := GridContainer.new()
	actions.columns = 3
	actions.add_theme_constant_override("h_separation", 10)
	actions.add_theme_constant_override("v_separation", 10)
	vb.add_child(actions)
	talk_button = _make_action_button("TalkButton", "Поговорить", _on_talk, actions)
	help_button = _make_action_button("HelpButton", "Помочь", _on_help, actions)
	report_button = _make_action_button("ReportButton", "Сообщить", _on_report, actions)
	privacy_button = _make_action_button("PrivacyButton", "Данные", _on_privacy, actions)
	music_button = _make_action_button("MusicButton", "Музыка: вкл", _on_music_toggle, actions)

func _style_button(button: Button, base: Color, text_color: Color) -> void:
	var normal := _button_style(base, base.darkened(0.08))
	var hover := _button_style(base.lightened(0.08), COLOR_SAND)
	var pressed := _button_style(base.darkened(0.18), COLOR_SAND)
	var focus := _button_style(base.lightened(0.12), COLOR_SAND)
	button.add_theme_stylebox_override("normal", normal)
	button.add_theme_stylebox_override("hover", hover)
	button.add_theme_stylebox_override("pressed", pressed)
	button.add_theme_stylebox_override("focus", focus)
	button.add_theme_stylebox_override("disabled", _button_style(Color("#b8ad9d"), Color("#81786c")))
	button.add_theme_color_override("font_color", text_color)
	button.add_theme_color_override("font_hover_color", text_color)
	button.add_theme_color_override("font_pressed_color", text_color)
	button.add_theme_color_override("font_focus_color", text_color)
	button.add_theme_color_override("font_disabled_color", Color("#81786c"))

func _button_style(bg: Color, border: Color) -> StyleBoxFlat:
	var style := StyleBoxFlat.new()
	style.bg_color = bg
	style.border_color = border
	style.set_border_width_all(2)
	style.set_corner_radius_all(12)
	style.content_margin_left = 12
	style.content_margin_right = 12
	style.content_margin_top = 8
	style.content_margin_bottom = 8
	return style

func _input_style(bg: Color, border: Color) -> StyleBoxFlat:
	var style := StyleBoxFlat.new()
	style.bg_color = bg
	style.border_color = border
	style.set_border_width_all(2)
	style.set_corner_radius_all(10)
	style.content_margin_left = 12
	style.content_margin_right = 12
	style.content_margin_top = 8
	style.content_margin_bottom = 8
	return style

func _make_action_button(node_name: String, text: String, callback: Callable, parent: Node) -> Button:
	var button := Button.new()
	button.name = node_name
	button.text = text
	button.custom_minimum_size = Vector2(0, 54)
	button.add_theme_font_size_override("font_size", 17)
	_style_button(button, COLOR_MINT, Color("#fffaf0"))
	button.pressed.connect(callback)
	parent.add_child(button)
	return button

const SFX_PATHS := {
	"click": "res://assets/sfx/click.wav",
	"coin": "res://assets/sfx/coin.wav",
	"success": "res://assets/sfx/success.wav",
}
const MUSIC_PATH := "res://assets/music/cozy_theme.wav"

func _init_audio() -> void:
	for key in SFX_PATHS.keys():
		var p := AudioStreamPlayer.new()
		p.name = key
		p.stream = load(SFX_PATHS[key])
		add_child(p)
		sfx[key] = p
	music_player = AudioStreamPlayer.new()
	music_player.name = "MusicPlayer"
	music_player.stream = load(MUSIC_PATH)
	music_player.volume_db = -20.0
	add_child(music_player)
	if music_player.stream != null:
		if music_player.stream is AudioStreamWAV:
			var wav := music_player.stream as AudioStreamWAV
			wav.loop_mode = AudioStreamWAV.LOOP_FORWARD
			wav.loop_begin = 0
			var bytes_per_frame := 2 if wav.format == AudioStreamWAV.FORMAT_16_BITS else 1
			if wav.stereo:
				bytes_per_frame *= 2
			wav.loop_end = maxi(1, wav.data.size() / bytes_per_frame)
		# Playback starts in _apply_music_setting after the saved preference loads.

func _can_play_audio() -> bool:
	# Headless has no real output device, but Godot still creates playback objects.
	# Skipping playback there keeps automated tests deterministic and leak-free.
	return DisplayServer.get_name() != "headless"

func _play(key: String) -> void:
	if _can_play_audio() and sfx.has(key):
		sfx[key].play()

func _on_music_toggle() -> void:
	music_enabled = not music_enabled
	if music_enabled and _can_play_audio():
		music_player.play()
	else:
		music_player.stop()
	_update_music_button()
	_save()

func _update_music_button() -> void:
	if music_button != null:
		music_button.text = "Музыка: вкл" if music_enabled else "Музыка: выкл"

func _apply_music_setting() -> void:
	if music_player == null:
		return
	if music_enabled and _can_play_audio():
		music_player.play()
	else:
		music_player.stop()
	_update_music_button()


func _stop_audio() -> void:
	# AudioStreamPlayer keeps a playback object alive until its stream is explicitly released.
	# This is used on scene shutdown and by automated integration/screenshot harnesses.
	if music_player != null and is_instance_valid(music_player):
		music_player.stop()
		music_player.stream = null
	for key in sfx.keys():
		var player: AudioStreamPlayer = sfx[key]
		if player != null and is_instance_valid(player):
			player.stop()
			player.stream = null

func _exit_tree() -> void:
	_stop_audio()

func _on_privacy() -> void:
	_play("click")
	var existing := find_child("PrivacyPanel", true, false)
	if existing != null: # повторное нажатие закрывает панель
		existing.queue_free()
		return
	var panel := PanelContainer.new()
	panel.name = "PrivacyPanel"
	var layout := VBoxContainer.new()
	layout.add_theme_constant_override("separation", 10)
	panel.add_child(layout)
	var lbl := RichTextLabel.new()
	lbl.name = "PrivacyText"
	lbl.bbcode_enabled = true
	lbl.custom_minimum_size = Vector2(520, 300)
	lbl.text = "[b]О данных и памяти NPC[/b]\n• Всё хранится только на твоём устройстве.\n• Приложение не требует интернет и не передаёт данные.\n• Память NPC: твои поступки и разговоры, до 10 событий краткосрочно + важные надолго.\n• Удали приложение — удалятся все сохранения.\n• Вопросы: кнопка «Сообщить»."
	layout.add_child(lbl)
	var reset_button := Button.new()
	reset_button.name = "ResetProgressButton"
	reset_button.text = "Сбросить прогресс"
	reset_button.pressed.connect(_on_reset_progress)
	layout.add_child(reset_button)
	add_child(panel)
	panel.position = Vector2(24, 80)

func _on_reset_progress() -> void:
	_reset_progress()
	_play("click")
	_log("[b]Прогресс сброшен. Городок ждёт нового знакомства.[/b]")
	var panel := find_child("PrivacyPanel", true, false)
	if panel != null:
		panel.queue_free()

func _reset_progress() -> void:
	for path in [SaveGame.path(), SaveGame.path() + SaveGame.BACKUP_SUFFIX, SaveGame.path() + SaveGame.TEMP_SUFFIX]:
		if FileAccess.file_exists(path):
			DirAccess.remove_absolute(ProjectSettings.globalize_path(path))
	for npc in npcs:
		npc.queue_free()
	npcs.clear()
	clock = GameClock.new()
	inventory = Inventory.new()
	ritual = DailyRitual.new()
	player_name = ""
	coins = 0
	log_label.text = ""
	log_lines = 0
	last_ritual_log = ""
	session_time = 0.0
	autosave_time = 0.0
	recall_shown = false
	last_talk_time = -999.0
	_spawn_town()
	town_map.npcs = npcs
	name_input.text = ""
	name_input.editable = true
	name_button.disabled = false
	if ritual.reset_for_day(clock.day):
		_log_ritual()
	_update_coins()
	_intro()
	_update_journal_hint()

func _spawn_town() -> void:
	var marta := NPC.new()
	marta.identity.npc_name = "Марта"
	marta.identity.traits = PackedStringArray(["добрая", "болтливая"])
	marta.schedule.add_slot(8, "пекарня", "печёт хлеб")
	marta.schedule.add_slot(13, "рынок", "покупает цветы")
	marta.schedule.add_slot(18, "площадь", "гуляет")
	add_child(marta)
	npcs.append(marta)
	var boris := NPC.new()
	boris.identity.npc_name = "Борис"
	boris.identity.traits = PackedStringArray(["ворчливый"])
	boris.schedule.add_slot(9, "мастерская", "чинит вещи")
	boris.schedule.add_slot(19, "таверна", "читает газету")
	add_child(boris)
	npcs.append(boris)
	var luka := NPC.new()
	luka.identity.npc_name = "Лука"
	luka.identity.traits = PackedStringArray(["мечтательный", "тихий"])
	luka.schedule.add_slot(10, "причал", "ловит рыбу")
	add_child(luka)
	npcs.append(luka)
	var anya := NPC.new()
	anya.identity.npc_name = "Аня"
	anya.identity.traits = PackedStringArray(["энергичная", "любопытная"])
	anya.schedule.add_slot(7, "рынок", "торгует цветами")
	anya.schedule.add_slot(20, "площадь", "танцует")
	add_child(anya)
	npcs.append(anya)

func _intro() -> void:
	if player_name == "":
		_log("Марта: Привет! Как тебя зовут?")
	else:
		_log("Марта: " + npcs[0].greet(clock.day))

func _on_name_submitted() -> void:
	var n := SaveGame.sanitize_name(name_input.text)
	if n.length() < 2 or player_name != "":
		return
	player_name = n
	name_input.text = n
	name_input.editable = false
	name_button.disabled = true
	for npc in npcs: # хук 0:30 — все NPC запоминают имя
		npc.memory.player_name = n
		npc.memory.add_event("познакомился(ась) с игроком " + n, 8, clock.day, true)
	_log("Марта: Запомню, %s! Заходи в гости." % n)
	_update_journal_hint()
	_play("click")
	_save()

func _on_talk() -> void:
	if npcs.is_empty():
		return
	var npc := _npc_at_hour(clock.hour())
	var spot := npc.schedule.place_at(clock.hour())
	_log(DialogueComposer.compose(npc, clock.day, clock.hour()))
	_log("%s (%s)" % [npc.identity.npc_name, spot["place"]])
	_play("click")
	if session_time - last_talk_time >= TALK_COOLDOWN:
		ritual.record("talk")
		last_talk_time = session_time
	var others := _others_at(npc, spot["place"])
	if others.size() > 0:
		var other: NPC = others[0]
		if npc.identity.relationships.get(other.identity.npc_name, 0.0) > 0.5:
			_log("%s дружелюбно встречает %s." % [npc.identity.npc_name, other.identity.npc_name])
		_log(npc.gossip_with(other, clock.day))
		if not npc.memory.recall_about_player().is_empty():
			ritual.record("gossip")
	_after_event()

func _on_help() -> void:
	if npcs.is_empty():
		return
	var npc := _npc_at_hour(clock.hour())
	_log("%s: %s" % [npc.identity.npc_name, npc.react_to_action("помог по хозяйству", 5, clock.day)])
	ritual.record("kind")
	_play("click")
	_after_event()

func _after_event() -> void:
	_log_ritual()
	var reward := ritual.claim()
	if reward > 0:
		coins += reward
		_log("[b]Цели дня выполнены! +%d монет[/b]" % reward)
		_play("coin")
		_play("success")
		_save()
	_update_coins()
	_update_journal_hint()

func _log_ritual() -> void:
	var parts: Array[String] = []
	for s in ritual.status():
		var mark := "☑" if s["done"] else "☐"
		parts.append("%s %s (%d/%d)" % [mark, s["text"], s["current"], s["target"]])
	var line := "Цели дня: " + " · ".join(parts)
	if line != last_ritual_log:
		_log(line)
		last_ritual_log = line

func _update_journal_hint() -> void:
	if empty_journal_hint != null:
		empty_journal_hint.visible = log_lines <= 2
		if log_lines <= 2:
			empty_journal_hint.text = "История уже началась. Здесь появятся новые события." if log_lines > 0 else "Здесь появится история городка.\nПознакомься с жителями — и начнётся."
	if journal_hint == null:
		return
	if player_name.is_empty():
		journal_hint.text = "Подсказка: введи имя — все жители городка его запомнят."
	elif ritual.all_done():
		journal_hint.text = "Подсказка: цели дня выполнены. Можно вернуться к жителям и узнать новости."
	else:
		journal_hint.text = "Подсказка: поговори с жителями, помоги по хозяйству и узнай свежую сплетню."

func _update_coins() -> void:
	coins_label.text = "Монеты: %d" % coins

func _others_at(npc: NPC, place: String) -> Array[NPC]:
	var out: Array[NPC] = []
	for other in npcs:
		if other != npc and other.schedule.place_at(clock.hour())["place"] == place:
			out.append(other)
	return out

func _npc_at_hour(hour: int) -> NPC:
	for npc in npcs:
		if npc.schedule.place_at(hour)["place"] != "home":
			return npc
	return npcs[0]

func _show_recall() -> void:
	recall_shown = true
	for npc in npcs: # хук 5:00 — вспоминает прошлую сессию
		var evs := npc.memory.recall_about_player()
		if evs.size() > 0:
			_log("[i]%s вспоминает: %s[/i]" % [npc.identity.npc_name, evs[0]["text"]])
			return

func _on_report() -> void:
	if report_dialog == null:
		_build_report_dialog()
	report_text.clear()
	report_dialog.popup_centered(Vector2i(620, 520))

func _build_report_dialog() -> void:
	report_dialog = AcceptDialog.new()
	report_dialog.name = "ReportDialog"
	report_dialog.title = "Сообщить о проблеме"
	report_dialog.dialog_hide_on_ok = true
	report_dialog.confirmed.connect(_submit_report)
	add_child(report_dialog)
	report_text = TextEdit.new()
	report_text.name = "ReportText"
	report_text.placeholder_text = "Опиши, что произошло (до 1000 символов)…"
	report_text.custom_minimum_size = Vector2(560, 260)
	report_dialog.add_child(report_text)

func _submit_report() -> void:
	var text := report_text.text.strip_edges().left(1000)
	var ok := ReportService.submit("other", text, {"day": clock.day, "time": clock.time_string()})
	_log("[color=#2e7d32]Спасибо, сообщение сохранено (всего: %d).[/color]" % ReportService.count() if ok else "[color=#bb4444]Не удалось сохранить сообщение.[/color]")

func _log(t: String) -> void:
	if log_lines >= MAX_LOG_LINES:
		log_label.text = ""
		log_lines = 0
	log_label.append_text(t + "\n")
	log_lines += 1
	_update_journal_hint()
	# Явно показываем последние строки целиком, без обрезанной верхней строки.
	log_label.scroll_to_line(maxi(0, log_lines - 4))

func _save() -> void:
	var arr: Array = []
	for n in npcs:
		arr.append(n.to_dict())
	SaveGame.save_state({
		"player_name": player_name, "coins": coins,
		"clock": clock.to_dict(), "inventory": inventory.to_dict(),
		"ritual": ritual.to_dict(), "music_enabled": music_enabled,
		"unlocks": [], "npcs": arr,
	})

func _load() -> void:
	var state := SaveGame.load_state(ALLOWED_UNLOCKS)
	if state.is_empty():
		return
	player_name = state.get("player_name", "")
	coins = int(state.get("coins", 0))
	clock.from_dict(state.get("clock", {}))
	inventory.from_dict(state.get("inventory", {}))
	ritual.from_dict(state.get("ritual", {}))
	if state.has("music_enabled"):
		music_enabled = bool(state["music_enabled"])
	if not player_name.is_empty():
		name_input.text = player_name
		name_input.editable = false
		name_button.disabled = true
	var saved: Array = state.get("npcs", [])
	for i in mini(saved.size(), npcs.size()):
		npcs[i].from_dict(saved[i])
