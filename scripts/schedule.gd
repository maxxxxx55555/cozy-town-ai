class_name DaySchedule
extends RefCounted
## Дневное расписание NPC: час → место/активность.

var slots: Array[Dictionary] = []

func add_slot(hour: int, place: String, activity: String) -> void:
	slots.append({"hour": hour, "place": place, "activity": activity})
	slots.sort_custom(func(a, b): return a["hour"] < b["hour"])

func place_at(hour: int) -> Dictionary:
	# Ночная логика общая для всех NPC: после закрытия городка все возвращаются домой.
	if hour >= 22 or hour < 6:
		return {"hour": hour, "place": "home", "activity": "sleep"}
	var result := {"hour": 0, "place": "home", "activity": "sleep"}
	for s in slots:
		if s["hour"] <= hour:
			result = s
	return result
