class_name ReportService
extends RefCounted
## In-app report (обязательно для Play при AI-контенте). Локальное хранение.

const REPORT_PATH := "user://reports.json"
const MAX_REPORTS := 50
const REASONS := ["rude", "bug", "wrong_memory", "other"]

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
	if not FileAccess.file_exists(REPORT_PATH):
		return []
	var f := FileAccess.open(REPORT_PATH, FileAccess.READ)
	var parsed = JSON.parse_string(f.get_as_text())
	f.close()
	if typeof(parsed) != TYPE_ARRAY:
		return []
	return parsed

static func count() -> int:
	return list().size()

static func _write(items: Array) -> bool:
	var f := FileAccess.open(REPORT_PATH, FileAccess.WRITE)
	if f == null:
		return false
	f.store_string(JSON.stringify(items))
	f.close()
	return true
