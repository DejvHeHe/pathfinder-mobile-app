package cz.pfspells

import cz.pfspells.data.Tracker
import cz.pfspells.data.trackEvent
import cz.pfspells.data.CompanionKind
import cz.pfspells.data.CreatureSize
import cz.pfspells.data.StrictBlock
import cz.pfspells.data.StrictRules
import cz.pfspells.data.creatureSize
import cz.pfspells.data.withCompanion
import cz.pfspells.data.gearBonuses
import cz.pfspells.data.ItemSlot
import cz.pfspells.data.ItemBonusGuess
import cz.pfspells.data.abilityBonus
import cz.pfspells.data.ResourceUnit
import cz.pfspells.data.withSubclass
import cz.pfspells.data.RaceCatalog
import cz.pfspells.data.raceInfo
import cz.pfspells.data.raceMod
import cz.pfspells.data.scores
import cz.pfspells.data.ImportedCharacter
import cz.pfspells.data.FeatRepository
import cz.pfspells.data.FeatPick
import cz.pfspells.data.Feat
import cz.pfspells.data.FeatDatabase
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Ability
import cz.pfspells.data.Spellbook
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.CharacterStore
import cz.pfspells.data.Character
import cz.pfspells.data.isCustom
import cz.pfspells.data.CUSTOM_ITEM_PREFIX
import cz.pfspells.data.CustomItemStore
import cz.pfspells.data.CustomItem
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Inventory
import cz.pfspells.data.InventoryStore
import cz.pfspells.data.Item
import cz.pfspells.data.ItemCategory
import cz.pfspells.data.ItemDatabase
import cz.pfspells.data.ItemRepository
import cz.pfspells.data.newEntryId
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SpellRepository
import cz.pfspells.data.nowDisplayText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.russhwolf.settings.Settings

sealed interface Screen {
    /** kořen: seznam postav / inventářů podle sekce */
    data object Decks : Screen
    data object Compendium : Screen
    data object Database : Screen
    data object About : Screen
    data class CharacterDetail(val charId: String) : Screen
    /** výběr spellů pro classu [classId] postavy (null = hlavní classa) */
    data class Picker(val charId: String, val classId: String? = null) : Screen
    data class SpellDetail(val spellId: String, val charId: String? = null, val classId: String? = null) : Screen
    /** výběr featů pro postavu */
    data class FeatPicker(val charId: String) : Screen
    /** Detail featu z databáze ([featId]) a/nebo featu postavy (index [pick] v `Character.feats`). */
    data class FeatDetail(val featId: String?, val charId: String? = null, val pick: Int? = null) : Screen

    // inventář
    data class InventoryDetail(val invId: String) : Screen
    data class ItemPicker(val invId: String) : Screen
    /** Detail itemu z databáze ([itemId]) a/nebo položky inventáře ([entryId]). */
    data class ItemDetail(val itemId: String?, val invId: String?, val entryId: String? = null) : Screen

    /** Detail zobrazený na širokém okně v pravém panelu. */
    val isAside: Boolean get() = this is SpellDetail || this is ItemDetail || this is FeatDetail
}

enum class Section { SPELLBOOK, INVENTORY }

data class ItemFilter(
    val query: String = "",
    val category: ItemCategory? = null,
    /** jen pro zbraně: Simple / Martial / Exotic; prázdné = vše */
    val proficiency: Set<String> = emptySet(),
    val maxPrice: Double? = null,
    /** jen vlastní katalog („My items“) */
    val mine: Boolean = false,
)

sealed interface ItemDbState {
    data object NotLoaded : ItemDbState
    data object Loading : ItemDbState
    data class Ready(val db: ItemDatabase) : ItemDbState
    data class Failed(val message: String) : ItemDbState
}

data class FeatFilter(
    val query: String = "",
    /** null = všechny typy */
    val type: String? = null,
    /** skrýt featy s nesplněnou prerekvizitou */
    val onlyMet: Boolean = false,
    val pfsOnly: Boolean = false,
    /** prohledávat i shrnutí a text */
    val fullText: Boolean = false,
)

sealed interface FeatDbState {
    data object NotLoaded : FeatDbState
    data object Loading : FeatDbState
    data class Ready(val db: FeatDatabase) : FeatDbState
    data class Failed(val message: String) : FeatDbState
}

data class BrowserFilter(
    val query: String = "",
    val fullText: Boolean = false,
    /** null = všechny levely */
    val level: Int? = null,
    /** null = všechny školy */
    val school: String? = null,
    /** jen v kompendiu; null = všechny classy */
    val classId: String? = null,
    /** jen v pickeru: skrýt spelly, které už postava má */
    val hideInDeck: Boolean = false,
    /** jen spelly povolené v Pathfinder Society */
    val pfsOnly: Boolean = false,
    /** jen oblíbené spelly */
    val favoritesOnly: Boolean = false,
    val sort: SpellSort = SpellSort.NAME,
)

enum class SpellSort { NAME, LEVEL, SCHOOL }

/** Hláška pro uživatele; text se skládá až při vykreslení, takže reaguje na přepnutí jazyka. */
class UiMessage(val isError: Boolean = false, val text: (Strings) -> String)

sealed interface DbState {
    data object Loading : DbState
    data class Ready(val db: SpellDatabase) : DbState
    data class Failed(val message: String) : DbState
}

class AppState(
    private val spellRepo: SpellRepository = SpellRepository(),
    private val itemRepo: ItemRepository = ItemRepository(),
    private val inventoryStore: InventoryStore = InventoryStore(),
    private val settings: Settings = Settings(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ---- vlastní katalog itemů (sdílený mezi inventáři) ----
    private val customStore = CustomItemStore(settings)
    val customItems = mutableStateListOf<CustomItem>().apply { addAll(customStore.load()) }

    /** Přidá do katalogu, nebo nahradí položku se stejným id. */
    fun saveCustomItem(item: CustomItem) {
        val i = customItems.indexOfFirst { it.id == item.id }
        if (i >= 0) customItems[i] = item else customItems.add(item)
        customStore.save(customItems.toList())
    }

    fun deleteCustomItem(catalogId: String) {
        customItems.removeAll { it.id == catalogId }
        customStore.save(customItems.toList())
    }

    /** Item z databáze nebo z vlastního katalogu (id s prefixem `my-`). */
    fun findItem(itemId: String): Item? =
        if (itemId.startsWith(CUSTOM_ITEM_PREFIX)) customItems.firstOrNull { CUSTOM_ITEM_PREFIX + it.id == itemId }?.toItem()
        else itemDb?.get(itemId)

    // ---- oblíbené spelly (společné pro všechny postavy; id oddělená čárkou – id jsou slugy bez čárek) ----
    var favorites: Set<String> by mutableStateOf(
        settings.getString(FAVORITES_KEY, "").split(',').filter { it.isNotEmpty() }.toSet(),
    )
        private set

    fun isFavorite(spellId: String) = spellId in favorites

    fun toggleFavorite(spellId: String) {
        favorites = if (spellId in favorites) favorites - spellId else favorites + spellId
        settings.putString(FAVORITES_KEY, favorites.joinToString(","))
    }

    // ---- jazyk rozhraní (uložený; výchozí podle systému) ----
    var lang: Lang by mutableStateOf(Lang.of(settings.getStringOrNull(LANG_KEY)) ?: Lang.system())
        private set
    val strings: Strings get() = lang.strings

    fun changeLang(l: Lang) {
        if (l != lang) trackEvent("language", mapOf("lang" to l.code))
        lang = l
        settings.putString(LANG_KEY, l.code)
    }

    /** Přísný režim: nejde přidat víc kouzel, než classa na spell levelu smí, ani feat nad počet / s nesplněnou prerekvizitou. */
    var strictMode by mutableStateOf(settings.getBoolean(STRICT_KEY, false))
        private set

    fun changeStrictMode(on: Boolean) {
        if (on == strictMode) return
        strictMode = on
        settings.putBoolean(STRICT_KEY, on)
        trackEvent("strict-mode", mapOf("on" to on.toString()))
    }

    /** V přísném režimu ohlásí důvod a vrátí true (akce se neprovede). */
    private fun strictBlocked(block: StrictBlock?): Boolean {
        if (!strictMode || block == null) return false
        showToast(UiMessage(isError = true) { it.strictBlocked(block) })
        return true
    }

    /** Hlavní sekce appky (záložky Spellbook / Inventory). */
    var section by mutableStateOf(Section.SPELLBOOK)

    fun switchSection(s: Section) {
        if (s != section) trackEvent("section", mapOf("section" to s.name.lowercase()))
        section = s
        openRoot(Screen.Decks)
        if (s == Section.INVENTORY) ensureItems()
    }

    // ---- databáze itemů (načítá se líně, až při prvním otevření inventáře) ----
    var items: ItemDbState by mutableStateOf(ItemDbState.NotLoaded)
        private set
    var itemsDownloading by mutableStateOf(false)
        private set
    var itemsMessage: UiMessage? by mutableStateOf(null)
        private set

    fun ensureItems() {
        if (items != ItemDbState.NotLoaded && items !is ItemDbState.Failed) return
        items = ItemDbState.Loading
        scope.launch {
            items = try {
                ItemDbState.Ready(itemRepo.load())
            } catch (e: Exception) {
                ItemDbState.Failed(e.message ?: e.toString())
            }
        }
    }

    val itemDb: ItemDatabase? get() = (items as? ItemDbState.Ready)?.db

    fun downloadItems() {
        if (itemsDownloading) return
        itemsDownloading = true
        itemsMessage = null
        scope.launch {
            try {
                val fresh = itemRepo.download(nowDisplayText())
                items = ItemDbState.Ready(fresh)
                val n = fresh.items.size
                itemsMessage = UiMessage { it.downloadedItems(n) }
                trackEvent("database-download", mapOf("db" to "items", "ok" to "true"))
            } catch (e: Exception) {
                trackEvent("database-download", mapOf("db" to "items", "ok" to "false"))
                val msg = e.message ?: e::class.simpleName
                itemsMessage = UiMessage(isError = true) { it.downloadFailed(msg) }
            } finally {
                itemsDownloading = false
            }
        }
    }

    fun resetItems() {
        scope.launch {
            trackEvent("database-reset", mapOf("db" to "items"))
            items = ItemDbState.Ready(itemRepo.resetToBundled())
            itemsMessage = UiMessage { it.bundledItemsRestored }
        }
    }

    // ---- skryté ceny: nad 200 gp se zobrazí „???“, 10 rychlých ťuknutí je odhalí (jen do zavření appky) ----
    var pricesRevealed by mutableStateOf(false)
        private set
    /** krátká hláška dole na obrazovce (odhalení cen, vyšší zatížení…) */
    var toast: UiMessage? by mutableStateOf(null)
        private set
    private var toastJob: kotlinx.coroutines.Job? = null

    fun showToast(msg: UiMessage) {
        toast = msg
        toastJob?.cancel()
        toastJob = scope.launch {
            kotlinx.coroutines.delay(2200)
            toast = null
        }
    }
    private var tapCount = 0
    private var lastTap = 0L

    /** Volá se při každém dotyku obrazovky (čas v ms). */
    fun registerTap(timeMs: Long) {
        tapCount = if (timeMs - lastTap <= 700) tapCount + 1 else 1
        lastTap = timeMs
        if (tapCount >= 10) {
            tapCount = 0
            pricesRevealed = !pricesRevealed
            val revealed = pricesRevealed
            if (revealed) trackEvent("prices-revealed")
            showToast(UiMessage { if (revealed) it.pricesRevealed else it.pricesHidden })
        }
    }

    // ---- inventáře ----
    val inventories = mutableStateListOf<Inventory>().apply { addAll(inventoryStore.load()) }

    fun inventory(id: String): Inventory? = inventories.firstOrNull { it.id == id }

    fun createInventory(name: String): Inventory {
        val inv = Inventory(newId(), name.ifBlank { strings.defaultInventoryName })
        inventories.add(inv)
        trackEvent("inventory-create")
        persistInventories()
        return inv
    }

    fun updateInventory(id: String, transform: (Inventory) -> Inventory) {
        val i = inventories.indexOfFirst { it.id == id }
        if (i < 0) return
        val before = inventories[i].load
        inventories[i] = transform(inventories[i])
        // nasazené vybavení → bonusy propojené postavy (a STR zpět do nosnosti, např. Belt of Giant Strength)
        characterOfInventory(id)?.let { owner ->
            val gear = inventories[i].gearBonuses()
            if (gear != owner.gear) {
                updateCharacter(owner.id) { it.copy(gear = gear) }
                val str = character(owner.id)?.scores?.str
                if (str != null && inventories[i].strength != str) inventories[i] = inventories[i].copy(strength = str)
            }
        }
        persistInventories()
        val after = inventories[i].load
        if (after > before) showToast(UiMessage(isError = true) { it.loadIncreased(after) })
    }

    fun deleteInventory(id: String) {
        inventories.removeAll { it.id == id }
        persistInventories()
        // odpojit od postavy (i s bonusy vybavení)
        characters.filter { it.inventoryId == id }.forEach { c -> updateCharacter(c.id) { it.copy(inventoryId = null, gear = emptyList()) } }
    }

    // ---- propojení postava ↔ inventář (STR pro nosnost se drží stejná na obou stranách) ----
    fun characterOfInventory(invId: String): Character? = characters.firstOrNull { it.inventoryId == invId }

    /** Propojí postavu s inventářem (null = odpojit). Inventář patří nejvýš jedné postavě; převezme její STR. */
    fun linkInventory(charId: String, invId: String?) {
        if (invId != null) {
            characters.filter { it.inventoryId == invId && it.id != charId }
                .forEach { o -> updateCharacter(o.id) { it.copy(inventoryId = null, gear = emptyList()) } }
        }
        val gear = invId?.let { inventory(it) }?.gearBonuses() ?: emptyList()
        updateCharacter(charId) { it.copy(inventoryId = invId, gear = gear) }
        val c = character(charId) ?: return
        if (invId != null) updateInventory(invId) { inv ->
            // velikost (mazlíček / rasa) → nosnost (jen když je známá)
            val size = c.creatureSize
            inv.copy(strength = c.scores.str, size = size?.name ?: inv.size, quadruped = if (size != null) c.size != null && c.quadruped else inv.quadruped)
        }
    }

    /** Nový inventář pojmenovaný po postavě, rovnou propojený. */
    fun createInventoryFor(charId: String): Inventory? {
        val c = character(charId) ?: return null
        val inv = createInventory(c.name)
        linkInventory(charId, inv.id)
        return inv
    }

    /** STR z inventáře – u propojeného inventáře se změní i atribut postavy (základ = výsledná STR − rasa). */
    fun setInventoryStrength(invId: String, value: Int) {
        updateInventory(invId) { it.copy(strength = value) }
        characterOfInventory(invId)?.let { c ->
            updateCharacter(c.id) { it.copy(abilities = it.abilities.with(Ability.STR, (value - it.abilityBonus(Ability.STR)).coerceIn(1, 99))) }
        }
    }

    fun importInventories(list: List<Inventory>) {
        trackEvent("import", mapOf("kind" to "inventories", "count" to list.size.toString()))
        for (inv in list) {
            var name = inv.name
            while (inventories.any { it.name == name }) name = "$name ${strings.importedSuffix}"
            inventories.add(inv.copy(id = newId(), name = name))
        }
        persistInventories()
    }

    /**
     * Přidá item z databáze nebo z vlastního katalogu; stejný item se stejnou poznámkou se jen přičte.
     * Item z katalogu se uloží jako vlastní položka (bez `itemId`) – inventář tak nezávisí na katalogu.
     */
    fun addItem(invId: String, item: Item, qty: Int = 1, note: String = "") = updateInventory(invId) { inv ->
        val dbId = item.id.takeUnless { item.isCustom }
        val existing = inv.entries.indexOfFirst { it.itemId == dbId && it.name == item.name && it.note == note.trim() }
        if (existing >= 0) {
            inv.copy(entries = inv.entries.mapIndexed { i, e -> if (i == existing) e.copy(qty = (e.qty + qty).coerceAtMost(9999)) else e })
        } else {
            inv.copy(entries = inv.entries + InvEntry(
                id = newEntryId(), itemId = dbId, name = item.name, category = item.category.name, qty = qty,
                priceGp = item.priceGp, weightLb = item.weightLb, note = note.trim(),
                info = if (item.isCustom) item.subtitle else item.summary,
                // slot z databáze a bonusy známých předmětů (čelenky, opasky, plášť odolnosti…) – nasadí se ručně
                slot = ItemSlot.parse(item.slot), bonuses = ItemBonusGuess.of(item.name),
            ))
        }
    }

    /** Nasadit / sundat položku (bonusy pak platí pro propojenou postavu). */
    fun setEquipped(invId: String, entryId: String, on: Boolean) = updateEntry(invId, entryId) { it.copy(equipped = on) }

    fun addCustomEntry(invId: String, entry: InvEntry) = updateInventory(invId) { it.copy(entries = it.entries + entry) }

    fun updateEntry(invId: String, entryId: String, transform: (InvEntry) -> InvEntry) = updateInventory(invId) { inv ->
        inv.copy(entries = inv.entries.map { if (it.id == entryId) transform(it) else it })
    }

    fun setEntryQty(invId: String, entryId: String, qty: Int) = updateInventory(invId) { inv ->
        if (qty <= 0) inv.copy(entries = inv.entries.filterNot { it.id == entryId })
        else inv.copy(entries = inv.entries.map { if (it.id == entryId) it.copy(qty = qty.coerceAtMost(9999)) else it })
    }

    fun removeEntry(invId: String, entryId: String) = setEntryQty(invId, entryId, 0)

    private fun persistInventories() = inventoryStore.save(inventories.toList())

    // filtry inventáře a pickeru itemů
    var inventoryCategory by mutableStateOf<ItemCategory?>(null)
    var inventoryQuery by mutableStateOf("")
    var itemFilter by mutableStateOf(ItemFilter())
    private var itemPickerInvId: String? = null
    var itemPickerList = LazyListState()
        private set

    private fun prepareItemPicker(invId: String) {
        ensureItems()
        if (itemPickerInvId != invId) {
            itemPickerInvId = invId
            itemFilter = ItemFilter()
            itemPickerList = LazyListState()
        }
    }

    // ---- navigace ----
    val stack = mutableStateListOf<Screen>(Screen.Decks)
    val current: Screen get() = stack.last()

    fun push(screen: Screen) {
        if (screen is Screen.Picker) preparePicker(screen.charId, screen.classId)
        if (screen is Screen.FeatPicker || screen is Screen.FeatDetail) ensureFeats()
        if (screen is Screen.ItemPicker) prepareItemPicker(screen.invId)
        if (screen == Screen.Compendium) trackEvent("compendium-open")
        stack.add(screen)
    }

    /** Detail spellu: když už je nějaký otevřený, nahradí ho (bez hromadění v historii). */
    fun showSpell(spellId: String, charId: String?, classId: String? = null) = showAside(Screen.SpellDetail(spellId, charId, classId))

    fun showItem(itemId: String?, invId: String?, entryId: String? = null) = showAside(Screen.ItemDetail(itemId, invId, entryId))

    fun showFeat(featId: String?, charId: String?, pick: Int? = null) {
        ensureFeats()
        showAside(Screen.FeatDetail(featId, charId, pick))
    }

    private fun showAside(screen: Screen) {
        if (current.isAside) stack[stack.lastIndex] = screen else stack.add(screen)
    }

    /** Otevře obrazovku jako novou „kořenovou“ nad seznamem postav (boční panel na širokém okně). */
    fun openRoot(screen: Screen) {
        if (screen is Screen.Picker) preparePicker(screen.charId, screen.classId)
        if (screen is Screen.FeatPicker) ensureFeats()
        if (screen is Screen.ItemPicker) prepareItemPicker(screen.invId)
        if (screen == Screen.Compendium && current != Screen.Compendium) trackEvent("compendium-open")
        when (screen) {
            is Screen.InventoryDetail, is Screen.ItemPicker -> section = Section.INVENTORY
            is Screen.CharacterDetail, is Screen.Picker, is Screen.FeatPicker, Screen.Compendium -> section = Section.SPELLBOOK
            else -> {}
        }
        stack.clear()
        stack.add(Screen.Decks)
        if (screen != Screen.Decks) stack.add(screen)
    }

    /** Zpět v hlavním sloupci širokého okna: zavře detail spellu a o krok vrátí hlavní obrazovku. */
    fun popMain() {
        while (stack.size > 1 && current.isAside) stack.removeAt(stack.lastIndex)
        pop()
    }

    /** @return false, když už není kam se vracet. */
    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    // ---- spelly ----
    var db: DbState by mutableStateOf(DbState.Loading)
        private set

    /** Načtená databáze kouzel (null = ještě se načítá / chyba) – i pro prerekvizity „ability to cast X“. */
    val spellDb: cz.pfspells.data.SpellDatabase? get() = (db as? DbState.Ready)?.db
    var downloading by mutableStateOf(false)
        private set
    var downloadMessage: UiMessage? by mutableStateOf(null)
        private set

    init {
        scope.launch {
            db = try {
                DbState.Ready(spellRepo.load())
            } catch (e: Exception) {
                DbState.Failed(e.message ?: e.toString())
            }
        }
    }

    fun downloadSpells() {
        if (downloading) return
        downloading = true
        downloadMessage = null
        scope.launch {
            try {
                val fresh = spellRepo.download(nowDisplayText())
                db = DbState.Ready(fresh)
                val n = fresh.spells.size
                downloadMessage = UiMessage { it.downloadedSpells(n) }
                trackEvent("database-download", mapOf("db" to "spells", "ok" to "true"))
            } catch (e: Exception) {
                trackEvent("database-download", mapOf("db" to "spells", "ok" to "false"))
                val msg = e.message ?: e::class.simpleName
                downloadMessage = UiMessage(isError = true) { it.downloadFailed(msg) }
            } finally {
                downloading = false
            }
        }
    }

    fun resetSpells() {
        scope.launch {
            trackEvent("database-reset", mapOf("db" to "spells"))
            db = DbState.Ready(spellRepo.resetToBundled())
            downloadMessage = UiMessage { it.bundledSpellsRestored }
        }
    }

    // ---- filtry prohlížeče (přežijí odchod na detail spellu a zpět) ----
    var compendiumFilter by mutableStateOf(BrowserFilter())
    val compendiumList = LazyListState()
    var pickerFilter by mutableStateOf(BrowserFilter())
    private var pickerKey: String? = null
    var pickerList = LazyListState()

    /** Při otevření pickeru pro jinou postavu / classu začít s čistými filtry. */
    fun preparePicker(charId: String, classId: String?) {
        val key = "$charId/${classId ?: character(charId)?.primary?.classId}"
        if (pickerKey != key) {
            pickerKey = key
            pickerFilter = BrowserFilter()
            pickerList = LazyListState()
        }
    }

    // ---- postavy ----
    private val characterStore = CharacterStore(settings)
    val characters = mutableStateListOf<Character>().apply { addAll(characterStore.load()) }

    fun character(id: String): Character? = characters.firstOrNull { it.id == id }

    /** Spellbook classy [classId] postavy (null = hlavní classa) – pro obrazovky kouzel. */
    fun spellbook(charId: String, classId: String? = null): Spellbook? {
        val c = character(charId) ?: return null
        val e = c.classEntry(classId) ?: return null
        return Spellbook(c, e)
    }

    /** Přidá importované postavy s novými id; při shodě jména připojí „(import)“. */
    /** Přidá importované postavy jako nové (jména se nepřepisují); přibalený inventář se přidá a propojí. */
    fun importCharacters(imported: List<ImportedCharacter>): Int {
        trackEvent("import", mapOf("kind" to "characters", "count" to imported.size.toString()))
        // nová id předem – mazlíček odkazuje na vlastníka pořadím v exportu
        val newIds = imported.map { newId() }
        for ((index, ic) in imported.withIndex()) {
            var name = ic.character.name
            while (characters.any { it.name == name }) name = "$name ${strings.importedSuffix}"
            var inventoryId: String? = null
            // bonusy nasazeného vybavení z přibaleného inventáře (STR pro nosnost už s nimi)
            val withGear = ic.character.copy(gear = ic.inventory?.gearBonuses() ?: emptyList())
            ic.inventory?.let { inv ->
                var invName = inv.name
                while (inventories.any { it.name == invName }) invName = "$invName ${strings.importedSuffix}"
                val id = newId()
                inventories.add(inv.copy(id = id, name = invName, strength = withGear.scores.str))
                inventoryId = id
            }
            val owner = ic.ownerIndex?.takeIf { it in newIds.indices && it != index }?.let { newIds[it] }
            characters.add(withGear.copy(id = newIds[index], name = name, inventoryId = inventoryId, ownerId = owner))
        }
        persistInventories()
        persist()
        return imported.size
    }

    /** Featy, které by import přidal (neopakovatelné featy, které postava už má, se přeskočí). */
    fun featsToImport(charId: String, picks: List<FeatPick>): List<FeatPick> {
        val c = character(charId) ?: return emptyList()
        val db = featDb
        val have = c.feats.mapNotNull { it.featId }.toMutableSet()
        return picks.filter { p ->
            val id = p.featId ?: return@filter true
            val repeatable = db?.get(id)?.repeatable == true
            (repeatable || id !in have).also { have += id }
        }
    }

    fun importFeats(charId: String, picks: List<FeatPick>) {
        val add = featsToImport(charId, picks)
        trackEvent("import", mapOf("kind" to "feats", "count" to add.size.toString()))
        updateCharacter(charId) { it.copy(feats = it.feats + add) }
    }

    private fun newId(): String = Random.nextLong().toULong().toString(36)

    fun createCharacter(
        name: String, classId: String, level: Int,
        subclass: Map<String, String> = emptyMap(), race: String? = null, raceBonus: Ability? = null,
    ): Character {
        val r = RaceCatalog.get(race)
        val c = Character(
            newId(), name.ifBlank { strings.newCharacter }, listOf(ClassEntry(classId, level.coerceIn(1, 20), subclass = subclass)),
            race = r?.id, raceBonus = raceBonus.takeIf { r?.anyBonus == true },
        )
        characters.add(c)
        persist()
        trackEvent("character-create", mapOf("class" to classId, "level" to c.classes.first().level.toString(), "race" to (r?.id ?: "none")))
        return c
    }

    fun updateCharacter(id: String, transform: (Character) -> Character) {
        val i = characters.indexOfFirst { it.id == id }
        if (i < 0) return
        characters[i] = transform(characters[i])
        persist()
    }

    /** Smaže postavu; její mazlíčci zůstanou jako samostatné postavy. */
    fun deleteCharacter(id: String) {
        characters.removeAll { it.id == id }
        for (i in characters.indices) if (characters[i].ownerId == id) characters[i] = characters[i].copy(ownerId = null)
        persist()
    }

    // ---- mazlíčci a společníci (postava s ownerId) ----

    /** Mazlíčci postavy v pořadí založení. */
    fun companionsOf(ownerId: String): List<Character> = characters.filter { it.ownerId == ownerId }

    /** Postavy na nejvyšší úrovni seznamu (mazlíček bez existujícího vlastníka se ukáže taky). */
    val rootCharacters: List<Character>
        get() = characters.filter { c -> c.ownerId == null || characters.none { it.id == c.ownerId } }

    /** Nový mazlíček; s druhem zvířete ([species]) a progresí ([companionLevel]) se atributy, velikost a HD dopočítají. */
    fun createCompanion(
        ownerId: String, name: String, kind: CompanionKind, classId: String, level: Int,
        size: CreatureSize?, quadruped: Boolean, species: String? = null, companionLevel: Int? = null,
    ): Character {
        val base = Character(
            newId(), name.trim().ifBlank { cz.pfspells.data.SpeciesCatalog.get(species)?.name ?: strings.companionKind(kind) },
            listOf(ClassEntry(classId, level.coerceIn(1, 20))),
            ownerId = ownerId,
        )
        val c = base.withCompanion(kind, species, companionLevel, size, quadruped)
        characters.add(c)
        persist()
        trackEvent("companion-create", mapOf("kind" to kind.name.lowercase(), "class" to classId, "species" to (species ?: "none")))
        return c
    }

    /** Druh mazlíčka, druh zvířete, progrese a velikost; velikost a STR se propíšou do propojeného inventáře (nosnost). */
    fun setCompanionInfo(
        charId: String, kind: CompanionKind?, size: CreatureSize?, quadruped: Boolean,
        species: String? = null, companionLevel: Int? = null,
    ) {
        updateCharacter(charId) { it.withCompanion(kind, species, companionLevel, size, quadruped) }
        syncInventory(charId)
    }

    /** Rarita a vlastní pravidla pro návrat po smrti (prázdný text = výchozí podle druhu). */
    fun setCompanionNotes(charId: String, rarity: cz.pfspells.data.CompanionRarity?, respawnRules: String?) =
        updateCharacter(charId) { it.copy(rarity = rarity, respawnRules = respawnRules?.trim()?.takeIf { t -> t.isNotEmpty() }) }

    /** Úroveň zvířecího společníka (stepper v listu mazlíčka). */
    fun setCompanionLevel(charId: String, level: Int) {
        val c = character(charId) ?: return
        setCompanionInfo(charId, c.companion, c.size, c.quadruped, c.species, level)
    }

    /** Úprava jedné classy postavy (null = hlavní classa). */
    fun updateClass(charId: String, classId: String?, transform: (ClassEntry) -> ClassEntry) = updateCharacter(charId) { c ->
        val key = classId ?: c.primary?.classId ?: return@updateCharacter c
        c.updateClass(key, transform)
    }

    /** Přidá classu (multiclass); classa už v postavě je → nic. */
    fun addClass(charId: String, classId: String, level: Int, subclass: Map<String, String> = emptyMap()) = updateCharacter(charId) { c ->
        if (c.classes.any { it.classId == classId }) c
        else c.copy(classes = c.classes + ClassEntry(classId, level.coerceIn(1, 20), subclass = subclass))
    }

    /** Odebere classu i s jejími spelly (poslední classa zůstane). */
    fun removeClass(charId: String, classId: String) = updateCharacter(charId) { c ->
        if (c.classes.size <= 1) c
        else c.copy(
            classes = c.classes.filterNot { it.classId == classId },
            castToday = c.castToday.filterKeys { !it.startsWith("$classId:") },
        )
    }

    // ---- featy (databáze se načítá líně, až když je potřeba) ----
    var featDbState: FeatDbState by mutableStateOf(FeatDbState.NotLoaded)
        private set
    val featDb: FeatDatabase? get() = (featDbState as? FeatDbState.Ready)?.db
    var featFilter by mutableStateOf(FeatFilter())
    var featList = LazyListState()
        private set

    fun ensureFeats() {
        if (featDbState != FeatDbState.NotLoaded && featDbState !is FeatDbState.Failed) return
        featDbState = FeatDbState.Loading
        scope.launch {
            featDbState = try {
                FeatDbState.Ready(FeatRepository.load())
            } catch (e: Exception) {
                FeatDbState.Failed(e.message ?: e.toString())
            }
        }
    }

    /** Přidá feat; neopakovatelný feat, který postava už má, se podruhé nepřidá. */
    fun addFeat(charId: String, feat: Feat) {
        val ch = character(charId) ?: return
        if (!feat.repeatable && ch.feats.any { it.featId == feat.id }) return
        if (strictBlocked(StrictRules.featBlock(ch, feat, featDb, spellDb))) return
        updateCharacter(charId) { it.copy(feats = it.feats + FeatPick(feat.id, feat.name)) }
    }

    fun addCustomFeat(charId: String, name: String, text: String) {
        val ch = character(charId) ?: return
        if (strictBlocked(StrictRules.featBlock(ch, null, featDb, spellDb))) return
        updateCharacter(charId) { it.copy(feats = it.feats + FeatPick(null, name.trim(), text = text.trim().ifEmpty { null })) }
    }

    fun updateFeat(charId: String, index: Int, transform: (FeatPick) -> FeatPick) = updateCharacter(charId) { c ->
        if (index !in c.feats.indices) c else c.copy(feats = c.feats.mapIndexed { i, p -> if (i == index) transform(p) else p })
    }

    fun removeFeat(charId: String, index: Int) = updateCharacter(charId) { c ->
        if (index !in c.feats.indices) c else c.copy(feats = c.feats.filterIndexed { i, _ -> i != index })
    }

    fun setExtraFeats(charId: String, n: Int) = updateCharacter(charId) { it.copy(extraFeats = n.coerceIn(-20, 40)) }

    /** Nastaví podtřídu classy ([key] = `SubclassSlot`, null = zrušit). Změna domén vyprázdní doménový slot. */
    fun setSubclass(charId: String, classId: String, key: String, value: String?) = updateClass(charId, classId) { e ->
        val domainsChanged = key.startsWith("domain") || key.startsWith("subdomain")
        e.copy(subclass = e.subclass.withSubclass(key, value), bonusSpells = if (domainsChanged) emptyMap() else e.bonusSpells)
    }

    /** Doménový slot: počet připravených kopií kouzla domény. */
    fun setBonusSpellCount(charId: String, classId: String, spellId: String, count: Int) = updateClass(charId, classId) { e ->
        val m = e.bonusSpells.toMutableMap()
        if (count <= 0) m.remove(spellId) else m[spellId] = count.coerceAtMost(9)
        e.copy(bonusSpells = m)
    }

    // ---- počítadla schopností ----
    fun addTracker(charId: String, name: String, max: Int, unit: ResourceUnit, note: String) = updateCharacter(charId) {
        it.copy(trackers = it.trackers + Tracker(newId(), name.trim(), max.coerceIn(1, 999), unit, note.trim()))
    }

    fun updateTracker(charId: String, t: Tracker) = updateCharacter(charId) { c ->
        c.copy(trackers = c.trackers.map { if (it.id == t.id) t.copy(max = t.max.coerceIn(1, 999)) else it })
    }

    fun removeTracker(charId: String, id: String) = updateCharacter(charId) { c ->
        c.copy(trackers = c.trackers.filterNot { it.id == id }, castToday = c.castToday - "res:$id")
    }

    /** Skryje / znovu ukáže předvyplněnou schopnost classy. */
    fun setPresetHidden(charId: String, presetId: String, hidden: Boolean) = updateCharacter(charId) { c ->
        c.copy(hiddenPresets = if (hidden) c.hiddenPresets + presetId else c.hiddenPresets - presetId)
    }

    /** Herní režim (odškrtávání seslaných kouzel) – jen stav UI, seslaná kouzla se ukládají u postavy. */
    private var gameModeOn by mutableStateOf(false)
    var gameMode: Boolean
        get() = gameModeOn
        set(v) {
            if (v && !gameModeOn) trackEvent("game-mode")
            gameModeOn = v
        }

    fun setCast(charId: String, key: String, n: Int) = updateCharacter(charId) { it.withCast(key, n) }

    /** Odpočinek: obnoví všechna kouzla a sloty postavy. */
    fun rest(charId: String) {
        updateCharacter(charId) { it.copy(castToday = emptyMap()) }
        trackEvent("rest")
        showToast(UiMessage { it.restedToast })
    }

    /** Rasa postavy (null = žádná); velikost rasy se propíše do propojeného inventáře (nosnost). */
    fun setRace(charId: String, raceId: String?) {
        updateCharacter(charId) { c ->
            val r = RaceCatalog.get(raceId)
            // volba „+2 k jednomu atributu“ platí jen pro rasy, které ji mají
            c.copy(race = r?.id, raceBonus = c.raceBonus.takeIf { r?.anyBonus == true })
        }
        syncInventory(charId)
    }

    /** Rasa s „+2 k jednomu atributu“ (human, half-elf, half-orc): který atribut. */
    fun setRaceBonus(charId: String, ability: Ability?) {
        updateCharacter(charId) { it.copy(raceBonus = ability) }
        syncInventory(charId)
    }

    /** Výsledná STR a velikost rasy → propojený inventář. */
    private fun syncInventory(charId: String) {
        val c = character(charId) ?: return
        val inv = c.inventoryId ?: return
        val size = c.creatureSize
        updateInventory(inv) {
            it.copy(strength = c.scores.str, size = size?.name ?: it.size, quadruped = if (size != null) c.size != null && c.quadruped else it.quadruped)
        }
    }

    /** Ranky ve skillu (klíč [cz.pfspells.data.Skills.key]); max = úroveň postavy, 0 = odebrat. */
    fun setSkillRanks(charId: String, key: String, ranks: Int) = updateCharacter(charId) { c ->
        val r = ranks.coerceIn(0, c.level)
        c.copy(skills = if (r == 0) c.skills - key else c.skills + (key to r))
    }

    /** Odebere skill (i druh Craft / Perform / Profession). */
    fun removeSkill(charId: String, key: String) = updateCharacter(charId) { it.copy(skills = it.skills - key) }

    fun setAlignment(charId: String, a: cz.pfspells.data.Alignment?) = updateCharacter(charId) { it.copy(alignment = a) }

    fun setDeity(charId: String, deity: String) = updateCharacter(charId) { it.copy(deity = deity.trim()) }

    /** Ručně potvrdí (nebo zruší potvrzení) prerekvizitu featu – platí pro všechny featy se stejným textem. */
    fun setPrereqConfirmed(charId: String, text: String, confirmed: Boolean) = updateCharacter(charId) { c ->
        val key = cz.pfspells.data.FeatPrereqs.confirmKey(text)
        c.copy(confirmedPrereqs = if (confirmed) c.confirmedPrereqs + key else c.confirmedPrereqs - key)
    }

    fun setExtraSkillPoints(charId: String, n: Int) = updateCharacter(charId) { it.copy(extraSkillPoints = n.coerceIn(-50, 200)) }

    fun setAbility(charId: String, ability: Ability, value: Int) {
        updateCharacter(charId) { it.copy(abilities = it.abilities.with(ability, value.coerceIn(1, 99))) }
        // STR → nosnost propojeného inventáře
        val c = character(charId) ?: return
        if (ability == Ability.STR) c.inventoryId?.let { inv -> updateInventory(inv) { it.copy(strength = c.scores.str) } }
    }

    fun setClassLevel(charId: String, classId: String?, level: Int) =
        updateClass(charId, classId) { it.copy(level = level.coerceIn(1, 20)) }

    fun setSpellCount(charId: String, classId: String?, spellId: String, count: Int) {
        // přísný režim hlídá jen přidání (víc kopií / nové kouzlo), ubírat jde vždy
        val ch = character(charId) ?: return
        val entry = ch.classEntry(classId) ?: return
        val db = spellDb
        if (strictMode && db != null && count > (entry.spells[spellId] ?: 0)) {
            val spell = db.get(spellId)
            if (spell != null && strictBlocked(StrictRules.spellBlock(ch, entry.classId, spell, db))) return
        }
        writeSpellCount(charId, classId, spellId, count)
    }

    private fun writeSpellCount(charId: String, classId: String?, spellId: String, count: Int) = updateClass(charId, classId) { e ->
        val m = e.spells.toMutableMap()
        if (count <= 0) m.remove(spellId) else m[spellId] = count.coerceAtMost(99)
        e.copy(spells = m)
    }

    /** Připravující classa přidá kopii, spontánní / arcanist kouzlo jen jednou (známé). */
    fun addSpell(charId: String, classId: String?, spellId: String) {
        val book = spellbook(charId, classId) ?: return
        val current = book.spells[spellId] ?: 0
        val single = CasterClasses.get(book.classId)?.distinctSpells == true
        setSpellCount(charId, classId, spellId, if (single) 1 else current + 1)
    }

    private fun persist() = characterStore.save(characters.toList())

    private companion object {
        const val LANG_KEY = "ui_lang"
        const val FAVORITES_KEY = "favorite_spells_v1"
        const val STRICT_KEY = "strict_mode"
    }
}
