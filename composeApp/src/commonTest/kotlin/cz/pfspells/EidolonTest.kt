package cz.pfspells

import cz.pfspells.data.Ability
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.CompanionKind
import cz.pfspells.data.CreatureSize
import cz.pfspells.data.Eidolons
import cz.pfspells.data.EvolutionPick
import cz.pfspells.data.eidolonNaturalArmor
import cz.pfspells.data.eidolonRow
import cz.pfspells.data.evolutionPointsUsed
import cz.pfspells.data.scores
import cz.pfspells.data.stats
import cz.pfspells.data.suggestedCompanionLevel
import cz.pfspells.data.withCompanion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EidolonTest {
    private fun eidolon(form: String, level: Int, evolutions: List<EvolutionPick> = emptyList()) =
        Character("e", "Eido", listOf(ClassEntry("animal", 1)), ownerId = "m", evolutions = evolutions)
            .withCompanion(CompanionKind.EIDOLON, null, level, null, false, eidolonForm = form)

    @Test
    fun progression() {
        val r = Eidolons.row(4, unchained = false)
        assertEquals(3, r.hd)
        assertEquals(7, r.evolutionPool)
        assertEquals(1, Eidolons.row(1, unchained = true).evolutionPool)
        assertEquals(3, Eidolons.row(1, unchained = false).evolutionPool)
    }

    @Test
    fun bipedEidolon() {
        val e = eidolon("biped", 4)
        assertEquals(listOf(ClassEntry("eidolon", 3)), e.classes) // HD z tabulky, výchozí „animal“ zmizí
        assertEquals(16, e.abilities.str) // startovní atributy formy
        assertEquals(CreatureSize.MEDIUM, e.size)
        assertFalse(e.quadruped)
        // dobré záchrany bipeda: Fort + Will (2 + HD/2), Ref špatná (HD/3)
        val st = e.stats()
        assertEquals(3, st.bab)
        assertEquals(3, st.fort)
        assertEquals(1, st.ref)
        assertEquals(3, st.will)
        // Str/Dex bonus z tabulky (+1 na 4. úrovni)
        assertEquals(17, e.scores.str)
        // přirozená zbroj: forma +2, tabulka +2
        assertEquals(4, e.eidolonNaturalArmor)
        assertTrue(eidolon("quadruped", 1).quadruped)
    }

    @Test
    fun evolutions() {
        val evo = listOf(
            EvolutionPick(Eidolons.ABILITY_INCREASE, "Ability Increase", 2, ability = Ability.CON),
            EvolutionPick(Eidolons.IMPROVED_NATURAL_ARMOR, "Improved Natural Armor", 1),
            EvolutionPick("large", "Large", 4),
        )
        val e = eidolon("biped", 6, evo)
        assertEquals(7, e.evolutionPointsUsed)
        assertEquals(13 + 2, e.scores.con)
        assertEquals(2 + e.eidolonRow!!.armor + 2, e.eidolonNaturalArmor)
        assertEquals(CreatureSize.LARGE, e.size)
    }

    @Test
    fun unchainedSubtypeAndLevel() {
        val e = Character("e", "E", listOf(ClassEntry("animal", 1)))
            .withCompanion(CompanionKind.EIDOLON, null, 5, null, false, eidolonForm = "u_biped", eidolonSubtype = "Angel")
        assertEquals("Angel", e.eidolonSubtype)
        assertEquals(Eidolons.row(5, unchained = true).evolutionPool, e.eidolonRow!!.evolutionPool)
        // podtyp jen ze seznamu
        assertNull(e.withCompanion(CompanionKind.EIDOLON, null, 5, null, false, eidolonForm = "u_biped", eidolonSubtype = "Nonsense").eidolonSubtype)
        val owner = Character("m", "M", listOf(ClassEntry("summoner_unchained", 7)))
        assertEquals(7, owner.suggestedCompanionLevel(CompanionKind.EIDOLON))
    }
}
