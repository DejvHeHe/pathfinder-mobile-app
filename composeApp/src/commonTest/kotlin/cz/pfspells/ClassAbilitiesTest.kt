package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.AbilityKind
import cz.pfspells.data.AbilityRules
import cz.pfspells.data.Character
import cz.pfspells.data.ClassAbility
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.FoundryFormula
import cz.pfspells.data.ResourceUnit
import cz.pfspells.data.StrictBlock
import cz.pfspells.data.StrictRules
import cz.pfspells.data.SubclassPowers
import cz.pfspells.data.resources
import cz.pfspells.data.toPick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClassAbilitiesTest {
    private fun char(e: ClassEntry, abilities: Abilities = Abilities(wis = 14, cha = 16, int = 16)) =
        Character("c", "C", listOf(e), abilities = abilities)

    private fun uses(c: Character) = c.resources().associate { it.name to it.max }

    @Test
    fun formulas() {
        val c = char(ClassEntry("cleric", 9))
        fun f(s: String, level: Int = 9) = FoundryFormula.uses(s, level, c)
        assertEquals(5, f("3 + @abilities.wis.mod"))
        assertEquals(1, f("floor((@class.unlevel - 4) / 4)"))
        assertEquals(1, f("1 + if(gte(@class.unlevel, 17), 1)"))
        assertEquals(2, f("1 + if(gte(@class.unlevel, 17), 1)", 17))
        assertEquals(1, f("clamp(floor((@class.unlevel - 11) / 3), 1, 3)"))
        assertEquals(3, f("clamp(floor((@class.unlevel - 11) / 3), 1, 3)", 20))
        assertEquals(2, f("1 + if(gt(@class.unlevel, 8), 1) + if(gt(@class.unlevel, 14), 2)"))
        assertEquals(5, f("min(5, floor((@class.unlevel + 1) / 4)) * 2 + max(1, -3)"))
        assertEquals(0, f("@class.unlevel - 20")) // nikdy záporné
        assertEquals(9, f("@attributes.hd.total"))
        // neznámá proměnná / funkce nebo chyba syntaxe → null
        assertNull(f("@resources.grit.value"))
        assertNull(f("sqrt(4)"))
        assertNull(f("3 +"))
    }

    @Test
    fun everyTableFormulaEvaluates() {
        val c = char(ClassEntry("cleric", 20))
        val bad = cz.pfspells.data.SUBCLASS_POWER_TABLE.filter { it.uses != null && FoundryFormula.uses(it.uses!!, 20, c) == null }
        assertTrue(bad.isEmpty(), bad.joinToString { "${it.key} ${it.name}: ${it.uses}" })
    }

    @Test
    fun domainPowersByLevel() {
        val good = ClassEntry("cleric", 7, subclass = mapOf("domain1" to "good", "domain2" to "fire"))
        var u = uses(char(good))
        assertEquals(5, u["Touch of Good (Good)"])
        assertEquals(5, u["Fire Bolt (Fire)"])
        assertNull(u["Holy Lance (Good)"]) // od 8. úrovně
        u = uses(char(good.copy(level = 8)))
        assertEquals(1, u["Holy Lance (Good)"])
        // subdoména nahradí schopnost své domény (Archon pod Good: Aura of Menace místo Holy Lance)
        val archon = good.copy(level = 8, subclass = good.subclass + ("subdomain1" to "archon (good, law)"))
        assertEquals(listOf("subdomain:archon@good" to "Archon", "domain:fire" to "Fire"), SubclassPowers.keysOf(archon))
        u = uses(char(archon))
        assertEquals(8, u["Aura of Menace (Archon)"])
        assertNull(u["Holy Lance (Good)"])
        // druid s doménou, Sun bez schopnosti na 1. úrovni
        assertEquals(5, uses(char(ClassEntry("druid", 1, subclass = mapOf("domain1" to "fire"))))["Fire Bolt (Fire)"])
        assertTrue(uses(char(ClassEntry("cleric", 3, subclass = mapOf("domain1" to "sun")))).keys.none { it.endsWith("(Sun)") })
        // doména, kterou Foundry nemá → obecná schopnost 1. úrovně
        assertEquals(5, uses(char(ClassEntry("cleric", 1, subclass = mapOf("domain1" to "jungle"))))["Jungle domain power (1st level)"])
    }

    @Test
    fun bloodlineAndSchoolPowers() {
        val sorc = ClassEntry("sorcerer", 9, subclass = mapOf("bloodline" to "draconic"))
        val r = char(sorc).resources().associateBy { it.name }
        assertEquals(6, r["Draconic Claws (Draconic)"]!!.max)
        assertEquals(ResourceUnit.ROUNDS, r["Draconic Claws (Draconic)"]!!.unit)
        assertEquals(1, r["Breath Weapon (Draconic)"]!!.max)
        // bloodline bez dat ve Foundry → obecná schopnost
        assertEquals(6, uses(char(ClassEntry("sorcerer", 1, subclass = mapOf("bloodline" to "aquatic"))))["Aquatic bloodline power (1st level)"])
        // kouzelník se školou: schopnosti školy místo obecné „Arcane school power“
        val evoker = uses(char(ClassEntry("wizard", 8, subclass = mapOf("school" to "evocation"))))
        assertEquals(6, evoker["Force Missile (Evocation)"])
        assertEquals(8, evoker["Elemental Wall (Evocation)"])
        assertNull(evoker["Arcane school power (1st level)"])
        assertNotNull(uses(char(ClassEntry("wizard", 1)))["Arcane school power (1st level)"])
        assertEquals(4, uses(char(ClassEntry("wizard", 1, subclass = mapOf("school" to "element:fire"))))["Fire Jet (Fire)"])
    }

    @Test
    fun slotsByLevel() {
        assertEquals(listOf(1, 2, 2, 3, 6, 11), listOf(1, 2, 3, 4, 10, 20).map { AbilityRules.slots("witch", it) })
        assertEquals(listOf(0, 1, 2, 3, 8), listOf(1, 2, 4, 8, 20).map { AbilityRules.slots("shaman", it) })
        assertEquals(listOf(1, 1, 2, 3, 6), listOf(1, 2, 3, 7, 19).map { AbilityRules.slots("oracle", it) })
        assertNull(AbilityRules.groupOf("cleric"))
    }

    private val evilEye = ClassAbility("evil-eye", "Evil Eye", AbilityKind.HEX, listOf("witch"))
    private val agony = ClassAbility("agony", "Agony", AbilityKind.MAJOR_HEX, listOf("witch"), minLevel = 10)
    private val boon = ClassAbility(
        "abadar-s-boon", "Abadar's Boon", AbilityKind.REVELATION, listOf("oracle"), mysteries = listOf("godclaw"),
        uses = "@class.unlevel", unit = ResourceUnit.PER_DAY,
    )

    @Test
    fun picksAndStrictMode() {
        // mystérium: zjevení jiného mystéria se nenabízí, obecné a bez mystéria ano
        assertTrue(AbilityRules.matchesMystery(boon, "godclaw"))
        assertTrue(!AbilityRules.matchesMystery(boon, "flame"))
        assertTrue(AbilityRules.matchesMystery(boon, null))
        assertTrue(AbilityRules.matchesMystery(evilEye, "flame"))
        // vybrané zjevení s počtem použití → schopnost v sekci Schopnosti
        val oracle = char(ClassEntry("oracle", 3, abilities = listOf(boon.toPick())))
        assertEquals(3, uses(oracle)["Abadar's Boon"])
        // přísný režim: úroveň a počet
        val witch = char(ClassEntry("witch", 1))
        assertEquals(StrictBlock.ClassAbilityLevel(10), StrictRules.classAbilityBlock(witch, "witch", agony))
        assertNull(StrictRules.classAbilityBlock(witch, "witch", evilEye))
        val full = char(ClassEntry("witch", 1, abilities = listOf(evilEye.toPick())))
        assertEquals(StrictBlock.ClassAbilitySlotsFull(1), StrictRules.classAbilityBlock(full, "witch", evilEye.copy(id = "slumber")))
    }
}
