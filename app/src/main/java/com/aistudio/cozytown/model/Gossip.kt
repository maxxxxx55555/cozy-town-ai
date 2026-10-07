package com.aistudio.cozytown.model

object Gossip {
    fun line(fromNpcName: String, toNpcName: String, eventText: String): String {
        return "$fromNpcName шепчет $toNpcName: «Слыхал(а)? $eventText»"
    }
}
