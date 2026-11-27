package cz.pfspells.data

import cz.pfspells.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/*
 * Archetypy class z Archives of Nethys (tools/build_archetypes.py → Archetypes.json, načítá se líně).
 * Postava si u classy vybere archetypy (ClassEntry.archetypes); appka ukáže jejich text a nahrazené class features
 * a podle nich upraví schopnosti, podtřídu, sesílání a počty (ArchetypeEffects.kt).
 */

/** Archetyp classy; [replaces] = nahrazené class features tak, jak je uvádí AoN („Smite Evil“, „3rd-level Mercy“). */
@Serializable
data class Archetype(
    val id: String,
    val classId: String,
    val name: String,
    val replaces: List<String> = emptyList(),
    val summary: String = "",
    val pfs: Boolean = false,
    val source: String = "",
    val text: String = "",
    /** kouzla přidaná na spell list (z textu AoN, ověřená proti SpellList.json): spellId → spell level */
    val spells: Map<String, Int> = emptyMap(),
) {
    val searchName: String by lazy { name.lowercase() }
}

/** Vybraný archetyp u classy postavy – kopie jména a nahrazených schopností (funguje bez načtené databáze). */
@Serializable
data class ArchetypePick(
    val id: String,
    val name: String,
    val replaces: List<String> = emptyList(),
    /** kouzla, která archetyp přidává na spell list (spellId → spell level) */
    val spells: Map<String, Int> = emptyMap(),
)

fun Archetype.toPick() = ArchetypePick(id, name, replaces, spells)

class ArchetypeDatabase(val all: List<Archetype>) {
    private val byId = all.associateBy { it.id }
    private val byClass = all.groupBy { it.classId }
    fun get(id: String): Archetype? = byId[id]
    fun forClass(classId: String): List<Archetype> = byClass[Archetypes.baseClass(classId)].orEmpty()
}

object ArchetypeRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String) = ArchetypeDatabase(json.decodeFromString(ListSerializer(Archetype.serializer()), text))

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): ArchetypeDatabase = withContext(Dispatchers.Default) {
        parse(Res.readBytes("files/Archetypes.json").decodeToString())
    }
}

object Archetypes {
    /** Unchained classy používají archetypy základní classy (AoN je vede u ní). */
    fun baseClass(classId: String): String = classId.removeSuffix("_unchained")

    /** Classa může mít archetypy (NPC classy, prestižní classy a rasové HD tvorů je nemají). */
    fun supports(classId: String): Boolean = ClassCatalog.get(classId)?.let { !it.npc && !it.creature && it.prestige == null } == true

    private fun key(feature: String) = feature.trim().lowercase()

    /** Class features, které nahrazují dva vybrané archetypy zároveň (pravidla to nedovolují); jméno jako u prvního archetypu. */
    fun conflicts(picks: List<ArchetypePick>): List<String> {
        val all = picks.flatMap { p -> p.replaces.distinctBy(::key) }
        val twice = all.groupingBy(::key).eachCount().filter { it.value > 1 }.keys
        return all.filter { key(it) in twice }.distinctBy(::key).sortedBy(::key)
    }

    /** Class feature, kterou už nahrazuje jiný vybraný archetyp (přísný režim), nebo null. */
    fun blockedBy(e: ClassEntry, a: Archetype): String? {
        val taken = e.archetypes.flatMap { p -> p.replaces.map(::key) }.toSet()
        return a.replaces.firstOrNull { key(it) in taken }
    }
}
