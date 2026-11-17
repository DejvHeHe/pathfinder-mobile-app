package cz.pfspells

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import com.russhwolf.settings.PreferencesSettings
import cz.pfspells.data.DeckStore
import cz.pfspells.data.InvEntry
import cz.pfspells.data.InventoryStore
import cz.pfspells.data.ItemCategory
import cz.pfspells.data.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.util.prefs.Preferences
import kotlin.test.Test

/**
 * Vykreslí ukázkové obrazovky do composeApp/build/previews (ruční kontrola designu).
 * Spouští se jen s -Dpreviews=true, aby nezdržoval běžné testy.
 */
class PreviewRender {
    @Test
    fun render() = runBlocking {
        if (System.getProperty("previews") != "true") return@runBlocking
        val node = Preferences.userRoot().node("pfspells-preview-test")
        node.clear()
        val settings = PreferencesSettings(node)
        val state = withContext(Dispatchers.Main) {
            AppState(deckStore = DeckStore(settings), inventoryStore = InventoryStore(settings), itemRepo = ItemRepository(settings))
        }
        while (state.db !is DbState.Ready) delay(50)
        val elara = withContext(Dispatchers.Main) {
            state.createDeck("Brother Thomas", "cleric", 7)
            state.createDeck("Nyx", "sorcerer", 4)
            val d = state.createDeck("Elara", "wizard", 5)
            mapOf("detect-magic" to 1, "light" to 1, "magic-missile" to 2, "shield" to 1, "mage-armor" to 1,
                "web" to 1, "invisibility" to 1, "fireball" to 1).forEach { (k, v) -> state.setSpellCount(d.id, k, v) }
            d
        }
        withContext(Dispatchers.Main) { state.ensureItems() }
        while (state.items !is ItemDbState.Ready) delay(50)
        val inv = withContext(Dispatchers.Main) {
            state.createInventory("Party stash")
            val i = state.createInventory("Elara")
            val db = state.itemDb!!
            listOf("Longsword" to 1, "Shortbow" to 1, "Arrows (20)" to 1, "Leather armor" to 1, "Backpack, common" to 1,
                "Bedroll" to 1, "Rope, hemp (50 ft.)" to 1, "Torch" to 3, "Trail rations (per day)" to 5, "Bag of Holding (Type I)" to 1)
                .forEach { (n, q) -> (db.findByName(n) ?: db.items.firstOrNull { it.name.startsWith(n.substringBefore(" (").substringBefore(",")) })?.let { state.addItem(i.id, it, q) } }
            state.addCustomEntry(i.id, InvEntry("p", null, "Potion of Cure Light Wounds", ItemCategory.POTION.name, 2, 50.0, null, info = "1d8+1"))
            state.updateInventory(i.id) { it.copy(gp = 124, sp = 6, cp = 12, notes = "Chest at the Golden Stag inn – 2 scrolls, spare rope.") }
            state.inventory(i.id)!!
        }
        val out = File("build/previews").apply { mkdirs() }

        suspend fun shot(name: String, w: Int, h: Int, nav: AppState.() -> Unit) {
            withContext(Dispatchers.Main) { state.nav() }
            ImageComposeScene(w, h, Density(1f)) { App(state) }.use { scene ->
                var t = 0L
                repeat(12) { scene.render(t); t += 50_000_000; delay(40) }
                val img = scene.render(t)
                File(out, "$name.png").writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }

        shot("phone-decks", 390, 844) { openRoot(Screen.Decks) }
        shot("phone-deck", 390, 844) { openRoot(Screen.DeckDetail(elara.id)) }
        shot("phone-spell", 390, 844) { openRoot(Screen.DeckDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("phone-picker", 390, 844) { openRoot(Screen.DeckDetail(elara.id)); push(Screen.Picker(elara.id)) }
        shot("desktop", 1440, 1000) { openRoot(Screen.DeckDetail(elara.id)); showSpell("fireball", elara.id) }
        shot("desktop-compendium", 1440, 1000) { openRoot(Screen.Compendium) }
        shot("phone-inventory", 390, 844) { switchSection(Section.INVENTORY); push(Screen.InventoryDetail(inv.id)) }
        shot("phone-itempicker", 390, 844) { itemFilter = ItemFilter(category = ItemCategory.WEAPON); push(Screen.ItemPicker(inv.id)) }
        shot("desktop-inventory", 1440, 1000) { openRoot(Screen.InventoryDetail(inv.id)) }
        shot("desktop-itempicker", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(category = ItemCategory.WEAPON, proficiency = setOf("Simple", "Martial"), maxPrice = 50.0)
            showItem(itemDb!!.findByName("Longsword")!!.id, inv.id)
        }
        shot("desktop-wondrous", 1440, 1000) {
            openRoot(Screen.ItemPicker(inv.id)); itemFilter = ItemFilter(category = ItemCategory.WONDROUS)
            showItem(itemDb!!.items.first { it.name.startsWith("Bag of Holding") }.id, inv.id)
        }
        shot("phone-decks-inventory-tab", 390, 844) { switchSection(Section.INVENTORY) }
        shot("phone-about", 390, 844) { openRoot(Screen.Decks); push(Screen.About) }
        shot("desktop-about", 1440, 1000) { openRoot(Screen.About) }
        node.removeNode()
    }
}
