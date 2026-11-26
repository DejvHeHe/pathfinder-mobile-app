package cz.pfspells.data

import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray

/**
 * Převádí `SpellList.json` z projektu SpellTable-pf1e (data z Archives of Nethys, MIT)
 * na [Spell]. Stejný formát se používá pro přibalená data i pro stažení aktualizace.
 */
object SpellParser {
    const val SOURCE_URL =
        "https://raw.githubusercontent.com/iociveteres/SpellTable-pf1e/main/data/SpellList.json"

    private val json = Json { ignoreUnknownKeys = true }

    /** Vyhodí výjimku, když text není platný seznam spellů. */
    fun parse(text: String): List<Spell> {
        val root = json.parseToJsonElement(text).jsonArray
        val usedIds = HashSet<String>()
        val result = ArrayList<Spell>(root.size)
        for (el in root) {
            val obj = el as? JsonObject ?: continue
            val spell = parseOne(obj, usedIds) ?: continue
            result += spell
        }
        require(result.size > 100) { "The file contains only ${result.size} spells – it is probably not valid." }
        result.sortBy { it.searchName }
        return result
    }

    private fun parseOne(o: JsonObject, usedIds: MutableSet<String>): Spell? {
        val name = o.str("Name") ?: return null
        val levels = parseLevels(o)
        val subLevels = parseSubLevels(o)
        if (levels.isEmpty()) return null // spell bez class listu (jen SLA/doména) přeskočit

        var id = slug(name)
        if (!usedIds.add(id)) {
            var i = 2
            while (!usedIds.add("$id-$i")) i++
            id = "$id-$i"
        }
        return Spell(
            id = id,
            name = name,
            school = o.str("School") ?: "unknown",
            subschool = o.str("Subschool"),
            descriptor = o.str("Descriptors"),
            levels = levels,
            castingTime = o.str("Casting time"),
            components = o.str("Components"),
            range = o.str("Range"),
            effect = o.str("Effect"),
            targets = o.str("Target"),
            duration = o.str("Duration"),
            savingThrow = o.str("Saving throw"),
            spellResistance = o.str("Spell Resistance"),
            shortDescription = o.str("Short description"),
            description = o.str("Description") ?: "",
            mythic = o.str("Mythic"),
            source = o.str("Source"),
            url = o.str("Url aon"),
            pfsLegal = (o["PFS legal"] as? JsonPrimitive)?.let { it.booleanOrNull ?: it.contentOrNull?.equals("true", true) } ?: true,
            subLevels = subLevels,
        )
    }

    private fun parseLevels(o: JsonObject): Map<String, Int> {
        val list = (o["access_ways"] as? JsonObject)?.get("Level") as? JsonArray ?: return emptyMap()
        val out = LinkedHashMap<String, Int>()
        for (pair in list) {
            val arr = pair as? JsonArray ?: continue
            if (arr.size < 2) continue
            val raw = (arr[0] as? JsonPrimitive)?.contentOrNull ?: continue
            val lvl = (arr[1] as? JsonPrimitive)?.intOrNull ?: continue
            val key = normalizeListKey(raw)
            out[key] = minOf(out[key] ?: lvl, lvl)
        }
        return out
    }

    /** access_ways Domain / Subdomain / Mystery / Bloodline / Elemental School → „kind:name“ → spell level. */
    private fun parseSubLevels(o: JsonObject): Map<String, Int> {
        val aw = o["access_ways"] as? JsonObject ?: return emptyMap()
        val out = LinkedHashMap<String, Int>()
        for (kind in SubclassKind.entries) {
            val list = kind.sourceKey?.let { aw[it] } as? JsonArray ?: continue
            for (pair in list) {
                val arr = pair as? JsonArray ?: continue
                if (arr.size < 2) continue
                val raw = (arr[0] as? JsonPrimitive)?.contentOrNull ?: continue
                val lvl = (arr[1] as? JsonPrimitive)?.intOrNull ?: continue
                val key = subclassKey(kind, raw) ?: continue
                out[key] = minOf(out[key] ?: lvl, lvl)
            }
        }
        return out
    }

    /**
     * Klíč podtřídy („domain:fire“), opravy chyb ve zdroji: mezery, velká písmena, překlepy, „(u)“,
     * slepené záznamy („Elemental School air“ v bloodline) se zahodí.
     */
    internal fun subclassKey(kind: SubclassKind, raw: String): String? {
        var n = raw.trim().lowercase()
        if (n.isEmpty() || "elemental school" in n || n.startsWith("bloodline ")) return null
        n = n.removeSuffix(" (u)").replace(Regex("""\((pap|pzo)[^)]*\)"""), "").trim()
        n = SUBCLASS_FIXES[n] ?: n
        var k = kind
        // subdoména, která je ve skutečnosti doména (a naopak)
        if (k == SubclassKind.SUBDOMAIN && n == "scalykind") k = SubclassKind.DOMAIN
        if (k == SubclassKind.SUBDOMAIN && n == "law slavery") n = "slavery"
        return "${k.prefix}:$n"
    }

    private val SUBCLASS_FIXES = mapOf(
        "aristocra" to "aristocracy",
        "kyton (evil, kaw)" to "kyton (evil, law)",
    )

    /** Sjednotí pár výjimek ve zdroji („wizard“ místo „sorcerer/wizard“ apod.). */
    internal fun normalizeListKey(raw: String): String = when (val k = raw.trim().lowercase()) {
        "wizard", "sorcerer" -> "sorcerer/wizard"
        "cleric", "oracle" -> "cleric/oracle"
        "summoner (unchained)", "unchained summoner" -> "unchained summoner"
        else -> k
    }

    private fun JsonObject.str(key: String): String? {
        val p = this[key] as? JsonPrimitive ?: return null
        val s = p.contentOrNull?.trim() ?: return null
        return s.takeUnless { it.isEmpty() || it == "None" || it == "NULL" }
    }

    internal fun slug(name: String): String {
        val sb = StringBuilder()
        var dash = false
        for (c in name.lowercase()) {
            if (c in 'a'..'z' || c in '0'..'9') {
                sb.append(c); dash = false
            } else if (!dash && sb.isNotEmpty()) {
                sb.append('-'); dash = true
            }
        }
        return sb.toString().trimEnd('-')
    }
}
