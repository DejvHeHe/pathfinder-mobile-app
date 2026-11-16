package cz.pfspells.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Převod zdrojů itemů na [Item]:
 * - ItemsBase.json – běžné vybavení a zbroje (z Foundry VTT PF1, přibalené; viz tools/build_items.py),
 * - Weapons.csv / Ammunition.csv / MagicItems.csv – d20pfsrd/AoN data; stejné soubory appka stahuje při aktualizaci.
 */
object ItemParser {
    private const val BASE_URL = "https://raw.githubusercontent.com/Vireche/randomitemgenerator/master/data/"
    const val WEAPONS_URL = BASE_URL + "weapons.csv"
    const val AMMO_URL = BASE_URL + "ammunition.csv"
    const val MAGIC_URL = BASE_URL + "magic_items_full%20-%20Updated%2019Jan2020.csv"
    const val SOURCE_DESCRIPTION = "github.com/Vireche/randomitemgenerator (d20pfsrd data) + Foundry VTT PF1 (bundled gear)"

    private val json = Json { ignoreUnknownKeys = true }

    /** Spojí všechny zdroje, přidělí unikátní id a seřadí podle jména. */
    fun combine(vararg groups: List<Item>): List<Item> {
        val used = HashSet<String>()
        val out = ArrayList<Item>()
        for (item in groups.flatMap { it }) {
            var id = item.id
            if (!used.add(id)) {
                var i = 2
                while (!used.add("$id-$i")) i++
                id = "$id-$i"
            }
            out += if (id == item.id) item else item.copy(id = id)
        }
        out.sortBy { it.searchName }
        return out
    }

    // ---------- ItemsBase.json ----------

    fun parseBase(text: String): List<Item> {
        val arr = json.parseToJsonElement(text) as JsonArray
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val name = o.str("name") ?: return@mapNotNull null
            val cat = ItemCategory.of(o.str("cat"))
            Item(
                id = "${cat.name.lowercase()}-${SpellParser.slug(name)}",
                name = name,
                category = cat,
                subtitle = o.str("sub"),
                priceGp = o.dbl("price"),
                weightLb = o.dbl("weight"),
                description = o.str("desc") ?: "",
                source = o.str("source"),
                armorBonus = o.int("ac"),
                maxDex = o.int("maxDex"),
                armorCheck = o.int("acp"),
                spellFailure = o.int("asf"),
            )
        }
    }

    // ---------- Weapons.csv / Ammunition.csv ----------

    fun parseWeapons(text: String, ammo: Boolean): List<Item> {
        val rows = Csv.parseWithHeader(text)
        require(rows.isNotEmpty() && rows.first().containsKey("name")) { "Not a weapons table" }
        return rows.mapNotNull { r ->
            val name = r.v("name")?.fixQuotes() ?: return@mapNotNull null
            val hand = when {
                r.v("is_ammunition") == "1" || ammo -> null
                r.v("is_unarmed") == "1" -> "Unarmed"
                r.v("is_light") == "1" -> "Light"
                r.v("is_one_handed") == "1" -> "One-handed"
                r.v("is_two_handed") == "1" -> "Two-handed"
                r.v("is_ranged") == "1" -> "Ranged"
                else -> r.v("section")
            }
            val prof = r.v("proficiency")
            val cat = if (ammo || r.v("is_ammunition") == "1") ItemCategory.AMMO else ItemCategory.WEAPON
            Item(
                id = "${cat.name.lowercase()}-${SpellParser.slug(name)}",
                name = name,
                category = cat,
                subtitle = listOfNotNull(prof, hand).joinToString(" · ").ifEmpty { null },
                priceGp = parsePrice(r.v("cost")),
                weightLb = parseWeight(r.v("weight")),
                description = r.v("special")?.let { "Special: $it" } ?: "",
                source = r.v("source"),
                proficiency = prof,
                damageSmall = r.v("damage_small"),
                damageMedium = r.v("damage_medium"),
                critical = r.v("critical")?.replace("x", "×"),
                range = r.v("range"),
                damageType = r.v("damage_type")?.let(::damageTypeName),
                special = r.v("special"),
            )
        }
    }

    // ---------- MagicItems.csv (d20pfsrd magic_items_full) ----------

    fun parseMagic(text: String): List<Item> {
        val rows = Csv.parseWithHeader(text)
        require(rows.size > 100 && rows.first().containsKey("Name")) { "Not a magic item table" }
        return rows.mapNotNull { r ->
            val name = r.v("Name")?.fixQuotes() ?: return@mapNotNull null
            val group = r.v("Group") ?: ""
            val slot = r.v("Slot")?.takeUnless { it == "none" || it == "—" || it.equals(group, ignoreCase = true) }
            val cat = when {
                name.startsWith("Potion", true) || name.startsWith("Oil of", true) || group == "Potion" -> ItemCategory.POTION
                group == "Wand" -> ItemCategory.WAND
                group == "Ring" -> ItemCategory.RING
                group == "Rod" -> ItemCategory.ROD
                group == "Staff" -> ItemCategory.STAFF
                group == "Wondrous Item" -> ItemCategory.WONDROUS
                group == "Weapon" || group == "Legendary Weapon" || group == "Shadow Piercing" -> ItemCategory.MAGIC_WEAPON
                group == "Ammunition" -> ItemCategory.MAGIC_WEAPON
                group == "Armor" -> ItemCategory.MAGIC_ARMOR
                group == "Artifact" -> ItemCategory.ARTIFACT
                else -> ItemCategory.OTHER
            }
            Item(
                id = "magic-${SpellParser.slug(name)}",
                name = name,
                category = cat,
                subtitle = listOfNotNull(group.ifEmpty { null }, slot).joinToString(" · ").ifEmpty { null },
                priceGp = r.v("PriceValue")?.toDoubleOrNull() ?: parsePrice(r.v("Price")),
                weightLb = r.v("WeightValue")?.toDoubleOrNull() ?: parseWeight(r.v("Weight")),
                description = r.v("Description") ?: "",
                source = r.v("Source"),
                aura = r.v("Aura"),
                casterLevel = r.v("CL"),
                slot = slot,
                requirements = r.v("Requirements"),
            )
        }
    }

    // ---------- pomocné ----------

    /** „B“ → „Bludgeoning“, „P or S“ → „Piercing or Slashing“, „B and P“ … */
    internal fun damageTypeName(code: String): String =
        Regex("""\b[BPS]\b""").replace(code) {
            when (it.value) { "B" -> "Bludgeoning"; "P" -> "Piercing"; else -> "Slashing" }
        }

    /** „1,500 gp“, „5 sp“, „1 cp“ → gp. */
    internal fun parsePrice(s: String?): Double? {
        val t = s?.trim()?.lowercase()?.replace(",", "") ?: return null
        val m = Regex("""([\d.]+)\s*(pp|gp|sp|cp)?""").find(t) ?: return null
        val v = m.groupValues[1].toDoubleOrNull() ?: return null
        return when (m.groupValues[2]) {
            "pp" -> v * 10
            "sp" -> v / 10
            "cp" -> v / 100
            else -> v
        }
    }

    internal fun parseWeight(s: String?): Double? {
        val t = s?.trim()?.lowercase() ?: return null
        if (t.startsWith("1/2")) return 0.5
        if (t.startsWith("1/4")) return 0.25
        return Regex("""[\d.]+""").find(t.replace(",", ""))?.value?.toDoubleOrNull()
    }

    private fun Map<String, String>.v(key: String): String? =
        this[key]?.trim()?.takeUnless { it.isEmpty() || it == "-" || it == "—" || it.equals("NULL", true) }

    /** Opraví apostrofy rozbité špatným kódováním zdroje. */
    private fun String.fixQuotes() = replace('�', '\'').replace('’', '\'')

    private fun JsonObject.str(key: String) = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    private fun JsonObject.dbl(key: String) = (this[key] as? JsonPrimitive)?.doubleOrNull
    private fun JsonObject.int(key: String) = (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }
}

/** Minimální CSV parser (RFC 4180: uvozovky, "" uvnitř, nové řádky v poli). */
object Csv {
    fun parse(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = if (text.startsWith('﻿')) 1 else 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && text[i + 1] == '"') { field.append('"'); i++ } else inQuotes = false
                } else field.append(c)
            } else when (c) {
                '"' -> inQuotes = true
                ',' -> { row.add(field.toString()); field.clear() }
                '\r' -> {}
                '\n' -> { row.add(field.toString()); field.clear(); rows.add(row); row = ArrayList() }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
        return rows
    }

    fun parseWithHeader(text: String): List<Map<String, String>> {
        val rows = parse(text)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim() }
        return rows.drop(1).filter { r -> r.any { it.isNotBlank() } }.map { r ->
            val m = HashMap<String, String>(header.size * 2)
            header.forEachIndexed { idx, h -> m[h] = r.getOrElse(idx) { "" } }
            m
        }
    }
}

/** Dekóduje bajty jako UTF-8, při chybě jako Windows-1252 (některé zdrojové CSV). */
fun decodeText(bytes: ByteArray): String = try {
    bytes.decodeToString(throwOnInvalidSequence = true)
} catch (e: CharacterCodingException) {
    val cp1252 = mapOf(
        0x80 to '€', 0x82 to '‚', 0x84 to '„', 0x85 to '…', 0x91 to '\'', 0x92 to '\'', 0x93 to '"', 0x94 to '"',
        0x96 to '–', 0x97 to '—', 0x99 to '™',
    )
    buildString(bytes.size) {
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            append(cp1252[v] ?: v.toChar())
        }
    }
}
