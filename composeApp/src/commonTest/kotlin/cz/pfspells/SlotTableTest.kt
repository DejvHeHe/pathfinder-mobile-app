package cz.pfspells

import cz.pfspells.data.CasterClasses
import cz.pfspells.data.CastingKind
import cz.pfspells.data.bonusSpells
import cz.pfspells.data.levelSlots
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Sloty za den a spells known (CastingTable.kt z AoN). */
class SlotTableTest {
    private fun cls(id: String) = CasterClasses.get(id)!!
    private fun perDay(id: String, level: Int) = (0..9).map { cls(id).slotsPerDay(level, it) }
    private fun known(id: String, level: Int) = (0..9).map { cls(id).spellsKnown(level, it) }

    @Test
    fun everyCasterHasTables() {
        CasterClasses.all.forEach { c ->
            assertNotNull(c.slotsPerDay(20, 1), "${c.id}: chybí tabulka za den")
            if (c.kind != CastingKind.PREPARED && c.id != "occultist") assertNotNull(c.spellsKnown(20, 1), "${c.id}: chybí known")
        }
    }

    @Test
    fun tablesMatchSpellLevelProgression() {
        // spell level ≥ 1 má sloty právě tehdy, když ho classa na dané úrovni smí sesílat
        CasterClasses.all.forEach { c ->
            for (lvl in 1..20) for (sl in 1..9) {
                val has = c.slotsPerDay(lvl, sl) != null
                assertEquals(sl <= c.maxSpellLevel(lvl), has, "${c.id} úroveň $lvl, spell level $sl")
            }
        }
    }

    @Test
    fun coreTables() {
        // Core Rulebook: wizard 1 = 3 triky + 1; wizard 20 = 4 všude
        assertEquals(listOf(3, 1, null, null, null, null, null, null, null, null), perDay("wizard", 1))
        assertEquals(List(10) { 4 }, perDay("wizard", 20))
        // sorcerer 4: 6 / 3 za den, známá 6 / 3 / 1
        assertEquals(listOf(null, 6, 3, null, null, null, null, null, null, null), perDay("sorcerer", 4))
        assertEquals(listOf(6, 3, 1, null, null, null, null, null, null, null), known("sorcerer", 4))
        // paladin 4: 0 slotů (jen bonus z CHA)
        assertEquals(0, cls("paladin").slotsPerDay(4, 1))
        // bard 1: 1 za den, známá 4 triky + 2
        assertEquals(1, cls("bard").slotsPerDay(1, 1))
        assertEquals(listOf(4, 2), known("bard", 1).take(2))
    }

    @Test
    fun bonusSpellTable() {
        // Core Rulebook tabulka 1–3: 18 (+4) → 1st–4th po 1; 20 (+5) → 1st 2, 2nd–5th po 1
        assertEquals(listOf(0, 1, 1, 1, 1, 0), (0..5).map { bonusSpells(18, it) })
        assertEquals(listOf(0, 2, 1, 1, 1, 1, 0), (0..6).map { bonusSpells(20, it) })
        assertEquals(listOf(0, 0), listOf(bonusSpells(11, 1), bonusSpells(10, 1)))
        assertEquals(3, bonusSpells(30, 1)) // +10 → (10−1)/4+1
    }

    @Test
    fun levelSlotsCombinesBaseAndBonus() {
        val wiz = cls("wizard").levelSlots(classLevel = 5, abilityScore = 16, spellLevel = 1)
        assertEquals(3, wiz.perDay); assertEquals(1, wiz.bonus); assertEquals(4, wiz.limit); assertTrue(wiz.abilityOk)
        val sorc0 = cls("sorcerer").levelSlots(1, 16, 0)
        assertNull(sorc0.perDay); assertEquals(4, sorc0.limit)
        val low = cls("cleric").levelSlots(9, 12, 5)
        assertEquals(false, low.abilityOk)
    }
}
