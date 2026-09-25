class_name ReportService
extends RefCounted
## In-app report (обязательно для Play при AI-контенте). Локальное хранение.

const REPORT_PATH := "user://reports.json"
const BACKUP_SUFFIX := ".bak"
const TEMP_SUFFIX := ".tmp"
const MAX_REPORTS := 50
const REASONS := ["rude", "bug", "wrong_memory", "other"]
static var _path_override := ""

static func set_path(p: String) -> void:
	_path_override = p

static func path() -> String:
	return _path_override if _path_override != "" else REPORT_PATH

static func submit(reason: String, text: String, context: Dictionary = {}) -> bool:
	if not reason in REASONS:
		return false
	var clean := text.strip_edges()
	if clean.length() == 0 or clean.length() > 1000:
		return false
	var all := list()
	all.append({
		"reason": reason,
		"text": clean,
		"context": context.duplicate(),
		"ts": Time.get_unix_time_from_system(),
	})
	while all.size() > MAX_REPORTS:
		all.pop_front()
	return _write(all)

static func list() -> Array:
	var report_path := path()
	if FileAccess.file_exists(report_path):
		var primary := _read_valid(report_path)
		if bool(primary.get("valid", false)):
			return primary["items"]
	if FileAccess.file_exists(report_path + BACKUP_SUFFIX):
		var backup := _read_valid(report_path + BACKUP_SUFFIX)
		if bool(backup.get("valid", false)):
			return backup["items"]
	return []

static func _read_valid(path: String) -> Dictionary:
	if not FileAccess.file_exists(path):
		return {"valid": false, "items": []}
	var f := FileAccess.open(path, FileAccess.READ)
	if f == null:
		return {"valid": false, "items": []}
	var parser := JSON.new()
	var parse_error := parser.parse(f.get_as_text())
	f.close()
	if parse_error != OK or typeof(parser.data) != TYPE_ARRAY:
		return {"valid": false, "items": []}
	# JSON может быть синтаксически валидным, но содержать повреждённые/чужие записи.
	# Не пропускаем их в UI и не позволяем malformed context раздувать локальный отчёт.
	var clean: Array = []
	for raw_item in parser.data:
		if typeof(raw_item) != TYPE_DICTIONARY:
			continue
		var reason := str(raw_item.get("reason", ""))
		if not reason in REASONS:
			continue
		var text := str(raw_item.get("text", "")).strip_edges()
		if text.is_empty() or text.length() > 1000:
			continue
		var context = raw_item.get("context", {})
		if typeof(context) != TYPE_DICTIONARY:
			context = {}
		var ts = raw_item.get("ts", 0)
		if typeof(ts) != TYPE_INT and typeof(ts) != TYPE_FLOAT:
			ts = 0
		clean.append({"reason": reason, "text": text, "context": context.duplicate(), "ts": ts})
		while clean.size() > MAX_REPORTS:
			clean.pop_front()
	return {"valid": true, "items": clean}

static func count() -> int:
	return list().size()

static func _write(items: Array) -> bool:
	var report_path := path()
	var temp_path := report_path + TEMP_SUFFIX
	var f := FileAccess.open(temp_path, FileAccess.WRITE)
	if f == null:
		return false
	f.store_string(JSON.stringify(items))
	f.close()
	# Сохраняем предыдущий валидный primary, но не вытесняем им битый primary.
	if FileAccess.file_exists(report_path):
		var primary_valid := bool(_read_valid(report_path).get("valid", false))
		if primary_valid:
			if FileAccess.file_exists(report_path + BACKUP_SUFFIX):
				DirAccess.remove_absolute(ProjectSettings.globalize_path(report_path + BACKUP_SUFFIX))
			var rename_ok := DirAccess.rename_absolute(ProjectSettings.globalize_path(report_path), ProjectSettings.globalize_path(report_path + BACKUP_SUFFIX))
			if rename_ok != OK:
				return false
		else:
			DirAccess.remove_absolute(ProjectSettings.globalize_path(report_path))
	return DirAccess.rename_absolute(ProjectSettings.globalize_path(temp_path), ProjectSettings.globalize_path(report_path)) == OK
