package cz.pfspells.data

import cz.pfspells.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/*
 * Rasové traity z Archives of Nethys (tools/build_racial_traits.py):
 * - výchozí traity ras → RacialTraitTable.kt (RACE_DEFAULT_TRAITS),
 * - alternativní traity s tím, co nahrazují → RacialTraits.json (líně, s textem).
 * Postava si alternativní traity vybírá (Character.racialTraits); nahrazené výchozí traity pak nemá –
 * projeví se v prerekvizitách featů, bonusovém featu (human, half-elf) a skill bodech (human Skilled).
 */

/** Alternativní rasový trait; [replaces] = výchozí traity, které nahrazuje (malými písmeny, jako na AoN). */
@Serializable
data class RacialTrait(
    val id: String,
    val races: List<String>,
    val name: String,
    val replaces: List<String> = emptyList(),
    val pfs: Boolean = false,
    val source: String = "",
    val text: String = "",
) {
    val searchName: String by lazy { name.lowercase() }
}

/** Vybraný alternativní trait u postavy – kopie jména a nahrazených traitů (funguje bez načtené databáze). */
@Serializable
data class RacialTraitPick(
    val id: String,
    val name: String,
    val replaces: List<String> = emptyList(),
    /** zvolené atributy pro [AbilityVariant.choices] (pořadí jako volby; null = nezvoleno) */
    val abilities: List<Ability?> = emptyList(),
)

/** Volba atributu u traitu: [amount] k jednomu z [options]. */
class AbilityChoice(val amount: Int, val options: List<Ability>)

/**
 * Alternativní trait, který mění rasové úpravy atributů: [fixed] se přičte k úpravám rasy, [choices] zvolí hráč.
 * [replacesChoice] = nahrazuje „+2 k jednomu atributu“ (human, half-elf, half-orc – `Character.raceBonus` pak neplatí).
 */
class AbilityVariant(val fixed: Map<Ability, Int>, val choices: List<AbilityChoice> = emptyList(), val replacesChoice: Boolean = false)

/** Alternativní traity měnící atributy (texty AoN, zadané ručně – je jich pár a text nejde spolehlivě strojově číst). */
private val ABILITY_VARIANTS: Map<String, AbilityVariant> = mapOf(
    // „+2 Intelligence and Charisma instead of Wisdom and Charisma“
    "changeling:witchborn" to AbilityVariant(mapOf(Ability.WIS to -2, Ability.INT to 2)),
    // „+2 Dexterity and Intelligence instead of Dexterity and Charisma“
    "kitsune:keen-kitsune" to AbilityVariant(mapOf(Ability.CHA to -2, Ability.INT to 2)),
    // „Replace the leshy's +2 racial bonus to Constitution with a +2 racial bonus to Dexterity“
    "vine-leshy:agile" to AbilityVariant(mapOf(Ability.CON to -2, Ability.DEX to 2)),
    // „no Constitution penalty“
    "gathlain:tree-born" to AbilityVariant(mapOf(Ability.CON to 2)),
    // „pick two ability scores and gain a +2 racial bonus in each“
    "human:dual-talent" to AbilityVariant(emptyMap(), listOf(AbilityChoice(2, Ability.entries), AbilityChoice(2, Ability.entries)), replacesChoice = true),
    // „+2 bonus to Charisma and one other ability score of their choice“
    "half-elf:kindred-raised" to AbilityVariant(
        mapOf(Ability.CHA to 2), listOf(AbilityChoice(2, Ability.entries - Ability.CHA)), replacesChoice = true,
    ),
    // „+2 bonus to Strength and a −2 penalty to one mental ability score of their choice“
    "half-orc:orc-atavism" to AbilityVariant(
        mapOf(Ability.STR to 2), listOf(AbilityChoice(-2, listOf(Ability.INT, Ability.WIS, Ability.CHA))), replacesChoice = true,
    ),
)

fun RacialTrait.toPick() = RacialTraitPick(id, name, replaces)

class RacialTraitDatabase(val all: List<RacialTrait>) {
    private val byId = all.associateBy { it.id }
    fun get(id: String): RacialTrait? = byId[id]
    fun forRace(raceId: String): List<RacialTrait> = all.filter { raceId in it.races }
}

object RacialTraitRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String) = RacialTraitDatabase(json.decodeFromString(ListSerializer(RacialTrait.serializer()), text))

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): RacialTraitDatabase = withContext(Dispatchers.Default) {
        parse(Res.readBytes("files/RacialTraits.json").decodeToString())
    }
}

object RacialTraits {
    /** Úprava atributů alternativním traitem (null = trait atributy nemění). */
    fun abilityVariant(traitId: String): AbilityVariant? = ABILITY_VARIANTS[traitId]

    /** Vybraný trait nahrazuje rasové „+2 k jednomu atributu“ – volba `raceBonus` se nepoužije. */
    fun replacesRaceBonus(c: Character): Boolean = c.racialTraits.any { abilityVariant(it.id)?.replacesChoice == true }

    /** Vybrané traity s nezvoleným atributem. */
    fun missingChoices(c: Character): List<RacialTraitPick> = c.racialTraits.filter { p ->
        val v = abilityVariant(p.id) ?: return@filter false
        v.choices.indices.any { p.abilities.getOrNull(it) == null }
    }

    /** Úprava atributu [a] z alternativních traitů postavy (pevná + zvolená). */
    fun abilityMod(c: Character, a: Ability): Int = c.racialTraits.sumOf { p ->
        val v = abilityVariant(p.id) ?: return@sumOf 0
        (v.fixed[a] ?: 0) + v.choices.withIndex().sumOf { (i, ch) -> if (p.abilities.getOrNull(i) == a && a in ch.options) ch.amount else 0 }
    }

    /** Výchozí traity rasy podle AoN (null = rasa na AoN stránku nemá). */
    fun defaults(raceId: String?): List<String>? = raceId?.let { RACE_DEFAULT_TRAITS[it] }

    /** Výchozí traity nahrazené vybranými alternativními traity (malými písmeny). */
    fun replaced(c: Character): Set<String> = c.racialTraits.flatMap { it.replaces }.toSet()

    /** Výchozí traity, které nahrazují dva vybrané alternativní traity zároveň (to pravidla nedovolují). */
    fun conflicts(picks: List<RacialTraitPick>): List<String> =
        picks.flatMap { it.replaces }.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.sorted()

    /** Trait, který by nahradil už nahrazený výchozí trait (přísný režim), nebo null. */
    fun blockedBy(c: Character, t: RacialTrait): String? = t.replaces.firstOrNull { it in replaced(c) }
}

/** Postava má výchozí rasový trait [name] (rasa ho má a není nahrazený alternativním). */
fun Character.keepsRacialTrait(name: String): Boolean = name.lowercase() !in RacialTraits.replaced(this)
