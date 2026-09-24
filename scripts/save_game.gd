class_name SaveGame
extends RefCounted
## Сейвы: JSON + SHA-256 checksum. Tampered-сейв отклоняется, значения санируются.

const SAVE_PATH := "user://save.json"
static var _path_override := ""

## Тесты/инструменты могут переопределить путь, чтобы не конфликтовать.
static func set_path(p: String) -> void:
	_path_override = p

static func path() -> String:
	return _path_override if _path_override != "" else SAVE_PATH
const VERSION := 1
const MAX_COINS := 999999
const MAX_NAME := 24

static func save_state(state: Dictionary) -> bool:
	var data_json := JSON.stringify(state)
	var payload := {"data": data_json, "checksum": _checksum(data_json), "version": VERSION}
	var f := FileAccess.open(path(), FileAccess.WRITE)
	if f == null:
		return false
	f.store_string(JSON.stringify(payload))
	f.close()
	return true

static func load_state(allowed_unlocks: Array = []) -> Dictionary:
	if not FileAccess.file_exists(path()):
		return {}
	var f := FileAccess.open(path(), FileAccess.READ)
	var parsed = JSON.parse_string(f.get_as_text())
	f.close()
	if typeof(parsed) != TYPE_DICTIONARY:
		return {}
	var payload: Dictionary = parsed
	if not payload.has("data") or not payload.has("checksum"):
		return {}
	if int(payload.get("version", VERSION)) > VERSION:
		return {} # сейв из более новой версии игры — не рискуем ломать данные
	if _checksum(payload["data"]) != payload["checksum"]:
		return {} # tampered
	var data = JSON.parse_string(payload["data"])
	if typeof(data) != TYPE_DICTIONARY:
		return {}
	return sanitize(data, allowed_unlocks)

static func sanitize(data: Dictionary, allowed_unlocks: Array = []) -> Dictionary:
	if data.has("coins"):
		var t := typeof(data["coins"])
		if (t != TYPE_INT and t != TYPE_FLOAT) or float(data["coins"]) < 0.0:
			data["coins"] = 0
		data["coins"] = mini(int(data["coins"]), MAX_COINS)
	if data.has("player_name"):
		var n := str(data["player_name"]).strip_edges()
		n = n.replace("\n", "").replace("\r", "").replace("\t", "")
		data["player_name"] = n.left(MAX_NAME)
	if data.has("clock"):
		var c: Dictionary = data["clock"]
		c["day"] = clampi(int(c.get("day", 1)), 1, 9999)
		c["total_minutes"] = clampi(int(c.get("total_minutes", 8 * 60)), 0, 24 * 60 * 365)
		data["clock"] = c
	if data.has("unlocks"):
		var clean: Array = []
		if typeof(data["unlocks"]) == TYPE_ARRAY and allowed_unlocks.size() > 0:
			for u in data["unlocks"]:
				if u in allowed_unlocks and not u in clean:
					clean.append(u)
		data["unlocks"] = clean
	return data

static func _checksum(s: String) -> String:
	var ctx := HashingContext.new()
	ctx.start(HashingContext.HASH_SHA256)
	ctx.update(s.to_utf8_buffer())
	return ctx.finish().hex_encode()
