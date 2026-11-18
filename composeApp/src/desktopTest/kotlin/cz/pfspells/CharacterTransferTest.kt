package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.Character
import cz.pfspells.data.CharacterTransfer
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.Deck
import cz.pfspells.data.DeckImportException
import cz.pfspells.data.DeckTransfer
import cz.pfspells.data.Feat
import cz.pfspells.data.FeatDatabase
import cz.pfspells.data.FeatPick
import cz.pfspells.data.ImportError
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Inventory
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SpellParser
import cz.pfspells.data.SpellSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CharacterTransferTest {
    private val db by lazy {
        SpellDatabase(
            SpellParser.parse(File("src/commonMain/composeResources/files/SpellList.json").readText()),
            SpellSource.BUNDLED, null,
        )
    }
    private val featDb = FeatDatabase(listOf(Feat("power-attack", "Power Attack"), Feat("cleave", "Cleave")))

    private val thomas = Character(
        "x", "Brother Thomas",
        listOf(ClassEntry("cleric", 7, spells = mapOf("cure-light-wounds" to 3, "bless" to 1)), ClassEntry("fighter", 2)),
        abilities = Abilities(str = 14, wis = 17),
        feats = listOf(FeatPick("power-attack", "Power Attack"), FeatPick(null, "Blessed", text = "Re-roll once per day")),
        extraFeats = 1,
        inventoryId = "inv",
        castToday = mapOf("cleric:bless" to 1),
    )
    private val inv = Inventory("inv", "Thomas", strength = 14, gp = 50,
        entries = listOf(InvEntry("e", null, "Holy symbol", "GEAR", 1, 1.0, 0.0)))

    @Test
    fun roundTripWithInventory() {
        val text = CharacterTransfer.export(listOf(thomas), db) { inv }
        val ic = CharacterTransfer.parse(text, db, featDb, null).single()
        val c = ic.character
        assertEquals("Brother Thomas", c.name)
        assertEquals(listOf("cleric" to 7, "fighter" to 2), c.classes.map { it.classId to it.level })
        assertEquals(mapOf("cure-light-wounds" to 3, "bless" to 1), c.classes[0].spells)
        assertEquals(Abilities(str = 14, wis = 17), c.abilities)
        assertEquals(thomas.feats, c.feats)
        assertEquals(1, c.extraFeats)
        assertEquals(emptyMap(), c.castToday) // herní režim se neexportuje
        assertEquals("Thomas", ic.inventory!!.name)
        assertEquals(50, ic.inventory!!.gp)
        assertEquals(listOf("Holy symbol"), ic.inventory!!.entries.map { it.name })
    }

    @Test
    fun withoutInventory() {
        val text = CharacterTransfer.export(listOf(thomas), db) { null }
        assertNull(CharacterTransfer.parse(text, db, featDb, null).single().inventory)
    }

    @Test
    fun oldDeckExportStillImports() {
        val text = DeckTransfer.export(listOf(Deck("d", "Elara", "wizard", 5, mapOf("fireball" to 1))), db)
        val c = CharacterTransfer.parse(text, db, featDb, null).single().character
        assertEquals("Elara", c.name)
        assertEquals("wizard", c.primary!!.classId)
        assertEquals(mapOf("fireball" to 1), c.primary!!.spells)
        // i samotný deck bez obálky
        val single = """{"name":"Kyra","class":"cleric","level":3,"spells":[{"id":"bless","name":"Bless"}]}"""
        assertEquals("Kyra", CharacterTransfer.parse(single, db, featDb, null).single().character.name)
    }

    @Test
    fun featsOnly() {
        val text = CharacterTransfer.exportFeats(thomas.feats)
        assertEquals(thomas.feats, CharacterTransfer.parseFeats(text, featDb))
        // featy jde vzít i z exportu celé postavy
        val charText = CharacterTransfer.export(listOf(thomas), db) { null }
        assertEquals(thomas.feats, CharacterTransfer.parseFeats(charText, featDb))
        // feat spárovaný podle jména, když se id změnilo
        val renamed = """{"format":"pf-spellbook-feats","version":1,"feats":[{"id":"old-id","name":"cleave"}]}"""
        assertEquals("cleave", CharacterTransfer.parseFeats(renamed, featDb).single().featId)
    }

    @Test
    fun errors() {
        val e1 = assertFailsWith<DeckImportException> { CharacterTransfer.parse("""{"format":"pf-spellbook-characters","version":2,"characters":[]}""", db, null, null) }
        assertEquals(ImportError.NewerVersion(2), e1.reason)
        val e2 = assertFailsWith<DeckImportException> {
            CharacterTransfer.parse("""{"format":"pf-spellbook-characters","characters":[{"name":"X","classes":[{"class":"nope","level":1}]}]}""", db, null, null)
        }
        assertEquals(ImportError.UnknownClass("X", "nope"), e2.reason)
        val e3 = assertFailsWith<DeckImportException> { CharacterTransfer.parseFeats("""{"format":"pf-spellbook-feats","feats":[]}""", featDb) }
        assertEquals(ImportError.NoFeats, e3.reason)
    }
}
