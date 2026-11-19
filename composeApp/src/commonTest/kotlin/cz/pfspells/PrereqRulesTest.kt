package cz.pfspells

import cz.pfspells.data.Alignment
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.FeatPick
import cz.pfspells.data.deityConflicts
import cz.pfspells.data.FeatPrereqs
import cz.pfspells.data.PrereqStatus.MET
import cz.pfspells.data.PrereqStatus.UNKNOWN
import cz.pfspells.data.PrereqStatus.UNMET
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Odvozené prerekvizity: class features, přesvědčení, božstvo, velikost, typ, HD, záchrany, proficience, druh kouzel. */
class PrereqRulesTest {
    // Cleric 5 / Fighter 3, trpaslík, LG, Torag
    private val c = Character(
        "1", "Test",
        listOf(ClassEntry("cleric", 5), ClassEntry("fighter", 3)),
        race = "dwarf", alignment = Alignment.LG, deity = "Torag",
    )

    private fun st(text: String, ch: Character = c) = FeatPrereqs.status(ch, null, text)

    @Test
    fun classFeatures() {
        assertEquals(MET, st("Channel energy class feature"))
        assertEquals(MET, st("ability to channel energy"))
        assertEquals(MET, st("channel energy 3d6"))
        assertEquals(UNMET, st("channel energy 4d6"))
        assertEquals(UNKNOWN, st("channel positive energy class feature")) // klerik volí pozitivní / negativní
        assertEquals(MET, st("armor training class feature"))
        assertEquals(UNMET, st("weapon training class feature"))
        assertEquals(MET, st("bravery class feature"))
        assertEquals(UNMET, st("rage class feature"))
        assertEquals(MET, st("domain class feature"))
        assertEquals(MET, st("bardic performance or channel energy"))
        assertEquals(MET, st("brawler’s flurry class feature or channel energy class feature"))
        val druid = Character("2", "D", listOf(ClassEntry("druid", 4)))
        assertEquals(MET, st("wild shape class feature", druid))
        assertEquals(UNKNOWN, st("animal companion class feature", druid)) // pouto s přírodou: zvíře nebo doména
        assertEquals(MET, st("sneak attack +2d6", Character("3", "R", listOf(ClassEntry("rogue", 3)))))
    }

    @Test
    fun alignmentAndDeity() {
        assertEquals(MET, st("good alignment"))
        assertEquals(MET, st("any lawful alignment"))
        assertEquals(UNMET, st("evil alignment"))
        assertEquals(UNMET, st("nongood alignment"))
        assertEquals(MET, st("good or chaotic alignment"))
        assertEquals(UNMET, st("chaotic neutral alignment"))
        assertEquals(MET, st("chaotic neutral alignment", c.copy(alignment = Alignment.CN)))
        assertEquals(UNKNOWN, st("good alignment", c.copy(alignment = null)))
        assertEquals(MET, st("worshiper of Torag"))
        assertEquals(UNMET, st("worshiper of Iomedae"))
        assertEquals(UNKNOWN, st("worshiper of Iomedae", c.copy(deity = "")))
        assertEquals(MET, st("must worship a deity"))
        assertEquals(MET, st("worshipper of torag"))
        // přesvědčení božstva ze seznamu (Torag LG)
        assertEquals(MET, st("worshiper of a good deity"))
        assertEquals(MET, st("worshiper of a lawful deity"))
        assertEquals(UNMET, st("worshiper of an evil deity"))
        assertEquals(MET, st("alignment must match that of your worshiped deity"))
        assertEquals(UNMET, st("alignment must match that of your worshiped deity", c.copy(alignment = Alignment.NG)))
        assertEquals(MET, st("alignment must be within one step of your deity's", c.copy(alignment = Alignment.NG)))
        assertEquals(UNMET, st("alignment must be within one step of your deity's", c.copy(alignment = Alignment.CG)))
        // vlastní božstvo: jméno se porovná volně, přesvědčení appka nezná
        val custom = c.copy(deity = "Velký Dub")
        assertEquals(UNKNOWN, st("worshiper of a good deity", custom))
        assertEquals(UNMET, st("worshiper of Iomedae", custom))
    }

    @Test
    fun deityCatalog() {
        val torag = cz.pfspells.data.DeityCatalog.get("torag")!!
        assertEquals(Alignment.LG, torag.alignment)
        assertTrue("earth" in torag.domains)
        assertTrue(cz.pfspells.data.DeityCatalog.all.size > 400)
        // subdomény božstva (Torag: Caves, Metal…, ne Ash)
        val subs = torag.subdomains!!
        assertTrue("caves" in subs && "metal" in subs && "archon" in subs)
        assertTrue("ash" !in subs)
        // klerik s doménami, které Torag nedává
        val e = ClassEntry("cleric", 5, subclass = mapOf("domain1" to "earth", "subdomain1" to "metal", "domain2" to "fire", "subdomain2" to "ash"))
        assertEquals(listOf("fire", "ash"), e.deityConflicts(torag))
        assertEquals(emptyList(), e.deityConflicts(null))
        assertEquals(emptyList(), ClassEntry("druid", 5, subclass = mapOf("domain1" to "fire")).deityConflicts(torag))
    }

    @Test
    fun sizeTypeHitDiceSaves() {
        assertEquals(MET, st("Medium size"))
        assertEquals(UNMET, st("Small size or smaller"))
        assertEquals(MET, st("small size or smaller", c.copy(race = "halfling")))
        assertEquals(UNMET, st("size Large or larger"))
        assertEquals(MET, st("humanoid"))
        assertEquals(UNMET, st("giant subtype"))
        assertEquals(MET, st("dwarf subtype"))
        assertEquals(UNKNOWN, st("giant subtype", c.copy(race = null)))
        assertEquals(MET, st("8 Hit Dice"))
        assertEquals(UNMET, st("10 Hit Dice"))
        // cleric 5 (fort 4) + fighter 3 (fort 3)
        assertEquals(MET, st("base Fortitude save bonus +6"))
        assertEquals(UNMET, st("base Reflex saving throw bonus +4"))
    }

    @Test
    fun proficiencies() {
        assertEquals(MET, st("proficiency with heavy armor")) // fighter
        assertEquals(MET, st("Heavy armor proficiency"))
        assertEquals(MET, st("proficiency with bucklers and light shields"))
        assertEquals(MET, st("proficient with all martial weapons"))
        val wizard = Character("4", "W", listOf(ClassEntry("wizard", 5)))
        assertEquals(UNMET, st("proficiency with light armor", wizard))
        assertEquals(UNMET, st("proficiency with medium or heavy armor", wizard))
        assertEquals(MET, st("proficiency with light armor", wizard.copy(feats = listOf(FeatPick("x", "Armor Proficiency, Light")))))
        assertEquals(UNMET, st("Shield Proficiency", wizard))
    }

    @Test
    fun spellKinds() {
        assertEquals(MET, st("ability to cast divine spells"))
        assertEquals(UNMET, st("ability to cast arcane spells"))
        assertEquals(MET, st("ability to cast 3rd-level divine spells"))
        assertEquals(UNMET, st("ability to cast 4th-level spells"))
        assertEquals(MET, st("ability to cast spells"))
        assertEquals(UNMET, st("arcane spellcaster"))
        assertEquals(MET, st("ability to cast cleric spells"))
    }

    @Test
    fun manualConfirmation() {
        val text = "Halfling jinx trait"
        assertEquals(UNKNOWN, FeatPrereqs.evaluate(c, null, text).single().status)
        val confirmed = c.copy(confirmedPrereqs = setOf(FeatPrereqs.confirmKey(text)))
        val p = FeatPrereqs.evaluate(confirmed, null, text).single()
        assertEquals(MET, p.status)
        assertTrue(p.confirmed)
    }
}
