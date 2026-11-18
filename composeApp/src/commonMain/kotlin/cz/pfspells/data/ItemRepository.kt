package cz.pfspells.data

import com.russhwolf.settings.Settings
import cz.pfspells.resources.Res
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.random.Random

/**
 * Databáze itemů: běžné vybavení je vždy přibalené (ItemsBase.json),
 * zbraně, munice a magické itemy se berou ze stažené verze, jinak z přibalené.
 */
class ItemRepository(private val settings: Settings = Settings()) {

    private enum class Part(val cacheFile: String, val bundled: String, val url: String) {
        WEAPONS("items_weapons.csv", "files/Weapons.csv", ItemParser.WEAPONS_URL),
        AMMO("items_ammunition.csv", "files/Ammunition.csv", ItemParser.AMMO_URL),
        MAGIC("items_magic.csv", "files/MagicItems.csv", ItemParser.MAGIC_URL),
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun bundled(path: String) = decodeText(Res.readBytes(path))

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): ItemDatabase = withContext(Dispatchers.Default) {
        val base = ItemParser.parseBase(bundled("files/ItemsBase.json"))
        val cached = Part.entries.associateWith { AppFiles.readText(it.cacheFile) }
        val downloaded = cached.values.all { it != null }
        try {
            if (downloaded) {
                return@withContext ItemDatabase(
                    ItemParser.combine(base, *parseParts { cached[it]!! }.toTypedArray()),
                    SpellSource.DOWNLOADED,
                    settings.getStringOrNull(KEY_UPDATED),
                )
            }
        } catch (e: Exception) {
            Part.entries.forEach { AppFiles.delete(it.cacheFile) }
        }
        val texts = Part.entries.associateWith { bundled(it.bundled) }
        ItemDatabase(ItemParser.combine(base, *parseParts { texts[it]!! }.toTypedArray()), SpellSource.BUNDLED, null)
    }

    private fun parseParts(text: (Part) -> String): List<List<Item>> = listOf(
        ItemParser.parseWeapons(text(Part.WEAPONS), ammo = false),
        ItemParser.parseWeapons(text(Part.AMMO), ammo = true),
        ItemParser.parseMagic(text(Part.MAGIC)),
    )

    /** Stáhne aktuální tabulky, ověří je a teprve pak uloží. */
    suspend fun download(timestamp: String): ItemDatabase {
        val client = createHttpClient()
        val texts = try {
            Part.entries.associateWith { part ->
                val resp = client.get(part.url)
                if (!resp.status.isSuccess()) error("Server returned ${resp.status.value} for ${part.name.lowercase()}")
                decodeText(resp.body<ByteArray>())
            }
        } finally {
            client.close()
        }
        return withContext(Dispatchers.Default) {
            parseParts { texts[it]!! } // validace – při chybě se nic nepřepíše
            Part.entries.forEach { AppFiles.writeText(it.cacheFile, texts[it]!!) }
            settings.putString(KEY_UPDATED, timestamp)
            load()
        }
    }

    suspend fun resetToBundled(): ItemDatabase {
        Part.entries.forEach { AppFiles.delete(it.cacheFile) }
        settings.remove(KEY_UPDATED)
        return load()
    }

    private companion object {
        const val KEY_UPDATED = "items_updated_at"
    }
}

class InventoryStore(private val settings: Settings = Settings()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Inventory.serializer())

    fun load(): List<Inventory> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        return try { json.decodeFromString(serializer, raw) } catch (e: Exception) { emptyList() }
    }

    fun save(list: List<Inventory>) = settings.putString(KEY, json.encodeToString(serializer, list))

    private companion object {
        const val KEY = "inventories_v1"
    }
}

// ---------------- export / import inventářů ----------------

@Serializable
data class InventoryFile(
    val format: String = InventoryTransfer.FORMAT,
    val version: Int = 1,
    val inventories: List<InventoryDto>,
)

@Serializable
data class InventoryDto(
    val name: String,
    val strength: Int = 10,
    val coins: Coins = Coins(),
    val notes: String = "",
    val items: List<InvEntryDto> = emptyList(),
    val size: String = CreatureSize.MEDIUM.name,
    val quadruped: Boolean = false,
)

@Serializable
data class Coins(val pp: Int = 0, val gp: Int = 0, val sp: Int = 0, val cp: Int = 0)

@Serializable
data class InvEntryDto(
    val name: String,
    @SerialName("item") val itemId: String? = null,
    val category: String = ItemCategory.OTHER.name,
    val qty: Int = 1,
    val price: Double? = null,
    val weight: Double? = null,
    val note: String = "",
    val info: String? = null,
)

object InventoryTransfer {
    const val FORMAT = "pf-spellbook-inventories"
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    fun export(list: List<Inventory>): String = json.encodeToString(
        InventoryFile.serializer(),
        InventoryFile(inventories = list.map(::toDto)),
    )

    fun toDto(inv: Inventory) = InventoryDto(
        name = inv.name, strength = inv.strength, coins = Coins(inv.pp, inv.gp, inv.sp, inv.cp), notes = inv.notes,
        size = inv.size, quadruped = inv.quadruped,
        items = inv.entries.map { InvEntryDto(it.name, it.itemId, it.category, it.qty, it.priceGp, it.weightLb, it.note, it.info) },
    )

    /** Inventář z exportu (bez id); itemy se párují s databází podle id, pak jména. */
    fun fromDto(dto: InventoryDto, db: ItemDatabase?) = Inventory(
        id = "",
        name = dto.name.trim().ifEmpty { "Imported inventory" },
        strength = dto.strength.coerceIn(1, 60),
        pp = dto.coins.pp.coerceAtLeast(0), gp = dto.coins.gp.coerceAtLeast(0),
        sp = dto.coins.sp.coerceAtLeast(0), cp = dto.coins.cp.coerceAtLeast(0),
        notes = dto.notes,
        size = CreatureSize.of(dto.size).name,
        quadruped = dto.quadruped,
        entries = dto.items.filter { it.qty > 0 && it.name.isNotBlank() }.map { e ->
            val linked = e.itemId?.let { db?.get(it) } ?: db?.findByName(e.name)
            InvEntry(
                id = newEntryId(), itemId = linked?.id ?: e.itemId, name = e.name, category = e.category,
                qty = e.qty.coerceAtMost(9999), priceGp = e.price, weightLb = e.weight, note = e.note, info = e.info,
            )
        },
    )

    /** @throws DeckImportException */
    fun parse(text: String, db: ItemDatabase?): List<Inventory> {
        val t = text.trim()
        if (t.isEmpty()) throw DeckImportException(ImportError.Empty)
        val file = try {
            if (t.contains("\"inventories\"")) json.decodeFromString(InventoryFile.serializer(), t)
            else InventoryFile(inventories = listOf(json.decodeFromString(InventoryDto.serializer(), t)))
        } catch (e: Exception) {
            throw DeckImportException(ImportError.Invalid(inventory = true, e.message?.lineSequence()?.firstOrNull()))
        }
        if (file.format != FORMAT) throw DeckImportException(ImportError.UnknownFormat(file.format))
        if (file.version > 1) throw DeckImportException(ImportError.NewerVersion(file.version))
        if (file.inventories.isEmpty()) throw DeckImportException(ImportError.NoInventories)
        return file.inventories.map { fromDto(it, db) }
    }

    fun fileName(base: String): String = (SpellParser.slug(base).ifEmpty { "inventories" }) + ".pfinv.json"
}

fun newEntryId(): String = Random.nextLong().toULong().toString(36)
