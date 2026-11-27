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
        // „—“ ve zdroji = žádné prerekvizity
        FeatDatabase(json.decodeFromString(ListSerializer(Feat.serializer()), text).map { f ->
            if (FeatPrereqs.isNone(f.prereq)) f.copy(prereq = "") else f
        })
    }
}

// ---------------- prerekvizity ----------------

enum class PrereqStatus { MET, UNMET, UNKNOWN }

/** Jedna prerekvizita; [confirmed] = uživatel ji u postavy ručně potvrdil (pak je [status] MET). */
data class Prereq(val text: String, val status: PrereqStatus, val confirmed: Boolean = false)

/**
 * Hrubé vyhodnocení textu prerekvizit („Str 13, Power Attack, base attack bonus +1“). Ověřuje atributy, BAB,
 * úroveň postavy / classy, caster level, schopnost sesílat kouzla N. levelu, rasu (je-li u postavy vybraná) a featy postavy;
 * ostatní (skill ranky, rasové traity, class feature…) je „ověř ručně“. Nic se nevynucuje.
 */
object FeatPrereqs {
    private val ABILITY = Regex("""^(str|dex|con|int|wis|cha)[a-z]*\s+(\d+)""")
    private val BAB = Regex("""base attack bonus\s*\+?(\d+)""")
    private val CASTER_LEVEL = Regex("""^caster level\s+(\d+)""")
    private val CHAR_LEVEL = Regex("""^character level\s+(\d+)""")
    private val CLASS_LEVEL = Regex("""^([a-z][a-z' ()]*?)\s+level\s+(\d+)""")
    private val CAST_LEVEL = Regex("""ability to cast\s+(\d+)(?:st|nd|rd|th)-level(\s+(arcane|divine|psychic))?\s+spells""")

    /** Zkratka knihy přilepená ke jménu featu ve zdroji („Improved Dirty TrickAPG“). */
    private val SOURCE_SUFFIX = Regex("""(apg|arg|acg|uc|um|ue|ui|uw|oa|isg)$""")

    /** Zkratka knihy přilepená ke slovu („brawlerACG“, „vishkanyaARG“); bez „ue“ / „um“ (true, medium…). */
    private val GLUED_SOURCE = Regex("""(?<=[a-z]{3})(acg|apg|arg|uc|ui|oa|isg|iswg)\b""")
    private val FEAT_TYPE = Regex("""^(?:any|one|at least one|a)\s+([a-z ]+?)\s+feat$""")

    /** „any teamwork feat“, „one metamagic feat“, „teamwork“: postava má feat daného typu (podle databáze featů). */
    private fun featTypeStatus(c: Character, db: FeatDatabase?, t: String): PrereqStatus? {
        if (db == null) return null
        val type = FEAT_TYPE.find(t)?.groupValues?.get(1)?.trim() ?: t.takeIf { it == "teamwork" } ?: return null
        val known = db.types.any { it.lowercase() == type }
        if (!known) return null
        val has = c.feats.any { p -> p.featId?.let { db.get(it) }?.types?.any { it.lowercase() == type } == true }
        return met(has)
    }

    private val OR_SUFFIXES = listOf(" alignment", " domain", " class feature", " racial trait", " subtype", " spells")
    private val NTH_LEVEL_CLASS = Regex("""^(\d+)(?:st|nd|rd|th)-level\s+([a-z ()]+)$""")
    private val HD_SHORT = Regex("""^(\d+)\s+(?:or more\s+)?hd$""")

    private val RANK_SUFFIX = Regex("""\s+\d+\s+ranks?\b.*$""")
    private val SKILL_RANKS = Regex("""^([a-z ]+?)(?:\s*\(([^)]*)\))?\s+(\d+)\s+ranks?\b""")
    private val RANKS_IN = Regex("""^(\d+)\s+ranks?\s+in\s+(.+)$""")
    private val SKILL_REF = Regex("""^(?:at least one\s+)?([a-z ]+?)(?:\s*\(([^)]*)\))?(?:\s+skills?)?(?:\s*\(.*\))?$""")

    /** Rozdělí podle „ or “ mimo závorky. */
    private fun splitOr(t: String): List<String> {
        val out = mutableListOf<String>()
        var depth = 0
        var start = 0
        var i = 0
        while (i < t.length) {
            when {
                t[i] == '(' -> depth++
                t[i] == ')' -> depth = (depth - 1).coerceAtLeast(0)
                depth == 0 && t.startsWith(" or ", i) -> { out += t.substring(start, i); start = i + 4; i += 3 }
            }
            i++
        }
        out += t.substring(start)
        return out.map { it.trim().removePrefix("or ").trim() }.filter { it.isNotEmpty() }
    }

    private fun isSkillName(name: String) = name.trim() == "knowledge" || Skills.byName(name) != null

    /**
     * Skill ranky: „Acrobatics 5 ranks“, „Knowledge (planes) 3 ranks“, „Knowledge (arcana, …, or religion) 5 ranks“,
     * „Craft (any) 1 rank“, „5 ranks in Sense Motive“, „1 rank in at least one Knowledge skill“. Null = text není skill.
     * Postava bez zadaných skillů → „ověř ručně“ (skilly se u ní nevedou).
     */
    private fun skillStatus(c: Character, t: String): PrereqStatus? {
        val skillName: String
        val spec: String?
        val n: Int
        val a = SKILL_RANKS.find(t)?.takeIf { isSkillName(it.groupValues[1]) }
        if (a != null) {
            skillName = a.groupValues[1].trim(); spec = a.groupValues[2].ifEmpty { null }; n = a.groupValues[3].toInt()
        } else {
            val b = RANKS_IN.find(t) ?: return null
            val m = SKILL_REF.find(b.groupValues[2].trim())?.takeIf { isSkillName(it.groupValues[1]) } ?: return null
            n = b.groupValues[1].toInt(); skillName = m.groupValues[1].trim(); spec = m.groupValues[2].ifEmpty { null }
        }
        if (c.skills.isEmpty()) return PrereqStatus.UNKNOWN
        val keys = skillKeys(c, skillName, spec) ?: return null
        return met((keys.maxOfOrNull { c.ranks(it) } ?: 0) >= n)
    }

    /** Klíče ranků, z nichž stačí kterýkoli („Knowledge (any)“, „Perform (sing, or wind)“…). */
    private fun skillKeys(c: Character, skillName: String, spec: String?): List<String>? {
        val any = spec == null || spec.startsWith("any")
        val listed = spec?.split(',', ' ')?.map { it.trim() }?.filter { it.isNotEmpty() && it != "or" && it != "and" }.orEmpty()
        if (skillName == "knowledge") return (if (any) Skills.KNOWLEDGE else listed.filter { it in Skills.KNOWLEDGE }).map { "knowledge.$it" }
        val info = Skills.byName(skillName) ?: return null
        if (!info.subtyped) return listOf(info.id)
        return if (any) c.skills.keys.filter { Skills.idOf(it) == info.id } else listed.map { Skills.key(info.id, it) }
    }

    /** „—“ (pomlčka ve zdroji) = feat nemá prerekvizity. */
    fun isNone(prereq: String): Boolean = prereq.trim().trimEnd('.').let { it.isEmpty() || it == "—" || it == "–" || it == "-" }

    /** Rozdělí podle čárek a středníků mimo závorky. */
    fun split(prereq: String): List<String> {
        if (isNone(prereq)) return emptyList()
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
        val parts = out.map { it.trim().trimEnd('.').trim() }.filter { it.isNotEmpty() }
        // „Half-orc, or orc“ – část začínající „or“ je alternativa k předchozí
        val merged = mutableListOf<String>()
        for (p in parts) {
            if (merged.isNotEmpty() && p.startsWith("or ", ignoreCase = true)) merged[merged.lastIndex] = merged.last() + " " + p
            else merged += p
        }
        return merged
    }

    /** Klíč ručního potvrzení prerekvizity (stejný text u různých featů = jedno potvrzení). */
    fun confirmKey(text: String): String = text.lowercase().trim()

    /** [spells] = databáze kouzel pro „ability to cast dispel magic“ (null → takové prerekvizity zůstanou „?“). */
    fun evaluate(c: Character, db: FeatDatabase?, prereq: String, spells: SpellDatabase? = null): List<Prereq> =
        split(prereq).map { text ->
            val s = status(c, db, text, spells)
            if (s != PrereqStatus.MET && confirmKey(text) in c.confirmedPrereqs) Prereq(text, PrereqStatus.MET, confirmed = true)
            else Prereq(text, s)
        }

    /** Souhrn: některá nesplněná → UNMET, jinak některá neověřitelná → UNKNOWN, jinak MET. */
    fun overall(list: List<Prereq>): PrereqStatus = when {
        list.any { it.status == PrereqStatus.UNMET } -> PrereqStatus.UNMET
        list.any { it.status == PrereqStatus.UNKNOWN } -> PrereqStatus.UNKNOWN
        else -> PrereqStatus.MET
    }

    fun status(c: Character, db: FeatDatabase?, text: String, spells: SpellDatabase? = null): PrereqStatus {
        val t = text.lowercase().trim().replace('’', '\'').replace('�', '\'').replace(Regex("""\s+"""), " ")
            .replace(Regex("""\s*\(see [^)]*\)$"""), "") // „Int 13 (see special)“
            .replace(GLUED_SOURCE, "") // „brawlerACG level 8th“
            .replace(Regex("""^performance \("""), "perform (") // překlep ve zdroji
            .trim()
        // proficience mají „or“ / „and“ uvnitř („proficiency with medium or heavy armor“)
        PrereqRules.proficiency(c, t)?.let { return it }
        PrereqRules.size(c, t)?.let { return it } // „small size or smaller“
        PrereqRules.hitDiceOrSave(c, t)?.let { return it } // „5 or more Hit Dice“
        featTypeStatus(c, db, t)?.let { return it } // „any teamwork feat“
        PrereqRules.racialTrait(c, t)?.let { return it } // „defensive training and hatred racial traits“
        val alternatives = splitOr(t)
        if (alternatives.size > 1) {
            // společná přípona / předpona platí pro všechny části: „Knowledge (nature) or Survival 5 ranks“,
            // „good or lawful alignment“, „animal or plant domain“, „ability to cast possession or greater possession“
            val last = alternatives.last()
            val suffixes = listOfNotNull(RANK_SUFFIX.find(last)?.value) +
                OR_SUFFIXES.filter { last.endsWith(it) }
            val castPrefix = "ability to cast ".takeIf { alternatives.first().startsWith(it) }
            val parts = alternatives.map { p ->
                // nejdřív s příponou („animal“ + „ domain“ – samotné „animal“ by se vyhodnotilo jako typ tvora)
                val rank = RANK_SUFFIX.find(last)?.value
                val withSuffix = suffixes.filter { sfx -> !p.endsWith(sfx.trim()) && !(sfx == rank && p.any { it.isDigit() }) }.map { p + it }
                val variants = withSuffix + p + listOfNotNull(castPrefix?.takeIf { !p.startsWith(it) }?.let { it + p })
                // první varianta, kterou appka umí vyhodnotit
                variants.map { status(c, db, it, spells) }.firstOrNull { it != PrereqStatus.UNKNOWN } ?: PrereqStatus.UNKNOWN
            }
            return when {
                parts.any { it == PrereqStatus.MET } -> PrereqStatus.MET
                parts.all { it == PrereqStatus.UNMET } -> PrereqStatus.UNMET
                else -> PrereqStatus.UNKNOWN
            }
        }
        skillStatus(c, t)?.let { return it }
        ABILITY.find(t)?.let { m ->
            if (m.range.last == t.lastIndex) {
                val a = Ability.entries.first { it.name.lowercase() == m.groupValues[1] }
                return met(c.scores[a] >= m.groupValues[2].toInt())
            }
        }
        BAB.find(t)?.let { return met(c.stats().bab >= it.groupValues[1].toInt()) }
        CHAR_LEVEL.find(t)?.let { return met(c.level >= it.groupValues[1].toInt()) }
        CASTER_LEVEL.find(t)?.let { m ->
            val cl = c.classes.filter { it.caster != null }.maxOfOrNull { it.level } ?: 0
            return met(cl >= m.groupValues[1].toInt())
        }
        NTH_LEVEL_CLASS.find(t)?.let { m ->
            classByName(m.groupValues[2].trim())?.let { cls ->
                return met((c.classes.firstOrNull { it.classId == cls.id }?.level ?: 0) >= m.groupValues[1].toInt())
            }
        }
        HD_SHORT.find(t)?.let { return met(c.level >= it.groupValues[1].toInt()) }
        PrereqRules.spellKind(c, t)?.let { return it }
        CAST_LEVEL.find(t)?.let { m ->
            val n = m.groupValues[1].toInt()
            val can = c.classes.any { e -> e.caster?.let { it.maxSpellLevel(e.level) >= n } == true }
            // arcane / divine rozlišujeme jen zčásti – splněné s typem je „ověř ručně“
            return if (!can) PrereqStatus.UNMET else if (m.groupValues[3].isNotEmpty()) PrereqStatus.UNKNOWN else PrereqStatus.MET
        }
        CLASS_LEVEL.find(t)?.let { m ->
            val cls = classByName(m.groupValues[1].trim())
            if (cls != null) return met((c.classes.firstOrNull { it.classId == cls.id }?.level ?: 0) >= m.groupValues[2].toInt())
        }
        raceStatus(c, t)?.let { return it }
        PrereqRules.hitDiceOrSave(c, t)?.let { return it }
        PrereqRules.alignment(c, t)?.let { return it }
        PrereqRules.deity(c, t)?.let { return it }
        PrereqRules.size(c, t)?.let { return it }
        PrereqRules.creatureType(c, t)?.let { return it }
        // feat – přesné jméno, nebo „Weapon Focus (longsword)“ / „Weapon Focus with the chosen weapon“ → stačí mít Weapon Focus;
        // zdroj občas ke jménu přilepí zkratku knihy („Improved Dirty TrickAPG“)
        if (db != null) {
            val noFeat = t.removeSuffix(" feat").trim()
            val candidates = listOf(t, noFeat, t.substringBefore(" ("), t.substringBefore(" with "), t.replace(SOURCE_SUFFIX, ""), noFeat.replace(SOURCE_SUFFIX, ""))
                .map { it.trim() }.distinct()
            val name = candidates.firstOrNull { db.byName(it) != null }
            if (name != null) {
                val has = c.feats.any { p -> p.name.lowercase().let { it == t || it == name || it.startsWith("$name (") } }
                return if (has) PrereqStatus.MET else PrereqStatus.UNMET
            }
        }
        PrereqRules.classFeature(c, t)?.let { return it }
        PrereqRules.namedSpell(c, t, spells)?.let { return it }
        PrereqRules.domain(c, t)?.let { return it }
        return PrereqStatus.UNKNOWN
    }

    private fun met(b: Boolean) = if (b) PrereqStatus.MET else PrereqStatus.UNMET

    /**
     * Rasa: „Dwarf“, „Half-orc“, „gnome subtype“, „Human (Varisian)“. Null = text není rasa.
     * Bez vybrané rasy „ověř ručně“; s upřesněním v závorce (etnikum, varianta) při správné rase taky.
     */
    private fun raceStatus(c: Character, t: String): PrereqStatus? {
        val base = t.removeSuffix(" subtype").removeSuffix(" race").substringBefore(" (").trim()
        if (!RaceCatalog.isRaceWord(base)) return null
        val race = c.raceInfo ?: return PrereqStatus.UNKNOWN
        return when {
            !race.countsAs(base) -> PrereqStatus.UNMET
            "(" in t -> PrereqStatus.UNKNOWN
            else -> PrereqStatus.MET
        }
    }

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
    /** rasový bonusový feat (human, half-elf) */
    val race: Int = 0,
) {
    val total: Int get() = general + bonus.values.sum() + extra + race
}

/** Úrovně, na kterých classa dostává bonusový feat (fighter 1, 2, 4…). */
internal fun bonusFeatLevels(classId: String): List<Int> = BONUS_FEAT_LEVELS[classId].orEmpty()

fun Character.featSlots(): FeatSlots = FeatSlots(
    general = (level + 1) / 2,
    bonus = classes.mapNotNull { e ->
        // archetyp může bonusové featy nahradit („1st-level bonus feat“, „Bonus Feats“)
        val fx = e.archetypeEffect
        val n = (BONUS_FEAT_LEVELS[e.classId]?.count { it <= e.level } ?: 0) - fx.lostBonusFeats + fx.gainedBonusFeats
        if (n > 0) e.classId to n else null
    }.toMap(),
    extra = extraFeats,
    // human Bonus Feat / half-elf Adaptability – alternativní trait je může nahradit
    race = (raceInfo?.bonusFeats ?: 0).takeIf { keepsRacialTrait("bonus feat") && keepsRacialTrait("adaptability") } ?: 0,
)
