extends Control
## Точка входа. Хуки: 0:30 — NPC запоминает имя; 5:00 — вспоминает прошлое.

const RECALL_DELAY_SEC := 300.0
const AUTOSAVE_SEC := 30.0
const ALLOWED_UNLOCKS: Array = ["garden", "festival", "workshop_plus"]

var npcs: Array[NPC] = []
var clock := GameClock.new()
var inventory := Inventory.new()
var player_name := ""
var coins := 0
var session_time := 0.0
var autosave_time := 0.0
var recall_shown := false

var log_label: RichTextLabel
var time_label: Label
var name_input: LineEdit
var name_button: Button
var talk_button: Button
var report_button: Button

func _ready() -> void:
	_build_ui()
	_spawn_town()
	_load()
	_intro()

func _process(delta: float) -> void:
	session_time += delta
	autosave_time += delta
	clock.advance(delta)
	time_label.text = clock.time_string()
	if not recall_shown and session_time >= RECALL_DELAY_SEC:
		_show_recall()
	if autosave_time >= AUTOSAVE_SEC:
		autosave_time = 0.0
		_save()

func _notification(what: int) -> void:
	if what == NOTIFICATION_WM_CLOSE_REQUEST:
		_save()

func _build_ui() -> void:
	var vb := VBoxContainer.new()
	vb.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT, Control.PRESET_MODE_MINSIZE, 8)
	add_child(vb)
	time_label = Label.new()
	vb.add_child(time_label)
	log_label = RichTextLabel.new()
	log_label.size_flags_vertical = Control.SIZE_EXPAND_FILL
	log_label.bbcode_enabled = true
	vb.add_child(log_label)
	var hb := HBoxContainer.new()
	vb.add_child(hb)
	name_input = LineEdit.new()
	name_input.placeholder_text = "Твоё имя…"
	name_input.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	hb.add_child(name_input)
	name_button = Button.new()
	name_button.text = "Представиться"
	name_button.pressed.connect(_on_name_submitted)
	hb.add_child(name_button)
	talk_button = Button.new()
	talk_button.text = "Поговорить"
	talk_button.pressed.connect(_on_talk)
	hb.add_child(talk_button)
	report_button = Button.new()
	report_button.text = "Сообщить"
	report_button.pressed.connect(_on_report)
	hb.add_child(report_button)

func _spawn_town() -> void:
	var marta := NPC.new()
	marta.identity.npc_name = "Марта"
	marta.identity.traits = PackedStringArray(["добрая", "болтливая"])
	marta.schedule.add_slot(8, "пекарня", "печёт хлеб")
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
	var n := name_input.text.strip_edges()
	if n.length() < 2 or player_name != "":
		return
	player_name = n
	for npc in npcs: # хук 0:30 — все NPC запоминают имя
		npc.memory.player_name = n
		npc.memory.add_event("познакомился(ась) с игроком " + n, 8, clock.day, true)
	_log("Марта: Запомню, %s! Заходи в гости." % n)
	_save()

func _on_talk() -> void:
	if npcs.is_empty():
		return
	var npc := _npc_at_hour(clock.hour())
	var spot := npc.schedule.place_at(clock.hour())
	_log(DialogueComposer.compose(npc, clock.day, clock.hour()))
	_log("%s (%s)" % [npc.identity.npc_name, spot["place"]])
	var others := _others_at(npc, spot["place"])
	if others.size() > 0:
		var other: NPC = others[0]
		if npc.identity.relationships.get(other.identity.npc_name, 0.0) > 0.5:
			_log("%s дружелюбно встречает %s." % [npc.identity.npc_name, other.identity.npc_name])
		_log(npc.gossip_with(other, clock.day))

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
	var ok := ReportService.submit("other", "Жалоба из игры (день %d)" % clock.day, {"day": clock.day})
	_log("[color=#bb4444]Спасибо, жалоба сохранена (всего: %d).[/color]" % ReportService.count() if ok else "[color=#bb4444]Не удалось сохранить жалобу.[/color]")

func _log(t: String) -> void:
	log_label.append_text(t + "\n")

func _save() -> void:
	var arr: Array = []
	for n in npcs:
		arr.append(n.to_dict())
	SaveGame.save_state({
		"player_name": player_name, "coins": coins,
		"clock": clock.to_dict(), "inventory": inventory.to_dict(),
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
	var saved: Array = state.get("npcs", [])
	for i in mini(saved.size(), npcs.size()):
		npcs[i].from_dict(saved[i])
