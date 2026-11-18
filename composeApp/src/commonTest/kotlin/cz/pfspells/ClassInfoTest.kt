package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.Ability
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Character
import cz.pfspells.data.ClassCatalog
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.ClassGroup
import cz.pfspells.data.abilityMod
import cz.pfspells.data.group
import cz.pfspells.data.stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClassInfoTest {
    @Test
    fun everyCasterHasClassData() {
        CasterClasses.all.forEach { assertNotNull(ClassCatalog.get(it.id), it.id) }
        assertTrue(ClassCatalog.all.size >= 40)
        assertEquals(ClassGroup.NONE, ClassCatalog.get("fighter")!!.group)
        assertEquals(ClassGroup.FULL, ClassCatalog.get("wizard")!!.group)
        assertEquals(ClassGroup.FOUR, ClassCatalog.get("medium")!!.group)
        assertEquals(ClassGroup.NPC, ClassCatalog.get("commoner")!!.group)
    }

    @Test
    fun multiclassStats() {
        // Fighter 5 / Wizard 3: BAB 5 + 1, Fort 4 + 1, Ref 1 + 1, Will 1 + 3
        val c = Character("x", "X", listOf(ClassEntry("fighter", 5), ClassEntry("wizard", 3)))
        val st = c.stats()
        assertEquals(8, st.level)
        assertEquals(6, st.bab)
        assertEquals(5, st.fort)
        assertEquals(2, st.ref)
        assertEquals(4, st.will)
    }

    @Test
    fun abilities() {
        assertEquals(listOf(-5, -1, 0, 0, 4), listOf(1, 9, 10, 11, 18).map { abilityMod(it) })
        val a = Abilities().with(Ability.INT, 18)
        assertEquals(18, a[Ability.INT])
        assertEquals(10, a[Ability.STR])
    }
}
