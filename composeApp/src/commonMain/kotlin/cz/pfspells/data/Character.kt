package cz.pfspells.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

enum class Ability { STR, DEX, CON, INT, WIS, CHA }

/** Přesvědčení: osa zákon–chaos ([law] 1 / 0 / −1) a dobro–zlo ([good] 1 / 0 / −1). */
enum class Alignment(val law: Int, val good: Int) {
    LG(1, 1), NG(0, 1), CG(-1, 1),
    LN(1, 0), N(0, 0), CN(-1, 0),
    LE(1, -1), NE(0, -1), CE(-1, -1),
}

/** Atributy postavy (bonusové sloty, prerekvizity featů, nosnost). */
@Serializable
data class Abilities(
    val str: Int = 10,
    val dex: Int = 10,
    val con: Int = 10,
    val int: Int = 10,
    val wis: Int = 10,
    val cha: Int = 10,
) {
    operator fun get(a: Ability): Int = when (a) {
        Ability.STR -> str
        Ability.DEX -> dex
        Ability.CON -> con
        Ability.INT -> int
        Ability.WIS -> wis
        Ability.CHA -> cha
    }

    fun with(a: Ability, v: Int): Abilities = when (a) {
        Ability.STR -> copy(str = v)
        Ability.DEX -> copy(dex = v)
        Ability.CON -> copy(con = v)
        Ability.INT -> copy(int = v)
        Ability.WIS -> copy(wis = v)
        Ability.CHA -> copy(cha = v)
    }
}

/** Jedna classa postavy. Classa se v postavě vyskytuje nejvýš jednou (klíč = [classId]). */
@Serializable
data class ClassEntry(
    val classId: String,
    val level: Int,
    /** podtřída – klíče [SubclassSlot] („domain1“ → „fire“, „school“ → „evocation“ / „element:fire“…) */
    val subclass: Map<String, String> = emptyMap(),
    /** spellId → počet (připravující: kopie; spontánní / arcanist: známá, hodnota 1) */
    val spells: Map<String, Int> = emptyMap(),
    /** kouzla připravená do doménového slotu (cleric, druid): spellId → počet */
    val bonusSpells: Map<String, Int> = emptyMap(),
)

enum class ResourceUnit { PER_DAY, ROUNDS, POINTS }

/** Vlastní počítadlo postavy: např. „Ring of Feather Fall 3× za den“. Použití se počítá v `castToday` pod „res:<id>“. */
@Serializable
data class Tracker(
    val id: String,
    val name: String,
    val max: Int = 1,
    val unit: ResourceUnit = ResourceUnit.PER_DAY,
    val note: String = "",
)

/** Feat postavy; [featId] null = vlastní feat (jen [name] a [text]). */
@Serializable
data class FeatPick(
    val featId: String?,
    val name: String,
    val note: String = "",
    val text: String? = null,
)

/** Druh mazlíčka / společníka – postava s [Character.ownerId]. */
enum class CompanionKind { ANIMAL_COMPANION, MOUNT, FAMILIAR, EIDOLON, PET }

/** Rarita mazlíčka – jen štítek (v PF1 pravidla raritu nemají; pro vlastní kampaň). */
enum class CompanionRarity { COMMON, UNCOMMON, RARE, UNIQUE }

@Serializable
data class Character(
    val id: String,
    val name: String,
    /** multiclass; úroveň postavy = součet */
    val classes: List<ClassEntry>,
    /** základní hodnoty atributů (bez rasy) – výsledné jsou [scores] */
    val abilities: Abilities = Abilities(),
    val feats: List<FeatPick> = emptyList(),
    /** ručně přidané featy navíc (rasa human, traity…) */
    val extraFeats: Int = 0,
    /** propojený inventář */
    val inventoryId: String? = null,
    /** herní režim: kolik bylo dnes sesláno (klíče podle classy a spellu / slotu, počítadla „res:…“) */
    val castToday: Map<String, Int> = emptyMap(),
    /** vlastní počítadla (schopnosti, předměty… s počtem použití) */
    val trackers: List<Tracker> = emptyList(),
    /** skryté předvyplněné schopnosti class (id z [FeaturePresets]) */
    val hiddenPresets: Set<String> = emptySet(),
    /** rasa ([RaceCatalog] id, null = nevybraná) – úpravy atributů, rasové prerekvizity featů, velikost pro nosnost */
    val race: String? = null,
    /** atribut s rasovým +2 podle volby (human, half-elf, half-orc) */
    val raceBonus: Ability? = null,
    /** ranky ve skillech: klíč [Skills.key] („perception“, „knowledge.arcana“, „craft:alchemy“) → ranky */
    val skills: Map<String, Int> = emptyMap(),
    /** ručně přidané skill body navíc (oblíbená classa, traity…) */
    val extraSkillPoints: Int = 0,
    /** přesvědčení (null = nezadané) */
    val alignment: Alignment? = null,
    /** božstvo (volný text, prázdné = nezadané) */
    val deity: String = "",
    /** ručně potvrzené prerekvizity featů (text malými písmeny) – platí jako splněné */
    val confirmedPrereqs: Set<String> = emptySet(),
    /** mazlíček / společník: id postavy, které patří (null = samostatná postava) */
    val ownerId: String? = null,
    /** druh mazlíčka (jen u [ownerId] != null) */
    val companion: CompanionKind? = null,
    /** velikost tvora (mazlíček bez rasy) – přebije velikost rasy; nosnost propojeného inventáře */
    val size: CreatureSize? = null,
    /** čtyřnohý tvor (nosnost; jen s [size]) */
    val quadruped: Boolean = false,
    /** druh zvířete ([SpeciesCatalog] id) – startovní atributy, velikost, útoky */
    val species: String? = null,
    /** efektivní úroveň druida zvířecího společníka (null = bez progrese) – HD, Str/Dex bonus, přirozená zbroj, triky */
    val companionLevel: Int? = null,
    /** rarita mazlíčka (štítek, null = nezadaná) */
    val rarity: CompanionRarity? = null,
    /** vlastní pravidla pro návrat / náhradu po smrti (null = výchozí text podle druhu – `Strings.defaultRespawn`) */
    val respawnRules: String? = null,
    /** bonusy nasazeného vybavení z propojeného inventáře (kopie – drží ji AppState při změně inventáře / propojení) */
    val gear: List<ItemBonus> = emptyList(),
) {
    val isCompanion: Boolean get() = ownerId != null

    val level: Int get() = classes.sumOf { it.level }
    val primary: ClassEntry? get() = classes.firstOrNull()

    fun classEntry(classId: String?): ClassEntry? =
        if (classId == null) primary else classes.firstOrNull { it.classId == classId }

    fun updateClass(classId: String, transform: (ClassEntry) -> ClassEntry): Character =
        copy(classes = classes.map { if (it.classId == classId) transform(it) else it })

    /** Herní režim: kolik bylo dnes sesláno pod klíčem [castKey] / [slotKey]. */
    fun cast(key: String): Int = castToday[key] ?: 0

    fun withCast(key: String, n: Int): Character =
        copy(castToday = if (n <= 0) castToday - key else castToday + (key to n))

    companion object {
        /** připravující classa: seslané kopie konkrétního spellu */
        fun castKey(classId: String, spellId: String) = "$classId:$spellId"

        /** spontánní classa / arcanist: použité sloty spell levelu (velké L – id spellů jsou malými písmeny) */
        fun slotKey(classId: String, spellLevel: Int) = "$classId:L$spellLevel"
    }
}

/**
 * Spellbook jedné classy postavy – pohled pro obrazovky kouzel (výběr, detail).
 * [id] je id postavy, [classId] určuje classu.
 */
data class Spellbook(val character: Character, val entry: ClassEntry) {
    val id: String get() = character.id
    val name: String get() = character.name
    val classId: String get() = entry.classId
    val classLevel: Int get() = entry.level
    val spells: Map<String, Int> get() = entry.spells
}

/** Postava jako deck starého exportního formátu (hlavní classa) – do fáze nového exportu. */
fun Character.toDeck(): Deck? = primary?.let { Deck(id, name, it.classId, it.level, it.spells) }

/** Převod starého decku (jedna classa) na postavu. */
fun Deck.toCharacter(): Character = Character(id, name, listOf(ClassEntry(classId, classLevel, spells = spells)))

/**
 * Postavy v `Settings` (`characters_v1`). Při prvním spuštění nové verze se převedou decky z `decks_v1`
 * (starý klíč zůstane jako záloha a dál se nepoužívá).
 */
class CharacterStore(private val settings: Settings = Settings()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Character.serializer())

    fun load(): List<Character> {
        val raw = settings.getStringOrNull(KEY)
        if (raw == null) {
            val migrated = DeckStore(settings).load().map { it.toCharacter() }
            save(migrated)
            return migrated
        }
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Character>) = settings.putString(KEY, json.encodeToString(serializer, list))

    private companion object {
        const val KEY = "characters_v1"
    }
}
