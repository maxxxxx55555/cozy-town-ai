class_name DailyRitual
extends RefCounted
## Дневные цели — петля удержания. Сброс в начале нового дня.

const GOALS := [
	{"id": "talk", "text": "Поговори с жителями", "target": 2},
	{"id": "kind", "text": "Сделай добрый поступок", "target": 1},
	{"id": "gossip", "text": "Узнай сплетню", "target": 1},
]
const REWARD := 25

var day := 0
var progress := {}
var claimed := false

func reset_for_day(d: int) -> bool:
	if day == d and not progress.is_empty():
		return false
	day = d
	progress.clear()
	claimed = false
	for g in GOALS:
		progress[g["id"]] = 0
	return true

func record(event_id: String) -> void:
	if progress.has(event_id):
		progress[event_id] = int(progress[event_id]) + 1

func status() -> Array:
	var out: Array = []
	for g in GOALS:
		var cur := int(progress.get(g["id"], 0))
		out.append({"id": g["id"], "text": g["text"], "current": mini(cur, int(g["target"])),
			"target": g["target"], "done": cur >= int(g["target"])})
	return out

func all_done() -> bool:
	for s in status():
		if not s["done"]:
			return false
	return true

func claim() -> int:
	if claimed or not all_done():
		return 0
	claimed = true
	return REWARD

func to_dict() -> Dictionary:
	return {"day": day, "progress": progress.duplicate(), "claimed": claimed}

func from_dict(d: Dictionary) -> void:
	day = d.get("day", 0)
	claimed = d.get("claimed", false)
	progress.clear()
	for g in GOALS: # только известные id, только неотрицательные значения
		var v = d.get("progress", {}).get(g["id"], 0)
		if typeof(v) == TYPE_INT or typeof(v) == TYPE_FLOAT:
			progress[g["id"]] = maxi(int(v), 0)
