package cz.pfspells

import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.FeatPrereqs
import cz.pfspells.data.PrereqStatus.MET
import cz.pfspells.data.PrereqStatus.UNKNOWN
import cz.pfspells.data.PrereqStatus.UNMET
import cz.pfspells.data.RacialTrait
import cz.pfspells.data.RacialTraitPick
import cz.pfspells.data.RacialTraits
import cz.pfspells.data.Ability
import cz.pfspells.data.featSlots
import cz.pfspells.data.scores
import cz.pfspells.data.skillPoints
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RacialTraitsTest {
    private fun char(race: String, vararg picks: RacialTraitPick) =
        Character("c", "C", listOf(ClassEntry("fighter", 4)), race = race, racialTraits = picks.toList())

    private val jinx = RacialTraitPick("halfling:halfling-jinx", "Halfling Jinx", listOf("halfling luck"))
    private val dualTalent = RacialTraitPick("human:dual-talent", "Dual Talent", listOf("+2 to one ability score", "bonus feat", "skilled"))

    private fun st(c: Character, text: String) = FeatPrereqs.status(c, null, text)

    @Test
    fun defaultsFromAon() {
        assertTrue("Stonecunning" in RacialTraits.defaults("dwarf")!!)
        assertTrue("Halfling Luck" in RacialTraits.defaults("halfling")!!)
        assertTrue("Kitsune Magic" in RacialTraits.defaults("kitsune")!!)
        assertEquals(RacialTraits.defaults("lashunta-male"), RacialTraits.defaults("lashunta-female"))
        assertNull(RacialTraits.defaults("ogre"))
    }

    @Test
    fun prerequisites() {
        val halfling = char("halfling")
        assertEquals(MET, st(halfling, "halfling luck racial trait"))
        assertEquals(UNKNOWN, st(halfling, "halfling jinx racial trait"))
        // vybraný alternativní trait: má ho; nahrazený výchozí: nemá
        val jinxed = char("halfling", jinx)
        assertEquals(MET, st(jinxed, "halfling jinx racial trait"))
        assertEquals(UNMET, st(jinxed, "halfling luck racial trait"))
        assertEquals(MET, st(jinxed, "keen senses racial trait"))
        // trait známý jen z AoN (ne z ruční tabulky): kitsune magic
        assertEquals(MET, st(char("kitsune"), "kitsune magic racial trait"))
        // „A or B“: stačí jeden, nahrazený nevadí
        assertEquals(MET, st(jinxed, "halfling luck or fearless racial trait"))
    }

    @Test
    fun humanBonusFeatAndSkilled() {
        val human = char("human")
        val talented = char("human", dualTalent)
        assertEquals(1, human.featSlots().race)
        assertEquals(0, talented.featSlots().race)
        assertEquals(human.skillPoints() - 4, talented.skillPoints()) // Skilled: +1 za každou ze 4 úrovní
    }

    @Test
    fun conflictsAndStrictMode() {
        val other = RacialTraitPick("halfling:lucky", "Lucky", listOf("halfling luck", "fearless"))
        assertEquals(listOf("halfling luck"), RacialTraits.conflicts(listOf(jinx, other)))
        assertTrue(RacialTraits.conflicts(listOf(jinx)).isEmpty())
        val t = RacialTrait("halfling:lucky", listOf("halfling"), "Lucky", listOf("fearless", "halfling luck"))
        assertEquals("halfling luck", RacialTraits.blockedBy(char("halfling", jinx), t))
        assertNull(RacialTraits.blockedBy(char("halfling"), t))
    }

    @Test
    fun abilityScoreVariants() {
        // Keen Kitsune: +2 DEX, +2 INT místo +2 DEX, +2 CHA (−2 STR zůstává)
        val kitsune = char("kitsune", RacialTraitPick("kitsune:keen-kitsune", "Keen Kitsune", listOf("ability score modifiers")))
        assertEquals(listOf(8, 12, 10, 12, 10, 10), kitsune.scores.let { listOf(it.str, it.dex, it.con, it.int, it.wis, it.cha) })
        // Dual Talent: +2 ke dvěma zvoleným atributům, volba „+2 k jednomu“ (raceBonus) neplatí
        val dual = dualTalent.copy(abilities = listOf(Ability.STR, Ability.WIS))
        val human = char("human", dual).copy(raceBonus = Ability.CHA)
        assertEquals(12, human.scores.str)
        assertEquals(12, human.scores.wis)
        assertEquals(10, human.scores.cha)
        assertTrue(RacialTraits.missingChoices(human).isEmpty())
        assertEquals(listOf("Dual Talent"), RacialTraits.missingChoices(char("human", dualTalent)).map { it.name })
        // Orc Atavism: +2 STR, −2 ke zvolenému mentálnímu (jiný než mentální se nepočítá)
        val atavism = RacialTraitPick("half-orc:orc-atavism", "Orc Atavism", listOf("ability score modifiers"))
        val orc = char("half-orc", atavism.copy(abilities = listOf(Ability.INT))).copy(raceBonus = Ability.DEX)
        assertEquals(listOf(12, 10, 8), listOf(orc.scores.str, orc.scores.dex, orc.scores.int))
        assertEquals(10, char("half-orc", atavism.copy(abilities = listOf(Ability.DEX))).scores.dex)
        // Tree-Born: gathlain bez postihu k Odolnosti
        assertEquals(10, char("gathlain", RacialTraitPick("gathlain:tree-born", "Tree-Born")).scores.con)
        assertEquals(8, char("gathlain").scores.con)
    }
}
