class_name Inventory
extends RefCounted
## Инвентарь с защитой: отрицательные значения и повреждённые идентификаторы отбрасываются.

const MAX_ITEM_COUNT := 999
const MAX_ID_LENGTH := 32

var items := {} # id -> count

func add_item(id: String, count := 1) -> bool:
	if not _is_safe_id(id) or count <= 0:
		return false
	var next_count := mini(int(items.get(id, 0)) + int(count), MAX_ITEM_COUNT)
	if next_count <= 0:
		return false
	items[id] = next_count
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
		var id := str(k).strip_edges()
		var v = d[k]
		if not _is_safe_id(id) or (typeof(v) != TYPE_INT and typeof(v) != TYPE_FLOAT):
			continue
		var c := mini(int(v), MAX_ITEM_COUNT)
		if c > 0:
			items[id] = c

func _is_safe_id(id: String) -> bool:
	if id.is_empty() or id.length() > MAX_ID_LENGTH or ".." in id or "/" in id or "\\" in id:
		return false
	for c in id:
		if not (c.to_lower() >= "a" and c.to_lower() <= "z") and not (c >= "0" and c <= "9") and c != "_" and c != "-" and c != ":":
			return false
	return true
