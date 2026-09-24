class_name NPCMemory
extends RefCounted
## Краткосрочная (≤10) + долгосрочная память NPC.

const SHORT_TERM_MAX := 10
const LONG_TERM_IMPORTANCE := 7

var short_term: Array[Dictionary] = []
var long_term: Array[Dictionary] = []
var player_name := ""

func add_event(text: String, importance: int, day: int, about_player := false) -> void:
	short_term.append({"text": text, "importance": importance, "day": day, "about_player": about_player})
	if short_term.size() > SHORT_TERM_MAX:
		_consolidate()

func _consolidate() -> void:
	var kept: Array[Dictionary] = []
	for e in short_term:
		if e["importance"] >= LONG_TERM_IMPORTANCE or e["about_player"]:
			long_term.append(e)
		else:
			kept.append(e)
	if kept.size() > SHORT_TERM_MAX:
		kept = kept.slice(kept.size() - SHORT_TERM_MAX)
	short_term = kept

func recall_about_player() -> Array[Dictionary]:
	var out: Array[Dictionary] = []
	for e in short_term + long_term:
		if e["about_player"]:
			out.append(e)
	out.sort_custom(func(a, b): return a["importance"] > b["importance"])
	return out

func to_dict() -> Dictionary:
	return {"short_term": short_term.duplicate(), "long_term": long_term.duplicate(), "player_name": player_name}

func from_dict(d: Dictionary) -> void:
	player_name = d.get("player_name", "")
	short_term.clear()
	long_term.clear()
	for e in d.get("short_term", []):
		short_term.append(e)
	for e in d.get("long_term", []):
		long_term.append(e)
