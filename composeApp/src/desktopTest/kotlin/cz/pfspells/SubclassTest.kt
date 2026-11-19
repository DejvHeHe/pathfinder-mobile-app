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
import cz.pfspells.data.withSubclass
import cz.pfspells.data.DRUID_DOMAINS
import cz.pfspells.data.domainsFor
import cz.pfspells.data.subdomainParents
import cz.pfspells.data.subdomainsOf
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

    @Test
    fun subclassMapChanges() {
        var sub = emptyMap<String, String>()
            .withSubclass(SubclassSlot.DOMAIN1, "fire").withSubclass(SubclassSlot.SUBDOMAIN1, "ash")
        assertEquals("ash", sub[SubclassSlot.SUBDOMAIN1])
        // zrušená doména ruší i subdoménu
        sub = sub.withSubclass(SubclassSlot.DOMAIN1, null)
        assertTrue(sub.isEmpty())
        // jiná specializace ruší opoziční školy
        sub = emptyMap<String, String>().withSubclass(SubclassSlot.SCHOOL, "evocation")
            .withSubclass(SubclassSlot.OPPOSITION1, "illusion").withSubclass(SubclassSlot.SCHOOL, "abjuration")
        assertEquals(mapOf(SubclassSlot.SCHOOL to "abjuration"), sub)
    }

    @Test
    fun subdomainsBelongToTheirDomain() {
        assertEquals(listOf("arson", "ash", "smoke"), idx.subdomainsOf("fire", "cleric"))
        assertTrue(idx.subdomainsOf("law", "cleric").any { it.startsWith("archon") })
        assertTrue(idx.subdomainsOf("good", "cleric").any { it.startsWith("archon") })
        assertTrue("metal" in idx.subdomainsOf("earth", "cleric"))
        assertFalse("metal" in idx.subdomainsOf("earth", "druid"))
        // druid jen přírodní domény
        assertEquals(DRUID_DOMAINS, idx.domainsFor("druid"))
        assertTrue(idx.domainsFor("cleric").size > 30)
        // téměř každá subdoména ze zdroje má známou doménu
        val unknown = idx.names(SubclassKind.SUBDOMAIN).filter { subdomainParents(it).isEmpty() }
        assertTrue(unknown.size <= 3, "neznámé subdomény: $unknown")
        // jiná doména zruší cizí subdoménu, vlastní nechá
        val sub = mapOf(SubclassSlot.DOMAIN1 to "fire", SubclassSlot.SUBDOMAIN1 to "ash")
        assertEquals(null, sub.withSubclass(SubclassSlot.DOMAIN1, "water")[SubclassSlot.SUBDOMAIN1])
        assertEquals("ash", sub.withSubclass(SubclassSlot.DOMAIN1, "fire")[SubclassSlot.SUBDOMAIN1])
    }
}
