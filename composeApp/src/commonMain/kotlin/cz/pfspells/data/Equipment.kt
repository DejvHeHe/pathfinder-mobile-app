package cz.pfspells.data

import kotlinx.serialization.Serializable

/*
 * Nasazené vybavení: položka inventáře může být „nasazená“ ve slotu a nést bonusy (atribut, záchrany, AC, skill).
 * Bonusy nasazených položek propojeného inventáře se kopírují do `Character.gear` (AppState) a odtud jdou do
 * výsledných atributů (`scores`), součtů skillů a souhrnu záchran / AC. Sčítání podle PF1: bonusy stejného typu
 * se nesčítají (platí nejvyšší), dodge, untyped a circumstance se sčítají, postihy se sčítají vždy.
 */

/** Slot magického předmětu (Core Rulebook, Magic Items); prsteny jsou dva. */
enum class ItemSlot(val capacity: Int = 1) {
    HEAD, HEADBAND, EYES, SHOULDERS, NECK, CHEST, BODY, ARMOR, BELT, WRISTS, HANDS, RING(2), FEET, SHIELD;

    companion object {
        /** Slot z databáze itemů („headband“, „wrist“, „eye“…); null = bez slotu. */
        fun parse(s: String?): ItemSlot? = when (s?.trim()?.lowercase()) {
            "head" -> HEAD
            "headband" -> HEADBAND
            "eyes", "eye" -> EYES
            "shoulders" -> SHOULDERS
            "neck", "amulet" -> NECK
            "chest" -> CHEST
            "body" -> BODY
            "armor" -> ARMOR
            "belt" -> BELT
            "wrists", "wrist" -> WRISTS
            "hands" -> HANDS
            "ring" -> RING
            "feet" -> FEET
            "shield" -> SHIELD
            else -> null
        }
    }
}

/** Na co bonus platí; [SKILL] s klíčem skillu v [ItemBonus.skill]. */
enum class BonusTarget { STR, DEX, CON, INT, WIS, CHA, FORT, REF, WILL, AC, SKILL }

enum class BonusType {
    ENHANCEMENT, RESISTANCE, DEFLECTION, NATURAL_ARMOR, ARMOR, SHIELD, COMPETENCE, INSIGHT, LUCK, MORALE,
    SACRED, PROFANE, ALCHEMICAL, CIRCUMSTANCE, DODGE, UNTYPED;

    /** Bonusy tohoto typu se sčítají (jinak platí nejvyšší). */
    val stacks: Boolean get() = this == DODGE || this == UNTYPED || this == CIRCUMSTANCE
}

@Serializable
data class ItemBonus(
    val target: BonusTarget,
    val value: Int,
    val type: BonusType = BonusType.ENHANCEMENT,
    /** klíč skillu ([Skills.key], např. „stealth“) – jen pro [BonusTarget.SKILL] */
    val skill: String? = null,
)

/** Výsledek sčítání bonusů podle PF1 (stejný typ → nejvyšší; dodge / untyped / circumstance a postihy se sčítají). */
fun stackBonuses(list: List<ItemBonus>): Int =
    list.groupBy { it.type }.entries.sumOf { (type, bs) ->
        val penalties = bs.filter { it.value < 0 }.sumOf { it.value }
        val positive = bs.filter { it.value > 0 }
        penalties + if (type.stacks) positive.sumOf { it.value } else (positive.maxOfOrNull { it.value } ?: 0)
    }

fun Ability.bonusTarget(): BonusTarget = when (this) {
    Ability.STR -> BonusTarget.STR
    Ability.DEX -> BonusTarget.DEX
    Ability.CON -> BonusTarget.CON
    Ability.INT -> BonusTarget.INT
    Ability.WIS -> BonusTarget.WIS
    Ability.CHA -> BonusTarget.CHA
}

/** Bonus k atributu z nasazeného vybavení. */
fun Character.gearMod(a: Ability): Int = stackBonuses(gear.filter { it.target == a.bonusTarget() })

/** Bonus ke skillu z vybavení (klíč [Skills.key]; „knowledge.arcana“ bere i bonus ke všem Knowledge – „knowledge“). */
fun Character.gearSkill(key: String): Int =
    stackBonuses(gear.filter { it.target == BonusTarget.SKILL && (it.skill == key || it.skill == key.substringBefore('.').substringBefore(':')) })

/** Bonus k záchraně z vybavení ([BonusTarget.FORT] / REF / WILL). */
fun Character.gearSave(target: BonusTarget): Int = stackBonuses(gear.filter { it.target == target })

/** Bonusy k AC z vybavení po typech (souhrn – AC appka nepočítá): typ → výsledná hodnota. */
fun Character.gearAc(): Map<BonusType, Int> =
    gear.filter { it.target == BonusTarget.AC }.groupBy { it.type }.mapValues { (_, bs) -> stackBonuses(bs) }.filterValues { it != 0 }

/** Bonusy nasazených položek inventáře (každá položka jednou, i když je jich víc kusů). */
fun Inventory.gearBonuses(): List<ItemBonus> = entries.filter { it.equipped }.flatMap { it.bonuses }

/** Sloty s víc nasazenými položkami, než se vejde (dvě helmy, tři prsteny…). */
fun Inventory.slotConflicts(): List<ItemSlot> =
    entries.filter { it.equipped && it.slot != null }.groupBy { it.slot!! }
        .filter { (slot, list) -> list.sumOf { it.qty.coerceAtLeast(1) } > slot.capacity }.keys.toList()

/**
 * Bonusy známých magických předmětů podle jména (Core Rulebook): čelenky a opasky atributů, plášť odolnosti,
 * prsten ochrany, amulet přirozené zbroje, náramky zbroje. Ostatní předměty → prázdný seznam (zadá se ručně).
 */
object ItemBonusGuess {
    private val PLUS = Regex("""\+(\d+)""")

    fun of(name: String): List<ItemBonus> {
        val n = name.lowercase()
        val v = PLUS.find(n)?.groupValues?.get(1)?.toIntOrNull() ?: return emptyList()
        fun ab(vararg t: BonusTarget) = t.map { ItemBonus(it, v) }
        return when {
            "headband of vast intelligence" in n -> ab(BonusTarget.INT)
            "headband of inspired wisdom" in n -> ab(BonusTarget.WIS)
            "headband of alluring charisma" in n -> ab(BonusTarget.CHA)
            "headband of mental superiority" in n -> ab(BonusTarget.INT, BonusTarget.WIS, BonusTarget.CHA)
            "belt of giant strength" in n -> ab(BonusTarget.STR)
            "belt of incredible dexterity" in n -> ab(BonusTarget.DEX)
            "belt of mighty constitution" in n -> ab(BonusTarget.CON)
            "belt of physical perfection" in n -> ab(BonusTarget.STR, BonusTarget.DEX, BonusTarget.CON)
            "cloak of resistance" in n -> listOf(BonusTarget.FORT, BonusTarget.REF, BonusTarget.WILL).map { ItemBonus(it, v, BonusType.RESISTANCE) }
            "ring of protection" in n -> listOf(ItemBonus(BonusTarget.AC, v, BonusType.DEFLECTION))
            "amulet of natural armor" in n -> listOf(ItemBonus(BonusTarget.AC, v, BonusType.NATURAL_ARMOR))
            "bracers of armor" in n -> listOf(ItemBonus(BonusTarget.AC, v, BonusType.ARMOR))
            // Headband of Mental Prowess / Belt of Physical Might: dva atributy podle volby – ručně
            else -> emptyList()
        }
    }
}
