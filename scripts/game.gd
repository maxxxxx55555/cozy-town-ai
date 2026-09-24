extends Node
## Точка входа. Хук: NPC запоминает имя (0:30), вспоминает прошлую сессию (5:00).

var npcs: Array[NPC] = []
var day := 1

func _ready() -> void:
	_spawn_town()
	var state := SaveGame.load_state()
	if state.has("npcs"):
		_restore(state)
	print(npcs[0].greet(day))

func _spawn_town() -> void:
	var marta := NPC.new()
	marta.identity.npc_name = "Марта"
	marta.identity.traits = PackedStringArray(["добрая", "болтливая"])
	marta.schedule.add_slot(8, "пекарня", "печёт хлеб")
	marta.schedule.add_slot(18, "площадь", "гуляет")
	add_child(marta)
	npcs.append(marta)

	var boris := NPC.new()
	boris.identity.npc_name = "Борис"
	boris.identity.traits = PackedStringArray(["ворчливый"])
	boris.schedule.add_slot(9, "мастерская", "чинит вещи")
	add_child(boris)
	npcs.append(boris)

func _restore(state: Dictionary) -> void:
	day = state.get("day", 1)
	var saved: Array = state.get("npcs", [])
	for i in mini(saved.size(), npcs.size()):
		npcs[i].from_dict(saved[i])

func to_save_dict() -> Dictionary:
	var arr: Array = []
	for n in npcs:
		arr.append(n.to_dict())
	return {"day": day, "coins": 0, "npcs": arr}
