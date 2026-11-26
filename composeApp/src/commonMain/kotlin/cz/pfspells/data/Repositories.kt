package cz.pfspells.data

import com.russhwolf.settings.Settings
import cz.pfspells.resources.Res
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

enum class SpellSource { BUNDLED, DOWNLOADED }

class SpellDatabase(val spells: List<Spell>, val source: SpellSource, val updatedAt: String?) {
    private val byId = spells.associateBy { it.id }
    private val byName by lazy { spells.associateBy { it.searchName } }
    fun get(id: String): Spell? = byId[id]
    fun findByName(name: String): Spell? = byName[name.trim().lowercase()]
    /** domény, mystéria, bloodline, elementární školy, patroni, spirity, disciplíny → spelly */
    val subclasses: SubclassIndex by lazy { SubclassIndex(spells) }
}

/**
 * Spelly: přednostně stažená verze (soubor v úložišti appky), jinak data přibalená v resources.
 */
class SpellRepository(private val settings: Settings = Settings()) {

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): SpellDatabase = withContext(Dispatchers.Default) {
        val cached = AppFiles.readText(CACHE_FILE)
        if (cached != null) {
            try {
                return@withContext SpellDatabase(
                    SpellParser.parse(cached), SpellSource.DOWNLOADED, settings.getStringOrNull(KEY_UPDATED)
                )
            } catch (e: Exception) {
                AppFiles.delete(CACHE_FILE) // poškozený soubor → zpět na přibalená data
            }
        }
        val bundled = Res.readBytes("files/SpellList.json").decodeToString()
        SpellDatabase(SpellParser.parse(bundled), SpellSource.BUNDLED, null)
    }

    /** Stáhne aktuální databázi, ověří ji a uloží. [timestamp] je jen popisek pro UI. */
    suspend fun download(timestamp: String): SpellDatabase {
        val client = createHttpClient()
        val text = try {
            val resp: HttpResponse = client.get(SpellParser.SOURCE_URL)
            if (!resp.status.isSuccess()) error("Server returned ${resp.status.value}")
            resp.bodyAsText()
        } finally {
            client.close()
        }
        return withContext(Dispatchers.Default) {
            val spells = SpellParser.parse(text) // když neprojde, nic se nepřepíše
            AppFiles.writeText(CACHE_FILE, text)
            settings.putString(KEY_UPDATED, timestamp)
            SpellDatabase(spells, SpellSource.DOWNLOADED, timestamp)
        }
    }

    /** Smaže staženou verzi a vrátí přibalená data. */
    suspend fun resetToBundled(): SpellDatabase {
        AppFiles.delete(CACHE_FILE)
        settings.remove(KEY_UPDATED)
        return load()
    }

    private companion object {
        const val CACHE_FILE = "SpellList.json"
        const val KEY_UPDATED = "spells_updated_at"
    }
}

class DeckStore(private val settings: Settings = Settings()) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Deck.serializer())

    fun load(): List<Deck> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(decks: List<Deck>) {
        settings.putString(KEY, json.encodeToString(serializer, decks))
    }

    private companion object {
        const val KEY = "decks_v1"
    }
}
