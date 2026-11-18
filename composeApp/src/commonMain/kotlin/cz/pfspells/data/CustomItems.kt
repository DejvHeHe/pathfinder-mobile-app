package cz.pfspells.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// ---------------- vlastní katalog itemů (sdílený mezi inventáři) ----------------

/** Prefix id itemu z vlastního katalogu (odlišení od databáze). */
const val CUSTOM_ITEM_PREFIX = "my-"

val Item.isCustom: Boolean get() = id.startsWith(CUSTOM_ITEM_PREFIX)

/** Vlastní item uložený v katalogu; do inventáře se přidává jako položka bez `itemId`. */
@Serializable
data class CustomItem(
    val id: String,
    val name: String,
    val category: String = ItemCategory.OTHER.name,
    val priceGp: Double? = null,
    val weightLb: Double? = null,
    /** krátké info (např. „1d8+1, CL 1“) */
    val info: String? = null,
) {
    val cat: ItemCategory get() = ItemCategory.of(category)

    /** Jako [Item] pro výběr a detail (info jde do `subtitle`). */
    fun toItem(): Item = Item(
        id = CUSTOM_ITEM_PREFIX + id, name = name, category = cat, subtitle = info,
        priceGp = priceGp, weightLb = weightLb, description = "", source = null,
    )

    companion object {
        fun fromEntry(e: InvEntry, id: String = newEntryId()) =
            CustomItem(id, e.name, e.category, e.priceGp, e.weightLb, e.info)
    }
}

class CustomItemStore(private val settings: Settings = Settings()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(CustomItem.serializer())

    fun load(): List<CustomItem> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        return try { json.decodeFromString(serializer, raw) } catch (e: Exception) { emptyList() }
    }

    fun save(list: List<CustomItem>) = settings.putString(KEY, json.encodeToString(serializer, list))

    private companion object {
        const val KEY = "custom_items_v1"
    }
}

// ---------------- lektvary, svitky a hůlky ze spellů ----------------

/**
 * Výroba magického itemu ze spellu (Core Rulebook, Magic Items – ceny):
 * lektvar 50 gp × SL × CL (max 3. level), svitek 25 gp × SL × CL, hůlka 750 gp × SL × CL (max 4. level, 50 nábojů).
 * Spell 0. levelu se počítá jako ½. Drahé materiální složky se k ceně nepřičítají automaticky.
 */
enum class SpellItemKind(val category: ItemCategory, val maxSpellLevel: Int, private val baseGp: Double, val prefix: String) {
    POTION(ItemCategory.POTION, 3, 50.0, "Potion of"),
    SCROLL(ItemCategory.SCROLL, 9, 25.0, "Scroll of"),
    WAND(ItemCategory.WAND, 4, 750.0, "Wand of");

    fun price(spellLevel: Int, casterLevel: Int): Double =
        baseGp * (if (spellLevel <= 0) 0.5 else spellLevel.toDouble()) * casterLevel

    fun allows(spellLevel: Int): Boolean = spellLevel <= maxSpellLevel

    fun name(spell: Spell): String = "$prefix ${spell.name}"

    /** Položka inventáře (vlastní item, bez `itemId`); info je v originále jako ostatní herní údaje. */
    fun entry(spell: Spell, spellLevel: Int, casterLevel: Int, qty: Int = 1): InvEntry = InvEntry(
        id = newEntryId(), itemId = null, name = name(spell), category = category.name, qty = qty,
        priceGp = price(spellLevel, casterLevel), weightLb = null,
        info = buildString {
            append("SL $spellLevel, CL $casterLevel")
            if (this@SpellItemKind == WAND) append(", 50 charges")
        },
    )

    companion object {
        /** Nejnižší úroveň sesilatele pro spell daného levelu (jako wizard/cleric: 1, 1, 3, 5, 7 …). */
        fun minCasterLevel(spellLevel: Int): Int = if (spellLevel <= 1) 1 else 2 * spellLevel - 1
    }
}
