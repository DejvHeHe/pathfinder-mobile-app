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

    @Test
    fun raceSizeGoesToInventory() = onMain {
        val state = AppState(inventoryStore = InventoryStore(settings), itemRepo = ItemRepository(settings), settings = settings)
        val c = state.createCharacter("Pip", "rogue", 2)
        val inv = state.createInventoryFor(c.id)!!
        assertEquals("MEDIUM", state.inventory(inv.id)!!.size)
        state.setRace(c.id, "halfling")
        assertEquals("SMALL", state.inventory(inv.id)!!.size)
        // neznámá rasa se neuloží
        state.setRace(c.id, "nonsense")
        assertNull(state.character(c.id)!!.race)
        // propojení nového inventáře převezme velikost rasy
        state.setRace(c.id, "gnome")
        val other = state.createInventory("Other")
        state.linkInventory(c.id, other.id)
        assertEquals("SMALL", state.inventory(other.id)!!.size)
        // nosnost počítá s rasou: gnóm −2 STR
        state.setAbility(c.id, Ability.STR, 14)
        assertEquals(12, state.inventory(other.id)!!.strength)
        // STR z inventáře → základ postavy bez rasy
        state.setInventoryStrength(other.id, 10)
        assertEquals(12, state.character(c.id)!!.abilities.str)
        // human +2 k síle
        state.setRace(c.id, "human")
        state.setRaceBonus(c.id, Ability.STR)
        assertEquals(14, state.inventory(other.id)!!.strength)
        // jiná rasa volbu zahodí
        state.setRace(c.id, "elf")
        assertNull(state.character(c.id)!!.raceBonus)
    }

    @Test
    fun companionsAndStrictMode() = onMain {
        val state = AppState(inventoryStore = InventoryStore(settings), itemRepo = ItemRepository(settings), settings = settings)
        val owner = state.createCharacter("Lini", "druid", 4)
        val wolf = state.createCompanion(
            owner.id, "Droogami", cz.pfspells.data.CompanionKind.ANIMAL_COMPANION, "animal", 4,
            cz.pfspells.data.CreatureSize.LARGE, quadruped = true,
        )
        assertEquals(listOf(wolf.id), state.companionsOf(owner.id).map { it.id })
        assertEquals(listOf(owner.id), state.rootCharacters.map { it.id })

        // velikost mazlíčka → nosnost propojeného inventáře
        val inv = state.createInventoryFor(wolf.id)!!
        assertEquals("LARGE", state.inventory(inv.id)!!.size)
        assertEquals(true, state.inventory(inv.id)!!.quadruped)

        // přísný režim: HD 4 → 2 featy, třetí nejde přidat (a volba se pamatuje)
        state.changeStrictMode(true)
        repeat(3) { state.addCustomFeat(wolf.id, "Feat $it", "") }
        assertEquals(2, state.character(wolf.id)!!.feats.size)
        state.changeStrictMode(false)
        state.addCustomFeat(wolf.id, "Extra", "")
        assertEquals(3, state.character(wolf.id)!!.feats.size)

        // smazání vlastníka: mazlíček zůstane jako samostatná postava
        state.deleteCharacter(owner.id)
        assertNull(state.character(wolf.id)!!.ownerId)
        assertEquals(listOf(wolf.id), state.rootCharacters.map { it.id })
    }

    @Test
    fun equippedGearAffectsCharacter() = onMain {
        val state = AppState(inventoryStore = InventoryStore(settings), itemRepo = ItemRepository(settings), settings = settings)
        val c = state.createCharacter("Amiri", "barbarian", 5)
        state.setAbility(c.id, Ability.STR, 16)
        val inv = state.createInventoryFor(c.id)!!
        val belt = cz.pfspells.data.InvEntry(
            "belt", name = "Belt Of Giant Strength +4", slot = cz.pfspells.data.ItemSlot.BELT,
            bonuses = cz.pfspells.data.ItemBonusGuess.of("Belt Of Giant Strength +4"),
        )
        state.addCustomEntry(inv.id, belt)
        assertEquals(16, state.character(c.id)!!.scores.str) // nenasazeno

        // nasadit: STR 20 u postavy i v nosnosti inventáře, základ zůstane 16
        state.setEquipped(inv.id, "belt", true)
        assertEquals(20, state.character(c.id)!!.scores.str)
        assertEquals(16, state.character(c.id)!!.abilities.str)
        assertEquals(20, state.inventory(inv.id)!!.strength)

        // stepper síly v inventáři mění základ (bez bonusu opasku)
        state.setInventoryStrength(inv.id, 22)
        assertEquals(18, state.character(c.id)!!.abilities.str)

        // odpojení inventáře bonusy zruší
        state.linkInventory(c.id, null)
        assertEquals(18, state.character(c.id)!!.scores.str)
    }
}
