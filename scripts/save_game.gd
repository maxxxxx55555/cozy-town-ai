class_name SaveGame
extends RefCounted
## Сейвы: JSON + SHA-256 checksum. Tampered-сейв отклоняется.

const SAVE_PATH := "user://save.json"

static func save_state(state: Dictionary) -> bool:
	var data_json := JSON.stringify(state)
	var payload := {"data": data_json, "checksum": _checksum(data_json)}
	var f := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
	if f == null:
		return false
	f.store_string(JSON.stringify(payload))
	f.close()
	return true

static func load_state() -> Dictionary:
	if not FileAccess.file_exists(SAVE_PATH):
		return {}
	var f := FileAccess.open(SAVE_PATH, FileAccess.READ)
	var parsed = JSON.parse_string(f.get_as_text())
	f.close()
	if typeof(parsed) != TYPE_DICTIONARY:
		return {}
	var payload: Dictionary = parsed
	if not payload.has("data") or not payload.has("checksum"):
		return {}
	if _checksum(payload["data"]) != payload["checksum"]:
		return {} # tampered
	var data = JSON.parse_string(payload["data"])
	if typeof(data) != TYPE_DICTIONARY:
		return {}
	return sanitize(data)

static func sanitize(data: Dictionary) -> Dictionary:
	if data.has("coins"):
		var t := typeof(data["coins"])
		if (t != TYPE_INT and t != TYPE_FLOAT) or float(data["coins"]) < 0.0:
			data["coins"] = 0
	return data

static func _checksum(s: String) -> String:
	var ctx := HashingContext.new()
	ctx.start(HashingContext.HASH_SHA256)
	ctx.update(s.to_utf8_buffer())
	return ctx.finish().hex_encode()
