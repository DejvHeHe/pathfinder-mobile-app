package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.Ability
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
        listOf(
            ClassEntry(
                "cleric", 7, spells = mapOf("cure-light-wounds" to 3, "bless" to 1),
                subclass = mapOf("domain1" to "earth", "subdomain1" to "caves", "domain2" to "good"),
                bonusSpells = mapOf("magic-stone" to 1),
            ),
            ClassEntry("fighter", 2),
        ),
        abilities = Abilities(str = 14, wis = 17),
        feats = listOf(FeatPick("power-attack", "Power Attack"), FeatPick(null, "Blessed", text = "Re-roll once per day")),
        extraFeats = 1,
        inventoryId = "inv",
        castToday = mapOf("cleric:bless" to 1),
        race = "human",
        raceBonus = Ability.WIS,
        skills = mapOf("heal" to 3, "craft:armor" to 1),
        extraSkillPoints = 2,
        alignment = cz.pfspells.data.Alignment.LG,
        deity = "Torag",
        confirmedPrereqs = setOf("stonecunning racial trait"),
        trackers = listOf(cz.pfspells.data.Tracker("t1", "Wand of CLW", 50, note = "charges")),
        hiddenPresets = setOf("fighter-bravery"),
        hpBonus = 2, hpOverride = 61, damage = 12, acMisc = 1,
    )
    private val inv = Inventory("inv", "Thomas", strength = 14, gp = 50,
        entries = listOf(
            InvEntry("e", null, "Holy symbol", "GEAR", 1, 1.0, 0.0),
            InvEntry("r", null, "Ring of Protection +1", "RING", 1, 2000.0, 0.0, equipped = true,
                slot = cz.pfspells.data.ItemSlot.RING,
                bonuses = listOf(cz.pfspells.data.ItemBonus(cz.pfspells.data.BonusTarget.AC, 1, cz.pfspells.data.BonusType.DEFLECTION))),
        ))

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
        assertEquals("human", c.race)
        assertEquals(Ability.WIS, c.raceBonus)
        assertEquals(mapOf("heal" to 3, "craft:armor" to 1), c.skills)
        assertEquals(2, c.extraSkillPoints)
        // podtřída, doménový slot, přesvědčení, božstvo, potvrzené prerekvizity, počítadla
        assertEquals(thomas.classes[0].subclass, c.classes[0].subclass)
        assertEquals(thomas.classes[0].bonusSpells, c.classes[0].bonusSpells)
        assertEquals(cz.pfspells.data.Alignment.LG, c.alignment)
        assertEquals("Torag", c.deity)
        assertEquals(setOf("stonecunning racial trait"), c.confirmedPrereqs)
        assertEquals(thomas.trackers.map { it.copy(id = "") }, c.trackers.map { it.copy(id = "") }) // počítadla dostanou nová id
        assertEquals(thomas.hiddenPresets, c.hiddenPresets)
        assertEquals(emptyMap(), c.castToday) // herní režim se neexportuje
        // obrana a životy; zranění se (jako seslaná kouzla) neexportuje
        assertEquals(2, c.hpBonus)
        assertEquals(61, c.hpOverride)
        assertEquals(1, c.acMisc)
        assertEquals(0, c.damage)
        assertEquals("Thomas", ic.inventory!!.name)
        assertEquals(50, ic.inventory!!.gp)
        assertEquals(listOf("Holy symbol", "Ring of Protection +1"), ic.inventory!!.entries.map { it.name })
        // nasazené vybavení (slot a bonusy) zůstane
        val ring = ic.inventory!!.entries[1]
        assertEquals(true, ring.equipped)
        assertEquals(inv.entries[1].slot, ring.slot)
        assertEquals(inv.entries[1].bonuses, ring.bonuses)
    }

    @Test
    fun companionKeepsOwnerByIndex() {
        val wolf = Character(
            "w", "Wolf", listOf(ClassEntry("animal", 3)), ownerId = "x",
            companion = cz.pfspells.data.CompanionKind.ANIMAL_COMPANION,
            size = cz.pfspells.data.CreatureSize.LARGE, quadruped = true,
            species = "wolf", companionLevel = 7,
            rarity = cz.pfspells.data.CompanionRarity.RARE, respawnRules = "Returns at the next full moon.",
        )
        val list = CharacterTransfer.parse(CharacterTransfer.export(listOf(thomas, wolf), db) { null }, db, featDb, null)
        assertNull(list[0].ownerIndex)
        assertEquals(0, list[1].ownerIndex)
        val w = list[1].character
        assertEquals(cz.pfspells.data.CompanionKind.ANIMAL_COMPANION, w.companion)
        assertEquals(cz.pfspells.data.CreatureSize.LARGE, w.size)
        assertEquals(true, w.quadruped)
        assertEquals("animal", w.classes.single().classId)
        assertEquals("wolf", w.species)
        assertEquals(7, w.companionLevel)
        assertEquals(cz.pfspells.data.CompanionRarity.RARE, w.rarity)
        assertEquals("Returns at the next full moon.", w.respawnRules)
        // mazlíček bez vlastníka v exportu se importuje jako samostatná postava
        assertNull(CharacterTransfer.parse(CharacterTransfer.export(listOf(wolf), db) { null }, db, featDb, null).single().ownerIndex)
    }

    @Test
    fun classAbilitiesRoundTrip() {
        val hex = cz.pfspells.data.AbilityPick("evil-eye", "Evil Eye", cz.pfspells.data.AbilityKind.HEX)
        val boon = cz.pfspells.data.AbilityPick("abadar-s-boon", "Abadar's Boon", cz.pfspells.data.AbilityKind.REVELATION, "@class.unlevel", cz.pfspells.data.ResourceUnit.PER_DAY)
        val witch = Character("w", "Hex", listOf(ClassEntry("witch", 4, abilities = listOf(hex)), ClassEntry("oracle", 1, abilities = listOf(boon, boon))))
        val c = CharacterTransfer.parse(CharacterTransfer.export(listOf(witch), db) { null }, db, featDb, null).single().character
        assertEquals(listOf(hex), c.classes[0].abilities)
        assertEquals(listOf(boon), c.classes[1].abilities) // duplicita se zahodí
    }

    @Test
    fun racialTraitsRoundTrip() {
        val jinx = cz.pfspells.data.RacialTraitPick("halfling:halfling-jinx", "Halfling Jinx", listOf("halfling luck"))
        val pip = Character("p", "Pip", listOf(ClassEntry("rogue", 2)), race = "halfling", racialTraits = listOf(jinx))
        val c = CharacterTransfer.parse(CharacterTransfer.export(listOf(pip), db) { null }, db, featDb, null).single().character
        assertEquals(listOf(jinx), c.racialTraits)
        // bez (známé) rasy se traity zahodí
        val noRace = CharacterTransfer.parse(CharacterTransfer.export(listOf(pip.copy(race = "nonsense")), db) { null }, db, featDb, null).single().character
        assertEquals(emptyList(), noRace.racialTraits)
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
