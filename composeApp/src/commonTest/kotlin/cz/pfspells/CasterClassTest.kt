package cz.pfspells

import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Spell
import cz.pfspells.data.SpellParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CasterClassTest {
    private fun max(cls: String, level: Int) = CasterClasses.get(cls)!!.maxSpellLevel(level)

    @Test
    fun fullCasters() {
        // Core Rulebook, tabulky Wizard / Sorcerer
        assertEquals(listOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 9, 9), (1..20).map { max("wizard", it) })
        assertEquals(listOf(1, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 9), (1..20).map { max("sorcerer", it) })
    }

    @Test
    fun sixLevelCasters() {
        // Bard: 1st na 1, 2nd na 4, 3rd na 7, 4th na 10, 5th na 13, 6th na 16
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 6, 6), (1..20).map { max("bard", it) })
    }

    @Test
    fun fourLevelCasters() {
        // Paladin: nic do 3, 1st na 4, 2nd na 7, 3rd na 10, 4th na 13
        assertEquals(listOf(-1, -1, -1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4), (1..20).map { max("paladin", it) })
    }

    private fun spell(vararg levels: Pair<String, Int>) = Spell(
        "x", "X", "evocation", null, null, mapOf(*levels), null, null, null, null, null, null, null, null, null, "", null, null, null,
    )

    @Test
    fun canCastUsesLowestListLevel() {
        val s = spell("ranger" to 1, "druid" to 2)
        val hunter = CasterClasses.get("hunter")!!
        assertEquals(1, hunter.spellLevelOf(s))
        assertTrue(hunter.canCast(s, 1))
    }

    @Test
    fun noCantripsForAlchemistAndPaladin() {
        val cantrip = spell("alchemist" to 0, "paladin" to 0)
        assertFalse(CasterClasses.get("alchemist")!!.canCast(cantrip, 10))
        assertFalse(CasterClasses.get("paladin")!!.canCast(cantrip, 10))
    }

    @Test
    fun parserBasics() {
        val json = buildString {
            append("[")
            for (i in 1..150) {
                if (i > 1) append(",")
                append("""{"Name":"Spell $i","School":"evocation","Subschool":"None","Range":"long","Description":"A\nB","access_ways":{"Level":[["sorcerer/wizard",3],["wizard",2]]}}""")
            }
            append(""",{"Name":"No list","access_ways":{"SLA":[["SLA",1]]}}]""")
        }
        val spells = SpellParser.parse(json)
        assertEquals(150, spells.size)
        val s = spells.first { it.name == "Spell 1" }
        assertEquals("spell-1", s.id)
        assertEquals(null, s.subschool)
        assertEquals(mapOf("sorcerer/wizard" to 2), s.levels)
        assertEquals(listOf("A", "B"), s.paragraphs)
    }
}
