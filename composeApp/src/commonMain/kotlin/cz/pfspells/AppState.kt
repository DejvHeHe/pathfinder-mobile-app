package cz.pfspells

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cz.pfspells.data.Deck
import cz.pfspells.data.DeckStore
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

sealed interface Screen {
    data object Decks : Screen
    data object Compendium : Screen
    data object Database : Screen
    data class DeckDetail(val deckId: String) : Screen
    data class Picker(val deckId: String) : Screen
    data class SpellDetail(val spellId: String, val deckId: String? = null) : Screen

    // inventář
    data class InventoryDetail(val invId: String) : Screen
    data class ItemPicker(val invId: String) : Screen
    /** Detail itemu z databáze ([itemId]) a/nebo položky inventáře ([entryId]). */
    data class ItemDetail(val itemId: String?, val invId: String?, val entryId: String? = null) : Screen

    /** Detail zobrazený na širokém okně v pravém panelu. */
    val isAside: Boolean get() = this is SpellDetail || this is ItemDetail
}

enum class Section { SPELLBOOK, INVENTORY }

data class ItemFilter(
    val query: String = "",
    val category: ItemCategory? = null,
    /** jen pro zbraně: Simple / Martial / Exotic; prázdné = vše */
    val proficiency: Set<String> = emptySet(),
    val maxPrice: Double? = null,
)

sealed interface ItemDbState {
    data object NotLoaded : ItemDbState
    data object Loading : ItemDbState
    data class Ready(val db: ItemDatabase) : ItemDbState
    data class Failed(val message: String) : ItemDbState
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
    /** jen v pickeru: skrýt spelly, které už v decku jsou */
    val hideInDeck: Boolean = false,
)

sealed interface DbState {
    data object Loading : DbState
    data class Ready(val db: SpellDatabase) : DbState
    data class Failed(val message: String) : DbState
}

class AppState(
    private val spellRepo: SpellRepository = SpellRepository(),
    private val deckStore: DeckStore = DeckStore(),
    private val itemRepo: ItemRepository = ItemRepository(),
    private val inventoryStore: InventoryStore = InventoryStore(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Hlavní sekce appky (záložky Spellbook / Inventory). */
    var section by mutableStateOf(Section.SPELLBOOK)

    fun switchSection(s: Section) {
        section = s
        openRoot(Screen.Decks)
        if (s == Section.INVENTORY) ensureItems()
    }

    // ---- databáze itemů (načítá se líně, až při prvním otevření inventáře) ----
    var items: ItemDbState by mutableStateOf(ItemDbState.NotLoaded)
        private set
    var itemsDownloading by mutableStateOf(false)
        private set
    var itemsMessage: String? by mutableStateOf(null)
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
                itemsMessage = "Downloaded ${fresh.items.size} items."
            } catch (e: Exception) {
                itemsMessage = "Download failed: ${e.message ?: e::class.simpleName}"
            } finally {
                itemsDownloading = false
            }
        }
    }

    fun resetItems() {
        scope.launch {
            items = ItemDbState.Ready(itemRepo.resetToBundled())
            itemsMessage = "Bundled item database restored."
        }
    }

    // ---- skryté ceny: nad 200 gp se zobrazí „???“, 10 rychlých ťuknutí je odhalí (jen do zavření appky) ----
    var pricesRevealed by mutableStateOf(false)
        private set
    var revealToast: String? by mutableStateOf(null)
        private set
    private var tapCount = 0
    private var lastTap = 0L

    /** Volá se při každém dotyku obrazovky (čas v ms). */
    fun registerTap(timeMs: Long) {
        tapCount = if (timeMs - lastTap <= 700) tapCount + 1 else 1
        lastTap = timeMs
        if (tapCount >= 10) {
            tapCount = 0
            pricesRevealed = !pricesRevealed
            revealToast = if (pricesRevealed) "All prices revealed" else "High prices hidden"
            scope.launch {
                kotlinx.coroutines.delay(2200)
                revealToast = null
            }
        }
    }

    // ---- inventáře ----
    val inventories = mutableStateListOf<Inventory>().apply { addAll(inventoryStore.load()) }

    fun inventory(id: String): Inventory? = inventories.firstOrNull { it.id == id }

    fun createInventory(name: String): Inventory {
        val inv = Inventory(newId(), name.ifBlank { "Inventory" })
        inventories.add(inv)
        persistInventories()
        return inv
    }

    fun updateInventory(id: String, transform: (Inventory) -> Inventory) {
        val i = inventories.indexOfFirst { it.id == id }
        if (i < 0) return
        inventories[i] = transform(inventories[i])
        persistInventories()
    }

    fun deleteInventory(id: String) {
        inventories.removeAll { it.id == id }
        persistInventories()
    }

    fun importInventories(list: List<Inventory>) {
        for (inv in list) {
            var name = inv.name
            while (inventories.any { it.name == name }) name = "$name (imported)"
            inventories.add(inv.copy(id = newId(), name = name))
        }
        persistInventories()
    }

    /** Přidá item z databáze; stejný item bez poznámky se jen přičte. */
    fun addItem(invId: String, item: Item, qty: Int = 1, note: String = "") = updateInventory(invId) { inv ->
        val existing = inv.entries.indexOfFirst { it.itemId == item.id && it.note == note.trim() }
        if (existing >= 0) {
            inv.copy(entries = inv.entries.mapIndexed { i, e -> if (i == existing) e.copy(qty = (e.qty + qty).coerceAtMost(9999)) else e })
        } else {
            inv.copy(entries = inv.entries + InvEntry(
                id = newEntryId(), itemId = item.id, name = item.name, category = item.category.name, qty = qty,
                priceGp = item.priceGp, weightLb = item.weightLb, note = note.trim(), info = item.summary,
            ))
        }
    }

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
        if (screen is Screen.Picker) preparePicker(screen.deckId)
        if (screen is Screen.ItemPicker) prepareItemPicker(screen.invId)
        stack.add(screen)
    }

    /** Detail spellu: když už je nějaký otevřený, nahradí ho (bez hromadění v historii). */
    fun showSpell(spellId: String, deckId: String?) = showAside(Screen.SpellDetail(spellId, deckId))

    fun showItem(itemId: String?, invId: String?, entryId: String? = null) = showAside(Screen.ItemDetail(itemId, invId, entryId))

    private fun showAside(screen: Screen) {
        if (current.isAside) stack[stack.lastIndex] = screen else stack.add(screen)
    }

    /** Otevře obrazovku jako novou „kořenovou“ nad seznamem decků (boční panel na širokém okně). */
    fun openRoot(screen: Screen) {
        if (screen is Screen.Picker) preparePicker(screen.deckId)
        if (screen is Screen.ItemPicker) prepareItemPicker(screen.invId)
        when (screen) {
            is Screen.InventoryDetail, is Screen.ItemPicker -> section = Section.INVENTORY
            is Screen.DeckDetail, is Screen.Picker, Screen.Compendium -> section = Section.SPELLBOOK
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
    var downloading by mutableStateOf(false)
        private set
    var downloadMessage: String? by mutableStateOf(null)
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
                downloadMessage = "Downloaded ${fresh.spells.size} spells."
            } catch (e: Exception) {
                downloadMessage = "Download failed: ${e.message ?: e::class.simpleName}"
            } finally {
                downloading = false
            }
        }
    }

    fun resetSpells() {
        scope.launch {
            db = DbState.Ready(spellRepo.resetToBundled())
            downloadMessage = "Bundled database restored."
        }
    }

    // ---- filtry prohlížeče (přežijí odchod na detail spellu a zpět) ----
    var compendiumFilter by mutableStateOf(BrowserFilter())
    val compendiumList = LazyListState()
    var pickerFilter by mutableStateOf(BrowserFilter())
    var pickerDeckId: String? = null
    var pickerList = LazyListState()

    /** Při otevření pickeru pro jiný deck začít s čistými filtry. */
    fun preparePicker(deckId: String) {
        if (pickerDeckId != deckId) {
            pickerDeckId = deckId
            pickerFilter = BrowserFilter()
            pickerList = LazyListState()
        }
    }

    // ---- decky ----
    val decks = mutableStateListOf<Deck>().apply { addAll(deckStore.load()) }

    fun deck(id: String): Deck? = decks.firstOrNull { it.id == id }

    /** Přidá importované decky s novými id; při shodě jména připojí „(import)“. */
    fun importDecks(imported: List<Deck>): Int {
        for (d in imported) {
            var name = d.name
            while (decks.any { it.name == name }) name = "$name (imported)"
            decks.add(d.copy(id = newId(), name = name))
        }
        persist()
        return imported.size
    }

    private fun newId(): String = Random.nextLong().toULong().toString(36)

    fun createDeck(name: String, classId: String, level: Int): Deck {
        val id = newId()
        val deck = Deck(id, name.ifBlank { "New deck" }, classId, level.coerceIn(1, 20))
        decks.add(deck)
        persist()
        return deck
    }

    fun updateDeck(id: String, transform: (Deck) -> Deck) {
        val i = decks.indexOfFirst { it.id == id }
        if (i < 0) return
        decks[i] = transform(decks[i])
        persist()
    }

    fun deleteDeck(id: String) {
        decks.removeAll { it.id == id }
        persist()
    }

    fun setSpellCount(deckId: String, spellId: String, count: Int) = updateDeck(deckId) { d ->
        val m = d.spells.toMutableMap()
        if (count <= 0) m.remove(spellId) else m[spellId] = count.coerceAtMost(99)
        d.copy(spells = m)
    }

    fun addSpell(deckId: String, spellId: String) {
        val current = deck(deckId)?.spells?.get(spellId) ?: 0
        setSpellCount(deckId, spellId, current + 1)
    }

    private fun persist() = deckStore.save(decks.toList())
}
