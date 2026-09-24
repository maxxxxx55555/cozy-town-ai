class_name DialogueComposer
extends RefCounted
## Процедурный генератор реплик NPC (без LLM): память + характер + эмоция + место.

const OPENERS := {
	"warm": ["Ох, %s! Как я рада тебя видеть.", "Привет, %s! Я тебя ждала.", "%s! Садись рядом."],
	"cold": ["Опять ты, %s.", "Чего тебе, %s?", "%s. Я не забыла."],
	"neutral": ["Привет, %s.", "А, это ты, %s.", "Доброго дня, %s."],
}

const ACTIVITY_LINES := [
	"Я сейчас в %s — %s.",
	"Как видишь, я в %s: %s.",
]

const TRAIT_LINES := {
	"добрая": "Если что — я всегда помогу.",
	"болтливая": "Столько новостей, не расскажешь и за день!",
	"ворчливый": "Молодёжь совсем распустилась.",
	"мечтательный": "Сегодня вода была как небо.",
	"тихий": "…Я просто посижу рядом.",
	"энергичная": "Побежали на площадь?",
	"любопытная": "Расскажи, что у тебя нового!",
}

static func compose(npc: NPC, day: int, hour := 9) -> String:
	var pname := npc.memory.player_name
	if pname == "":
		return "Привет! Как тебя зовут?"
	var lines: Array[String] = []
	var tone := npc.identity.tone()
	var openers: Array = OPENERS.get(tone, OPENERS["neutral"])
	lines.append(openers[posmod(npc.identity.npc_name.hash() + day, openers.size())] % pname)
	var spot := npc.schedule.place_at(hour)
	lines.append(ACTIVITY_LINES[posmod(day, ACTIVITY_LINES.size())] % [spot["place"], spot["activity"]])
	for t in npc.identity.traits:
		if TRAIT_LINES.has(t):
			lines.append(TRAIT_LINES[t])
			break
	var evs := npc.memory.recall_about_player()
	if evs.size() > 0:
		lines.append("Помню: %s." % evs[0]["text"])
	if npc.identity.mood == "happy":
		lines.append("Настроение хорошее.")
	elif npc.identity.mood == "angry":
		lines.append("Настроение, сам понимаешь, не очень.")
	return " ".join(lines)
