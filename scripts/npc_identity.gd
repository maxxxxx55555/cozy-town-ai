class_name NPCIdentity
extends RefCounted
## Имя, характер, эмоции, trust к игроку, отношения NPC↔NPC.

var npc_name := ""
var traits := PackedStringArray()
var mood := "neutral"         # happy, sad, angry, neutral
var mood_intensity := 0.5     # 0..1
var trust := 0.0              # -1..1 к игроку
var relationships := {}       # npc_name -> float -1..1

func apply_karma(k: int) -> void:
	trust = clampf(trust + k * 0.1, -1.0, 1.0)
	if k > 0:
		mood = "happy"
	elif k < 0:
		mood = "angry"
	mood_intensity = clampf(mood_intensity + abs(k) * 0.1, 0.0, 1.0)

func tone() -> String:
	if trust > 0.3:
		return "warm"
	if trust < -0.3:
		return "cold"
	return "neutral"
