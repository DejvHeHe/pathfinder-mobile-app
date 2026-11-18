package cz.pfspells

import cz.pfspells.data.ClassEntry
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SpellParser
import cz.pfspells.data.SpellSource
import cz.pfspells.data.SubclassKind
import cz.pfspells.data.SubclassSlot
import cz.pfspells.data.bonusSpellsByLevel
import cz.pfspells.data.hasSubclassSlot
import cz.pfspells.data.isOppositionSpell
import cz.pfspells.data.isSchoolSpell
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Podtřídy z access_ways v přibaleném SpellList.json. */
class SubclassTest {
    private val db by lazy {
        SpellDatabase(SpellParser.parse(File("src/commonMain/composeResources/files/SpellList.json").readText()), SpellSource.BUNDLED, null)
    }
    private val idx get() = db.subclasses
    private fun names(kind: SubclassKind, name: String, lvl: Int) = idx.spells(kind, name)[lvl].orEmpty().map { it.name }

    @Test
    fun parsesSubclassLists() {
        assertTrue("Burning Hands" in names(SubclassKind.DOMAIN, "fire", 1))
        assertTrue("Fireball" in names(SubclassKind.DOMAIN, "fire", 3))
        assertTrue("Identify" in names(SubclassKind.BLOODLINE, "arcane", 1))
        assertTrue("Burning Hands" in names(SubclassKind.MYSTERY, "flame", 1))
        assertTrue("Fireball" in names(SubclassKind.ELEMENT, "fire", 3))
        // čištění zdroje: mezery, slepené záznamy, překlepy
        val domains = idx.names(SubclassKind.DOMAIN)
        assertTrue("repose" in domains && "war" in domains && " war" !in domains)
        assertFalse(idx.names(SubclassKind.BLOODLINE).any { "elemental school" in it })
        assertTrue("aristocracy" in idx.names(SubclassKind.SUBDOMAIN) && "aristocra" !in idx.names(SubclassKind.SUBDOMAIN))
        assertTrue("juju" in idx.names(SubclassKind.MYSTERY))
        println("domains=${domains.size} subdomains=${idx.names(SubclassKind.SUBDOMAIN).size} mysteries=${idx.names(SubclassKind.MYSTERY).size} bloodlines=${idx.names(SubclassKind.BLOODLINE).size}")
    }

    @Test
    fun clericDomainsWithSubdomain() {
        val e = ClassEntry("cleric", 7, subclass = mapOf(SubclassSlot.DOMAIN1 to "fire", SubclassSlot.DOMAIN2 to "magic", SubclassSlot.SUBDOMAIN2 to "arcane"))
        val bonus = e.bonusSpellsByLevel(idx)
        assertEquals((1..4).toList(), bonus.keys.sorted()) // cleric 7 → max 4. level
        assertTrue(bonus[1]!!.any { it.name == "Burning Hands" })
        assertTrue(bonus[1]!!.any { it.name == "Magic Aura" }) // arcane subdoména nahrazuje 1. level magic
        assertTrue(e.hasSubclassSlot())
    }

    @Test
    fun sorcererBloodlineByLevel() {
        val e = ClassEntry("sorcerer", 5, subclass = mapOf(SubclassSlot.BLOODLINE to "arcane"))
        // bloodline spell N od úrovně 2N+1 → na 5. úrovni levely 1 a 2
        assertEquals(listOf(1, 2), e.bonusSpellsByLevel(idx).keys.sorted())
        assertFalse(e.hasSubclassSlot())
    }

    @Test
    fun oracleMysteryByLevel() {
        val e = ClassEntry("oracle", 4, subclass = mapOf(SubclassSlot.MYSTERY to "flame"))
        assertEquals(listOf(1, 2), e.bonusSpellsByLevel(idx).keys.sorted())
    }

    @Test
    fun wizardSchools() {
        val fireball = db.get("fireball")!!
        val charm = db.get("charm-person")!!
        val evoker = ClassEntry("wizard", 5, subclass = mapOf(SubclassSlot.SCHOOL to "evocation", SubclassSlot.OPPOSITION1 to "enchantment"))
        assertTrue(evoker.isSchoolSpell(fireball))
        assertTrue(evoker.isOppositionSpell(charm))
        assertTrue(evoker.hasSubclassSlot())
        val fireSchool = ClassEntry("wizard", 5, subclass = mapOf(SubclassSlot.SCHOOL to "element:fire", SubclassSlot.OPPOSITION1 to "element:water"))
        assertTrue(fireSchool.isSchoolSpell(fireball))
        assertFalse(ClassEntry("wizard", 5, subclass = mapOf(SubclassSlot.SCHOOL to "universalist")).hasSubclassSlot())
    }
}
