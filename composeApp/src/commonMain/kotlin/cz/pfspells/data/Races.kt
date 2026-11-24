package cz.pfspells.data

/** Skupiny ras podle Advanced Race Guide. */
enum class RaceGroup { CORE, FEATURED, UNCOMMON, OTHER }

/**
 * Hratelná rasa (data z Foundry VTT PF1, `tools/build_races.py` → [RACE_TABLE]).
 * [mods] = rasové úpravy atributů, [anyBonus] = +2 k jednomu atributu podle volby (human, half-elf, half-orc).
 * [subtypes] = podtypy tvora („elf“, „human“…) – half-elf se počítá jako elf i člověk.
 */
data class RaceInfo(
    val id: String,
    val name: String,
    val group: RaceGroup,
    val size: CreatureSize,
    val speed: Int,
    val mods: Map<Ability, Int>,
    val anyBonus: Boolean,
    val type: String,
    val subtypes: List<String>,
    /** rasové bonusy ke skillům (id z [Skills], „knowledge“ / „craft“ = všechny) */
    val skillMods: Map<String, Int> = emptyMap(),
    /** rasové class skills (kobold: Stealth) */
    val classSkills: Set<String> = emptySet(),
    /** vidění ve tmě (ft, 0 = nemá) a šero */
    val darkvision: Int = 0,
    val lowLightVision: Boolean = false,
) {
    /** Rasová úprava atributu; [bonus] = volba „+2 k jednomu atributu“. */
    fun mod(a: Ability, bonus: Ability?): Int = (mods[a] ?: 0) + if (anyBonus && bonus == a) 2 else 0

    /** Rasový bonusový feat na 1. úrovni (human; half-elf – Adaptability = Skill Focus). */
    val bonusFeats: Int get() = if (id == "human" || id == "half-elf") 1 else 0

    /** Platí rasa jako [race] (jméno / id rasy nebo podtyp)? */
    fun countsAs(race: String): Boolean {
        val r = race.lowercase().trim()
        return name.lowercase() == r || id == r || r in subtypes
    }
}

object RaceCatalog {
    val all: List<RaceInfo> get() = RACE_TABLE

    private val byId by lazy { RACE_TABLE.associateBy { it.id } }

    fun get(id: String?): RaceInfo? = id?.let { byId[it] }

    /** Slova, která v prerekvizitách featů znamenají rasu („dwarf“, „half-orc“, „elf“ jako podtyp…). */
    private val raceWords: Set<String> by lazy {
        RACE_TABLE.flatMap { listOf(it.name.lowercase(), it.id) }.toSet() + setOf("elf", "human", "orc", "dwarf", "gnome", "halfling")
    }

    fun isRaceWord(word: String): Boolean = word.lowercase().trim() in raceWords
}

/** Rasa postavy (null = nevybraná). */
val Character.raceInfo: RaceInfo? get() = RaceCatalog.get(race)

/** Velikost postavy: zadaná u mazlíčka ([Character.size]), jinak podle rasy; null = neznámá. */
val Character.creatureSize: CreatureSize? get() = size ?: raceInfo?.size

/** Rasová úprava atributu postavy (0 bez rasy). */
fun Character.raceMod(a: Ability): Int = raceInfo?.mod(a, raceBonus) ?: 0

/** Úprava atributu oproti základu: rasa + zvířecí společník + nasazené vybavení. */
fun Character.abilityBonus(a: Ability): Int = raceMod(a) + companionMod(a) + gearMod(a)

/** Výsledné atributy = základ + rasa + zvířecí společník + vybavení (prerekvizity, sloty, schopnosti, nosnost). */
val Character.scores: Abilities
    get() = Ability.entries.fold(abilities) { acc, a -> acc.with(a, (abilities[a] + abilityBonus(a)).coerceAtLeast(1)) }
