package cz.pfspells.data

/**
 * Předvyplněná schopnost classy s denním počtem použití (Core Rulebook, APG, ACG, UC, Unchained).
 * [max] = maximum pro úroveň classy a postavu; null = bez omezení (např. wild shape na 20. úrovni).
 */
class FeaturePreset(
    val id: String,
    val classId: String,
    val name: String,
    val unit: ResourceUnit,
    val minLevel: Int = 1,
    val max: (level: Int, c: Character) -> Int?,
)

private fun Character.mod(a: Ability) = abilityMod(scores[a])

object FeaturePresets {
    val all: List<FeaturePreset> = listOf(
        // paladin / antipaladin: 1/den na 1. úrovni, +1 na 4. a každé 3. další
        FeaturePreset("paladin-smite", "paladin", "Smite Evil", ResourceUnit.PER_DAY) { l, _ -> 1 + (l - 1) / 3 },
        FeaturePreset("paladin-loh", "paladin", "Lay on Hands", ResourceUnit.PER_DAY, minLevel = 2) { l, c -> (l / 2 + c.mod(Ability.CHA)).coerceAtLeast(0) },
        FeaturePreset("antipaladin-smite", "antipaladin", "Smite Good", ResourceUnit.PER_DAY) { l, _ -> 1 + (l - 1) / 3 },
        FeaturePreset("antipaladin-toc", "antipaladin", "Touch of Corruption", ResourceUnit.PER_DAY, minLevel = 2) { l, c -> (l / 2 + c.mod(Ability.CHA)).coerceAtLeast(0) },
        // cleric
        FeaturePreset("cleric-channel", "cleric", "Channel Energy", ResourceUnit.PER_DAY) { _, c -> (3 + c.mod(Ability.CHA)).coerceAtLeast(0) },
        // rage / bloodrage / bardic performance: 4 + mod kol, +2 za každou další úroveň
        FeaturePreset("barbarian-rage", "barbarian", "Rage", ResourceUnit.ROUNDS) { l, c -> (4 + c.mod(Ability.CON) + 2 * (l - 1)).coerceAtLeast(0) },
        FeaturePreset("ubarbarian-rage", "barbarian_unchained", "Rage", ResourceUnit.ROUNDS) { l, c -> (4 + c.mod(Ability.CON) + 2 * (l - 1)).coerceAtLeast(0) },
        FeaturePreset("bloodrager-rage", "bloodrager", "Bloodrage", ResourceUnit.ROUNDS) { l, c -> (4 + c.mod(Ability.CON) + 2 * (l - 1)).coerceAtLeast(0) },
        FeaturePreset("bard-performance", "bard", "Bardic Performance", ResourceUnit.ROUNDS) { l, c -> (4 + c.mod(Ability.CHA) + 2 * (l - 1)).coerceAtLeast(0) },
        // ki pool
        FeaturePreset("monk-ki", "monk", "Ki Pool", ResourceUnit.POINTS, minLevel = 4) { l, c -> (l / 2 + c.mod(Ability.WIS)).coerceAtLeast(0) },
        FeaturePreset("umonk-ki", "monk_unchained", "Ki Pool", ResourceUnit.POINTS, minLevel = 3) { l, c -> (l / 2 + c.mod(Ability.WIS)).coerceAtLeast(0) },
        FeaturePreset("ninja-ki", "ninja", "Ki Pool", ResourceUnit.POINTS, minLevel = 2) { l, c -> (l / 2 + c.mod(Ability.CHA)).coerceAtLeast(0) },
        // magus, gunslinger, swashbuckler
        FeaturePreset("magus-pool", "magus", "Arcane Pool", ResourceUnit.POINTS) { l, c -> (maxOf(1, l / 2) + c.mod(Ability.INT)).coerceAtLeast(0) },
        FeaturePreset("gunslinger-grit", "gunslinger", "Grit", ResourceUnit.POINTS) { _, c -> maxOf(1, c.mod(Ability.WIS)) },
        FeaturePreset("swashbuckler-panache", "swashbuckler", "Panache", ResourceUnit.POINTS) { _, c -> maxOf(1, c.mod(Ability.CHA)) },
        // inquisitor, cavalier, samurai: 1/den, +1 na 4. a každé 3. další
        FeaturePreset("inquisitor-judgment", "inquisitor", "Judgment", ResourceUnit.PER_DAY) { l, _ -> 1 + (l - 1) / 3 },
        FeaturePreset("cavalier-challenge", "cavalier", "Challenge", ResourceUnit.PER_DAY) { l, _ -> 1 + (l - 1) / 3 },
        FeaturePreset("samurai-challenge", "samurai", "Challenge", ResourceUnit.PER_DAY) { l, _ -> 1 + (l - 1) / 3 },
        // druid: wild shape od 4. úrovně 1/den, +1 každé 2 úrovně, na 20. bez omezení
        FeaturePreset("druid-wildshape", "druid", "Wild Shape", ResourceUnit.PER_DAY, minLevel = 4) { l, _ -> if (l >= 20) null else 1 + (l - 4) / 2 },
        // warpriest
        FeaturePreset("warpriest-fervor", "warpriest", "Fervor", ResourceUnit.PER_DAY, minLevel = 2) { l, c -> (l / 2 + c.mod(Ability.WIS)).coerceAtLeast(0) },
        FeaturePreset("warpriest-blessings", "warpriest", "Blessings", ResourceUnit.PER_DAY) { l, _ -> 3 + l / 2 },
        // alchemist, investigator, brawler
        FeaturePreset("alchemist-bombs", "alchemist", "Bombs", ResourceUnit.PER_DAY) { l, c -> (l + c.mod(Ability.INT)).coerceAtLeast(0) },
        FeaturePreset("investigator-inspiration", "investigator", "Inspiration", ResourceUnit.POINTS) { l, c -> maxOf(1, l / 2 + c.mod(Ability.INT)) },
        FeaturePreset("brawler-flexibility", "brawler", "Martial Flexibility", ResourceUnit.PER_DAY) { l, _ -> 3 + l / 2 },
        // kouzelník: schopnost školy 1. úrovně (většina škol 3 + INT)
        FeaturePreset("wizard-school-power", "wizard", "Arcane school power (1st level)", ResourceUnit.PER_DAY) { _, c -> (3 + c.mod(Ability.INT)).coerceAtLeast(0) },
    )

    private val byId = all.associateBy { it.id }
    fun get(id: String): FeaturePreset? = byId[id]
}

/** Jedna položka sekce „Schopnosti“: předvyplněná schopnost classy nebo vlastní počítadlo. */
data class Resource(
    /** klíč v `castToday` */
    val key: String,
    val name: String,
    val unit: ResourceUnit,
    /** null = bez omezení */
    val max: Int?,
    val classId: String? = null,
    val preset: FeaturePreset? = null,
    val tracker: Tracker? = null,
)

/**
 * Schopnosti 1. úrovně podle podtřídy (Core Rulebook / APG): doménová schopnost klerika a druida 3 + Wis za den,
 * schopnost bloodline čaroděje 3 + Cha za den. Výjimky: Sun (pasivní) a Knowledge se nepočítají, Animal = rounds
 * 3 + úroveň (speak with animals), Liberation = rounds podle úrovně, Arcane bloodline má místo schopnosti arcane bond.
 */
private fun subclassPresets(e: ClassEntry): List<FeaturePreset> {
    fun title(v: String) = v.split(' ', '-').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    val domains = when (e.classId) {
        "cleric" -> listOfNotNull(e.subclass[SubclassSlot.DOMAIN1], e.subclass[SubclassSlot.DOMAIN2])
        "druid" -> listOfNotNull(e.subclass[SubclassSlot.DOMAIN1])
        else -> emptyList()
    }.map { it.lowercase() }.distinct()
    val domainPowers = domains.mapNotNull { d ->
        val id = "domain-power:${e.classId}:$d"
        val name = "${title(d)} domain power (1st level)"
        when (d) {
            "sun", "knowledge" -> null
            "animal" -> FeaturePreset(id, e.classId, "Animal domain: speak with animals", ResourceUnit.ROUNDS) { l, _ -> 3 + l }
            "liberation" -> FeaturePreset(id, e.classId, "Liberation domain: liberation", ResourceUnit.ROUNDS) { l, _ -> l }
            else -> FeaturePreset(id, e.classId, name, ResourceUnit.PER_DAY) { _, c -> (3 + c.mod(Ability.WIS)).coerceAtLeast(0) }
        }
    }
    val bloodline = e.subclass[SubclassSlot.BLOODLINE]?.lowercase()
        ?.takeIf { e.classId == "sorcerer" && it != "arcane" }
        ?.let { b ->
            FeaturePreset("bloodline-power:$b", e.classId, "${title(b)} bloodline power (1st level)", ResourceUnit.PER_DAY) { _, c ->
                (3 + c.mod(Ability.CHA)).coerceAtLeast(0)
            }
        }
    return domainPowers + listOfNotNull(bloodline)
}

/** Předvyplněné schopnosti class postavy (dostupné na její úrovni, bez skrytých) + vlastní počítadla. */
fun Character.resources(includeHidden: Boolean = false): List<Resource> {
    val presets = classes.flatMap { e ->
        (FeaturePresets.all.filter { it.classId == e.classId && e.level >= it.minLevel } + subclassPresets(e)).map { p ->
            Resource("res:${p.id}", p.name, p.unit, p.max(e.level, this), e.classId, preset = p)
        }
    }.filter { includeHidden || it.preset!!.id !in hiddenPresets }
    val custom = trackers.map { t -> Resource("res:${t.id}", t.name, t.unit, t.max, tracker = t) }
    return presets + custom
}
