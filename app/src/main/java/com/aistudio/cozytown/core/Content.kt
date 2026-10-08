package com.aistudio.cozytown.core

/**
 * Весь контент городка: предметы, места, рецепты, апгрейды,
 * достижения, события, заказы, сюжетные арки NPC.
 * Чистые данные — без Android и UI.
 */

data class ItemDef(
    val id: String,
    val name: String,
    val emoji: String,
    val source: String,       // где добывается
    val sellPrice: Int,
    val rarity: Int = 0       // 0 обычный, 1 необычный, 2 редкий
)

object Items {
    val ALL = listOf(
        ItemDef("flour", "Мука", "🌾", "пекарня", 2),
        ItemDef("bread", "Хлеб", "🍞", "пекарня", 3),
        ItemDef("flower", "Цветы", "🌸", "рынок", 2),
        ItemDef("honey", "Мёд", "🍯", "рынок", 4),
        ItemDef("ribbon", "Лента", "🎀", "рынок", 3),
        ItemDef("plank", "Доска", "🪵", "мастерская", 3),
        ItemDef("nail", "Гвозди", "🔩", "мастерская", 2),
        ItemDef("fish", "Рыба", "🐟", "причал", 4),
        ItemDef("shell", "Ракушка", "🐚", "причал", 3),
        ItemDef("tea", "Травяной чай", "🌿", "сад", 4),
        ItemDef("berry", "Ягоды", "🍒", "сад", 3),
        ItemDef("mushroom", "Гриб", "🍄", "сад", 3),
        ItemDef("pearl", "Жемчуг", "⚪", "причал", 12, 2),
        ItemDef("amber", "Янтарь", "🟠", "причал", 14, 2),
        ItemDef("pie", "Пирог", "🥧", "мастерская (рецепт)", 10, 1),
        ItemDef("bouquet", "Букет", "💐", "мастерская (рецепт)", 8, 1),
        ItemDef("fishtart", "Рыбный тарт", "🍥", "мастерская (рецепт)", 9, 1),
        ItemDef("teaset", "Чайный набор", "🫖", "мастерская (рецепт)", 9, 1),
        ItemDef("box", "Шкатулка", "📦", "мастерская (рецепт)", 11, 1),
        ItemDef("wreath", "Венок", "🌺", "мастерская (рецепт)", 8, 1),
        ItemDef("necklace", "Ожерелье", "📿", "мастерская (рецепт)", 26, 2),
        ItemDef("charm", "Оберег", "🧿", "мастерская (рецепт)", 28, 2)
    )

    fun byId(id: String): ItemDef? = ALL.find { it.id == id }
}

data class PlaceDef(
    val id: String,
    val name: String,
    val yields: List<String>,        // id предметов
    val rareYields: List<String> = emptyList(),
    val requiresUpgrade: String? = null,
    val flavor: String = ""
)

object Places {
    val ALL = listOf(
        PlaceDef("home", "Дом", emptyList(), flavor = "Тёплый плед и тишина. Здесь можно отдохнуть."),
        PlaceDef("пекарня", "Пекарня", listOf("flour", "bread"), flavor = "Пахнет корицей и тёплым хлебом."),
        PlaceDef("рынок", "Рынок", listOf("flower", "honey", "ribbon"), flavor = "Прилавки, голоса, цветы."),
        PlaceDef("мастерская", "Мастерская", listOf("plank", "nail"), flavor = "Стружка и запах смолы."),
        PlaceDef("площадь", "Площадь", listOf("tea"), flavor = "Сердце городка. Здесь все новости."),
        PlaceDef("таверна", "Таверна", listOf("tea", "mushroom"), flavor = "Тихо, пахнет травами и чаем."),
        PlaceDef("причал", "Причал", listOf("fish", "shell"), rareYields = listOf("pearl", "amber"), flavor = "Вода шепчет свои истории."),
        PlaceDef("сад", "Сад", listOf("tea", "berry", "mushroom"), requiresUpgrade = "garden", flavor = "Ягоды, травы и шмели."),
        PlaceDef("маяк", "Маяк", listOf("shell"), rareYields = listOf("amber"), requiresUpgrade = "lighthouse", flavor = "Сверху видно весь городок.")
    )

    fun byId(id: String): PlaceDef? = ALL.find { it.id == id }
    fun open(upgrades: Set<String>): List<PlaceDef> = ALL.filter { it.requiresUpgrade == null || it.requiresUpgrade in upgrades }
}

data class RecipeDef(
    val id: String,
    val name: String,
    val emoji: String,
    val inputs: Map<String, Int>,
    val result: String,
    val xp: Int
)

object Recipes {
    val ALL = listOf(
        RecipeDef("pie", "Пирог с мёдом", "🥧", mapOf("bread" to 1, "honey" to 1), "pie", 8),
        RecipeDef("bouquet", "Букет", "💐", mapOf("flower" to 3), "bouquet", 6),
        RecipeDef("fishtart", "Рыбный тарт", "🍥", mapOf("fish" to 1, "flour" to 1), "fishtart", 7),
        RecipeDef("teaset", "Чайный набор", "🫖", mapOf("tea" to 1, "honey" to 1), "teaset", 7),
        RecipeDef("box", "Шкатулка", "📦", mapOf("plank" to 2, "nail" to 2), "box", 9),
        RecipeDef("wreath", "Венок", "🌺", mapOf("flower" to 2, "berry" to 1), "wreath", 6),
        RecipeDef("necklace", "Ожерелье", "📿", mapOf("pearl" to 1, "shell" to 2), "necklace", 16),
        RecipeDef("charm", "Оберег", "🧿", mapOf("amber" to 1, "plank" to 1), "charm", 16)
    )

    fun byId(id: String): RecipeDef? = ALL.find { it.id == id }
}

data class UpgradeDef(
    val id: String,
    val name: String,
    val emoji: String,
    val price: Int,
    val description: String
)

object Upgrades {
    val ALL = listOf(
        UpgradeDef("lanterns", "Фонарики", "🏮", 60, "Вечером городок светится. +1 к максимуму сил."),
        UpgradeDef("cat", "Рыжий кот", "🐈", 80, "Живёт на площади. Изредка приносит удачу — редкие находки чаще."),
        UpgradeDef("flowerbeds", "Клумбы", "🌷", 100, "Городок цветёт. На рынке иногда находишь лишний цветок."),
        UpgradeDef("fountain", "Фонтан", "⛲", 120, "Жители любуются им: сплетни распространяются быстрее."),
        UpgradeDef("garden", "Общий сад", "🌱", 150, "Открывает Сад: ягоды, травы и грибы."),
        UpgradeDef("fair", "Ярмарочный шатёр", "🎪", 200, "Заказы жителей оплачиваются щедрее (+50% монет)."),
        UpgradeDef("lighthouse", "Старый маяк", "🗼", 250, "Открывает Маяк и историю Осипа. Ночью все спят крепче: +1 к максимуму сил."),
        UpgradeDef("sweet_shop", "Лавка сладостей", "🍬", 300, "Скупает у тебя всё по двойной цене: продажа предметов +50%."),
        UpgradeDef("ferry", "Паром", "⛴", 420, "На причале теперь двойной улов: иногда рыба идёт парой."),
        UpgradeDef("library", "Библиотека", "📚", 550, "Смотрительница объясняет мир: весь опыт +25%."),
        UpgradeDef("telescope", "Телескоп", "🔭", 650, "С маяка видно дальше: редкие находки встречаются чаще."),
        UpgradeDef("townhall", "Ратуша", "🏛", 800, "Городок расцвёл: +1 к максимуму сил и +10 монет каждый день.")
    )

    fun byId(id: String): UpgradeDef? = ALL.find { it.id == id }
}

/** Уровни доверия NPC. */
object TrustTiers {
    val TIERS = listOf("Знакомец", "Приятель", "Друг", "Душа города")
    fun tier(trust: Float): Int = when {
        trust >= 0.8f -> 3
        trust >= 0.5f -> 2
        trust >= 0.2f -> 1
        else -> 0
    }
    fun name(trust: Float): String = TIERS[tier(trust)]
}

data class NpcDef(
    val name: String,
    val traits: List<String>,
    val loved: List<String>,
    val liked: List<String>,
    val schedule: List<Triple<Int, String, String>>,
    val bio: String,
    val portraitKey: String
)

object NpcDefs {
    val ALL = listOf(
        NpcDef(
            "Марта", listOf("добрая", "болтливая"),
            loved = listOf("pie"), liked = listOf("bread", "flower"),
            schedule = listOf(
                Triple(8, "пекарня", "печёт хлеб"),
                Triple(13, "рынок", "покупает цветы"),
                Triple(18, "площадь", "гуляет")
            ),
            bio = "Пекарь. Помнит каждого, кто хоть раз пробовал её хлеб.",
            portraitKey = "marta"
        ),
        NpcDef(
            "Борис", listOf("ворчливый"),
            loved = listOf("box"), liked = listOf("teaset", "plank"),
            schedule = listOf(
                Triple(9, "мастерская", "чинит вещи"),
                Triple(19, "таверна", "читает газету")
            ),
            bio = "Мастер на все руки. Ворчит, но чинит всё, что приносят.",
            portraitKey = "boris"
        ),
        NpcDef(
            "Лука", listOf("мечтательный", "тихий"),
            loved = listOf("necklace"), liked = listOf("fish", "shell"),
            schedule = listOf(
                Triple(10, "причал", "ловит рыбу")
            ),
            bio = "Рыбак. Говорят, он слышит, о чём поёт вода.",
            portraitKey = "luka"
        ),
        NpcDef(
            "Аня", listOf("энергичная", "любопытная"),
            loved = listOf("bouquet", "wreath"), liked = listOf("flower", "ribbon"),
            schedule = listOf(
                Triple(7, "рынок", "торгует цветами"),
                Triple(20, "площадь", "танцует")
            ),
            bio = "Цветочница. Мечтает о собственной лавке.",
            portraitKey = "anya"
        ),
        NpcDef(
            "Осип", listOf("бывалый", "немногословный"),
            loved = listOf("charm"), liked = listOf("fish", "teaset"),
            schedule = listOf(
                Triple(9, "причал", "чинит лодку"),
                Triple(17, "таверна", "рассказывает истории")
            ),
            bio = "Старый моряк. Знает, почему погас старый маяк.",
            portraitKey = "osip"
        ),
        NpcDef(
            "Соня", listOf("озорная", "любознательная"),
            loved = listOf("wreath"), liked = listOf("shell", "berry"),
            schedule = listOf(
                Triple(9, "площадь", "ищет приключения"),
                Triple(14, "причал", "собирает ракушки"),
                Triple(18, "сад", "лазит по деревьям")
            ),
            bio = "Непоседа. Уверена, что под причалом зарыт клад.",
            portraitKey = "sonya"
        )
    )

    fun byName(name: String): NpcDef? = ALL.find { it.name == name }
}

data class RequestTemplate(
    val npc: String,
    val text: String,
    val item: String,
    val count: Int,
    val coins: Int,
    val xp: Int
)

object RequestTemplates {
    val ALL = listOf(
        RequestTemplate("Марта", "принеси мне %s — гости уже за столом", "bread", 1, 18, 10),
        RequestTemplate("Марта", "нужна %s для теста, совсем нет времени", "flour", 2, 20, 10),
        RequestTemplate("Борис", "принеси %s — крыльцо само не починится", "plank", 2, 22, 12),
        RequestTemplate("Борис", "закончились %s, выручи", "nail", 2, 18, 10),
        RequestTemplate("Лука", "поделись %s, я задержался на маяке", "fish", 1, 20, 10),
        RequestTemplate("Лука", "мне нужна %s для новой лески… шучу, для коллекции", "shell", 2, 20, 10),
        RequestTemplate("Аня", "принеси %s — сделаю венок к празднику", "flower", 3, 24, 12),
        RequestTemplate("Аня", "мне очень нужна %s для одного букета", "ribbon", 1, 18, 10),
        RequestTemplate("Осип", "принеси %s — вечер будет долгим", "fish", 2, 24, 12),
        RequestTemplate("Осип", "нужен %s, снасть совсем разболталась", "plank", 1, 16, 8),
        RequestTemplate("Соня", "нашла место для венка, но нужна %s!", "berry", 2, 18, 10),
        RequestTemplate("Соня", "принеси %s, я делаю тайник", "shell", 2, 18, 10),
        RequestTemplate("Марта", "гостям нужен %s, а у меня руки в тесте", "honey", 1, 20, 10),
        RequestTemplate("Борис", "принеси %s — полка в таверне шатается", "nail", 3, 24, 12)
    )
}

data class TownEventDef(
    val id: String,
    val name: String,
    val log: String
)

object TownEvents {
    const val NONE = "none"
    val ALL = listOf(
        TownEventDef("rain", "Тёплый дождь", "С неба моросит тёплый дождь. На причале клюёт вдвое лучше."),
        TownEventDef("market_day", "Базарный день", "Рынок гудит: заказы сегодня оплачиваются щедрее."),
        TownEventDef("cat_visit", "Рыжий гость", "На площадь пришёл рыжий кот. Погладь его в журнале событий — говорят, к удаче."),
        TownEventDef("festival", "Праздник фонариков", "Все в хорошем настроении. Разговоры сегодня теплее обычного."),
        TownEventDef("merchant", "Странствующий торговец", "Торговец раскинул лоток на площади: редкости можно купить за монеты."),
        TownEventDef("storm", "Шумное море", "Волны шумят — к причалу сегодня лучше не ходить."),
        TownEventDef("quiet", "Тихий день", "Над городком плывёт тихий день. Иногда и это — счастье.")
    )
    fun byId(id: String): TownEventDef? = ALL.find { it.id == id }
}

data class QuestStepDef(
    val text: String,
    val needTrust: Float = 0f,
    val needItems: Map<String, Int> = emptyMap(),
    val needTalks: Int = 0,
    val requiresUpgrade: String? = null,
    val rewardCoins: Int = 0,
    val rewardItems: Map<String, Int> = emptyMap(),
    val rewardTrust: Float = 0.1f,
    val rewardShards: Int = 1,
    val story: String = ""
)

data class QuestDef(
    val npc: String,
    val title: String,
    val steps: List<QuestStepDef>
)

object Quests {
    val ALL = listOf(
        QuestDef(
            "Марта", "Тёплый хлеб",
            listOf(
                QuestStepDef("Поговори с Мартой и услышь историю пекарни.", needTalks = 1, rewardCoins = 10, rewardTrust = 0.1f, rewardShards = 0, story = "Марта рассказала, как печь спасла городок от голодной зимы."),
                QuestStepDef("Принеси Марте муку (2 шт).", needItems = mapOf("flour" to 2), rewardItems = mapOf("bread" to 3), rewardTrust = 0.15f, story = "Из твоей муки Марта испекла хлеб для всей улицы."),
                QuestStepDef("Стань для Марты ближе (доверие 0.4) и поговори с ней.", needTrust = 0.4f, needTalks = 1, rewardCoins = 20, rewardTrust = 0.1f, story = "Марта призналась: она печёт один лишний хлеб — для друга, который так и не вернулся."),
                QuestStepDef("Принеси Марте мёд, чтобы испечь особенный пирог.", needItems = mapOf("honey" to 1), rewardCoins = 40, rewardTrust = 0.15f, rewardShards = 2, story = "Марта испекла пирог по старому рецепту и оставила кусочек у маяка. Теперь по воскресеньям она печёт для всех — в городке появилась традиция.")
            )
        ),
        QuestDef(
            "Борис", "Старая лодка",
            listOf(
                QuestStepDef("Помоги Борису в мастерской.", needTrust = 0f, rewardCoins = 10, rewardTrust = 0.1f, story = "Борис ворчал, но инструмент из твоих рук взял."),
                QuestStepDef("Принеси Борису доски (2) и гвозди (2).", needItems = mapOf("plank" to 2, "nail" to 2), rewardCoins = 15, rewardTrust = 0.15f, story = "Борис чинит старую лодку — ту, на которой когда-то плавал его отец."),
                QuestStepDef("Заслужи доверие Бориса (0.4) и поговори с ним.", needTrust = 0.4f, needTalks = 1, rewardCoins = 20, rewardTrust = 0.1f, story = "Борис рассказал, что лодку они с отцом строили два лета. «Не закончили. Я закончу»."),
                QuestStepDef("Принеси смолу… то есть янтарь — Борис вставит его в нос лодки, как делал отец.", needItems = mapOf("amber" to 1), rewardCoins = 50, rewardTrust = 0.2f, story = "Лодка спущена на воду. Борис молча кивнул тебе — от него это дороже слов. Теперь на причале чаще находят жемчуг.")
            )
        ),
        QuestDef(
            "Лука", "Песня воды",
            listOf(
                QuestStepDef("Поговори с Лукой на причале.", needTalks = 2, rewardCoins = 10, rewardTrust = 0.1f, story = "Лука говорит, что вода поёт по-разному в разное время дня."),
                QuestStepDef("Принеси Луке ракушки (2 шт).", needItems = mapOf("shell" to 2), rewardTrust = 0.15f, story = "Лука прикладывает ракушки к уху: «В этой живёт вчерашний прибой»."),
                QuestStepDef("Стань Луке другом (доверие 0.5) и поговори с ним вечером.", needTrust = 0.5f, needTalks = 1, rewardCoins = 20, rewardTrust = 0.1f, story = "Лука спел тебе песню воды. Ты — первый, кому он её доверил."),
                QuestStepDef("Найди жемчуг и принеси его Луке.", needItems = mapOf("pearl" to 1), rewardCoins = 40, rewardTrust = 0.2f, rewardShards = 2, story = "Лука сделал из жемчуга ожерелье и отпустил его в море — «пусть песня вернётся домой».")
            )
        ),
        QuestDef(
            "Аня", "Цветочная лавка",
            listOf(
                QuestStepDef("Принеси Ане цветы (3 шт).", needItems = mapOf("flower" to 3), rewardCoins = 10, rewardTrust = 0.1f, story = "Аня сплела из твоих цветов венок и станцевала."),
                QuestStepDef("Поговори с Аней и заслужи её доверие (0.3).", needTrust = 0.3f, needTalks = 1, rewardCoins = 15, rewardTrust = 0.1f, story = "Аня шепнула: мечтает о собственной лавке, но боится, что не хватит смелости."),
                QuestStepDef("Принеси Ане ленты (2) и букет.", needItems = mapOf("ribbon" to 2, "bouquet" to 1), rewardCoins = 25, rewardTrust = 0.15f, story = "Вы вместе украсили прилавок. Пол-городка пришло посмотреть."),
                QuestStepDef("Помоги Ане открыть лавку: нужно доверие 0.6 и поговорить с ней.", needTrust = 0.6f, needTalks = 1, rewardCoins = 50, rewardTrust = 0.2f, rewardShards = 2, story = "Лавка «Цветы у рынка» открыта! Теперь на рынке каждый день появляется лишний цветок — подарок Ани.")
            )
        ),
        QuestDef(
            "Осип", "Погасший маяк",
            listOf(
                QuestStepDef("Поговори с Осипом.", needTalks = 1, rewardCoins = 10, rewardTrust = 0.1f, story = "Осип рассказал, что маяк погас в ночь, когда он ушёл в последнее плавание."),
                QuestStepDef("Принеси Осипу рыбу (2 шт).", needItems = mapOf("fish" to 2), rewardCoins = 15, rewardTrust = 0.15f, story = "Осип угостил тебя ухой и показал карту со старыми отметками."),
                QuestStepDef("Заслужи доверие Осипа (0.5) и поговори с ним.", needTrust = 0.5f, needTalks = 1, rewardCoins = 25, rewardTrust = 0.1f, story = "Осип готов зажечь маяк снова, но боится: «Он светил тем, кого уже нет»."),
                QuestStepDef("Принеси Осипу янтарь для линзы (нужен открытый «Старый маяк»).", needItems = mapOf("amber" to 1), needTrust = 0.6f, requiresUpgrade = "lighthouse", rewardCoins = 60, rewardTrust = 0.2f, rewardShards = 2, story = "Маяк горит. Осип смотрит на море и улыбается: «Теперь все знают дорогу домой».")
            )
        ),
        QuestDef(
            "Соня", "Тайна ракушек",
            listOf(
                QuestStepDef("Принеси Соне ракушку.", needItems = mapOf("shell" to 1), rewardCoins = 8, rewardTrust = 0.1f, story = "Соня показала свою карту сокровищ. Крестик — под причалом."),
                QuestStepDef("Поговори с Соней ещё пару раз.", needTalks = 2, rewardCoins = 10, rewardTrust = 0.1f, story = "Соня уверена: клад оставил капитан, который очень любил ракушки."),
                QuestStepDef("Найди жемчуг — ключ к кладу, по словам Сони.", needItems = mapOf("pearl" to 1), rewardCoins = 20, rewardTrust = 0.15f, story = "Соня ахнула: жемчуг подошёл к замку старой шкатулки!"),
                QuestStepDef("Поговори с Соней (доверие 0.5) — и откройте клад вместе.", needTrust = 0.5f, needTalks = 1, rewardCoins = 80, rewardTrust = 0.2f, rewardShards = 2, story = "В шкатулке — старые монеты и записка: «Клад — тому, кто умеет дружить». Соня поделилась с тобой по-честному.")
            )
        )
    )

    fun forNpc(name: String): QuestDef? = ALL.find { it.npc == name }
}

object Achievements {
    /**
     * id, название, описание, награда монет, осколки мозаики.
     * Осколки дают только «вехи» — иначе мозаика собирается за несколько дней.
     */
    data class Def(val id: String, val name: String, val desc: String, val coins: Int, val shards: Int = 0)

    val ALL = listOf(
        Def("meet", "Знакомство", "Представься жителям", 10),
        Def("talk10", "Собеседник", "Поговори с жителями 10 раз", 20),
        Def("talk50", "Душа разговора", "Поговори с жителями 50 раз", 60, 1),
        Def("help5", "Добрая душа", "Помоги жителям 5 раз", 20),
        Def("help20", "Надёжные руки", "Помоги жителям 20 раз", 60, 1),
        Def("gather20", "Собиратель", "Собери 20 ресурсов", 20),
        Def("gather100", "Хранитель закромов", "Собери 100 ресурсов", 80, 1),
        Def("craft5", "Мастер-подмастерье", "Приготовь 5 вещей", 20),
        Def("craft20", "Золотые руки", "Приготовь 20 вещей", 60, 1),
        Def("gift5", "Тёплый жест", "Подари 5 подарков", 20, 1),
        Def("gift15", "Щедрое сердце", "Подари 15 подарков", 60, 1),
        Def("gossip10", "Ухо востро", "Узнай 10 сплетен", 20),
        Def("request5", "Отзывчивость", "Выполни 5 заказов", 30, 1),
        Def("request20", "Городской помощник", "Выполни 20 заказов", 80, 1),
        Def("quest1", "Первая история", "Пройди сюжетную арку жителя", 50, 2),
        Def("quest6", "Летописец", "Пройди все арки городка", 200, 4),
        Def("rich500", "Копилка", "Накопи 500 монет", 40),
        Def("level5", "Свой человек", "Достигни 5 уровня", 40, 1),
        Def("level10", "Старожил", "Достигни 10 уровня", 80, 2),
        Def("upg3", "Обустройство", "Улучши городок 3 раза", 40, 1),
        Def("upg_all", "Город мечты", "Открой все улучшения", 150, 3),
        Def("rare1", "Блеск", "Найди редкий предмет", 30, 1),
        Def("friend1", "Настоящий друг", "Доверие любого жителя 0.8", 50, 1),
        Def("streak3", "Три дня подряд", "Возвращайся 3 дня подряд", 30, 1),
        Def("streak7", "Неделя заботы", "Возвращайся 7 дней подряд", 80, 2),
        Def("days7", "Неделя в городке", "Проведи в городке 7 дней", 40, 1),
        Def("shards30", "Половина пути", "Собери 30 осколков мозаики", 60, 0),
        Def("puzzle_full", "Мозаика собрана", "Собери всю мозаику городка", 100, 0)
    )

    fun byId(id: String): Def? = ALL.find { it.id == id }
}

object DailyGoalPool {
    data class Def(val id: String, val text: String, val target: Int)

    val ALL = listOf(
        Def("talk", "Поговори с жителями", 2),
        Def("kind", "Сделай добрый поступок", 1),
        Def("gossip", "Узнай сплетню", 1),
        Def("gather", "Собери ресурсы", 3),
        Def("craft", "Приготовь что-нибудь", 1),
        Def("gift", "Подари подарок", 1),
        Def("request", "Выполни заказ жителя", 1)
    )
}

/** Реплики для диалогов и подарков. */
object Lines {
    val GIFT_LOVED = listOf(
        "Это же %s! Откуда ты знаешь, что я это люблю?",
        "Ой! %s — моя слабость. Спасибо!",
        "Ты принёс мне %s? Ну всё, ты мой любимый человек."
    )
    val GIFT_LIKED = listOf(
        "О, %s! Приятно, спасибо.",
        "%s? Как мило с твоей стороны.",
        "Спасибо за %s — пригодится!"
    )
    val GIFT_NEUTRAL = listOf(
        "Спасибо… положу куда-нибудь.",
        "Хм, %s. Ну… спасибо.",
        "О, спасибо. Неожиданно."
    )
    val HELP_LINES = listOf(
        "Вот это помощь! Спасибо.",
        "Ох, выручаешь. Я запомню.",
        "Ловко у тебя выходит."
    )
    val AWAY_LINES = listOf(
        "%s расспрашивал(а) всех, когда ты вернёшься.",
        "%s вспоминал(а) твою историю за чаем.",
        "%s приберёг(ла) для тебя новости.",
        "Пока тебя не было, %s и ещё пол-городка обсуждали ярмарку.",
        "Пока тебя не было, над рекой был туман, и %s видел(а) в нём дракона."
    )
    val REST_LINES = listOf(
        "Ты немного отдохнул(ла). Силы возвращаются.",
        "Чай, плед и тишина. Хорошо."
    )
    val PET_CAT = "Рыжий кот мурчит как трактор. +1 к силам и капля удачи."
}

const val PUZZLE_TOTAL = 60
