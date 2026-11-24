package cz.pfspells.data

import kotlinx.serialization.Serializable

/** Kategorie itemu – pořadí = pořadí skupin v inventáři. */
/** [label] = anglický název (porovnávání s daty); v UI se zobrazuje `Strings.category`. */
enum class ItemCategory(val label: String) {
    WEAPON("Weapons"),
    AMMO("Ammunition"),
    ARMOR("Armor & shields"),
    GEAR("Adventuring gear"),
    ALCHEMICAL("Alchemical"),
    MOUNT("Animals & mounts"),
    POTION("Potions & oils"),
    SCROLL("Scrolls"),
    WAND("Wands"),
    RING("Rings"),
    ROD("Rods"),
    STAFF("Staves"),
    WONDROUS("Wondrous items"),
    MAGIC_WEAPON("Magic weapons"),
    MAGIC_ARMOR("Magic armor"),
    ARTIFACT("Artifacts"),
    OTHER("Other");

    companion object {
        fun of(name: String?): ItemCategory = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

/** Item z databáze (texty anglicky, originál). Ceny v gp, váha v lb. */
data class Item(
    val id: String,
    val name: String,
    val category: ItemCategory,
    /** např. „Martial · One-handed“, „Medium armor“, „Wondrous · neck“ */
    val subtitle: String?,
    val priceGp: Double?,
    val weightLb: Double?,
    val description: String,
    val source: String?,
    // zbraně
    val proficiency: String? = null,
    val damageSmall: String? = null,
    val damageMedium: String? = null,
    val critical: String? = null,
    val range: String? = null,
    val damageType: String? = null,
    val special: String? = null,
    // zbroje
    val armorBonus: Int? = null,
    val maxDex: Int? = null,
    val armorCheck: Int? = null,
    val spellFailure: Int? = null,
    // magické
    val aura: String? = null,
    val casterLevel: String? = null,
    val slot: String? = null,
    val requirements: String? = null,
) {
    val searchName: String = name.lowercase()
    val paragraphs: List<String> by lazy { description.split('\n').map { it.trim() }.filter { it.isNotEmpty() } }

    /** Krátké shrnutí pro poznámku v inventáři (např. „1d8 / 19–20/×2“, „+6 AC, ACP −5“). */
    val summary: String?
        get() = when {
            damageMedium != null && damageMedium != "-" -> listOfNotNull(damageMedium, critical?.takeIf { it != "-" }).joinToString(" / ")
            armorBonus != null -> buildString {
                append("+$armorBonus AC")
                if (armorCheck != null && armorCheck != 0) append(", ACP −${kotlin.math.abs(armorCheck)}")
            }
            else -> null
        }
}

class ItemDatabase(val items: List<Item>, val source: SpellSource, val updatedAt: String?) {
    private val byId = items.associateBy { it.id }
    private val byName by lazy { items.associateBy { it.searchName } }
    fun get(id: String): Item? = byId[id]
    fun findByName(name: String): Item? = byName[name.trim().lowercase()]
    val counts: Map<ItemCategory, Int> by lazy { items.groupingBy { it.category }.eachCount() }
}

// ---------------- inventář ----------------

@Serializable
data class InvEntry(
    val id: String,
    /** id itemu v databázi, null = vlastní item */
    val itemId: String? = null,
    val name: String,
    val category: String = ItemCategory.OTHER.name,
    val qty: Int = 1,
    val priceGp: Double? = null,
    val weightLb: Double? = null,
    val note: String = "",
    /** krátké herní údaje (zbraň/zbroj) */
    val info: String? = null,
    /** nasazeno – bonusy platí pro propojenou postavu */
    val equipped: Boolean = false,
    /** slot magického předmětu (null = bez slotu – zbraň, svitek…) */
    val slot: ItemSlot? = null,
    /** bonusy předmětu (atribut, záchrany, AC, skill) – platí jen nasazené */
    val bonuses: List<ItemBonus> = emptyList(),
) {
    val cat: ItemCategory get() = ItemCategory.of(category)
}

@Serializable
data class Inventory(
    val id: String,
    val name: String,
    val strength: Int = 10,
    val pp: Int = 0,
    val gp: Int = 0,
    val sp: Int = 0,
    val cp: Int = 0,
    val notes: String = "",
    val entries: List<InvEntry> = emptyList(),
    /** velikost tvora ([CreatureSize] jako text – neznámá hodnota nerozbije načtení) */
    val size: String = CreatureSize.MEDIUM.name,
    /** čtyřnohý tvor (kůň, pes…) unese víc */
    val quadruped: Boolean = false,
) {
    val creatureSize: CreatureSize get() = CreatureSize.of(size)
    val capacityMultiplier: Double get() = creatureSize.multiplier(quadruped)
    val heavyLoad: Double get() = CarryingCapacity.heavyLoad(strength, capacityMultiplier)
    val lightLoad: Double get() = CarryingCapacity.lightLoad(strength, capacityMultiplier)
    val mediumLoad: Double get() = CarryingCapacity.mediumLoad(strength, capacityMultiplier)
    val load: Load get() = CarryingCapacity.load(strength, totalWeight, capacityMultiplier)

    val coinsGp: Double get() = pp * 10.0 + gp + sp / 10.0 + cp / 100.0
    /** 50 mincí = 1 lb (Core Rulebook) */
    val coinWeight: Double get() = (pp + gp + sp + cp) / 50.0
    val itemsWeight: Double get() = entries.sumOf { (it.weightLb ?: 0.0) * it.qty }
    val totalWeight: Double get() = itemsWeight + coinWeight
}

/** Nosnost PF1 (Medium tvor): light / medium / heavy load v lb podle STR. */
object CarryingCapacity {
    private val heavy = intArrayOf(
        0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 115, 130, 150, 175, 200, 230, 260, 300, 350,
        400, 460, 520, 600, 700, 800, 920, 1040, 1200, 1400,
    )

    /** [mult] = násobitel podle velikosti tvora ([CreatureSize.multiplier]). */
    fun heavyLoad(str: Int, mult: Double = 1.0): Double = baseHeavy(str) * mult

    private fun baseHeavy(str: Int): Double = when {
        str <= 0 -> 0.0
        str < 30 -> heavy[str].toDouble()
        else -> baseHeavy(str - 10) * 4 // každých +10 STR = ×4
    }

    fun lightLoad(str: Int, mult: Double = 1.0): Double = kotlin.math.floor(heavyLoad(str, mult) / 3)
    fun mediumLoad(str: Int, mult: Double = 1.0): Double = kotlin.math.floor(heavyLoad(str, mult) * 2 / 3)

    fun load(str: Int, weight: Double, mult: Double = 1.0): Load = when {
        weight <= lightLoad(str, mult) -> Load.LIGHT
        weight <= mediumLoad(str, mult) -> Load.MEDIUM
        weight <= heavyLoad(str, mult) -> Load.HEAVY
        else -> Load.OVERLOADED
    }
}

/**
 * Stupeň zatížení a jeho postihy (Core Rulebook, tabulka Encumbrance). Texty v `Strings.loadName` / `loadPenalties`.
 * [maxDex] null = bez omezení, [runMultiplier] 0 = nemůže běžet.
 */
enum class Load(val maxDex: Int?, val checkPenalty: Int, val runMultiplier: Int) {
    LIGHT(null, 0, 4),
    MEDIUM(3, -3, 4),
    HEAVY(1, -6, 3),
    OVERLOADED(0, -6, 0),
}

/** Velikost tvora a násobitel nosnosti (Core Rulebook: dvounohý / čtyřnohý). */
enum class CreatureSize(private val biped: Double, private val quad: Double) {
    FINE(0.125, 0.25),
    DIMINUTIVE(0.25, 0.5),
    TINY(0.5, 0.75),
    SMALL(0.75, 1.0),
    MEDIUM(1.0, 1.5),
    LARGE(2.0, 3.0),
    HUGE(4.0, 6.0),
    GARGANTUAN(8.0, 12.0),
    COLOSSAL(16.0, 24.0);

    fun multiplier(quadruped: Boolean): Double = if (quadruped) quad else biped

    companion object {
        fun of(name: String?): CreatureSize = entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}
