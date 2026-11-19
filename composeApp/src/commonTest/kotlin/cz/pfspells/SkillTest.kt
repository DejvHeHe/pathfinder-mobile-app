package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.FeatPrereqs
import cz.pfspells.data.PrereqStatus.MET
import cz.pfspells.data.PrereqStatus.UNKNOWN
import cz.pfspells.data.PrereqStatus.UNMET
import cz.pfspells.data.Skills
import cz.pfspells.data.isClassSkill
import cz.pfspells.data.skillPoints
import cz.pfspells.data.skillTotal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Skilly: class skills, celkový bonus, skill body, prerekvizity featů na ranky. */
class SkillTest {
    // Rogue 3 / Wizard 2, elf (DEX +2, INT +2 → INT 16)
    private val c = Character(
        "1", "Test",
        listOf(ClassEntry("rogue", 3), ClassEntry("wizard", 2)),
        abilities = Abilities(dex = 14, int = 14, wis = 12),
        race = "elf",
        skills = mapOf(
            "acrobatics" to 5, "perception" to 3, "knowledge.arcana" to 2, "craft:alchemy" to 4, "spellcraft" to 1,
        ),
    )

    private fun st(text: String) = FeatPrereqs.status(c, null, text)

    @Test
    fun classSkills() {
        assertTrue(c.isClassSkill("acrobatics")) // rogue
        assertTrue(c.isClassSkill("spellcraft")) // wizard
        assertTrue(c.isClassSkill("knowledge.arcana")) // wizard: všechny Knowledge
        assertTrue(c.isClassSkill("knowledge.local")) // rogue jen dungeoneering a local
        assertTrue(c.isClassSkill("craft:alchemy"))
        assertFalse(c.isClassSkill("survival"))
    }

    @Test
    fun totals() {
        // Acrobatics: 5 ranků + DEX 16 (+3) + class 3
        assertEquals(11, c.skillTotal("acrobatics"))
        // Perception: 3 + WIS 12 (+1) + class 3 + elf 2
        assertEquals(9, c.skillTotal("perception"))
        // trained only bez ranků
        assertNull(c.skillTotal("disableDevice"))
        // bez ranků, ne class skill: jen atribut
        assertEquals(1, c.skillTotal("survival"))
    }

    @Test
    fun points() {
        // INT 16 → +3: rogue (8+3)×3 + wizard (2+3)×2
        assertEquals(33 + 10, c.skillPoints())
        assertEquals(43 + 5, c.copy(extraSkillPoints = 5).skillPoints())
        // člověk +1 za úroveň
        assertEquals(c.copy(race = "human").let { it.skillPoints() }, (8 + 2) * 3 + (2 + 2) * 2 + 5)
    }

    @Test
    fun prerequisites() {
        assertEquals(MET, st("Acrobatics 5 ranks"))
        assertEquals(UNMET, st("Acrobatics 6 ranks"))
        assertEquals(MET, st("Perception 1 rank"))
        assertEquals(MET, st("5 ranks in Acrobatics"))
        assertEquals(MET, st("Knowledge (arcana) 2 ranks"))
        assertEquals(UNMET, st("Knowledge (planes) 1 rank"))
        assertEquals(MET, st("Knowledge (arcana, dungeoneering, local, nature, planes, or religion) 2 ranks"))
        assertEquals(MET, st("1 rank in at least one Knowledge skill"))
        assertEquals(MET, st("Craft (alchemy) 3 ranks"))
        assertEquals(MET, st("Craft (any) 4 ranks"))
        assertEquals(UNMET, st("Craft (traps) 1 rank"))
        assertEquals(UNMET, st("Perform (dance) 1 rank"))
        // „A or B N ranks“ – číslo platí pro obě
        assertEquals(MET, st("Knowledge (nature) or Acrobatics 5 ranks"))
        assertEquals(UNMET, st("Knowledge (nature) or Survival 5 ranks"))
        // postava bez skillů → ověř ručně
        assertEquals(UNKNOWN, FeatPrereqs.status(c.copy(skills = emptyMap()), null, "Acrobatics 5 ranks"))
        // „Intimidate 5 ranks“ není atribut INT
        assertEquals(UNMET, st("Intimidate 5 ranks"))
    }

    @Test
    fun keys() {
        assertEquals("craft:alchemy", Skills.key("craft", " Alchemy "))
        assertEquals("craft", Skills.idOf("craft:alchemy"))
        assertEquals("alchemy", Skills.subtypeOf("craft:alchemy"))
        assertNull(Skills.subtypeOf("perception"))
    }
}
