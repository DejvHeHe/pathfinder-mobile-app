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

/**
 * Subdoména → domény, ke kterým patří (zdroj spellů vazbu neobsahuje). Podle tabulky na
 * [aonprd.com/ClericDomains.aspx](https://aonprd.com/ClericDomains.aspx); klíč = jméno bez závorky
 * („archon (good, law)“ → „archon“). Subdoména bez záznamu (boreal, sleep) se nenabízí.
 */
private val SUBDOMAIN_DOMAINS: Map<String, List<String>> = mapOf(
        "aeon" to listOf("knowledge"), "agathion" to listOf("good"), "alchemy" to listOf("artifice", "magic"),
        "ambush" to listOf("trickery"), "ancestors" to listOf("repose"), "arcane" to listOf("magic"),
        "archon" to listOf("good", "law"), "aristocracy" to listOf("nobility"), "arson" to listOf("fire"),
        "ash" to listOf("fire"), "azata" to listOf("chaos", "good"), "blood" to listOf("war"), "cannibalism" to listOf("evil"),
        "captivation" to listOf("charm"), "catastrophe" to listOf("destruction"), "caves" to listOf("earth"),
        "chivalry" to listOf("glory"), "cloud" to listOf("air"), "competition" to listOf("strength"),
        "construct" to listOf("artifice"), "cooperation" to listOf("community"), "corruption" to listOf("evil"),
        "curse" to listOf("luck"), "daemon" to listOf("evil"), "dark tapestry" to listOf("void"), "day" to listOf("sun"),
        "decay" to listOf("plant"), "deception" to listOf("trickery"), "defense" to listOf("protection"),
        "demodand" to listOf("chaos", "evil"), "demon" to listOf("chaos", "evil"), "devil" to listOf("evil", "law"),
        "divine" to listOf("magic"), "dragon" to listOf("scalykind"), "duels" to listOf("war"),
        "education" to listOf("community", "knowledge"), "entropy" to listOf("chaos"),
        "espionage" to listOf("knowledge", "trickery"), "exploration" to listOf("travel"), "family" to listOf("community"),
        "fate" to listOf("luck"), "fear" to listOf("evil"), "feather" to listOf("animal"), "ferocity" to listOf("strength"),
        "fist" to listOf("strength"), "flotsam" to listOf("water"), "flowing" to listOf("water"),
        "fortifications" to listOf("protection"), "freedom" to listOf("liberation"), "friendship" to listOf("good"),
        "fur" to listOf("animal"), "greed" to listOf("trickery"), "growth" to listOf("plant"), "hatred" to listOf("destruction"),
        "heroism" to listOf("glory"), "home" to listOf("community"), "honor" to listOf("glory"),
        "hubris" to listOf("glory", "nobility"), "ice" to listOf("water"), "imagination" to listOf("luck"),
        "industry" to listOf("artifice"), "inevitable" to listOf("law"), "innuendo" to listOf("trickery"),
        "insanity" to listOf("madness"), "insect" to listOf("animal"), "isolation" to listOf("void"), "judgment" to listOf("law"),
        "kyton" to listOf("evil", "law"), "language" to listOf("rune"), "leadership" to listOf("nobility"),
        "legend" to listOf("glory"), "legislation" to listOf("law", "rune"), "leshy" to listOf("plant"), "light" to listOf("sun"),
        "lightning" to listOf("air"), "loss" to listOf("darkness"), "love" to listOf("charm"), "loyalty" to listOf("law"),
        "lust" to listOf("charm"), "martyr" to listOf("nobility"), "medicine" to listOf("healing"),
        "memory" to listOf("knowledge"), "metal" to listOf("earth"), "monsoon" to listOf("weather"), "moon" to listOf("darkness"),
        "murder" to listOf("death"), "night" to listOf("darkness"), "nightmare" to listOf("madness"), "oceans" to listOf("water"),
        "petrification" to listOf("earth"), "plague" to listOf("death", "evil"), "portal" to listOf("travel"),
        "protean" to listOf("chaos"), "psychopomp" to listOf("death", "repose"), "purity" to listOf("protection"),
        "radiation" to listOf("earth"), "rage" to listOf("destruction"), "redemption" to listOf("good"),
        "resolve" to listOf("strength"), "restoration" to listOf("healing"), "resurrection" to listOf("healing"),
        "revelation" to listOf("sun"), "revelry" to listOf("chaos"), "revolution" to listOf("liberation"),
        "riot" to listOf("chaos"), "rites" to listOf("magic"), "rivers" to listOf("water"), "sahkil" to listOf("evil"),
        "saurian" to listOf("scalykind"), "seasons" to listOf("weather"), "self-realization" to listOf("liberation", "strength"),
        "shadow" to listOf("darkness", "death"), "slavery" to listOf("law"), "smoke" to listOf("fire"),
        "solitude" to listOf("protection"), "souls" to listOf("repose"), "sovereignty" to listOf("law"),
        "stars" to listOf("void"), "storms" to listOf("weather"), "tactics" to listOf("war"), "thievery" to listOf("trickery"),
        "thirst" to listOf("sun"), "thorns" to listOf("plant"), "thought" to listOf("knowledge"), "toil" to listOf("artifice"),
        "torture" to listOf("destruction"), "trade" to listOf("travel"), "trap" to listOf("artifice"),
        "truth" to listOf("madness"), "tyranny" to listOf("law"), "undead" to listOf("death"), "venom" to listOf("scalykind"),
        "wards" to listOf("rune"), "whimsy" to listOf("chaos"), "wind" to listOf("air")
)

/** Domény, ke kterým subdoména patří (prázdné = neznámá). */
fun subdomainParents(subdomain: String): List<String> = SUBDOMAIN_DOMAINS[subdomain.substringBefore(" (").trim()].orEmpty()

/** Domény druida (Nature Bond): jen přírodní domény a jejich subdomény (kromě Metal). */
val DRUID_DOMAINS = listOf("air", "animal", "earth", "fire", "plant", "water", "weather")
private val DRUID_FORBIDDEN_SUBDOMAINS = setOf("metal")

/** Domény, které classa smí vybrat (druid jen [DRUID_DOMAINS]). */
fun SubclassIndex.domainsFor(classId: String): List<String> {
    val all = names(SubclassKind.DOMAIN)
    return if (classId == "druid") all.filter { it in DRUID_DOMAINS } else all
}

/** Subdomény dané domény (pro druida bez Metal). */
fun SubclassIndex.subdomainsOf(domain: String, classId: String): List<String> =
    names(SubclassKind.SUBDOMAIN).filter { sd ->
        domain in subdomainParents(sd) && !(classId == "druid" && sd.substringBefore(" (") in DRUID_FORBIDDEN_SUBDOMAINS)
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

/**
 * Nová mapa podtřídy po změně jednoho klíče ([key] = `SubclassSlot`, null = zrušit):
 * zrušená doména ruší i svou subdoménu, jiná specializace kouzelníka ruší opoziční školy.
 */
fun Map<String, String>.withSubclass(key: String, value: String?): Map<String, String> {
    var sub = if (value == null) this - key else this + (key to value)
    if (value == null && key == SubclassSlot.DOMAIN1) sub = sub - SubclassSlot.SUBDOMAIN1
    if (value == null && key == SubclassSlot.DOMAIN2) sub = sub - SubclassSlot.SUBDOMAIN2
    if (key == SubclassSlot.SCHOOL) sub = sub - SubclassSlot.OPPOSITION1 - SubclassSlot.OPPOSITION2
    // jiná doména → subdoména, která k ní nepatří, odpadne
    for ((d, sd) in listOf(SubclassSlot.DOMAIN1 to SubclassSlot.SUBDOMAIN1, SubclassSlot.DOMAIN2 to SubclassSlot.SUBDOMAIN2)) {
        val domain = sub[d]
        val subdomain = sub[sd]
        if (key == d && domain != null && subdomain != null && domain !in subdomainParents(subdomain)) sub = sub - sd
    }
    return sub
}
