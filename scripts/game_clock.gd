class_name GameClock
extends RefCounted
## Игровое время: 1 реальная секунда = 1 игровая минута.

var day := 1
var total_minutes := 8 * 60 # старт в 08:00
var _pending_seconds := 0.0
const MAX_STEP := 600.0 # секунд за кадр: защита от speedhack/сна

func advance(real_seconds: float) -> void:
	var seconds := clampf(real_seconds, 0.0, MAX_STEP)
	_pending_seconds += seconds
	var minutes := int(floor(_pending_seconds))
	_pending_seconds -= float(minutes)
	total_minutes += minutes
	while total_minutes >= 24 * 60:
		total_minutes -= 24 * 60
		day += 1

func hour() -> int:
	return total_minutes / 60

func minute() -> int:
	return total_minutes % 60

func time_string() -> String:
	return "День %d, %02d:%02d" % [day, hour(), minute()]

func to_dict() -> Dictionary:
	return {"day": day, "total_minutes": total_minutes, "pending_seconds": _pending_seconds}

func from_dict(d: Dictionary) -> void:
	var raw_day = d.get("day", 1)
	var raw_minutes = d.get("total_minutes", 8 * 60)
	day = clampi(int(raw_day) if (typeof(raw_day) == TYPE_INT or typeof(raw_day) == TYPE_FLOAT) else 1, 1, 9999)
	total_minutes = clampi(int(raw_minutes) if (typeof(raw_minutes) == TYPE_INT or typeof(raw_minutes) == TYPE_FLOAT) else 8 * 60, 0, 24 * 60 - 1)
	var raw_pending = d.get("pending_seconds", 0.0)
	_pending_seconds = clampf(float(raw_pending) if (typeof(raw_pending) == TYPE_INT or typeof(raw_pending) == TYPE_FLOAT) else 0.0, 0.0, 1.0)
