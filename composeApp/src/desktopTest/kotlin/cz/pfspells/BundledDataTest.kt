package cz.pfspells

import cz.pfspells.data.CasterClasses
import cz.pfspells.data.SpellParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    /** Alternativní rasové traity z AoN: soubor jde načíst, rasy existují v appce, nahrazované traity sedí na výchozí. */
    @Test
    fun bundledRacialTraits() {
        val db = cz.pfspells.data.RacialTraitRepository.parse(File("src/commonMain/composeResources/files/RacialTraits.json").readText())
        assertTrue(db.all.size > 500)
        assertEquals(db.all.size, db.all.map { it.id }.toSet().size)
        assertTrue(db.all.all { it.races.all { r -> cz.pfspells.data.RaceCatalog.get(r) != null } })
        assertTrue(db.all.all { it.text.isNotBlank() && "<" !in it.text })
        val jinx = db.forRace("halfling").first { it.name == "Halfling Jinx" }
        assertEquals(listOf("halfling luck"), jinx.replaces)
        assertTrue(db.forRace("human").size > 30)
        // většina nahrazovaných traitů je mezi výchozími traity rasy (zbytek: úpravy atributů, typ, překlepy AoN)
        val all = db.all.flatMap { t -> t.replaces.map { t.races.first() to it } }
        val known = all.count { (r, n) -> cz.pfspells.data.RacialTraits.defaults(r).orEmpty().any { it.equals(n, ignoreCase = true) } }
        println("Replaced traits matching defaults: $known / ${all.size}")
        assertTrue(known > all.size * 0.85)
    }

    /** Prestižní classy z AoN: tabulka po úrovních, texty v PrestigeClasses.json, id bez kolize se základními classami. */
    @Test
    fun bundledPrestigeClasses() {
        val all = cz.pfspells.data.PRESTIGE_TABLE
        assertTrue(all.size > 100)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(all.all { p -> p.maxLevel in 1..10 && listOf(p.forts, p.refs, p.wills).all { it.size == p.maxLevel } && p.castingKinds.size == p.maxLevel })
        assertTrue(all.none { cz.pfspells.data.CLASS_TABLE.any { c -> c.id == it.id } })
        assertTrue(all.count { it.advanceKinds.isNotEmpty() } > 50)
        val texts = cz.pfspells.data.PrestigeTextRepository.parse(File("src/commonMain/composeResources/files/PrestigeClasses.json").readText())
        assertEquals(all.map { it.id }.toSet(), texts.keys)
        assertTrue("Spell Critical" in texts.getValue("eldritch_knight").features)
        assertTrue(all.all { p -> p.classSkills.all { s -> s == "knowledge" || s.startsWith("knowledge.") || cz.pfspells.data.Skills.all.any { it.id == s } } })
    }

    /** Archetypy z AoN: soubor jde načíst, classy existují v appce, texty bez HTML. */
    @Test
    fun bundledArchetypes() {
        val db = cz.pfspells.data.ArchetypeRepository.parse(File("src/commonMain/composeResources/files/Archetypes.json").readText())
        assertTrue(db.all.size > 1000)
        assertEquals(db.all.size, db.all.map { it.id }.toSet().size)
        assertTrue(db.all.all { cz.pfspells.data.Archetypes.supports(it.classId) })
        assertTrue(db.all.all { "<" !in it.text && "<" !in it.summary })
        val withText = db.all.count { it.text.isNotBlank() && it.source.isNotBlank() }
        println("Archetypes with text and source: $withText / ${db.all.size}")
        assertTrue(withText > db.all.size * 0.95)
        val ss = db.forClass("paladin").first { it.name == "Sacred Servant" }
        assertTrue("Smite Evil" in ss.replaces)
        assertTrue(ss.source.startsWith("Advanced Player's Guide"))
        assertTrue("celestial spirit" in ss.text)
        // ručně zadané efekty patří ke skutečným archetypům
        val missing = cz.pfspells.data.ARCHETYPE_MODS.keys.filter { db.get(it) == null }
        assertEquals(emptyList(), missing)
        // kouzla přidaná archetypy (z textu i ručně) a změněné spell listy existují v databázi kouzel
        val spells = SpellParser.parse(File("src/commonMain/composeResources/files/SpellList.json").readText())
        val ids = spells.map { it.id }.toSet()
        assertEquals(emptyList(), db.all.flatMap { a -> a.spells.keys.filter { it !in ids }.map { "${a.id}: $it" } })
        assertTrue(db.all.sumOf { it.spells.size } > 200)
        assertEquals(mapOf("confusion-lesser" to 1, "detect-thoughts" to 2), db.get("witch:rhetorician")!!.spells.filterValues { it <= 2 })
        val listKeys = spells.flatMap { it.levels.keys }.toSet()
        for ((id, m) in cz.pfspells.data.ARCHETYPE_MODS) {
            val ch = m.casting ?: continue
            assertEquals(emptyList(), (ch.lists.orEmpty() + ch.addLists).filter { it !in listKeys }, id)
            assertEquals(emptyList(), ch.spells.keys.map(SpellParser::slug).filter { it !in ids }, id)
            ch.table?.let { assertNotNull(cz.pfspells.data.CasterClasses.get(it), id) }
        }
        // unchained classa dostane archetypy základní classy
        assertEquals(db.forClass("rogue"), db.forClass("rogue_unchained"))
        assertTrue(db.forClass("rogue").isNotEmpty())
    }
}
