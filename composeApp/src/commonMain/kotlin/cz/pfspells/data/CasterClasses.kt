package cz.pfspells.data

enum class Progression {
    /** wizard, cleric, druid, witch, shaman: 9 levelů, připravuje */
    PREPARED_9,
    /** sorcerer, oracle, psychic, arcanist: 9 levelů, o úroveň později */
    SPONTANEOUS_9,
    /** bard, magus, inquisitor, …: 6 levelů */
    SIX,
    /** paladin, ranger, bloodrager, antipaladin: 4 levely od 4. úrovně */
    FOUR,
    /** medium (Occult Adventures): knacky (0) od 1. úrovně, jinak jako [FOUR] */
    FOUR_KNACKS;

    /** Nejvyšší spell level dostupný na úrovni [classLevel], -1 = žádné spelly. */
    fun maxSpellLevel(classLevel: Int): Int {
        val l = classLevel.coerceIn(1, 20)
        return when (this) {
            PREPARED_9 -> minOf(9, (l + 1) / 2)
            SPONTANEOUS_9 -> if (l < 4) 1 else minOf(9, l / 2)
            SIX -> minOf(6, (l + 2) / 3)
            FOUR -> if (l < 4) -1 else minOf(4, (l - 1) / 3)
            FOUR_KNACKS -> if (l < 4) 0 else minOf(4, (l - 1) / 3)
        }
    }
}

/** Jak classa sesílá. */
enum class CastingKind {
    /** připravuje konkrétní kopie (wizard, cleric, magus, paladin…) */
    PREPARED,
    /** zná omezený počet kouzel, sesílá je ze slotů (sorcerer, bard, oracle…) */
    SPONTANEOUS,
    /** arcanist: připraví omezený počet kouzel a sesílá je spontánně ze slotů */
    HYBRID,
}

/** Druh magie classy (štítek ve výběru classy; alchemist a investigator mají extrakty, ne kouzla). */
enum class MagicTradition { ARCANE, DIVINE, PSYCHIC, ALCHEMICAL }

data class CasterClass(
    val id: String,
    val name: String,
    val progression: Progression,
    /** Spell listy ze zdroje, ze kterých classa bere. Při více listech platí nižší level. */
    val listKeys: List<String>,
    val kind: CastingKind,
    /** atribut sesílání (bonusové sloty, nejvyšší sesílatelný level) */
    val ability: Ability,
    val hasCantrips: Boolean = progression != Progression.FOUR,
    /** classa, jejíž tabulka kouzel za den / známých platí (archetyp: Eldritch Scion = bard, Living Grimoire = warpriest) */
    val tableId: String = id,
    /** kouzla navíc na listu (archetyp): spellId → spell level */
    val extraSpells: Map<String, Int> = emptyMap(),
    /** druh magie změněný archetypem (Filidh → divine, Mindblade → psychic) */
    val traditionOverride: MagicTradition? = null,
) {
    fun maxSpellLevel(classLevel: Int): Int = progression.maxSpellLevel(classLevel)

    val tradition: MagicTradition get() = traditionOverride ?: when (id) {
        "wizard", "sorcerer", "arcanist", "witch", "bard", "skald", "magus",
        "summoner", "summoner_unchained", "bloodrager" -> MagicTradition.ARCANE
        "cleric", "oracle", "druid", "shaman", "inquisitor", "warpriest", "hunter",
        "paladin", "antipaladin", "ranger" -> MagicTradition.DIVINE
        "psychic", "mesmerist", "occultist", "spiritualist", "medium" -> MagicTradition.PSYCHIC
        "alchemist", "investigator" -> MagicTradition.ALCHEMICAL
        else -> error("neznámý druh magie classy $id")
    }

    private val tables by lazy {
        CASTING_TABLE[tableId]?.let { (perDay, known) -> parseSlotTable(perDay) to known?.let(::parseSlotTable) }
    }

    /** Základní počet kouzel za den (bez bonusu z atributu); null = classa tento level nemá. Spontánní triky jsou bez limitu → null. */
    fun slotsPerDay(classLevel: Int, spellLevel: Int): Int? =
        tables?.first?.getOrNull(classLevel.coerceIn(1, 20) - 1)?.getOrNull(spellLevel)

    /** Kolik kouzel smí classa vybrat: spontánní = známá, arcanist = připravená, ostatní null (řídí se sloty). */
    fun spellsKnown(classLevel: Int, spellLevel: Int): Int? =
        tables?.second?.getOrNull(classLevel.coerceIn(1, 20) - 1)?.getOrNull(spellLevel)

    /** Spontánní a arcanist: každé kouzlo jen jednou (kopie nedávají smysl). */
    val distinctSpells: Boolean get() = kind != CastingKind.PREPARED

    /** Spell level pro tuto classu, nebo null, když spell není na jejím listu. */
    fun spellLevelOf(spell: Spell): Int? =
        (listKeys.mapNotNull { spell.levels[it] } + listOfNotNull(extraSpells[spell.id])).minOrNull()

    fun canCast(spell: Spell, classLevel: Int): Boolean {
        val lvl = spellLevelOf(spell) ?: return false
        if (lvl == 0 && !hasCantrips) return false
        return lvl <= maxSpellLevel(classLevel)
    }
}

object CasterClasses {
    private const val SW = "sorcerer/wizard"
    private const val CO = "cleric/oracle"

    val all: List<CasterClass> = listOf(
        CasterClass("wizard", "Wizard", Progression.PREPARED_9, listOf(SW), CastingKind.PREPARED, Ability.INT),
        CasterClass("sorcerer", "Sorcerer", Progression.SPONTANEOUS_9, listOf(SW), CastingKind.SPONTANEOUS, Ability.CHA),
        CasterClass("arcanist", "Arcanist", Progression.SPONTANEOUS_9, listOf(SW), CastingKind.HYBRID, Ability.INT),
        CasterClass("cleric", "Cleric", Progression.PREPARED_9, listOf(CO), CastingKind.PREPARED, Ability.WIS),
        CasterClass("oracle", "Oracle", Progression.SPONTANEOUS_9, listOf(CO), CastingKind.SPONTANEOUS, Ability.CHA),
        CasterClass("druid", "Druid", Progression.PREPARED_9, listOf("druid"), CastingKind.PREPARED, Ability.WIS),
        CasterClass("witch", "Witch", Progression.PREPARED_9, listOf("witch"), CastingKind.PREPARED, Ability.INT),
        CasterClass("shaman", "Shaman", Progression.PREPARED_9, listOf("shaman"), CastingKind.PREPARED, Ability.WIS),
        CasterClass("psychic", "Psychic", Progression.SPONTANEOUS_9, listOf("psychic"), CastingKind.SPONTANEOUS, Ability.INT),
        CasterClass("bard", "Bard", Progression.SIX, listOf("bard"), CastingKind.SPONTANEOUS, Ability.CHA),
        CasterClass("skald", "Skald", Progression.SIX, listOf("skald", "bard"), CastingKind.SPONTANEOUS, Ability.CHA),
        CasterClass("inquisitor", "Inquisitor", Progression.SIX, listOf("inquisitor"), CastingKind.SPONTANEOUS, Ability.WIS),
        CasterClass("magus", "Magus", Progression.SIX, listOf("magus"), CastingKind.PREPARED, Ability.INT),
        CasterClass("alchemist", "Alchemist", Progression.SIX, listOf("alchemist"), CastingKind.PREPARED, Ability.INT, hasCantrips = false),
        CasterClass("investigator", "Investigator", Progression.SIX, listOf("investigator", "alchemist"), CastingKind.PREPARED, Ability.INT, hasCantrips = false),
        CasterClass("summoner", "Summoner", Progression.SIX, listOf("summoner"), CastingKind.SPONTANEOUS, Ability.CHA),
        CasterClass("summoner_unchained", "Summoner (Unchained)", Progression.SIX, listOf("unchained summoner"), CastingKind.SPONTANEOUS, Ability.CHA),
        CasterClass("warpriest", "Warpriest", Progression.SIX, listOf("warpriest", CO), CastingKind.PREPARED, Ability.WIS),
        CasterClass("hunter", "Hunter", Progression.SIX, listOf("hunter", "druid", "ranger"), CastingKind.SPONTANEOUS, Ability.WIS),
        CasterClass("mesmerist", "Mesmerist", Progression.SIX, listOf("mesmerist"), CastingKind.SPONTANEOUS, Ability.CHA),
        CasterClass("occultist", "Occultist", Progression.SIX, listOf("occultist"), CastingKind.SPONTANEOUS, Ability.INT),
        CasterClass("spiritualist", "Spiritualist", Progression.SIX, listOf("spiritualist"), CastingKind.SPONTANEOUS, Ability.WIS),
        CasterClass("paladin", "Paladin", Progression.FOUR, listOf("paladin"), CastingKind.PREPARED, Ability.CHA),
        CasterClass("antipaladin", "Antipaladin", Progression.FOUR, listOf("antipaladin"), CastingKind.PREPARED, Ability.CHA),
        CasterClass("ranger", "Ranger", Progression.FOUR, listOf("ranger"), CastingKind.PREPARED, Ability.WIS),
        CasterClass("bloodrager", "Bloodrager", Progression.FOUR, listOf("bloodrager"), CastingKind.SPONTANEOUS, Ability.CHA),
        CasterClass("medium", "Medium", Progression.FOUR_KNACKS, listOf("medium"), CastingKind.SPONTANEOUS, Ability.CHA),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): CasterClass? = byId[id]
}

/**
 * „3 1 - -|4 2 - -|…“ → řádek pro každou úroveň classy (1–20), hodnota pro každý spell level od 0; „-“ = null.
 * Data: [CASTING_TABLE] (generuje `tools/build_casting.py` z tabulek class na Archives of Nethys).
 */
internal fun parseSlotTable(s: String): List<List<Int?>> =
    s.split('|').map { row -> row.split(' ').map { it.toIntOrNull() } }

/** Bonusová kouzla za den z atributu (Core Rulebook, tabulka 1–3): od modifikátoru ≥ level, +1 za každé další 4. Triky nemají. */
fun bonusSpells(abilityScore: Int, spellLevel: Int): Int {
    val mod = abilityMod(abilityScore)
    return if (spellLevel <= 0 || mod < spellLevel) 0 else (mod - spellLevel) / 4 + 1
}

/** Limity jednoho spell levelu pro classu na dané úrovni s daným atributem sesílání. */
data class LevelSlots(
    val spellLevel: Int,
    /** základ za den z tabulky; null = bez limitu (spontánní triky) */
    val perDay: Int?,
    /** bonus z atributu */
    val bonus: Int,
    /** kolik kouzel smí classa na tomto levelu vybrat (připravená kopie / známá); null = bez limitu */
    val limit: Int?,
    /** atribut sesílání je aspoň 10 + level */
    val abilityOk: Boolean,
)

fun CasterClass.levelSlots(classLevel: Int, abilityScore: Int, spellLevel: Int): LevelSlots {
    val base = slotsPerDay(classLevel, spellLevel)
    val bonus = if (base != null) bonusSpells(abilityScore, spellLevel) else 0
    val limit = when (kind) {
        CastingKind.PREPARED -> base?.plus(bonus)
        CastingKind.SPONTANEOUS, CastingKind.HYBRID -> spellsKnown(classLevel, spellLevel)
    }
    return LevelSlots(spellLevel, base, bonus, limit, abilityScore >= 10 + spellLevel)
}
