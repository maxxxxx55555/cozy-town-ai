class_name GameClock
extends RefCounted
## Игровое время: 1 реальная секунда = 1 игровая минута.

var day := 1
var total_minutes := 8 * 60 # старт в 08:00
const MAX_STEP := 600.0 # секунд за кадр: защита от speedhack/сна

func advance(real_seconds: float) -> void:
	total_minutes += int(clampf(real_seconds, 0.0, MAX_STEP))
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
	return {"day": day, "total_minutes": total_minutes}

func from_dict(d: Dictionary) -> void:
	day = d.get("day", 1)
	total_minutes = d.get("total_minutes", 8 * 60)
