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

    /** Hexy a zjevení z Foundry: soubor jde načíst, vzorce spočítat a zjevení pasují na mystéria appky. */
    @Test
    fun bundledClassAbilities() {
        val db = cz.pfspells.data.ClassAbilityRepository.parse(File("src/commonMain/composeResources/files/ClassAbilities.json").readText())
        assertTrue(db.all.size > 400)
        assertEquals(db.all.size, db.all.map { it.id }.toSet().size)
        assertTrue(db.forClass("witch").size > 80)
        assertTrue(db.forClass("shaman").size > 50)
        assertTrue(db.forClass("oracle").size > 250)
        assertTrue(db.all.all { it.text.isNotBlank() && "@UUID" !in it.text && "<p>" !in it.text })
        val c = cz.pfspells.data.Character("c", "C", listOf(cz.pfspells.data.ClassEntry("oracle", 20)))
        val bad = db.all.filter { it.uses != null && cz.pfspells.data.FoundryFormula.uses(it.uses!!, 20, c) == null }
        assertTrue(bad.isEmpty(), bad.joinToString { it.name })

        // mystéria a domény appky (ze zdroje kouzel) mají data ve Foundry
        val index = cz.pfspells.data.SubclassIndex(SpellParser.parse(File("src/commonMain/composeResources/files/SpellList.json").readText()))
        val mysteries = db.all.flatMap { it.mysteries }.toSet()
        val noRevelations = index.names(cz.pfspells.data.SubclassKind.MYSTERY).filter { it !in mysteries }
        println("Mysteries without revelations: $noRevelations")
        assertTrue(noRevelations.size <= 6, noRevelations.toString())
        val noPowers = index.names(cz.pfspells.data.SubclassKind.DOMAIN).filter { cz.pfspells.data.SubclassPowers.of("domain:$it") == null }
        println("Domains without Foundry powers: $noPowers")
        assertTrue(noPowers.size <= 4, noPowers.toString())
    }
}
