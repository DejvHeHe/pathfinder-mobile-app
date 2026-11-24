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
    private val byKey: Map<String, List<SubclassPower>> = SUBCLASS_POWER_TABLE.groupBy { it.key }

    /** Schopnosti podtřídy; null = Foundry podtřídu nezná. */
    fun of(key: String): List<SubclassPower>? = byKey[key]

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
            "sorcerer" -> listOfNotNull(sc[SubclassSlot.BLOODLINE]?.lowercase()?.let { "bloodline:$it" to title(it) })
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

    fun parse(text: String) = ClassAbilityDatabase(json.decodeFromString(ListSerializer(ClassAbility.serializer()), text))

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): ClassAbilityDatabase = withContext(Dispatchers.Default) {
        parse(Res.readBytes("files/ClassAbilities.json").decodeToString())
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

    /** Počet hexů / revelations podle úrovně classy (Core Rulebook, APG, ACG) – bez hexů z patrona / spiritu a extra featů. */
    fun slots(classId: String, level: Int): Int = when (classId) {
        "witch" -> 1 + level / 2
        "shaman" -> SHAMAN_HEX_LEVELS.count { it <= level }
        "oracle" -> REVELATION_LEVELS.count { it <= level }
        else -> 0
    }

    /** Revelation patří k mystériu orákula (nebo k žádnému konkrétnímu); bez zvoleného mystéria vše. */
    fun matchesMystery(a: ClassAbility, mystery: String?): Boolean =
        a.kind != AbilityKind.REVELATION || mystery == null || a.mysteries.isEmpty() || mystery.lowercase() in a.mysteries
}
