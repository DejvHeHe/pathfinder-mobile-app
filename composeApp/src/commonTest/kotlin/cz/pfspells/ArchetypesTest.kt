package cz.pfspells

import cz.pfspells.data.Archetype
import cz.pfspells.data.ArchetypePick
import cz.pfspells.data.Archetypes
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.toPick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArchetypesTest {
    private val sacredServant = Archetype("paladin:sacred-servant", "paladin", "Sacred Servant", listOf("Smite Evil", "Divine Bond", "Aura of Resolve"))
    private val hospitaler = Archetype("paladin:hospitaler", "paladin", "Hospitaler", listOf("Smite Evil", "Aura of Justice"))
    private val warden = Archetype("paladin:banishing-warden", "paladin", "Banishing Warden", listOf("Class Skills", "3rd-level Mercy"))

    @Test
    fun unchainedUsesBaseClass() {
        assertEquals("rogue", Archetypes.baseClass("rogue_unchained"))
        assertEquals("wizard", Archetypes.baseClass("wizard"))
    }

    @Test
    fun npcAndCreatureClassesHaveNone() {
        assertTrue(Archetypes.supports("paladin"))
        assertTrue(Archetypes.supports("fighter"))
        assertFalse(Archetypes.supports("commoner"))
        assertFalse(Archetypes.supports("animal"))
        assertFalse(Archetypes.supports("nonsense"))
    }

    @Test
    fun conflictsOnSameFeature() {
        assertEquals(listOf("Smite Evil"), Archetypes.conflicts(listOf(sacredServant.toPick(), hospitaler.toPick())))
        assertEquals(emptyList(), Archetypes.conflicts(listOf(sacredServant.toPick(), warden.toPick())))
        // stejná schopnost dvakrát v jednom archetypu není konflikt
        assertEquals(emptyList(), Archetypes.conflicts(listOf(ArchetypePick("x", "X", listOf("Mercy", "mercy")))))
    }

    @Test
    fun strictModeBlocksReplacedFeature() {
        val e = ClassEntry("paladin", 5, archetypes = listOf(sacredServant.toPick()))
        assertEquals("Smite Evil", Archetypes.blockedBy(e, hospitaler))
        assertNull(Archetypes.blockedBy(e, warden))
    }

    @Test
    fun strictModeMessage() {
        val b = cz.pfspells.data.StrictBlock.ArchetypeConflict("Smite Evil")
        assertTrue("Smite Evil" in EnStrings.strictBlocked(b))
        assertTrue("Smite Evil" in CsStrings.strictBlocked(b))
    }
}
