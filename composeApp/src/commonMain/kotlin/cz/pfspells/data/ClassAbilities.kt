package cz.pfspells.data

import cz.pfspells.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.math.ceil
import kotlin.math.floor

/*
 * Schopnosti class z Foundry VTT PF1 (packs/class-abilities, generuje tools/build_class_abilities.py):
 * - schopnosti podtříd (domény, subdomény, bloodline, školy kouzelníka) s úrovní a vzorcem počtu použití → ClassAbilityTable.kt,
 * - hexy (witch, shaman) a revelations (oracle) k výběru → ClassAbilities.json (líně, s textem).
 */

// ---------------- schopnosti podtříd ----------------

/** Schopnost, kterou dává podtřída na úrovni [level]; [uses] = vzorec Foundry (null = bez počtu použití). */
class SubclassPower(
    val key: String,
    val level: Int,
    val name: String,
    val uses: String? = null,
    val unit: ResourceUnit? = null,
)

object SubclassPowers {
    /**
     * Foundry (domény, bloodline, školy, blessingy) + AoN (spirity, bloodline bloodragera i čaroděje, disciplíny,
     * inquisitions); podtřída, kterou zná Foundry, bere jen Foundry (AoN by schopnosti zdvojil).
     */
    private val byKey: Map<String, List<SubclassPower>> = SUBCLASS_POWER_TABLE.groupBy { it.key }.let { foundry ->
        foundry + AON_SUBCLASS_POWER_TABLE.groupBy { it.key }.filterKeys { it !in foundry }
    }

    /** Bloodline čaroděje ze zdroje kouzel, které jsou variantou jiné („fire“ → Elemental). */
    private val BLOODLINE_ALIASES = mapOf("air" to "elemental", "earth" to "elemental", "fire" to "elemental", "water" to "elemental")

    /** Bonusové featy bloodline (čaroděj 7. / 13. / 19., bloodrager 6. a každá 3.) – AoN; prázdné = neznámé. */
    fun bonusFeats(e: ClassEntry): List<String> = keysOf(e).firstNotNullOfOrNull { BLOODLINE_BONUS_FEATS[it.first] }.orEmpty()

    /** Schopnosti podtřídy; null = podtřídu nezná Foundry ani AoN. */
    fun of(key: String): List<SubclassPower>? = byKey[key] ?: key.takeIf { it.startsWith("wspirit:") }?.let(::wandering)

    /**
     * Wandering spirit šamana: schopnost ducha od 4. úrovně, větší od 12., pravá od 20. (bez manifestace) –
     * klíč „wspirit:life“, schopnosti z „spirit:life“ s posunutou úrovní.
     */
    private fun wandering(key: String): List<SubclassPower>? =
        byKey["spirit:" + key.substringAfter(':')]?.filter { it.level < 20 }?.map { p ->
            SubclassPower(key, when (p.level) { 1 -> 4; 8 -> 12; else -> 20 }, p.name, p.uses, p.unit)
        }

    /** Úroveň šamana, od které má wandering spirit. */
    const val WANDERING_SPIRIT_LEVEL = 4

    /** „archon (good, law)“ → „archon“ */
    private fun base(v: String) = v.substringBefore('(').trim().lowercase()

    /**
     * Klíče podtříd classy postavy v [SUBCLASS_POWER_TABLE] (subdoména má přednost před doménou, kterou nahrazuje),
     * s popiskem pro název schopnosti („Fire“, „Ash“, „Draconic“).
     */
    fun keysOf(e: ClassEntry): List<Pair<String, String>> {
        val sc = e.subclass
        fun domain(d: String?, sub: String?): Pair<String, String>? {
            val dom = d?.lowercase()?.trim()?.ifEmpty { null } ?: return null
            if (sub != null) {
                val b = base(sub)
                listOf("subdomain:$b@$dom", "subdomain:$b").firstOrNull { byKey.containsKey(it) }?.let { return it to title(b) }
            }
            return "domain:$dom" to title(dom)
        }
        return when (e.classId) {
            "cleric" -> listOfNotNull(
                domain(sc[SubclassSlot.DOMAIN1], sc[SubclassSlot.SUBDOMAIN1]),
                domain(sc[SubclassSlot.DOMAIN2], sc[SubclassSlot.SUBDOMAIN2]),
            ).distinctBy { it.first }
            "druid" -> listOfNotNull(domain(sc[SubclassSlot.DOMAIN1], sc[SubclassSlot.SUBDOMAIN1]))
            // inquisitor: doména, nebo místo ní inquisition
            "inquisitor" -> listOfNotNull(
                sc[SubclassSlot.INQUISITION]?.lowercase()?.let { "inquisition:$it" to title(it) }
                    ?: domain(sc[SubclassSlot.DOMAIN1], sc[SubclassSlot.SUBDOMAIN1]),
            )
            "warpriest" -> listOfNotNull(sc[SubclassSlot.BLESSING1], sc[SubclassSlot.BLESSING2])
                .map { it.lowercase() }.distinct().map { "blessing:$it" to title(it) }
            "shaman" -> listOfNotNull(
                sc[SubclassSlot.SPIRIT]?.lowercase()?.let { "spirit:$it" to title(it) },
                sc[SubclassSlot.WANDERING_SPIRIT]?.lowercase()?.takeIf { e.level >= WANDERING_SPIRIT_LEVEL && it != sc[SubclassSlot.SPIRIT]?.lowercase() }
                    ?.let { "wspirit:$it" to title(it) },
            )
            "bloodrager" -> listOfNotNull(sc[SubclassSlot.BLOODLINE]?.lowercase()?.let { "brbloodline:$it" to title(it) })
            "psychic" -> listOfNotNull(sc[SubclassSlot.DISCIPLINE]?.lowercase()?.let { "discipline:$it" to title(it) })
            "sorcerer" -> listOfNotNull(sc[SubclassSlot.BLOODLINE]?.lowercase()?.let { "bloodline:${BLOODLINE_ALIASES[it] ?: it}" to title(it) })
            "wizard" -> listOfNotNull(sc[SubclassSlot.SCHOOL]?.lowercase()?.let { v ->
                "school:$v" to title(v.removePrefix("element:"))
            })
            else -> emptyList()
        }
    }

    fun title(v: String) = v.split(' ', '-').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
}

// ---------------- vzorce Foundry ----------------

/**
 * Vyhodnocení vzorce počtu použití z Foundry („3 + @abilities.wis.mod“, „floor((@class.unlevel - 4) / 4)“,
 * „1 + if(gte(@class.unlevel, 17), 1)“). Proměnné: @class.unlevel / @class.level (úroveň classy),
 * @attributes.hd.total (HD postavy), @abilities.X.mod. Funkce floor, ceil, min, max, clamp, abs, if, gt, gte, lt, lte, eq.
 * Výsledek se zaokrouhlí dolů; neznámá proměnná nebo chyba → null.
 */
object FoundryFormula {
    fun eval(formula: String, vars: (String) -> Double?): Double? = try {
        Parser(formula, vars).run()
    } catch (e: IllegalArgumentException) {
        null
    }

    /** Max. použití pro classu na úrovni [classLevel]; nikdy záporné. */
    fun uses(formula: String, classLevel: Int, c: Character): Int? = eval(formula) { v ->
        when (v) {
            "@class.unlevel", "@class.level" -> classLevel.toDouble()
            "@attributes.hd.total" -> c.level.toDouble()
            else -> Ability.entries.firstOrNull { v == "@abilities.${it.name.lowercase()}.mod" }
                ?.let { abilityMod(c.scores[it]).toDouble() }
        }
    }?.let { floor(it).toInt().coerceAtLeast(0) }

    private class Parser(val s: String, val vars: (String) -> Double?) {
        var i = 0

        fun run(): Double {
            val v = expr()
            ws()
            require(i == s.length) { "trailing input" }
            return v
        }

        fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
        fun peek(): Char? { ws(); return s.getOrNull(i) }
        fun expect(c: Char) { require(peek() == c) { "expected $c" }; i++ }

        fun expr(): Double {
            var v = term()
            while (true) {
                when (peek()) {
                    '+' -> { i++; v += term() }
                    '-' -> { i++; v -= term() }
                    else -> return v
                }
            }
        }

        fun term(): Double {
            var v = unary()
            while (true) {
                when (peek()) {
                    '*' -> { i++; v *= unary() }
                    '/' -> { i++; val r = unary(); v = if (r == 0.0) 0.0 else v / r }
                    else -> return v
                }
            }
        }

        fun unary(): Double = if (peek() == '-') { i++; -unary() } else atom()

        fun atom(): Double {
            val c = peek() ?: throw IllegalArgumentException("unexpected end")
            return when {
                c == '(' -> { i++; val v = expr(); expect(')'); v }
                c.isDigit() -> {
                    val st = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                    s.substring(st, i).toDouble()
                }
                c == '@' -> {
                    val st = i++
                    while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '.' || s[i] == '_')) i++
                    vars(s.substring(st, i)) ?: throw IllegalArgumentException("unknown ${s.substring(st, i)}")
                }
                c.isLetter() -> {
                    val st = i
                    while (i < s.length && s[i].isLetter()) i++
                    val name = s.substring(st, i)
                    expect('(')
                    val args = mutableListOf(expr())
                    while (peek() == ',') { i++; args += expr() }
                    expect(')')
                    call(name, args)
                }
                else -> throw IllegalArgumentException("unexpected $c")
            }
        }

        fun call(f: String, a: List<Double>): Double {
            fun b(x: Boolean) = if (x) 1.0 else 0.0
            fun arg(n: Int) = a.getOrNull(n) ?: throw IllegalArgumentException("$f: missing argument")
            return when (f) {
                "floor" -> floor(arg(0))
                "ceil" -> ceil(arg(0))
                "abs" -> kotlin.math.abs(arg(0))
                "min" -> a.minOrNull() ?: throw IllegalArgumentException("min()")
                "max" -> a.maxOrNull() ?: throw IllegalArgumentException("max()")
                "clamp" -> arg(0).coerceIn(arg(1), maxOf(arg(1), arg(2)))
                "if" -> if (arg(0) != 0.0) arg(1) else a.getOrNull(2) ?: 0.0
                "gt" -> b(arg(0) > arg(1))
                "gte" -> b(arg(0) >= arg(1))
                "lt" -> b(arg(0) < arg(1))
                "lte" -> b(arg(0) <= arg(1))
                "eq" -> b(arg(0) == arg(1))
                else -> throw IllegalArgumentException("unknown function $f")
            }
        }
    }
}

// ---------------- hexy a revelations k výběru ----------------

@Serializable
enum class AbilityKind { HEX, MAJOR_HEX, GRAND_HEX, REVELATION }

/** Hex / revelation z `ClassAbilities.json`. [mysteries] = mystéria orákula (prázdné = jakékoli). */
@Serializable
data class ClassAbility(
    val id: String,
    val name: String,
    val kind: AbilityKind,
    val classes: List<String> = emptyList(),
    val mysteries: List<String> = emptyList(),
    /** hex ducha šamana (AoN, `SubclassAbilities.json`): spirity, které ho dávají; prázdné = běžný hex */
    val spirits: List<String> = emptyList(),
    val minLevel: Int = 1,
    val uses: String? = null,
    val unit: ResourceUnit? = null,
    /** Ex / Su / Sp */
    val type: String? = null,
    val source: String = "",
    val text: String = "",
) {
    val searchName: String by lazy { name.lowercase() }
}

/**
 * Vybraný hex / revelation u classy postavy – kopie jména a vzorce (počítadlo funguje i bez načtené databáze).
 * [id] = [ClassAbility.id].
 */
@Serializable
data class AbilityPick(
    val id: String,
    val name: String,
    val kind: AbilityKind,
    val uses: String? = null,
    val unit: ResourceUnit? = null,
)

fun ClassAbility.toPick() = AbilityPick(id, name, kind, uses, unit)

class ClassAbilityDatabase(val all: List<ClassAbility>) {
    private val byId = all.associateBy { it.id }
    fun get(id: String): ClassAbility? = byId[id]
    fun forClass(classId: String): List<ClassAbility> = all.filter { classId in it.classes }
}

object ClassAbilityRepository {
    private val json = Json { ignoreUnknownKeys = true }

    private fun list(text: String) = json.decodeFromString(ListSerializer(ClassAbility.serializer()), text)

    /**
     * [text] = ClassAbilities.json (Foundry), [spiritHexes] = SubclassAbilities.json (AoN) – hex ducha se stejným jménem
     * jako běžný hex šamana se vynechá (běžný hex jde vybrat i tak).
     */
    fun parse(text: String, spiritHexes: String? = null): ClassAbilityDatabase {
        val base = list(text)
        val shamanHexes = base.filter { "shaman" in it.classes }.map { it.searchName }.toSet()
        val extra = spiritHexes?.let(::list).orEmpty().filter { it.searchName !in shamanHexes }
        return ClassAbilityDatabase(base + extra)
    }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): ClassAbilityDatabase = withContext(Dispatchers.Default) {
        parse(
            Res.readBytes("files/ClassAbilities.json").decodeToString(),
            Res.readBytes("files/SubclassAbilities.json").decodeToString(),
        )
    }
}

/** Druh výběru classy: hexy (witch, shaman) nebo revelations (oracle). */
enum class AbilityGroup { HEXES, REVELATIONS }

object AbilityRules {
    private val SHAMAN_HEX_LEVELS = listOf(2, 4, 8, 10, 12, 16, 18, 20)
    private val REVELATION_LEVELS = listOf(1, 3, 7, 11, 15, 19)

    fun groupOf(classId: String): AbilityGroup? = when (classId) {
        "witch", "shaman" -> AbilityGroup.HEXES
        "oracle" -> AbilityGroup.REVELATIONS
        else -> null
    }

    /** Počet hexů / revelations podle úrovně classy (Core Rulebook, APG, ACG) – bez extra featů. */
    fun slots(classId: String, level: Int): Int = when (classId) {
        "witch" -> 1 + level / 2
        "shaman" -> SHAMAN_HEX_LEVELS.count { it <= level }
        "oracle" -> REVELATION_LEVELS.count { it <= level }
        else -> 0
    }

    /** Feat, který přidává jeden hex / revelation (jde vzít víckrát). */
    fun extraFeatId(classId: String): String? = when (groupOf(classId)) {
        AbilityGroup.HEXES -> "extra-hex"
        AbilityGroup.REVELATIONS -> "extra-revelation"
        null -> null
    }

    /** Kolikrát má postava Extra Hex / Extra Revelation (podle id featu z AoN nebo jména vlastního featu). */
    fun extraFeats(c: Character, classId: String): Int {
        val id = extraFeatId(classId) ?: return 0
        val name = id.replace('-', ' ')
        return c.feats.count { it.featId == id || (it.featId == null && it.name.trim().lowercase() == name) }
    }

    /** Wandering hex šamana: jeden hex wandering spiritu od 6. úrovně, dva od 14. (jen se zvoleným wandering spiritem). */
    fun wanderingHexes(e: ClassEntry): Int =
        if (e.classId != "shaman" || e.subclass[SubclassSlot.WANDERING_SPIRIT] == null) 0
        else listOf(6, 14).count { it <= e.level }

    /** Hexy / revelations classy postavy celkem: podle úrovně + Extra Hex / Extra Revelation + wandering hex. */
    fun slots(c: Character, e: ClassEntry): Int = slots(e.classId, e.level) + extraFeats(c, e.classId) + wanderingHexes(e)

    /** Revelation patří k mystériu orákula (nebo k žádnému konkrétnímu); bez zvoleného mystéria vše. */
    fun matchesMystery(a: ClassAbility, mystery: String?): Boolean =
        a.kind != AbilityKind.REVELATION || mystery == null || a.mysteries.isEmpty() || mystery.lowercase() in a.mysteries

    /** Hex ducha jen pro šamana s tím duchem (bez zvoleného ducha všechny); běžné hexy vždy. */
    fun matchesSpirit(a: ClassAbility, spirit: String?): Boolean =
        a.spirits.isEmpty() || spirit == null || spirit.lowercase() in a.spirits

    /** Hex ducha šamana: jeho duch, nebo wandering spirit od 6. úrovně (wandering hex). */
    fun matchesSpirits(a: ClassAbility, e: ClassEntry): Boolean {
        val spirit = e.subclass[SubclassSlot.SPIRIT]
        val wandering = e.subclass[SubclassSlot.WANDERING_SPIRIT]?.takeIf { wanderingHexes(e) > 0 }
        return matchesSpirit(a, spirit) || (wandering != null && wandering.lowercase() in a.spirits)
    }
}
