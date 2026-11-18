package cz.pfspells

import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Herní režim: seslaná kouzla a sloty u postavy. */
class CastTodayTest {
    private val c = Character("1", "Test", listOf(ClassEntry("wizard", 5), ClassEntry("sorcerer", 2)))

    @Test
    fun castCountsAndReset() {
        val key = Character.castKey("wizard", "magic-missile")
        val slot = Character.slotKey("sorcerer", 1)
        var x = c.withCast(key, 2).withCast(slot, 3)
        assertEquals(2, x.cast(key))
        assertEquals(3, x.cast(slot))
        x = x.withCast(key, 0)
        assertEquals(0, x.cast(key))
        assertTrue(key !in x.castToday)
    }

    @Test
    fun slotKeyDoesNotCollideWithSpellIds() {
        // id spellů jsou slugy malými písmeny – „L1“ s velkým L se s nimi nepotká
        assertTrue(Character.slotKey("wizard", 1) != Character.castKey("wizard", "l1"))
    }
}
