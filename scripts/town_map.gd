class_name TownMap
extends Control
## Карта городка: NPC по местам, цвет = настроение.

const PLACES := {
	"пекарня": Vector2(90, 60), "мастерская": Vector2(280, 120),
	"причал": Vector2(70, 250), "рынок": Vector2(250, 50),
	"площадь": Vector2(190, 180), "таверна": Vector2(320, 250),
	"home": Vector2(150, 20),
}
const MOOD_COLORS := {
	"happy": Color("#7ecb6a"), "angry": Color("#e06c5a"),
	"sleepy": Color("#8fa7d9"), "neutral": Color("#e8c66a"),
}

var npcs: Array = []
var hour := 8

func pos_for(npc: NPC, at_hour: int) -> Vector2:
	var place: String = npc.schedule.place_at(at_hour)["place"]
	var base: Vector2 = PLACES.get(place, Vector2(200, 200))
	var idx: int = maxi(npcs.find(npc), 0)
	var off := Vector2(float(idx % 3) * 26.0 - 26.0, float(idx / 3) * 26.0 - 13.0)
	return base + off

func _draw() -> void:
	var font := ThemeDB.fallback_font
	draw_rect(Rect2(Vector2.ZERO, size), Color("#8fbf6f")) # трава
	draw_rect(Rect2(Vector2(0, size.y - 70), Vector2(size.x, 70)), Color("#6fa8d8")) # река
	for a in PLACES.keys():
		for b in PLACES.keys():
			if a < b:
				draw_line(PLACES[a], PLACES[b], Color(0.6, 0.55, 0.45, 0.25), 6.0)
	for place in PLACES.keys():
		draw_circle(PLACES[place], 6.0, Color(0.45, 0.35, 0.25, 0.6))
		draw_string(font, PLACES[place] + Vector2(8, 4), place, HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color("#3a2f24"))
	for npc in npcs:
		var p := pos_for(npc, hour)
		var col: Color = MOOD_COLORS.get(npc.identity.mood, MOOD_COLORS["neutral"])
		draw_circle(p, 14.0, col)
		draw_arc(p, 16.0, 0, TAU, 24, col.darkened(0.3), 2.0)
		draw_string(font, p + Vector2(-24, 32), npc.identity.npc_name, HORIZONTAL_ALIGNMENT_LEFT, 48, 12, Color("#3a2f24"))
