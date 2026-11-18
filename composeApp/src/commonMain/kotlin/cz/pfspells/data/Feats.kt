package cz.pfspells.data

import cz.pfspells.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Feat z Archives of Nethys (`Feats.json`, generuje `tools/build_feats.py`, jen přibalená verze).
 * [sections] = dvojice [nadpis, text] (Benefit, Normal, Special…), prerekvizity jsou zvlášť v [prereq].
 */
@Serializable
data class Feat(
    val id: String,
    val name: String,
    val types: List<String> = emptyList(),
    val prereq: String = "",
    val summary: String = "",
    val pfs: Boolean = false,
    val source: String? = null,
    val flavor: String? = null,
    val sections: List<List<String>> = emptyList(),
) {
    val searchName: String by lazy { name.lowercase() }

    /** Lze vzít víckrát („Special: You can gain this feat multiple times“ – Weapon Focus, Extra Ki…). */
    val repeatable: Boolean by lazy {
        sections.any { sec ->
            sec.size >= 2 && sec[0].startsWith("Special", ignoreCase = true) &&
                REPEAT_PHRASES.any { sec[1].contains(it, ignoreCase = true) }
        }
    }
    val url: String get() = "https://aonprd.com/FeatDisplay.aspx?ItemName=" + name.replace(" ", "%20")
}

private val REPEAT_PHRASES = listOf("multiple times", "more than once", "can be taken multiple", "can select this feat multiple")

class FeatDatabase(val feats: List<Feat>) {
    private val byId = feats.associateBy { it.id }
    private val byName = feats.groupBy { it.searchName }
    val types: List<String> = feats.flatMap { it.types }.groupingBy { it }.eachCount().filter { it.value >= 5 }.keys.sorted()

    fun get(id: String): Feat? = byId[id]
    fun byName(name: String): Feat? = byName[name.trim().lowercase()]?.firstOrNull()
}

object FeatRepository {
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): FeatDatabase = withContext(Dispatchers.Default) {
        val text = Res.readBytes("files/Feats.json").decodeToString()
        FeatDatabase(json.decodeFromString(ListSerializer(Feat.serializer()), text))
    }
}

// ---------------- prerekvizity ----------------

enum class PrereqStatus { MET, UNMET, UNKNOWN }

data class Prereq(val text: String, val status: PrereqStatus)

/**
 * Hrubé vyhodnocení textu prerekvizit („Str 13, Power Attack, base attack bonus +1“). Ověřuje atributy, BAB,
 * úroveň postavy / classy, caster level, schopnost sesílat kouzla N. levelu a featy postavy; ostatní
 * (skill ranky, rasa, class feature…) je „ověř ručně“. Nic se nevynucuje.
 */
object FeatPrereqs {
    private val ABILITY = Regex("""^(str|dex|con|int|wis|cha)[a-z]*\s+(\d+)""")
    private val BAB = Regex("""base attack bonus\s*\+?(\d+)""")
    private val CASTER_LEVEL = Regex("""^caster level\s+(\d+)""")
    private val CHAR_LEVEL = Regex("""^character level\s+(\d+)""")
    private val CLASS_LEVEL = Regex("""^([a-z][a-z' ()]*?)\s+level\s+(\d+)""")
    private val CAST_LEVEL = Regex("""ability to cast\s+(\d+)(?:st|nd|rd|th)-level(\s+(arcane|divine|psychic))?\s+spells""")

    /** Rozdělí podle čárek a středníků mimo závorky. */
    fun split(prereq: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        val cur = StringBuilder()
        for (ch in prereq) {
            when {
                ch == '(' -> { depth++; cur.append(ch) }
                ch == ')' -> { depth = (depth - 1).coerceAtLeast(0); cur.append(ch) }
                (ch == ',' || ch == ';') && depth == 0 -> { out += cur.toString(); cur.clear() }
                else -> cur.append(ch)
            }
        }
        out += cur.toString()
        return out.map { it.trim().trimEnd('.').trim() }.filter { it.isNotEmpty() }
    }

    fun evaluate(c: Character, db: FeatDatabase?, prereq: String): List<Prereq> =
        split(prereq).map { Prereq(it, status(c, db, it)) }

    /** Souhrn: některá nesplněná → UNMET, jinak některá neověřitelná → UNKNOWN, jinak MET. */
    fun overall(list: List<Prereq>): PrereqStatus = when {
        list.any { it.status == PrereqStatus.UNMET } -> PrereqStatus.UNMET
        list.any { it.status == PrereqStatus.UNKNOWN } -> PrereqStatus.UNKNOWN
        else -> PrereqStatus.MET
    }

    fun status(c: Character, db: FeatDatabase?, text: String): PrereqStatus {
        val t = text.lowercase().trim()
        if (" or " in t) {
            val parts = t.split(" or ").map { status(c, db, it) }
            return when {
                parts.any { it == PrereqStatus.MET } -> PrereqStatus.MET
                parts.all { it == PrereqStatus.UNMET } -> PrereqStatus.UNMET
                else -> PrereqStatus.UNKNOWN
            }
        }
        ABILITY.find(t)?.let { m ->
            if (m.range.last == t.lastIndex) {
                val a = Ability.entries.first { it.name.lowercase() == m.groupValues[1] }
                return met(c.abilities[a] >= m.groupValues[2].toInt())
            }
        }
        BAB.find(t)?.let { return met(c.stats().bab >= it.groupValues[1].toInt()) }
        CHAR_LEVEL.find(t)?.let { return met(c.level >= it.groupValues[1].toInt()) }
        CASTER_LEVEL.find(t)?.let { m ->
            val cl = c.classes.filter { CasterClasses.get(it.classId) != null }.maxOfOrNull { it.level } ?: 0
            return met(cl >= m.groupValues[1].toInt())
        }
        CAST_LEVEL.find(t)?.let { m ->
            val n = m.groupValues[1].toInt()
            val can = c.classes.any { e -> CasterClasses.get(e.classId)?.let { it.maxSpellLevel(e.level) >= n } == true }
            // arcane / divine rozlišujeme jen zčásti – splněné s typem je „ověř ručně“
            return if (!can) PrereqStatus.UNMET else if (m.groupValues[3].isNotEmpty()) PrereqStatus.UNKNOWN else PrereqStatus.MET
        }
        CLASS_LEVEL.find(t)?.let { m ->
            val cls = classByName(m.groupValues[1].trim())
            if (cls != null) return met((c.classes.firstOrNull { it.classId == cls.id }?.level ?: 0) >= m.groupValues[2].toInt())
        }
        // feat – přesné jméno, nebo „Weapon Focus (longsword)“ → stačí mít Weapon Focus
        val base = t.substringBefore(" (").trim()
        if (db != null && (db.byName(t) != null || db.byName(base) != null)) {
            val has = c.feats.any { p -> p.name.lowercase().let { it == t || it == base || it.startsWith("$base (") } }
            return if (has) PrereqStatus.MET else PrereqStatus.UNMET
        }
        return PrereqStatus.UNKNOWN
    }

    private fun met(b: Boolean) = if (b) PrereqStatus.MET else PrereqStatus.UNMET

    private fun classByName(name: String): ClassInfo? =
        ClassCatalog.all.firstOrNull { it.name.lowercase() == name || it.id == name.replace(' ', '_') }
}

// ---------------- počet featů ----------------

/** Úrovně classy, na kterých dostává bonusový feat (Core a další příručky; „jen bojový“ apod. se nerozlišuje). */
private val BONUS_FEAT_LEVELS: Map<String, List<Int>> = mapOf(
    "fighter" to listOf(1) + (2..20 step 2),
    "wizard" to listOf(5, 10, 15, 20),
    "monk" to listOf(1, 2, 6, 10, 14, 18),
    "monk_unchained" to listOf(1, 2, 6, 10, 14, 18),
    "magus" to listOf(5, 11, 17),
    "warpriest" to (3..18 step 3).toList(),
    "brawler" to (2..20 step 3).toList(),
    "swashbuckler" to (4..20 step 4).toList(),
    "gunslinger" to (4..20 step 4).toList(),
    "cavalier" to listOf(6, 12, 18),
    "samurai" to listOf(6, 12, 18),
    "ranger" to listOf(2, 6, 10, 14, 18),
    "inquisitor" to (3..18 step 3).toList(),
    "hunter" to (3..18 step 3).toList(),
    "sorcerer" to listOf(7, 13, 19),
    "bloodrager" to (6..18 step 3).toList(),
)

data class FeatSlots(
    /** 1. úroveň a každá lichá */
    val general: Int,
    /** bonusové featy podle class: classId → počet */
    val bonus: Map<String, Int>,
    val extra: Int,
) {
    val total: Int get() = general + bonus.values.sum() + extra
}

fun Character.featSlots(): FeatSlots = FeatSlots(
    general = (level + 1) / 2,
    bonus = classes.mapNotNull { e ->
        val n = BONUS_FEAT_LEVELS[e.classId]?.count { it <= e.level } ?: 0
        if (n > 0) e.classId to n else null
    }.toMap(),
    extra = extraFeats,
)
