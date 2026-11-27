package cz.pfspells.data

/*
 * Familiár (Core Rulebook – Arcane Bond / Familiar; seznam familiárů z d20pfsrd, tools/build_companions.py → FamiliarTable.kt):
 * - druh familiára dává pánovi bonus (Cat: +3 Stealth, Toad: +3 HP…) – nepodmíněný bonus se ukládá pánovi
 *   (`Character.familiarBonus`, drží ho AppState) a počítá se jako bonusy vybavení; podmíněný („in bright light“) jen text,
 * - podle úrovně pána (`Character.companionLevel` u familiára) přirozená zbroj +1 za každé 2 úrovně, Int 6–15, schopnosti.
 * Statistiky tvora (atributy, velikost, rychlost, útoky, přirozená zbroj) z bestiáře d20pfsrd vyplní výběr druhu.
 */

/** Druh familiára: [bonus] = nepodmíněný bonus pánovi (null = jen text [bonusText]). */
class FamiliarInfo(
    val id: String,
    val name: String,
    val bonusText: String,
    val bonus: ItemBonus?,
    /** „use hawk statistics“ apod. */
    val note: String = "",
    val source: String = "",
    // statistiky tvora z bestiáře d20pfsrd (null / prázdné = stránka bez statbloku)
    val size: CreatureSize? = null,
    val speed: String = "",
    /** přirozená zbroj tvora (k ní se přičítá tabulka familiára) */
    val naturalArmor: Int = 0,
    val attacks: String = "",
    val abilities: Abilities? = null,
    val senses: String = "",
)

enum class FamiliarSpecial(val level: Int) {
    ALERTNESS(1), IMPROVED_EVASION(1), SHARE_SPELLS(1), EMPATHIC_LINK(1), DELIVER_TOUCH_SPELLS(3), SPEAK_WITH_MASTER(5),
    SPEAK_WITH_ANIMALS(7), SPELL_RESISTANCE(11), SCRY_ON_FAMILIAR(13),
}

object FamiliarCatalog {
    val all: List<FamiliarInfo> = FAMILIAR_TABLE
    private val byId = all.associateBy { it.id }

    fun get(id: String?): FamiliarInfo? = id?.let { byId[it] }
}

/** Tabulka familiára podle úrovně pána (Core Rulebook): přirozená zbroj, Int, schopnosti. */
object FamiliarProgression {
    /** Přirozená zbroj navíc: +1 na 1.–2. úrovni pána, +2 na 3.–4. … +10 na 19.–20. */
    fun naturalArmor(masterLevel: Int): Int = (masterLevel.coerceIn(1, 20) + 1) / 2

    /** Inteligence familiára: 6 na 1.–2. úrovni pána … 15 na 19.–20. */
    fun intelligence(masterLevel: Int): Int = 5 + (masterLevel.coerceIn(1, 20) + 1) / 2

    fun specials(masterLevel: Int): List<FamiliarSpecial> = FamiliarSpecial.entries.filter { it.level <= masterLevel }

    /** Spell resistance familiára = úroveň pána + 5 (od 11. úrovně). */
    fun spellResistance(masterLevel: Int): Int? = if (masterLevel >= 11) masterLevel + 5 else null
}

val Character.isFamiliar: Boolean get() = companion == CompanionKind.FAMILIAR

val Character.familiarInfo: FamiliarInfo? get() = if (isFamiliar) FamiliarCatalog.get(familiar) else null

/** Úroveň pána pro tabulku familiára (null = bez progrese). */
val Character.familiarLevel: Int? get() = if (isFamiliar) companionLevel else null

/** Bonusy pánovi od jeho familiárů (jen nepodmíněné). */
fun familiarBonuses(companions: List<Character>): List<ItemBonus> =
    companions.mapNotNull { it.familiarInfo?.bonus }

/**
 * Úroveň pána pro familiára podle class vlastníka: classy, které familiára dávají (wizard, witch, sorcerer,
 * arcanist, shaman – spirit animal), se sčítají. Vždy aspoň 1.
 */
fun Character.suggestedFamiliarLevel(): Int =
    classes.filter { it.classId in setOf("wizard", "witch", "sorcerer", "arcanist", "shaman") }.sumOf { it.level }.coerceIn(1, 20)
