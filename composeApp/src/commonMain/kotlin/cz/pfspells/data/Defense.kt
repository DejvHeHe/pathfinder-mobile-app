package cz.pfspells.data

/*
 * Obrana a životy (Core Rulebook, Combat): AC = 10 + zbroj + štít + Obratnost (omezená max Dex) + velikost
 * + přirozená zbroj + deflection + dodge + ostatní bonusy; dotyk bez zbroje, štítu a přirozené zbroje;
 * zaskočený bez (kladné) Obratnosti a dodge. HP: 1. HD postavy max, další průměr (polovina + 1, jako PFS),
 * rasové HD tvorů průměr (4,5 za d8), + Odolnost za HD (min. 1 za HD), Toughness, bonus navíc.
 */

/** Úprava AC (i útoku) podle velikosti. */
fun sizeAcMod(size: CreatureSize?): Int = when (size) {
    CreatureSize.FINE -> 8
    CreatureSize.DIMINUTIVE -> 4
    CreatureSize.TINY -> 2
    CreatureSize.SMALL -> 1
    CreatureSize.LARGE -> -1
    CreatureSize.HUGE -> -2
    CreatureSize.GARGANTUAN -> -4
    CreatureSize.COLOSSAL -> -8
    else -> 0
}

data class Defense(
    val ac: Int,
    val touch: Int,
    val flatFooted: Int,
    val armor: Int,
    val shield: Int,
    /** Obratnost započtená do AC (po max Dex) */
    val dex: Int,
    val maxDex: Int?,
    val size: Int,
    val natural: Int,
    val deflection: Int,
    val dodge: Int,
    /** ostatní typy bonusů (insight, luck, sacred…) + ruční [Character.acMisc] */
    val other: Int,
)

fun Character.defense(): Defense {
    val dexMod = abilityMod(scores.dex)
    val maxDex = maxDexBonus()
    val dex = if (maxDex != null) minOf(dexMod, maxDex) else dexMod
    val byType = gear.filter { it.target == BonusTarget.AC }.groupBy { it.type }.mapValues { (_, l) -> stackBonuses(l) }
    fun t(type: BonusType) = byType[type] ?: 0
    val armor = t(BonusType.ARMOR)
    val shield = t(BonusType.SHIELD)
    // přirozená zbroj zvířete + enhancement přirozené zbroje (Amulet of Natural Armor)
    val natural = (naturalArmor ?: 0) + t(BonusType.NATURAL_ARMOR)
    val deflection = t(BonusType.DEFLECTION)
    val dodge = t(BonusType.DODGE)
    val known = setOf(BonusType.ARMOR, BonusType.SHIELD, BonusType.NATURAL_ARMOR, BonusType.DEFLECTION, BonusType.DODGE)
    val other = byType.filterKeys { it !in known }.values.sum() + acMisc
    val size = sizeAcMod(creatureSize)
    val ac = 10 + armor + shield + dex + size + natural + deflection + dodge + other
    val touch = 10 + dex + size + deflection + dodge + other
    val flat = ac - maxOf(dex, 0) - dodge
    return Defense(ac, touch, flat, armor, shield, dex, maxDex, size, natural, deflection, dodge, other)
}

/** Toughness: +3 HP, od 4. HD +1 za každé HD. */
private fun Character.toughnessHp(): Int = if (feats.any { it.featId == "toughness" }) maxOf(3, level) else 0

/** Maximální HP (ruční [Character.hpOverride] má přednost). */
fun Character.maxHp(): Int {
    hpOverride?.let { return it.coerceAtLeast(1) }
    val con = abilityMod(scores.con)
    var hp = 0
    var creatureHalf = 0 // rasové HD tvorů: průměr (hd + 1) / 2 za kostku, sčítá se v polovinách
    var first = true
    for (e in classes) {
        val info = ClassCatalog.get(e.classId)
        val hd = info?.hd ?: 8
        repeat(e.level.coerceAtLeast(0)) {
            if (info?.creature == true) {
                creatureHalf += hd + 1
                hp += con
            } else {
                hp += maxOf(1, (if (first) hd else hd / 2 + 1) + con)
            }
            first = false
        }
    }
    hp += creatureHalf / 2
    // HP z vybavení / familiára (Toad: +3 HP)
    val bonusHp = stackBonuses(allBonuses.filter { it.target == BonusTarget.HP })
    return (hp + toughnessHp() + hpBonus + bonusHp).coerceAtLeast(1)
}

/** Aktuální HP (může být i záporné – v bezvědomí / umírá). */
fun Character.currentHp(): Int = maxHp() - damage
