package cz.pfspells

import cz.pfspells.data.CasterClasses
import cz.pfspells.data.SpellParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Ověří, že přibalená data jdou načíst a sedí na spell listy class. */
class BundledDataTest {
    @Test
    fun bundledSpellListParses() {
        val text = File("src/commonMain/composeResources/files/SpellList.json").readText()
        val t0 = System.currentTimeMillis()
        val spells = SpellParser.parse(text)
        println("Parsed ${spells.size} spells in ${System.currentTimeMillis() - t0} ms")
        assertTrue(spells.size > 2900)

        val fireball = spells.first { it.name == "Fireball" }
        assertEquals(3, CasterClasses.get("wizard")!!.spellLevelOf(fireball))
        assertEquals("Reflex half", fireball.savingThrow)

        // každá classa musí mít na svém listu spelly
        for (cls in CasterClasses.all) {
            val n = spells.count { cls.spellLevelOf(it) != null }
            println("${cls.name}: $n")
            assertTrue(n > 50, "${cls.name} má jen $n spellů")
        }
        assertEquals(spells.size, spells.map { it.id }.toSet().size)

        // PFS legal: většina ano, část ne (pole ze zdroje se opravdu čte)
        assertTrue(fireball.pfsLegal)
        val notPfs = spells.count { !it.pfsLegal }
        assertTrue(notPfs in 100 until spells.size / 2, "not PFS legal: $notPfs")

        // každá škola a subškola ze zdroje má český název
        spells.map { it.school }.distinct().forEach { assertTrue(CsStrings.school(it) != it.replaceFirstChar { c -> c.uppercase() }, it) }
    }
}
