class_name TownMap
extends Control
## Карта городка: NPC по местам, цвет = настроение.

const PLACES := {
	"пекарня": Vector2(90, 60), "мастерская": Vector2(280, 120),
	"причал": Vector2(70, 250), "рынок": Vector2(250, 50),
	"площадь": Vector2(190, 180), "таверна": Vector2(320, 250),
	"home": Vector2(150, 50),
}
const ROUTES := [
	["home", "пекарня"], ["home", "рынок"], ["пекарня", "площадь"],
	["рынок", "мастерская"], ["мастерская", "таверна"], ["причал", "площадь"],
]
const MOOD_COLORS := {
	"happy": Color("#5f9b78"), "angry": Color("#b85c45"),
	"sleepy": Color("#6f8fbe"), "neutral": Color("#d9a441"),
}
const PLACE_LABELS := {
	"пекарня": "пекарня", "мастерская": "мастерская", "причал": "причал",
	"рынок": "рынок", "площадь": "площадь", "таверна": "таверна", "home": "дом",
}
const PLACE_SPRITES := {
	"пекарня": "res://assets/sprites/bakery.png",
	"мастерская": "res://assets/sprites/workshop.png",
	"причал": "res://assets/sprites/pier.png",
	"рынок": "res://assets/sprites/market.png",
	"площадь": "res://assets/sprites/square.png",
	"таверна": "res://assets/sprites/tavern.png",
	"home": "res://assets/sprites/home.png",
}
const NPC_SPRITES := {
	"Марта": "res://assets/sprites/npc_marta.png",
	"Борис": "res://assets/sprites/npc_boris.png",
	"Лука": "res://assets/sprites/npc_luka.png",
	"Аня": "res://assets/sprites/npc_anya.png",
}

var npcs: Array = []
var hour := 8
var place_textures := {}
var npc_textures := {}

func _ready() -> void:
	_preload_textures()

func _preload_textures() -> void:
	for place in PLACE_SPRITES.keys():
		var path: String = PLACE_SPRITES[place]
		if ResourceLoader.exists(path):
			place_textures[place] = load(path)
	for npc_name in NPC_SPRITES.keys():
		var path: String = NPC_SPRITES[npc_name]
		if ResourceLoader.exists(path):
			npc_textures[npc_name] = load(path)

func display_place_name(place: String) -> String:
	return str(PLACE_LABELS.get(place, place))

func _map_point(place: String) -> Vector2:
	var base: Vector2 = PLACES.get(place, Vector2(200, 200))
	var width := maxf(size.x, 360.0)
	return Vector2(base.x / 360.0 * width, base.y)

func pos_for(npc: NPC, at_hour: int) -> Vector2:
	var place: String = npc.schedule.place_at(at_hour)["place"]
	var base := _map_point(place)
	var idx: int = maxi(npcs.find(npc), 0)
	# Four NPC may share a location. A 2×2 grid keeps sprites and labels legible.
	var col := idx % 2
	var row := idx / 2
	var off := Vector2(float(col) * 72.0 - 36.0, float(row) * 56.0 - 28.0)
	return base + off

const LABEL_SIZE := Vector2(96, 24)
const NPC_LABEL_SIZE := Vector2(76, 22)

func _label_fits(rect: Rect2, taken: Array[Rect2]) -> bool:
	if rect.position.x < 0.0 or rect.position.y < 0.0:
		return false
	var limit := Vector2(maxf(size.x, 360.0), maxf(size.y, 350.0))
	if rect.end.x > limit.x or rect.end.y > limit.y:
		return false
	for other in taken:
		if rect.intersects(other):
			return false
	return true

func _place_label_rect(marker: Vector2) -> Rect2:
	return Rect2(marker + Vector2(-48, -40), LABEL_SIZE)

func _find_label_rect(preferred: Vector2, box_size: Vector2, taken: Array[Rect2]) -> Rect2:
	var candidates: Array[Vector2] = [
		preferred,
		preferred + Vector2(0, 28),
		preferred + Vector2(0, -54),
		preferred + Vector2(28, 0),
		preferred + Vector2(-28, 0),
		preferred + Vector2(0, 56),
		preferred + Vector2(56, 0),
		preferred + Vector2(-56, 0),
	]
	for offset in candidates:
		var rect := Rect2(offset, box_size)
		if _label_fits(rect, taken):
			return rect
	# Полный резерв: сетка внутри карты гарантирует место даже при плотном расписании.
	var limit := Vector2(maxf(size.x, 360.0), maxf(size.y, 350.0))
	var step := Vector2(104.0, 28.0)
	for y in range(0, int(limit.y - box_size.y) + 1, int(step.y)):
		for x in range(0, int(limit.x - box_size.x) + 1, int(step.x)):
			var rect := Rect2(Vector2(float(x), float(y)), box_size)
			if _label_fits(rect, taken):
				return rect
	return Rect2(preferred, box_size)

func label_rects_for_hour(at_hour: int) -> Array[Rect2]:
	var rects: Array[Rect2] = []
	var taken: Array[Rect2] = []
	for place in PLACES.keys():
		var place_rect := _place_label_rect(_map_point(place))
		place_rect = _find_label_rect(place_rect.position, LABEL_SIZE, taken)
		rects.append(place_rect)
		taken.append(place_rect)
	for npc in npcs:
		var p := pos_for(npc, at_hour)
		var npc_rect := _find_label_rect(Vector2(p.x - 38.0, p.y + 24.0), NPC_LABEL_SIZE, taken)
		rects.append(npc_rect)
		taken.append(npc_rect)
	return rects

func npc_label_position(npc: NPC, at_hour: int) -> Vector2:
	var p := pos_for(npc, at_hour)
	var idx: int = maxi(npcs.find(npc), 0)
	var side: float = -56.0 if idx % 2 == 0 else 8.0
	return Vector2(p.x + side, p.y + 32.0)

func _draw() -> void:
	var font := ThemeDB.fallback_font
	var label_rects := label_rects_for_hour(hour)
	var place_index := 0
	draw_rect(Rect2(Vector2.ZERO, size), Color("#8fbf6f")) # трава
	draw_rect(Rect2(Vector2(0, size.y - 70), Vector2(size.x, 70)), Color("#6fa8d8")) # река
	for route in ROUTES:
		draw_line(_map_point(route[0]), _map_point(route[1]), Color(0.45, 0.38, 0.28, 0.28), 5.0)
	for place in PLACES.keys():
		var marker := _map_point(place)
		draw_circle(marker, 6.0, Color(0.45, 0.35, 0.25, 0.6))
		var place_texture: Texture2D = place_textures.get(place, null)
		if place_texture != null:
			draw_texture_rect(place_texture, Rect2(marker - Vector2(12, 12), Vector2(24, 24)), false)
		var place_label := display_place_name(place)
		var place_rect: Rect2 = label_rects[place_index]
		place_index += 1
		draw_rect(place_rect, Color(0.98, 0.93, 0.82, 0.86))
		draw_string(font, place_rect.position + Vector2(0, 18), place_label, HORIZONTAL_ALIGNMENT_CENTER, place_rect.size.x, 14, Color("#3a2f24"))
	for npc in npcs:
		var p := pos_for(npc, hour)
		var col: Color = MOOD_COLORS.get(npc.identity.mood, MOOD_COLORS["neutral"])
		var npc_texture: Texture2D = npc_textures.get(npc.identity.npc_name, null)
		if npc_texture != null:
			draw_circle(p, 17.0, Color(0.15, 0.12, 0.10, 0.28))
			draw_texture_rect(npc_texture, Rect2(p - Vector2(18, 18), Vector2(36, 36)), false)
		else:
			draw_circle(p, 14.0, col)
		draw_arc(p, 18.0, 0, TAU, 24, col, 2.5)
		var label_rect: Rect2 = label_rects[place_index + npcs.find(npc)]
		draw_rect(label_rect, Color(0.98, 0.93, 0.82, 0.86))
		draw_string(font, label_rect.position + Vector2(4, 16), npc.identity.npc_name, HORIZONTAL_ALIGNMENT_LEFT, label_rect.size.x - 8.0, 14, Color("#3a2f24"))
