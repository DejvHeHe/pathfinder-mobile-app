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
 * Schopnosti podtřídy s počtem použití dostupné na úrovni classy – z Foundry ([SubclassPowers]: domény, subdomény,
 * bloodline, školy kouzelníka; název „Fire Bolt (Fire)“). Podtřída, kterou Foundry nezná (bloodline z pozdějších knih…),
 * dostane obecnou schopnost 1. úrovně: doména 3 + Wis, bloodline 3 + Cha za den.
 */
private fun subclassPresets(e: ClassEntry): List<FeaturePreset> = phrenicPool(e) + SubclassPowers.keysOf(e).flatMap { (key, label) ->
    val powers = SubclassPowers.of(key)
    when {
        powers != null -> powers.filter { it.uses != null && e.level >= it.level }.map { p ->
            FeaturePreset("power:${e.classId}:$key:${p.name.lowercase()}", e.classId, "${p.name} ($label)", p.unit ?: ResourceUnit.PER_DAY, p.level) { l, c ->
                FoundryFormula.uses(p.uses!!, l, c)
            }
        }
        key.startsWith("domain:") -> listOf(
            FeaturePreset("power:${e.classId}:$key", e.classId, "$label domain power (1st level)", ResourceUnit.PER_DAY) { _, c ->
                (3 + c.mod(Ability.WIS)).coerceAtLeast(0)
            },
        )
        key.startsWith("bloodline:") -> listOf(
            FeaturePreset("power:${e.classId}:$key", e.classId, "$label bloodline power (1st level)", ResourceUnit.PER_DAY) { _, c ->
                (3 + c.mod(Ability.CHA)).coerceAtLeast(0)
            },
        )
        else -> emptyList()
    }
}

/** Psychic: phrenic pool = ½ úrovně (min. 1) + modifikátor atributu disciplíny (Wis / Cha, AoN). */
private fun phrenicPool(e: ClassEntry): List<FeaturePreset> {
    if (e.classId != "psychic") return emptyList()
    val ab = disciplinePoolAbility(e.subclass[SubclassSlot.DISCIPLINE]) ?: return emptyList()
    return listOf(FeaturePreset("psychic-phrenic-pool", "psychic", "Phrenic Pool", ResourceUnit.POINTS) { l, c ->
        (maxOf(1, l / 2) + c.mod(ab)).coerceAtLeast(0)
    })
}

/** Kouzelník se školou, kterou zná Foundry, má schopnosti školy z tabulky místo obecné „Arcane school power“. */
private fun ClassEntry.hasSchoolPowers(): Boolean =
    classId == "wizard" && SubclassPowers.keysOf(this).any { SubclassPowers.of(it.first) != null }

/** Vybrané hexy / revelations s počtem použití. */
private fun abilityPresets(e: ClassEntry): List<FeaturePreset> = e.abilities.filter { it.uses != null }.distinctBy { it.id }.map { a ->
    FeaturePreset("ability:${e.classId}:${a.id}", e.classId, a.name, a.unit ?: ResourceUnit.PER_DAY) { l, c -> FoundryFormula.uses(a.uses!!, l, c) }
}

/** Předvyplněné schopnosti class postavy (dostupné na její úrovni, bez skrytých) + vlastní počítadla. */
fun Character.resources(includeHidden: Boolean = false): List<Resource> {
    val presets = classes.flatMap { e ->
        val fx = e.archetypeEffect
        val base = FeaturePresets.all.filter { it.classId == e.classId && e.level >= it.minLevel }
            .filterNot { it.id == "wizard-school-power" && (e.hasSchoolPowers() || e.subclassKind != SubclassType.SCHOOL) }
            .filterNot { it.name.lowercase() in fx.removedPresets }
        // schopnosti podtřídy jen když ji classa (po archetypech) má a archetyp je nenahradil;
        // úroveň podtřídy může být nižší než úroveň classy (Sacred Servant: paladin −3)
        val subE = e.copy(level = e.subclassLevel)
        val sub = if (e.subclassKind == null || fx.noSubclassPowers || subE.level < 1) emptyList()
        else subclassPresets(subE).filterNot { it.name.lowercase() in fx.removedPresets }
        val extra = fx.presets.filter { e.level >= it.minLevel }
        (base + abilityPresets(e) + extra).map { p ->
            Resource("res:${p.id}", p.name, p.unit, p.max(e.level, this), e.classId, preset = p)
        } + sub.map { p -> Resource("res:${p.id}", p.name, p.unit, p.max(subE.level, this), e.classId, preset = p) }
    }.filter { includeHidden || it.preset!!.id !in hiddenPresets }
    val custom = trackers.map { t -> Resource("res:${t.id}", t.name, t.unit, t.max, tracker = t) }
    return presets + custom
}
