package cz.pfspells

import com.russhwolf.settings.PreferencesSettings
import cz.pfspells.data.Ability
import cz.pfspells.data.InventoryStore
import cz.pfspells.data.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Propojení postavy s inventářem: STR se drží stejná na obou stranách. */
class InventoryLinkTest {
    private val node = Preferences.userRoot().node("pfspells-invlink-test").also { it.clear() }
    private val settings = PreferencesSettings(node)

    @AfterTest
    fun cleanup() = node.removeNode()

    private fun <T> onMain(block: suspend () -> T): T = runBlocking { withContext(Dispatchers.Main) { block() } }

    @Test
    fun strengthStaysInSync() = onMain {
        val state = AppState(inventoryStore = InventoryStore(settings), itemRepo = ItemRepository(settings), settings = settings)
        val c = state.createCharacter("Valeros", "fighter", 3)
        state.setAbility(c.id, Ability.STR, 16)
        val inv = state.createInventoryFor(c.id)!!
        assertEquals(inv.id, state.character(c.id)!!.inventoryId)
        assertEquals(16, state.inventory(inv.id)!!.strength)

        // postava → inventář
        state.setAbility(c.id, Ability.STR, 18)
        assertEquals(18, state.inventory(inv.id)!!.strength)
        // inventář → postava
        state.setInventoryStrength(inv.id, 12)
        assertEquals(12, state.character(c.id)!!.abilities.str)

        // inventář patří jen jedné postavě
        val d = state.createCharacter("Kyra", "cleric", 3)
        state.linkInventory(d.id, inv.id)
        assertNull(state.character(c.id)!!.inventoryId)
        assertEquals(state.character(d.id)!!.abilities.str, state.inventory(inv.id)!!.strength)

        // smazání inventáře postavu odpojí
        state.deleteInventory(inv.id)
        assertNull(state.character(d.id)!!.inventoryId)
    }
}
