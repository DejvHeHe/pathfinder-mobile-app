package cz.pfspells

import cz.pfspells.data.Deck
import cz.pfspells.data.DeckImportException
import cz.pfspells.data.DeckTransfer
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SpellParser
import cz.pfspells.data.SpellSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeckTransferTest {
    private val db by lazy {
        SpellDatabase(
            SpellParser.parse(File("src/commonMain/composeResources/files/SpellList.json").readText()),
            SpellSource.BUNDLED, null,
        )
    }

    @Test
    fun roundTrip() {
        val deck = Deck("x", "Elara", "wizard", 7, mapOf("fireball" to 2, "magic-missile" to 3, "zmizely-spell" to 1))
        val text = DeckTransfer.export(listOf(deck), db)
        val imported = DeckTransfer.parse(text, db).single()
        assertEquals("Elara", imported.deck.name)
        assertEquals("wizard", imported.deck.classId)
        assertEquals(7, imported.deck.classLevel)
        assertEquals(mapOf("fireball" to 2, "magic-missile" to 3), imported.deck.spells)
        assertEquals(listOf("zmizely-spell"), imported.missingSpells)
    }

    @Test
    fun matchesByNameWhenIdDiffers() {
        val text = """{"name":"Test","class":"cleric","level":3,"spells":[{"id":"old-id","name":"cure light wounds","count":2}]}"""
        val imported = DeckTransfer.parse(text, db).single()
        assertEquals(mapOf("cure-light-wounds" to 2), imported.deck.spells)
    }

    @Test
    fun rejectsGarbageAndUnknownClass() {
        assertFailsWith<DeckImportException> { DeckTransfer.parse("ahoj", db) }
        assertFailsWith<DeckImportException> {
            DeckTransfer.parse("""{"name":"X","class":"gunslinger","level":3}""", db)
        }
    }
}
