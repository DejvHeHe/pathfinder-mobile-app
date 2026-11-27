package cz.pfspells.data

import kotlinx.serialization.Serializable

/*
 * Nasazené vybavení: položka inventáře může být „nasazená“ ve slotu a nést bonusy (atribut, záchrany, AC, skill).
 * Bonusy nasazených položek propojeného inventáře se kopírují do `Character.gear` (AppState) a odtud jdou do
 * výsledných atributů (`scores`), součtů skillů a souhrnu záchran / AC. Sčítání podle PF1: bonusy stejného typu
 * se nesčítají (platí nejvyšší), dodge, untyped a circumstance se sčítají, postihy se sčítají vždy.
 */

/**
 * Slot předmětu (Core Rulebook, Magic Items); prsteny jsou dva. [MAIN_HAND] = zbraň, [SHIELD] = druhá ruka (štít).
 * Pořadí = pořadí v dropdownu; uložené hodnoty se čtou podle jména.
 */
enum class ItemSlot(val capacity: Int = 1) {
    HEAD, HEADBAND, EYES, SHOULDERS, NECK, CHEST, BODY, ARMOR, BELT, WRISTS, HANDS, RING(2), FEET, MAIN_HAND, SHIELD;

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
            "weapon" -> MAIN_HAND
            else -> null
        }
    }
}

/**
 * Na co bonus platí; [SKILL] s klíčem skillu v [ItemBonus.skill].
 * [MAX_DEX] (max. bonus Obratnosti k AC – platí nejnižší) a [ARMOR_CHECK] (postih zbroje ke skillům Síly / Obratnosti)
 * nesou zbroje a štíty; zatížení inventáře je do `Character.gear` přidá jako typ UNTYPED (viz [loadBonuses]).
 */
enum class BonusTarget { STR, DEX, CON, INT, WIS, CHA, FORT, REF, WILL, AC, SKILL, MAX_DEX, ARMOR_CHECK, HP }

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
/** Bonusy vybavení a familiárů (Cat: +3 Stealth pánovi). */
val Character.allBonuses: List<ItemBonus> get() = if (familiarBonus.isEmpty()) gear else gear + familiarBonus

fun Character.gearMod(a: Ability): Int = stackBonuses(allBonuses.filter { it.target == a.bonusTarget() })

/** Bonus ke skillu z vybavení (klíč [Skills.key]; „knowledge.arcana“ bere i bonus ke všem Knowledge – „knowledge“). */
fun Character.gearSkill(key: String): Int =
    stackBonuses(allBonuses.filter { it.target == BonusTarget.SKILL && (it.skill == key || it.skill == key.substringBefore('.').substringBefore(':')) })

/** Bonus k záchraně z vybavení ([BonusTarget.FORT] / REF / WILL). */
fun Character.gearSave(target: BonusTarget): Int = stackBonuses(allBonuses.filter { it.target == target })

/** Bonusy k AC z vybavení po typech (souhrn – AC appka nepočítá): typ → výsledná hodnota. */
fun Character.gearAc(): Map<BonusType, Int> =
    allBonuses.filter { it.target == BonusTarget.AC }.groupBy { it.type }.mapValues { (_, bs) -> stackBonuses(bs) }.filterValues { it != 0 }

/**
 * Nasazené položky, které opravdu platí: ve slotu jen tolik, kolik se vejde (první v pořadí inventáře),
 * další ve stejném slotu se ignorují (konflikt). Položky bez slotu platí všechny.
 */
fun Inventory.workingEquipped(): List<InvEntry> {
    val used = mutableMapOf<ItemSlot, Int>()
    return entries.filter { e ->
        if (!e.equipped) return@filter false
        val slot = e.slot ?: return@filter true
        val n = used[slot] ?: 0
        used[slot] = n + e.qty.coerceAtLeast(1)
        n < slot.capacity
    }
}

/** Bonusy nasazených položek, které platí (každá položka jednou, i když je jich víc kusů). */
fun Inventory.gearBonuses(): List<ItemBonus> = workingEquipped().flatMap { it.bonuses }

/** Nasazené položky v daném slotu (v pořadí inventáře – první [ItemSlot.capacity] platí). */
fun Inventory.equippedIn(slot: ItemSlot): List<InvEntry> = entries.filter { it.equipped && it.slot == slot }

/** Nasazená položka platí (není navíc v plném slotu). */
fun Inventory.isWorking(entry: InvEntry): Boolean = entry.equipped && workingEquipped().any { it.id == entry.id }

/** Postihy zatížení (střední / těžké) jako bonusy typu UNTYPED – max Dex a postih ke skillům. */
fun loadBonuses(load: Load): List<ItemBonus> = listOfNotNull(
    load.maxDex?.let { ItemBonus(BonusTarget.MAX_DEX, it, BonusType.UNTYPED) },
    load.checkPenalty.takeIf { it != 0 }?.let { ItemBonus(BonusTarget.ARMOR_CHECK, it, BonusType.UNTYPED) },
)

/** Max. bonus Obratnosti k AC (nejnižší ze zbroje, štítu a zatížení); null = bez omezení. */
fun Character.maxDexBonus(): Int? = gear.filter { it.target == BonusTarget.MAX_DEX }.minOfOrNull { it.value }

/**
 * Postih zbroje ke skillům Síly a Obratnosti: zbroj + štít se sčítají, postih zatížení se s nimi nesčítá – platí horší.
 */
fun Character.armorCheckPenalty(): Int {
    val acp = gear.filter { it.target == BonusTarget.ARMOR_CHECK }
    val armor = acp.filter { it.type != BonusType.UNTYPED }.sumOf { it.value }
    val load = acp.filter { it.type == BonusType.UNTYPED }.sumOf { it.value }
    return minOf(armor, load, 0)
}

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

    /** Bonusy předmětu z databáze: podle jména + zbroj / štít (AC, max Dex, postih zbroje). */
    fun of(item: Item): List<ItemBonus> {
        val armor = item.armorBonus?.takeIf { it > 0 }?.let { ac ->
            val type = if (item.isShield) BonusType.SHIELD else BonusType.ARMOR
            listOfNotNull(
                ItemBonus(BonusTarget.AC, ac, type),
                item.maxDex?.takeUnless { item.isShield && it >= 99 }?.let { ItemBonus(BonusTarget.MAX_DEX, it, type) },
                item.armorCheck?.takeIf { it != 0 }?.let { ItemBonus(BonusTarget.ARMOR_CHECK, -kotlin.math.abs(it), type) },
            )
        } ?: emptyList()
        return armor + of(item.name)
    }

    /** Slot předmětu z databáze; zbroj a štít bez slotu v datech dostanou ARMOR / SHIELD. */
    fun slotOf(item: Item): ItemSlot? = ItemSlot.parse(item.slot)
        ?: when {
            item.armorBonus != null && item.armorBonus > 0 -> if (item.isShield) ItemSlot.SHIELD else ItemSlot.ARMOR
            item.category == ItemCategory.WEAPON || item.category == ItemCategory.MAGIC_WEAPON -> ItemSlot.MAIN_HAND
            else -> null
        }

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
