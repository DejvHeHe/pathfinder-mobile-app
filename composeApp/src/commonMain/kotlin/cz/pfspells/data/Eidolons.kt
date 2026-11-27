package cz.pfspells.data

import cz.pfspells.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

/*
 * Eidolon summonera / unchained summonera (d20pfsrd, tools/build_eidolons.py → EidolonTable.kt + Eidolons.json):
 * - progrese podle úrovně summonera (`Character.companionLevel` u eidolona): HD, BAB, záchrany, zbroj, Str/Dex bonus,
 *   body evolucí, max. útoků, schopnosti – tabulka podle formy (klasická / unchained „u_…“),
 * - základní forma: velikost, rychlost, přirozená zbroj, dobré záchrany, útok, startovní atributy, volné evoluce,
 * - evoluce (`Character.evolutions`) s cenou v bodech; mechanicky: Ability Increase (+2 k atributu),
 *   Improved Natural Armor (+2 přirozené zbroje), ostatní jen text.
 */

class EidolonRow(
    val level: Int,
    val hd: Int,
    val bab: Int,
    val goodSave: Int,
    val poorSave: Int,
    val skills: Int,
    val feats: Int,
    val armor: Int,
    val strDex: Int,
    val evolutionPool: Int,
    val maxAttacks: Int,
    val special: String,
)

class EidolonForm(
    val id: String,
    val name: String,
    val unchained: Boolean,
    val size: CreatureSize,
    val speed: String,
    val naturalArmor: Int,
    val goodSaves: Set<BonusTarget>,
    val attack: String,
    val abilities: Abilities,
    val freeEvolutions: String = "",
)

/** Vybraná evoluce eidolona – kopie jména a ceny; [ability] u Ability Increase. */
@Serializable
data class EvolutionPick(
    val id: String,
    val name: String,
    val cost: Int,
    val ability: Ability? = null,
    val note: String = "",
)

@Serializable
data class Evolution(val id: String, val name: String, val cost: Int, val text: String = "")

@Serializable
data class EidolonSubtype(val id: String, val name: String, val text: String = "")

@Serializable
data class EidolonData(val evolutions: List<Evolution> = emptyList(), val subtypes: List<EidolonSubtype> = emptyList())

object EidolonRepository {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): EidolonData = json.decodeFromString(EidolonData.serializer(), text)

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): EidolonData = withContext(Dispatchers.Default) {
        parse(Res.readBytes("files/Eidolons.json").decodeToString())
    }
}

object Eidolons {
    val forms: List<EidolonForm> = EIDOLON_FORMS
    private val byId = forms.associateBy { it.id }

    fun form(id: String?): EidolonForm? = id?.let { byId[it] }

    fun row(level: Int, unchained: Boolean): EidolonRow =
        (if (unchained) EIDOLON_PROGRESSION_UNCHAINED else EIDOLON_PROGRESSION)[level.coerceIn(1, 20) - 1]

    const val ABILITY_INCREASE = "ability_increase"
    const val IMPROVED_NATURAL_ARMOR = "improved_natural_armor"
}

val Character.isEidolon: Boolean get() = companion == CompanionKind.EIDOLON

val Character.eidolonFormInfo: EidolonForm? get() = if (isEidolon) Eidolons.form(eidolonForm) else null

/** Řádek progrese eidolona (null = bez progrese). Unchained tabulka podle formy („u_…“). */
val Character.eidolonRow: EidolonRow?
    get() = if (!isEidolon) null else companionLevel?.let { Eidolons.row(it, eidolonFormInfo?.unchained == true) }

/** Body evolucí: použité / k dispozici (null = bez progrese). */
val Character.evolutionPointsUsed: Int get() = evolutions.sumOf { it.cost }

/** Úprava atributu eidolona: Str/Dex bonus z tabulky + Ability Increase (+2). */
fun Character.eidolonMod(a: Ability): Int {
    if (!isEidolon) return 0
    val row = eidolonRow
    val table = if (row != null && (a == Ability.STR || a == Ability.DEX)) row.strDex else 0
    return table + 2 * evolutions.count { it.id == Eidolons.ABILITY_INCREASE && it.ability == a }
}

/** Přirozená zbroj eidolona: forma + zbroj z tabulky + Improved Natural Armor (+2 za každou). */
val Character.eidolonNaturalArmor: Int?
    get() {
        if (!isEidolon) return null
        val form = eidolonFormInfo
        val row = eidolonRow
        if (form == null && row == null) return null
        return (form?.naturalArmor ?: 0) + (row?.armor ?: 0) + 2 * evolutions.count { it.id == Eidolons.IMPROVED_NATURAL_ARMOR }
    }

/** Úroveň summonera vlastníka (klasický i unchained). */
fun Character.suggestedEidolonLevel(): Int =
    classes.filter { it.classId == "summoner" || it.classId == "summoner_unchained" }.sumOf { it.level }.coerceIn(1, 20)

/** Vlastník je unchained summoner → nabízet unchained formy. */
val Character.hasUnchainedSummoner: Boolean get() = classes.any { it.classId == "summoner_unchained" }
