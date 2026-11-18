package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.ResourceUnit
import cz.pfspells.data.Tracker
import cz.pfspells.data.resources
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Předvyplněné schopnosti class – počty použití podle pravidel. */
class ClassFeaturesTest {
    private fun max(classId: String, level: Int, abilities: Abilities = Abilities(), name: String): Int? {
        val c = Character("1", "T", listOf(ClassEntry(classId, level)), abilities = abilities)
        return c.resources().first { it.name == name }.max
    }

    @Test
    fun paladin() {
        // Smite Evil 1/den, +1 na 4., 7., 10.… úrovni; Lay on Hands ½ úrovně + CHA od 2. úrovně
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 3), (1..7).map { max("paladin", it, name = "Smite Evil") })
        assertEquals(6, max("paladin", 7, Abilities(cha = 16), "Lay on Hands"))
        assertEquals(0, Character("1", "T", listOf(ClassEntry("paladin", 1))).resources().count { it.name == "Lay on Hands" })
    }

    @Test
    fun rageAndPerformance() {
        // Rage: 4 + CON + 2 za každou úroveň po 1.
        assertEquals(15, max("barbarian", 5, Abilities(con = 16), "Rage"))
        assertEquals(6, max("bard", 1, Abilities(cha = 14), "Bardic Performance"))
    }

    @Test
    fun kiWildShapeChannel() {
        assertEquals(5, max("monk", 6, Abilities(wis = 14), "Ki Pool"))
        assertEquals(listOf(1, 1, 2, 2, 3), (4..8).map { max("druid", it, name = "Wild Shape") })
        assertNull(max("druid", 20, name = "Wild Shape"))
        assertEquals(3, max("cleric", 1, name = "Channel Energy"))
        assertEquals(1, max("magus", 1, Abilities(int = 10), "Arcane Pool"))
    }

    @Test
    fun hiddenAndCustom() {
        val c = Character(
            "1", "T", listOf(ClassEntry("paladin", 4)),
            trackers = listOf(Tracker("t", "Pearl of Power", 1, ResourceUnit.PER_DAY)),
            hiddenPresets = setOf("paladin-loh"),
        )
        assertEquals(listOf("Smite Evil", "Pearl of Power"), c.resources().map { it.name })
        assertEquals("res:t", c.resources().last().key)
    }
}
