class_name Gossip
extends RefCounted
## Реплики-сплетни между NPC.

static func line(from_npc: NPC, to_npc: NPC, event_text: String) -> String:
	return "%s шепчет %s: «Слыхал(а)? %s»" % [
		from_npc.identity.npc_name, to_npc.identity.npc_name, event_text]
