package cz.pfspells

import com.russhwolf.settings.PreferencesSettings
import cz.pfspells.data.CharacterStore
import cz.pfspells.data.Deck
import cz.pfspells.data.DeckStore
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterStoreTest {
    private val node = Preferences.userRoot().node("pfspells-character-test").also { it.clear() }
    private val settings = PreferencesSettings(node)

    @AfterTest
    fun cleanup() = node.removeNode()

    @Test
    fun migratesOldDecks() {
        DeckStore(settings).save(listOf(Deck("a", "Elara", "wizard", 5, mapOf("fireball" to 1, "magic-missile" to 2))))
        val chars = CharacterStore(settings).load()
        assertEquals(1, chars.size)
        val c = chars.single()
        assertEquals("Elara", c.name)
        assertEquals(5, c.level)
        assertEquals("wizard", c.primary!!.classId)
        assertEquals(mapOf("fireball" to 1, "magic-missile" to 2), c.primary!!.spells)
        // podruhé už se čte nový klíč (migrace proběhne jen jednou) a starý zůstává jako záloha
        DeckStore(settings).save(emptyList())
        assertEquals(chars, CharacterStore(settings).load())
    }

    @Test
    fun emptyWhenNothingStored() {
        assertEquals(emptyList(), CharacterStore(settings).load())
    }
}
