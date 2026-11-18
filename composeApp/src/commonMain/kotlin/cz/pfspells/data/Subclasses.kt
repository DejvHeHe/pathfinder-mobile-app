package cz.pfspells.data

/** Druh podtřídy podle `access_ways` v `SpellList.json`. */
enum class SubclassKind(val sourceKey: String, val prefix: String) {
    DOMAIN("Domain", "domain"),
    SUBDOMAIN("Subdomain", "subdomain"),
    MYSTERY("Mystery", "mystery"),
    BLOODLINE("Bloodline", "bloodline"),
    ELEMENT("Elemental School", "element"),
}

/** Index: podtřída → spelly po spell levelech (z [Spell.subLevels]). Počítá se jednou pro databázi. */
class SubclassIndex(spells: List<Spell>) {
    private val byKey: Map<String, Map<Int, List<Spell>>> = buildMap<String, MutableMap<Int, MutableList<Spell>>> {
        for (s in spells) for ((key, lvl) in s.subLevels) {
            getOrPut(key) { mutableMapOf() }.getOrPut(lvl) { mutableListOf() } += s
        }
    }.mapValues { (_, m) -> m.toList().sortedBy { it.first }.toMap() }

    /** Jména podtříd daného druhu, seřazená. */
    fun names(kind: SubclassKind): List<String> =
        byKey.keys.filter { it.startsWith(kind.prefix + ":") }.map { it.substringAfter(':') }.sorted()

    /** Spelly podtřídy po spell levelech (prázdné, když podtřídu nezná). */
    fun spells(kind: SubclassKind, name: String): Map<Int, List<Spell>> = byKey["${kind.prefix}:$name"] ?: emptyMap()
}

/** Osm škol magie (pro specializaci kouzelníka). */
val MAGIC_SCHOOLS = listOf("abjuration", "conjuration", "divination", "enchantment", "evocation", "illusion", "necromancy", "transmutation")

/** Hodnota specializace kouzelníka: škola, „universalist“, nebo elementární škola „element:fire“. */
const val UNIVERSALIST = "universalist"

/** Klíče v [ClassEntry.subclass]. */
object SubclassSlot {
    const val DOMAIN1 = "domain1"
    const val SUBDOMAIN1 = "subdomain1"
    const val DOMAIN2 = "domain2"
    const val SUBDOMAIN2 = "subdomain2"
    const val MYSTERY = "mystery"
    const val BLOODLINE = "bloodline"
    const val SCHOOL = "school"
    const val OPPOSITION1 = "opposition1"
    const val OPPOSITION2 = "opposition2"
}

/** Jak classa pracuje s podtřídou. */
enum class SubclassType {
    /** cleric: 2 domény (+ subdomény), doménový slot 1 / spell level */
    TWO_DOMAINS,
    /** druid: 1 doména (+ subdoména), doménový slot 1 / spell level */
    ONE_DOMAIN,
    /** oracle: mystérium – bonusová známá kouzla na sudých úrovních */
    MYSTERY,
    /** sorcerer: bloodline – bonusová známá kouzla od 3. úrovně každou lichou */
    BLOODLINE,
    /** wizard: specializace (škola / elementární škola) + opoziční školy, školní slot 1 / spell level */
    SCHOOL,
}

fun subclassType(classId: String): SubclassType? = when (classId) {
    "cleric" -> SubclassType.TWO_DOMAINS
    "druid" -> SubclassType.ONE_DOMAIN
    "oracle" -> SubclassType.MYSTERY
    "sorcerer" -> SubclassType.BLOODLINE
    "wizard" -> SubclassType.SCHOOL
    else -> null
}

/** Spelly domény s náhradami ze subdomény (subdoména nahrazuje spelly domény na svých levelech). */
fun domainSpells(index: SubclassIndex, domain: String?, subdomain: String?): Map<Int, List<Spell>> {
    if (domain == null) return emptyMap()
    val base = index.spells(SubclassKind.DOMAIN, domain)
    val sub = subdomain?.let { index.spells(SubclassKind.SUBDOMAIN, it) } ?: emptyMap()
    return (base.keys + sub.keys).sorted().associateWith { lvl -> sub[lvl] ?: base[lvl].orEmpty() }
}

/**
 * Bonusová kouzla podtřídy classy po spell levelech (jen do nejvyššího levelu, který classa na úrovni smí):
 * domény (cleric, druid), mystérium (oracle, spell level N od úrovně 2N), bloodline (sorcerer, od úrovně 2N + 1).
 * Kouzelník bonusová kouzla nemá (školní slot se řeší zvlášť).
 */
fun ClassEntry.bonusSpellsByLevel(index: SubclassIndex): Map<Int, List<Spell>> {
    val cls = CasterClasses.get(classId) ?: return emptyMap()
    val max = cls.maxSpellLevel(level)
    val all: Map<Int, List<Spell>> = when (subclassType(classId)) {
        SubclassType.TWO_DOMAINS -> {
            val a = domainSpells(index, subclass[SubclassSlot.DOMAIN1], subclass[SubclassSlot.SUBDOMAIN1])
            val b = domainSpells(index, subclass[SubclassSlot.DOMAIN2], subclass[SubclassSlot.SUBDOMAIN2])
            (a.keys + b.keys).associateWith { (a[it].orEmpty() + b[it].orEmpty()).distinctBy { s -> s.id } }
        }
        SubclassType.ONE_DOMAIN -> domainSpells(index, subclass[SubclassSlot.DOMAIN1], subclass[SubclassSlot.SUBDOMAIN1])
        SubclassType.MYSTERY -> subclass[SubclassSlot.MYSTERY]?.let { m ->
            index.spells(SubclassKind.MYSTERY, m).filterKeys { level >= 2 * it }
        } ?: emptyMap()
        SubclassType.BLOODLINE -> subclass[SubclassSlot.BLOODLINE]?.let { b ->
            index.spells(SubclassKind.BLOODLINE, b).filterKeys { level >= 2 * it + 1 }
        } ?: emptyMap()
        SubclassType.SCHOOL, null -> emptyMap()
    }
    return all.filterKeys { it in 1..max }.filterValues { it.isNotEmpty() }
}

/** Úroveň classy, od které má oracle / sorcerer bonusové kouzlo daného spell levelu. */
fun bonusSpellClassLevel(type: SubclassType, spellLevel: Int): Int? = when (type) {
    SubclassType.MYSTERY -> 2 * spellLevel
    SubclassType.BLOODLINE -> 2 * spellLevel + 1
    else -> null
}

/** Doménový / školní slot: classa má na každém spell levelu ≥ 1 jeden slot navíc (jen pro kouzla podtřídy). */
fun ClassEntry.hasSubclassSlot(): Boolean = when (subclassType(classId)) {
    SubclassType.TWO_DOMAINS, SubclassType.ONE_DOMAIN -> subclass[SubclassSlot.DOMAIN1] != null || subclass[SubclassSlot.DOMAIN2] != null
    SubclassType.SCHOOL -> subclass[SubclassSlot.SCHOOL]?.let { it != UNIVERSALIST } == true
    else -> false
}

/** Specializace kouzelníka: kouzlo patří do jeho školy (běžná škola podle [Spell.school], elementární podle seznamu). */
fun ClassEntry.isSchoolSpell(spell: Spell): Boolean {
    val school = subclass[SubclassSlot.SCHOOL] ?: return false
    return when {
        school == UNIVERSALIST -> false
        school.startsWith("element:") -> spell.subLevels.containsKey(school)
        else -> spell.school.equals(school, ignoreCase = true)
    }
}

/** Kouzlo z opoziční školy kouzelníka (příprava stojí 2 sloty). */
fun ClassEntry.isOppositionSpell(spell: Spell): Boolean =
    listOfNotNull(subclass[SubclassSlot.OPPOSITION1], subclass[SubclassSlot.OPPOSITION2]).any { opp ->
        if (opp.startsWith("element:")) spell.subLevels.containsKey(opp) else spell.school.equals(opp, ignoreCase = true)
    }
