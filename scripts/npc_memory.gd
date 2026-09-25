class_name NPCMemory
extends RefCounted
## Краткосрочная (≤10) + долгосрочная память NPC.

const SHORT_TERM_MAX := 10
const LONG_TERM_IMPORTANCE := 7
const MAX_LONG_TERM := 200 # потолок долговременной памяти (анти-DoS)

var short_term: Array[Dictionary] = []
var long_term: Array[Dictionary] = []
var player_name := ""

func add_event(text: String, importance: int, day: int, about_player := false) -> void:
	short_term.append({"text": text, "importance": importance, "day": day, "about_player": about_player})
	if short_term.size() > SHORT_TERM_MAX:
		_consolidate()
	if long_term.size() > MAX_LONG_TERM:
		_prune_long_term()

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
func _prune_long_term() -> void:
	while long_term.size() > MAX_LONG_TERM: # выкидываем наименее важное
		var worst := 0
		for i in range(1, long_term.size()):
			if int(long_term[i]["importance"]) < int(long_term[worst]["importance"]):
				worst = i
		long_term.remove_at(worst)

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
	player_name = str(d.get("player_name", "")).strip_edges().left(64)
	short_term.clear()
	long_term.clear()
	var raw_short = d.get("short_term", [])
	if typeof(raw_short) == TYPE_ARRAY:
		for e in raw_short:
			var clean := _sanitize_event(e)
			if not clean.is_empty():
				short_term.append(clean)
	var raw_long = d.get("long_term", [])
	if typeof(raw_long) == TYPE_ARRAY:
		for e in raw_long:
			var clean := _sanitize_event(e)
			if not clean.is_empty():
				long_term.append(clean)
	while short_term.size() > SHORT_TERM_MAX:
		short_term.pop_front()
	while long_term.size() > MAX_LONG_TERM:
		long_term.pop_front()

func _sanitize_event(e: Variant) -> Dictionary:
	if typeof(e) != TYPE_DICTIONARY:
		return {}
	var text := str(e.get("text", "")).strip_edges().left(240)
	if text.is_empty():
		return {}
	var raw_importance = e.get("importance", 0)
	var raw_day = e.get("day", 1)
	return {
		"text": text,
		"importance": clampi(int(raw_importance) if (typeof(raw_importance) == TYPE_INT or typeof(raw_importance) == TYPE_FLOAT) else 0, 0, 20),
		"day": clampi(int(raw_day) if (typeof(raw_day) == TYPE_INT or typeof(raw_day) == TYPE_FLOAT) else 1, 1, 9999),
		"about_player": bool(e.get("about_player", false)),
	}
