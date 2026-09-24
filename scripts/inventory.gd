class_name Inventory
extends RefCounted
## Инвентарь с защитой: отрицательные значения отбрасываются.

var items := {} # id -> count

func add_item(id: String, count := 1) -> bool:
	if count <= 0:
		return false
	items[id] = int(items.get(id, 0)) + count
	return true

func remove_item(id: String, count := 1) -> bool:
	if count <= 0 or not items.has(id) or int(items[id]) < count:
		return false
	items[id] = int(items[id]) - count
	if int(items[id]) == 0:
		items.erase(id)
	return true

func count(id: String) -> int:
	return int(items.get(id, 0))

func to_dict() -> Dictionary:
	return items.duplicate()

func from_dict(d: Dictionary) -> void:
	items.clear()
	for k in d.keys():
		var v = d[k]
		if typeof(v) == TYPE_INT or typeof(v) == TYPE_FLOAT:
			var c := int(v)
			if c > 0: # anti-cheat: отбрасываем ≤ 0
				items[k] = c
