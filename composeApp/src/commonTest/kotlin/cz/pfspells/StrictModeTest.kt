package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.CompanionKind
import cz.pfspells.data.CreatureSize
import cz.pfspells.data.Feat
import cz.pfspells.data.FeatDatabase
import cz.pfspells.data.FeatPick
import cz.pfspells.data.Spell
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SpellSource
import cz.pfspells.data.StrictBlock
import cz.pfspells.data.StrictRules
import cz.pfspells.data.creatureSize
import cz.pfspells.data.featSlots
import cz.pfspells.data.stats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StrictModeTest {
    private fun spell(id: String, vararg levels: Pair<String, Int>) = Spell(
        id, id.uppercase(), "evocation", null, null, mapOf(*levels), null, null, null, null, null, null, null, null, null, "", null, null, null,
    )

    private val magicMissile = spell("magic-missile", "sorcerer/wizard" to 1)
    private val shield = spell("shield", "sorcerer/wizard" to 1)
    private val sleep = spell("sleep", "sorcerer/wizard" to 1)
    private val web = spell("web", "sorcerer/wizard" to 2)
    private val db = SpellDatabase(listOf(magicMissile, shield, sleep, web), SpellSource.BUNDLED, null)

    private fun caster(classId: String, spells: Map<String, Int>, abilities: Abilities) =
        Character("c", "C", listOf(ClassEntry(classId, 1, spells = spells)), abilities = abilities)

    @Test
    fun preparedCopiesUpToSlotsPlusBonus() {
        // wizard 1: 1 kouzlo 1. levelu za den + 1 za Int 16
        val smart = Abilities(int = 16)
        assertNull(StrictRules.spellBlock(caster("wizard", mapOf("magic-missile" to 1), smart), "wizard", magicMissile, db))
        assertEquals(
            StrictBlock.SpellLevelFull(1, 2),
            StrictRules.spellBlock(caster("wizard", mapOf("magic-missile" to 1, "shield" to 1), smart), "wizard", magicMissile, db),
        )
    }

    @Test
    fun spontaneousKnownLimit() {
        // sorcerer 1 zná 2 kouzla 1. levelu; už známé kouzlo „přidat“ jde (nic se nemění)
        val c = caster("sorcerer", mapOf("magic-missile" to 1, "shield" to 1), Abilities(cha = 16))
        assertEquals(StrictBlock.SpellLevelFull(1, 2), StrictRules.spellBlock(c, "sorcerer", sleep, db))
        assertNull(StrictRules.spellBlock(c, "sorcerer", shield, db))
    }

    @Test
    fun abilityAndSpellLevel() {
        assertEquals(StrictBlock.AbilityTooLow(1), StrictRules.spellBlock(caster("wizard", emptyMap(), Abilities(int = 10)), "wizard", sleep, db))
        assertEquals(StrictBlock.SpellNotAvailable, StrictRules.spellBlock(caster("wizard", emptyMap(), Abilities(int = 18)), "wizard", web, db))
    }

    @Test
    fun featSlotsAndPrereqs() {
        val powerAttack = Feat("power-attack", "Power Attack", prereq = "Str 13")
        val featDb = FeatDatabase(listOf(powerAttack))
        val fighter = Character("f", "F", listOf(ClassEntry("fighter", 1)), abilities = Abilities(str = 10))
        // fighter 1: feat za 1. úroveň + bonusový feat
        assertEquals(2, fighter.featSlots().total)
        assertEquals(StrictBlock.FeatPrereqsUnmet, StrictRules.featBlock(fighter, powerAttack, featDb, null))
        assertNull(StrictRules.featBlock(fighter.copy(abilities = Abilities(str = 13)), powerAttack, featDb, null))
        val full = fighter.copy(feats = listOf(FeatPick(null, "A"), FeatPick(null, "B")))
        assertEquals(StrictBlock.FeatSlotsFull(2), StrictRules.featBlock(full, null, featDb, null))
    }

    @Test
    fun animalCompanionStats() {
        // Animal (rasové HD): d8, BAB ¾, dobrá Fort a Ref; feat za každé liché HD
        val wolf = Character(
            "w", "Wolf", listOf(ClassEntry("animal", 4)), ownerId = "c",
            companion = CompanionKind.ANIMAL_COMPANION, size = CreatureSize.MEDIUM, quadruped = true,
        )
        val st = wolf.stats()
        assertEquals(listOf(3, 4, 4, 1), listOf(st.bab, st.fort, st.ref, st.will))
        assertEquals(2, wolf.featSlots().total)
        assertEquals(CreatureSize.MEDIUM, wolf.creatureSize)
    }
}
