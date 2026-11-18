package cz.pfspells

import cz.pfspells.data.CasterClasses
import cz.pfspells.data.CreatureSize
import cz.pfspells.data.ImportError
import cz.pfspells.data.ItemCategory
import cz.pfspells.data.Load
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsTest {
    @Test
    fun czechPlurals() {
        assertEquals("1 kouzlo", CsStrings.spells(1))
        assertEquals("3 kouzla", CsStrings.spells(3))
        assertEquals("5 kouzel", CsStrings.spells(5))
        assertEquals("0 kouzel", CsStrings.spells(0))
        assertEquals("2 předměty", CsStrings.itemsCount(2))
        assertEquals("1 spell", EnStrings.spells(1))
        assertEquals("7 spells", EnStrings.spells(7))
    }

    @Test
    fun langCodes() {
        assertEquals(Lang.CS, Lang.of("cs"))
        assertEquals(Lang.EN, Lang.of("en"))
        assertEquals(null, Lang.of("xx"))
        assertEquals(CsStrings, Lang.CS.strings)
    }

    @Test
    fun everyEnumHasText() {
        for (s in listOf(EnStrings, CsStrings)) {
            ItemCategory.entries.forEach { assertTrue(s.category(it).isNotBlank()) }
            Load.entries.forEach { assertTrue(s.loadName(it).isNotBlank()) }
            assertTrue(s.importError(ImportError.UnknownClass("Elara", "fighter")).contains("fighter"))
        }
        for (s in listOf(EnStrings, CsStrings)) {
            CreatureSize.entries.forEach { assertTrue(s.creatureSize(it).isNotBlank()) }
            SpellSort.entries.forEach { assertTrue(s.spellSort(it).isNotBlank()) }
        }
        assertEquals("Weapons", EnStrings.category(ItemCategory.WEAPON))
        assertEquals("Zbraně", CsStrings.category(ItemCategory.WEAPON))
    }

    @Test
    fun czechClassAndSchoolNames() {
        // každá classa má český název
        CasterClasses.all.forEach { assertTrue(CsStrings.className(it.id, it.name) != it.name || it.name in setOf("Druid", "Bard", "Skald", "Paladin", "Antipaladin"), it.id) }
        assertEquals("Kouzelník", CsStrings.className("wizard", "Wizard"))
        assertEquals("Kouzelník (Wizard)", CsStrings.classOption("wizard", "Wizard"))
        assertEquals("Druid", CsStrings.classOption("druid", "Druid"))
        assertEquals("Wizard", EnStrings.className("wizard", "Wizard"))
        assertEquals("Čaroděj/Kouzelník", CsStrings.spellListName("sorcerer/wizard"))
        assertEquals("Sorcerer/wizard", EnStrings.spellListName("sorcerer/wizard"))
        assertEquals("Zaklínání", CsStrings.school("evocation"))
        assertEquals("tvoření nebo přivolávání", CsStrings.subschool("creation or calling"))
        assertEquals("Max Dex +3 · check penalty −3 · speed 30 → 20 ft, 20 → 15 ft · run ×4", EnStrings.loadPenalties(Load.MEDIUM))
        assertEquals(null, CsStrings.loadPenalties(Load.LIGHT))
    }
}
