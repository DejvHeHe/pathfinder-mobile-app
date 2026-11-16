package cz.pfspells.data

enum class Progression {
    /** wizard, cleric, druid, witch, shaman: 9 levelů, připravuje */
    PREPARED_9,
    /** sorcerer, oracle, psychic, arcanist: 9 levelů, o úroveň později */
    SPONTANEOUS_9,
    /** bard, magus, inquisitor, …: 6 levelů */
    SIX,
    /** paladin, ranger, bloodrager, antipaladin: 4 levely od 4. úrovně */
    FOUR;

    /** Nejvyšší spell level dostupný na úrovni [classLevel], -1 = žádné spelly. */
    fun maxSpellLevel(classLevel: Int): Int {
        val l = classLevel.coerceIn(1, 20)
        return when (this) {
            PREPARED_9 -> minOf(9, (l + 1) / 2)
            SPONTANEOUS_9 -> if (l < 4) 1 else minOf(9, l / 2)
            SIX -> minOf(6, (l + 2) / 3)
            FOUR -> if (l < 4) -1 else minOf(4, (l - 1) / 3)
        }
    }
}

data class CasterClass(
    val id: String,
    val name: String,
    val progression: Progression,
    /** Spell listy ze zdroje, ze kterých classa bere. Při více listech platí nižší level. */
    val listKeys: List<String>,
    val hasCantrips: Boolean = progression != Progression.FOUR,
) {
    fun maxSpellLevel(classLevel: Int): Int = progression.maxSpellLevel(classLevel)

    /** Spell level pro tuto classu, nebo null, když spell není na jejím listu. */
    fun spellLevelOf(spell: Spell): Int? =
        listKeys.mapNotNull { spell.levels[it] }.minOrNull()

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
        CasterClass("wizard", "Wizard", Progression.PREPARED_9, listOf(SW)),
        CasterClass("sorcerer", "Sorcerer", Progression.SPONTANEOUS_9, listOf(SW)),
        CasterClass("arcanist", "Arcanist", Progression.SPONTANEOUS_9, listOf(SW)),
        CasterClass("cleric", "Cleric", Progression.PREPARED_9, listOf(CO)),
        CasterClass("oracle", "Oracle", Progression.SPONTANEOUS_9, listOf(CO)),
        CasterClass("druid", "Druid", Progression.PREPARED_9, listOf("druid")),
        CasterClass("witch", "Witch", Progression.PREPARED_9, listOf("witch")),
        CasterClass("shaman", "Shaman", Progression.PREPARED_9, listOf("shaman")),
        CasterClass("psychic", "Psychic", Progression.SPONTANEOUS_9, listOf("psychic")),
        CasterClass("bard", "Bard", Progression.SIX, listOf("bard")),
        CasterClass("skald", "Skald", Progression.SIX, listOf("skald", "bard")),
        CasterClass("inquisitor", "Inquisitor", Progression.SIX, listOf("inquisitor")),
        CasterClass("magus", "Magus", Progression.SIX, listOf("magus")),
        CasterClass("alchemist", "Alchemist", Progression.SIX, listOf("alchemist"), hasCantrips = false),
        CasterClass("investigator", "Investigator", Progression.SIX, listOf("investigator", "alchemist"), hasCantrips = false),
        CasterClass("summoner", "Summoner", Progression.SIX, listOf("summoner")),
        CasterClass("summoner_unchained", "Summoner (Unchained)", Progression.SIX, listOf("unchained summoner")),
        CasterClass("warpriest", "Warpriest", Progression.SIX, listOf("warpriest", CO)),
        CasterClass("hunter", "Hunter", Progression.SIX, listOf("hunter", "druid", "ranger")),
        CasterClass("mesmerist", "Mesmerist", Progression.SIX, listOf("mesmerist")),
        CasterClass("occultist", "Occultist", Progression.SIX, listOf("occultist")),
        CasterClass("spiritualist", "Spiritualist", Progression.SIX, listOf("spiritualist")),
        CasterClass("paladin", "Paladin", Progression.FOUR, listOf("paladin")),
        CasterClass("antipaladin", "Antipaladin", Progression.FOUR, listOf("antipaladin")),
        CasterClass("ranger", "Ranger", Progression.FOUR, listOf("ranger")),
        CasterClass("bloodrager", "Bloodrager", Progression.FOUR, listOf("bloodrager")),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): CasterClass? = byId[id]
}
