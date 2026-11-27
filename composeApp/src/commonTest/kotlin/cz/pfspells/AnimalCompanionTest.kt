package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.CompanionKind
import cz.pfspells.data.CompanionProgression
import cz.pfspells.data.CompanionSpecial
import cz.pfspells.data.CreatureSize
import cz.pfspells.data.SpeciesCatalog
import cz.pfspells.data.featSlots
import cz.pfspells.data.naturalArmor
import cz.pfspells.data.scores
import cz.pfspells.data.stats
import cz.pfspells.data.suggestedCompanionLevel
import cz.pfspells.data.withCompanion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimalCompanionTest {
    private fun companion(species: String?, level: Int?) =
        Character("p", "Pet", listOf(ClassEntry("animal", 1)), ownerId = "o")
            .withCompanion(CompanionKind.ANIMAL_COMPANION, species, level, null, false)

    @Test
    fun tableMatchesAnimalHitDice() {
        // BAB, záchrany a featy z rasových HD „animal“ = sloupce tabulky (Core Rulebook)
        val bab = listOf(1, 2, 2, 3, 3, 4, 4, 5, 6, 6, 6, 7, 8, 9, 9, 9, 10, 11, 11, 12)
        val feats = listOf(1, 2, 2, 2, 3, 3, 3, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 8, 8, 8)
        for (lvl in 1..20) {
            val c = companion(null, lvl)
            assertEquals(CompanionProgression.at(lvl).hd, c.classes.single().level, "HD na úrovni $lvl")
            assertEquals(bab[lvl - 1], c.stats().bab, "BAB na úrovni $lvl")
            assertEquals(feats[lvl - 1], c.featSlots().total, "featy na úrovni $lvl")
        }
    }

    @Test
    fun wolfAdvancesAtSeventh() {
        val young = companion("wolf", 6)
        assertEquals(CreatureSize.MEDIUM, young.size)
        assertTrue(young.quadruped)
        assertEquals(15, young.scores.str) // 13 + Str/Dex bonus 2
        assertEquals(2 + 4, young.naturalArmor)

        val big = companion("wolf", 7)
        assertEquals(CreatureSize.LARGE, big.size)
        // 13 + 2 + 8 · 15 + 2 − 2 · 15 + 4
        assertEquals(listOf(23, 15, 19), listOf(big.scores.str, big.scores.dex, big.scores.con))
        assertEquals(2 + 2 + 4, big.naturalArmor)
        val st = big.stats()
        assertEquals(listOf(4, 5, 5, 2), listOf(st.bab, st.fort, st.ref, st.will))
    }

    @Test
    fun speciesSetsAbilitiesOnlyWhenChanged() {
        val wolf = companion("wolf", 1)
        assertEquals(SpeciesCatalog.get("wolf")!!.abilities, wolf.abilities)
        val edited = wolf.copy(abilities = wolf.abilities.copy(int = 3))
        // stejný druh (jen jiná úroveň) – ručně upravené atributy zůstanou
        assertEquals(3, edited.withCompanion(CompanionKind.ANIMAL_COMPANION, "wolf", 4, null, false).abilities.int)
        assertEquals(SpeciesCatalog.get("horse")!!.abilities, edited.withCompanion(CompanionKind.MOUNT, "horse", 4, null, false).abilities)
    }

    @Test
    fun specials() {
        assertEquals(listOf(CompanionSpecial.LINK, CompanionSpecial.SHARE_SPELLS), CompanionProgression.specialsUpTo(1))
        val at15 = CompanionProgression.specialsUpTo(15)
        assertTrue(CompanionSpecial.IMPROVED_EVASION in at15)
        assertFalse(CompanionSpecial.EVASION in at15)
        assertEquals(3, CompanionProgression.abilityIncreases(14))
        assertEquals(4, CompanionProgression.abilityIncreases(20))
    }

    @Test
    fun suggestedLevelFromOwner() {
        fun owner(vararg cls: Pair<String, Int>) = Character("o", "O", cls.map { ClassEntry(it.first, it.second) }, abilities = Abilities())
        assertEquals(5, owner("druid" to 5).suggestedCompanionLevel(CompanionKind.ANIMAL_COMPANION))
        assertEquals(2, owner("ranger" to 5).suggestedCompanionLevel(CompanionKind.ANIMAL_COMPANION))
        assertEquals(6, owner("paladin" to 6).suggestedCompanionLevel(CompanionKind.MOUNT))
        // familiár: úroveň pána (classy, které familiára dávají)
        assertEquals(3, owner("wizard" to 3).suggestedCompanionLevel(CompanionKind.FAMILIAR))
    }
}
