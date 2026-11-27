package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.Character
import cz.pfspells.data.ClassCatalog
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.ClassGroup
import cz.pfspells.data.Prestige
import cz.pfspells.data.PrestigeRequirements
import cz.pfspells.data.PrereqStatus
import cz.pfspells.data.Spellbook
import cz.pfspells.data.casterLevel
import cz.pfspells.data.featSlots
import cz.pfspells.data.group
import cz.pfspells.data.stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrestigeTest {
    private val ek = ClassCatalog.get("eldritch_knight")!!.prestige!!

    @Test
    fun table() {
        assertEquals(10, ek.maxLevel)
        assertEquals(10, ek.hd)
        assertEquals(listOf("arcane"), ek.advanceKinds)
        assertEquals(0, ek.advancesAt(1, "arcane"))
        assertEquals(9, ek.advancesAt(10, "arcane"))
        assertEquals(listOf(1, 5, 9), ek.bonusFeats)
        assertEquals(ClassGroup.PRESTIGE, ClassCatalog.get("eldritch_knight")!!.group)
        assertEquals(10, ClassCatalog.get("eldritch_knight")!!.maxLevel)
        assertEquals(20, ClassCatalog.get("wizard")!!.maxLevel)
        val mt = ClassCatalog.get("mystic_theurge")!!.prestige!!
        assertEquals(listOf("arcane", "divine"), mt.advanceKinds)
    }

    @Test
    fun statsFromTable() {
        // fighter 1 (+1 / Fort +2), wizard 5 (+2 / Will +4), eldritch knight 3 (+3, Fort +2, Ref +1, Will +1)
        val c = Character("c", "C", listOf(ClassEntry("fighter", 1), ClassEntry("wizard", 5), ClassEntry("eldritch_knight", 3)))
        val st = c.stats()
        assertEquals(1 + 2 + 3, st.bab)
        assertEquals(2 + 1 + 2, st.fort)
        assertEquals(0 + 1 + 1, st.ref)
        assertEquals(0 + 4 + 1, st.will)
        assertEquals(9, c.level)
    }

    @Test
    fun casterLevelAdvances() {
        val wiz = ClassEntry("wizard", 5)
        val base = Character("c", "C", listOf(ClassEntry("fighter", 1), wiz, ClassEntry("eldritch_knight", 3)))
        // bez volby se sesílání nezvyšuje
        assertEquals(5, base.casterLevel(wiz))
        assertEquals(listOf("arcane"), Prestige.missingAdvances(base.classes[2]))
        val c = base.copy(classes = base.classes.map { if (it.classId == "eldritch_knight") it.copy(advances = mapOf("arcane" to "wizard")) else it })
        assertEquals(7, c.casterLevel(wiz))
        assertEquals(1, c.casterLevel(c.classes[0])) // fighter se nemění
        // Spellbook (výběr kouzel, detail) počítá se sesílací úrovní
        assertEquals(7, Spellbook(c, wiz).classLevel)
    }

    @Test
    fun candidatesByKind() {
        val c = Character("c", "C", listOf(ClassEntry("wizard", 3), ClassEntry("cleric", 3), ClassEntry("witch", 3), ClassEntry("fighter", 2)))
        assertEquals(listOf("wizard", "witch"), Prestige.candidates(c, "arcane").map { it.classId })
        assertEquals(listOf("cleric"), Prestige.candidates(c, "divine").map { it.classId })
        assertEquals(listOf("witch"), Prestige.candidates(c, "class:witch").map { it.classId })
        val mt = ClassCatalog.get("mystic_theurge")!!.prestige!!
        // arcane má dvě možnosti → nevybráno, divine jen klerik
        assertEquals(mapOf("divine" to "cleric"), Prestige.defaultAdvances(c, mt))
    }

    @Test
    fun bonusFeats() {
        val c = Character("c", "C", listOf(ClassEntry("wizard", 5), ClassEntry("eldritch_knight", 5)))
        assertEquals(2, c.featSlots().bonus["eldritch_knight"])
    }

    @Test
    fun requirements() {
        val wizard = Character("c", "C", listOf(ClassEntry("wizard", 5)), abilities = Abilities(int = 16))
        val fighter = Character("f", "F", listOf(ClassEntry("fighter", 6)))
        val spells = { c: Character -> PrestigeRequirements.evaluate(c, ek, null, null).first { it.label == "Spells" }.status }
        assertEquals(PrereqStatus.MET, spells(wizard))
        assertEquals(PrereqStatus.UNMET, spells(fighter))
        assertEquals(PrereqStatus.UNMET, PrestigeRequirements.overall(PrestigeRequirements.evaluate(fighter, ek, null, null)))
        assertEquals("base attack bonus +5", PrestigeRequirements.prereqText("Base Attack Bonus", "+5."))
        // Mystic Theurge: dvě části, kouzelník splní jen tajemnou
        val mt = ClassCatalog.get("mystic_theurge")!!.prestige!!
        val parts = PrestigeRequirements.evaluate(wizard, mt, null, null).first { it.label == "Spells" }.parts
        assertEquals(listOf(PrereqStatus.UNMET, PrereqStatus.MET), parts.map { it.status })
    }

    @Test
    fun noArchetypesForPrestige() {
        assertTrue(!cz.pfspells.data.Archetypes.supports("eldritch_knight"))
        assertNotNull(ClassCatalog.get("arcane_trickster")?.prestige)
        assertNull(ClassCatalog.get("wizard")?.prestige)
    }
}
