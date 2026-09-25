class_name SaveGame
extends RefCounted
## Сейвы: JSON + SHA-256 checksum. Tampered-сейв отклоняется, значения санируются.

const SAVE_PATH := "user://save.json"
const BACKUP_SUFFIX := ".bak"
const TEMP_SUFFIX := ".tmp"
static var _path_override := ""

## Тесты/инструменты могут переопределить путь, чтобы не конфликтовать.
static func set_path(p: String) -> void:
	_path_override = p

static func path() -> String:
	return _path_override if _path_override != "" else SAVE_PATH

const VERSION := 2
const MAX_COINS := 999999
const MAX_NAME := 24

static func save_state(state: Dictionary) -> bool:
	var data_json := JSON.stringify(state)
	var payload := {"data": data_json, "checksum": _checksum(data_json), "version": VERSION}
	var final_path := path()
	var temp_path := final_path + TEMP_SUFFIX
	var backup_path := final_path + BACKUP_SUFFIX
	var f := FileAccess.open(temp_path, FileAccess.WRITE)
	if f == null:
		return false
	f.store_string(JSON.stringify(payload))
	f.close()
	var final_abs := ProjectSettings.globalize_path(final_path)
	var temp_abs := ProjectSettings.globalize_path(temp_path)
	var backup_abs := ProjectSettings.globalize_path(backup_path)
	# Пишем во временный файл и сохраняем предыдущий валидный снимок:
	# обрыв записи/закрытие процесса не оставляет повреждённый единственный сейв.
	# Валидный primary ротируется в backup только после успешной записи temp.
	# Если primary уже повреждён, старый валидный backup нельзя удалять.
	if FileAccess.file_exists(final_path):
		if _is_valid_snapshot(final_path):
			if FileAccess.file_exists(backup_path):
				DirAccess.remove_absolute(backup_abs)
			if DirAccess.rename_absolute(final_abs, backup_abs) != OK:
				return false
		else:
			DirAccess.remove_absolute(final_path)
	if DirAccess.rename_absolute(temp_abs, final_abs) != OK:
		return false
	return true

static func load_state(allowed_unlocks: Array = []) -> Dictionary:
	if FileAccess.file_exists(path()):
		# Не откатываемся к старому снимку, если игрок открыл игру из будущей версии.
		# Это может скрыть несовместимый формат и привести к тихой потере прогресса.
		if _is_newer_version(path()):
			return {}
		var state := _load_from(path(), allowed_unlocks)
		if not state.is_empty():
			return state
	return _load_from(path() + BACKUP_SUFFIX, allowed_unlocks)

static func _is_newer_version(file_path: String) -> bool:
	var f := FileAccess.open(file_path, FileAccess.READ)
	if f == null:
		return false
	var parsed = _parse_json(f.get_as_text())
	f.close()
	var raw_version = parsed.get("version", 0) if typeof(parsed) == TYPE_DICTIONARY else 0
	return _version_number(raw_version) > VERSION

static func _is_valid_snapshot(file_path: String) -> bool:
	if not FileAccess.file_exists(file_path):
		return false
	var f := FileAccess.open(file_path, FileAccess.READ)
	if f == null:
		return false
	var parsed = _parse_json(f.get_as_text())
	f.close()
	if typeof(parsed) != TYPE_DICTIONARY:
		return false
	var payload: Dictionary = parsed
	var version = payload.get("version", 0)
	var data = _parse_json(str(payload.get("data", "")))
	var version_number = _version_number(version)
	return version_number >= 0 and version_number <= VERSION and str(payload.get("checksum", "")) == _checksum(str(payload.get("data", ""))) and typeof(data) == TYPE_DICTIONARY

static func _version_number(value: Variant) -> int:
	if typeof(value) != TYPE_INT and typeof(value) != TYPE_FLOAT and typeof(value) != TYPE_STRING:
		return -1
	return int(value)

static func _load_from(file_path: String, allowed_unlocks: Array) -> Dictionary:
	if not FileAccess.file_exists(file_path):
		return {}
	var f := FileAccess.open(file_path, FileAccess.READ)
	if f == null:
		return {}
	var parsed = _parse_json(f.get_as_text())
	f.close()
	if typeof(parsed) != TYPE_DICTIONARY:
		return {}
	var payload: Dictionary = parsed
	if not payload.has("data") or not payload.has("checksum"):
		return {}
	var version = payload.get("version", 0)
	var version_number := _version_number(version)
	if version_number < 0 or version_number > VERSION:
		return {} # неизвестный/повреждённый/более новый формат сейва
	if str(payload.get("checksum", "")) != _checksum(str(payload["data"])):
		return {} # tampered
	var data = _parse_json(str(payload["data"]))
	if typeof(data) != TYPE_DICTIONARY:
		return {}
	return sanitize(data, allowed_unlocks)

static func sanitize_name(value: Variant) -> String:
	if typeof(value) != TYPE_STRING:
		return ""
	var n := (value as String).strip_edges()
	n = n.replace("\n", "").replace("\r", "").replace("\t", "")
	# Имя выводится в RichTextLabel с включённым BBCode.
	# Удаляем содержимое тегов, сохраняя обычный текст между ними.
	var tag := RegEx.new()
	tag.compile("\\[[^\\]]*\\]")
	n = tag.sub(n, "", true)
	return n.left(MAX_NAME)

static func sanitize(data: Dictionary, allowed_unlocks: Array = []) -> Dictionary:
	if data.has("coins"):
		var t := typeof(data["coins"])
		if (t != TYPE_INT and t != TYPE_FLOAT) or float(data["coins"]) < 0.0:
			data["coins"] = 0
		data["coins"] = mini(int(data["coins"]), MAX_COINS)
	if data.has("player_name"):
		data["player_name"] = sanitize_name(data["player_name"])
	if data.has("music_enabled"):
		data["music_enabled"] = bool(data["music_enabled"])
	if data.has("clock"):
		var c: Dictionary = data["clock"] if typeof(data["clock"]) == TYPE_DICTIONARY else {}
		var raw_day = c.get("day", 1)
		var raw_minutes = c.get("total_minutes", 8 * 60)
		c["day"] = clampi(int(raw_day) if (typeof(raw_day) == TYPE_INT or typeof(raw_day) == TYPE_FLOAT) else 1, 1, 9999)
		c["total_minutes"] = clampi(int(raw_minutes) if (typeof(raw_minutes) == TYPE_INT or typeof(raw_minutes) == TYPE_FLOAT) else 8 * 60, 0, 24 * 60 - 1)
		data["clock"] = c
	if data.has("inventory"):
		data["inventory"] = data["inventory"] if typeof(data["inventory"]) == TYPE_DICTIONARY else {}
	if data.has("ritual"):
		data["ritual"] = data["ritual"] if typeof(data["ritual"]) == TYPE_DICTIONARY else {}
	if data.has("npcs"):
		var clean_npcs: Array = []
		if typeof(data["npcs"]) == TYPE_ARRAY:
			for npc_data in data["npcs"]:
				if typeof(npc_data) == TYPE_DICTIONARY:
					clean_npcs.append(npc_data)
		data["npcs"] = clean_npcs
	if data.has("unlocks"):
		var clean: Array = []
		if typeof(data["unlocks"]) == TYPE_ARRAY and allowed_unlocks.size() > 0:
			for u in data["unlocks"]:
				if u in allowed_unlocks and not u in clean:
					clean.append(u)
		data["unlocks"] = clean
	return data

static func _parse_json(text: String) -> Variant:
	var parser := JSON.new()
	if parser.parse(text) != OK:
		return null
	return parser.data

static func _checksum(s: String) -> String:
	var ctx := HashingContext.new()
	ctx.start(HashingContext.HASH_SHA256)
	ctx.update(s.to_utf8_buffer())
	return ctx.finish().hex_encode()
