class_name LLMDialogue
extends RefCounted
## Опциональные LLM-диалоги (v1.1). ВЫКЛЮЧЕНЫ по умолчанию.
## Требования при включении: opt-in игрока, INTERNET в Android-пресете,
## декларация AI-generated content → text generation (Play), кнопка «Сообщить»,
## фильтрация ввода/вывода. Без сети используется DialogueComposer.

const ENABLED := false
const MODEL := "gpt-4o-mini"
const MAX_OUTPUT := 400

static func build_request(npc: NPC, player_message: String, endpoint: String, api_key: String) -> Dictionary:
	var evs := npc.memory.recall_about_player()
	var mem: String = str(evs[0]["text"]) if evs.size() > 0 else "ничего особенного"
	var system := "Ты %s (%s). Настроение: %s, отношения: %s. Помнишь: %s. Отвечай 1-2 короткие уютные фразы по-русски." % [
		npc.identity.npc_name, ", ".join(npc.identity.traits),
		npc.identity.mood, npc.identity.tone(), mem]
	var payload := {
		"model": MODEL,
		"messages": [
			{"role": "system", "content": system},
			{"role": "user", "content": sanitize_input(player_message)},
		],
		"max_tokens": 80,
		"temperature": 0.8,
	}
	return {
		"url": endpoint,
		"headers": ["Authorization: Bearer " + api_key, "Content-Type: application/json"],
		"body": JSON.stringify(payload),
	}

static func sanitize_input(text: String) -> String:
	return text.strip_edges().left(500)

static func filter_output(text: String) -> String:
	var t := text.strip_edges().left(MAX_OUTPUT)
	while "\n\n" in t:
		t = t.replace("\n\n", "\n")
	return t
