class_name NPC
extends Node
## NPC: память + характер + расписание + реакции + сплетни.

var identity := NPCIdentity.new()
var memory := NPCMemory.new()
var schedule := DaySchedule.new()

func greet(_day: int) -> String:
	var pname := memory.player_name
	if pname == "":
		return "Привет! Как тебя зовут?"
	var recalled := ""
	var evs := memory.recall_about_player()
	if evs.size() > 0:
		recalled = " Я помню: " + evs[0]["text"]
	match identity.tone():
		"warm":
			return "Рада тебя видеть, %s!%s" % [pname, recalled]
		"cold":
			return "Опять ты, %s.%s" % [pname, recalled]
		_:
			return "Привет, %s.%s" % [pname, recalled]

func react_to_action(action_text: String, karma: int, day: int) -> String:
	identity.apply_karma(karma)
	memory.add_event("игрок: " + action_text, abs(karma) + 5, day, true)
	match identity.tone():
		"warm":
			return "Спасибо! Я это запомню."
		"cold":
			return "Я это запомню…"
		_:
			return "Понял(а)."

func gossip_with(partner: NPC) -> String:
	var evs := memory.recall_about_player()
	if evs.is_empty():
		return "%s и %s молча кивают друг другу." % [identity.npc_name, partner.identity.npc_name]
	return Gossip.line(self, partner, evs[0]["text"])

func to_dict() -> Dictionary:
	return {
		"npc_name": identity.npc_name,
		"trust": identity.trust,
		"mood": identity.mood,
		"memory": memory.to_dict(),
	}

func from_dict(d: Dictionary) -> void:
	identity.npc_name = d.get("npc_name", "")
	identity.trust = d.get("trust", 0.0)
	identity.mood = d.get("mood", "neutral")
	memory.from_dict(d.get("memory", {}))
