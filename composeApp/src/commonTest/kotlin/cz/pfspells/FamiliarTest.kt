package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.BonusTarget
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.CompanionKind
import cz.pfspells.data.FamiliarCatalog
import cz.pfspells.data.FamiliarProgression
import cz.pfspells.data.FamiliarSpecial
import cz.pfspells.data.SpeciesCatalog
import cz.pfspells.data.companionMod
import cz.pfspells.data.companionRow
import cz.pfspells.data.familiarBonuses
import cz.pfspells.data.gearSave
import cz.pfspells.data.gearSkill
import cz.pfspells.data.maxHp
import cz.pfspells.data.naturalArmor
import cz.pfspells.data.suggestedCompanionLevel
import cz.pfspells.data.withCompanion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FamiliarTest {
    private val master = Character("m", "Ezren", listOf(ClassEntry("wizard", 7)), abilities = Abilities(con = 12))

    private fun familiar(id: String, level: Int?) =
        Character("f", "Fam", listOf(ClassEntry("animal", 1)), ownerId = "m")
            .withCompanion(CompanionKind.FAMILIAR, null, level, null, false, id)

    @Test
    fun progression() {
        assertEquals(1, FamiliarProgression.naturalArmor(1))
        assertEquals(4, FamiliarProgression.naturalArmor(7))
        assertEquals(10, FamiliarProgression.naturalArmor(20))
        assertEquals(6, FamiliarProgression.intelligence(2))
        assertEquals(9, FamiliarProgression.intelligence(7))
        assertEquals(15, FamiliarProgression.intelligence(19))
        assertNull(FamiliarProgression.spellResistance(10))
        assertEquals(16, FamiliarProgression.spellResistance(11))
        assertTrue(FamiliarSpecial.SPEAK_WITH_ANIMALS in FamiliarProgression.specials(7))
        assertTrue(FamiliarSpecial.SCRY_ON_FAMILIAR !in FamiliarProgression.specials(7))
    }

    @Test
    fun familiarCharacter() {
        val f = familiar("cat", 7)
        assertEquals("cat", f.familiar)
        assertEquals(7, f.classes.single().level) // HD = úroveň pána
        assertNull(f.companionRow) // tabulka zvířecího společníka neplatí
        assertEquals(FamiliarCatalog.get("cat")!!.naturalArmor + 4, f.naturalArmor)
        assertEquals(9 - f.abilities.int, f.companionMod(cz.pfspells.data.Ability.INT))
        // familiár jen u druhu FAMILIAR
        assertNull(Character("x", "X", listOf(ClassEntry("animal", 1))).withCompanion(CompanionKind.PET, null, null, null, false, "cat").familiar)
    }

    @Test
    fun bestiaryStats() {
        val f = familiar("cat", 5)
        // kočka z bestiáře: Str 3, Dex 15, Con 8, Int 2 (familiár má Int podle tabulky), Wis 12, Cha 7; Tiny
        assertEquals(cz.pfspells.data.Abilities(3, 15, 8, 2, 12, 7), f.abilities)
        assertEquals(cz.pfspells.data.CreatureSize.TINY, f.size)
        assertEquals(8, f.abilities.int + f.companionMod(cz.pfspells.data.Ability.INT))
        // přirozená zbroj: tvor + tabulka familiára
        val toad = familiar("toad", 5)
        assertEquals(FamiliarCatalog.get("toad")!!.naturalArmor + 3, toad.naturalArmor)
        // familiár založený bez statistik (atributy 10) je dostane při další úpravě
        val old = Character("f", "Old", listOf(ClassEntry("animal", 5)), ownerId = "m", companion = CompanionKind.FAMILIAR, companionLevel = 5, familiar = "cat")
        assertEquals(3, old.withCompanion(CompanionKind.FAMILIAR, null, 5, null, false, "cat").abilities.str)
        // ručně upravené atributy se při změně úrovně nepřepíšou
        val edited = f.copy(abilities = f.abilities.copy(str = 4))
        assertEquals(4, edited.withCompanion(CompanionKind.FAMILIAR, null, 6, f.size, false, "cat").abilities.str)
    }

    @Test
    fun masterBonus() {
        val cat = FamiliarCatalog.get("cat")!!
        assertEquals(BonusTarget.SKILL, cat.bonus!!.target)
        assertEquals("stealth", cat.bonus!!.skill)
        // podmíněný bonus (Hawk: „in bright light“) se nepočítá
        assertNull(FamiliarCatalog.get("hawk")!!.bonus)
        val withCat = master.copy(familiarBonus = familiarBonuses(listOf(familiar("cat", 7))))
        assertEquals(3, withCat.gearSkill("stealth"))
        val withToad = master.copy(familiarBonus = familiarBonuses(listOf(familiar("toad", 7))))
        assertEquals(master.maxHp() + 3, withToad.maxHp())
        val withRat = master.copy(familiarBonus = familiarBonuses(listOf(familiar("rat", 7))))
        assertEquals(2, withRat.gearSave(BonusTarget.FORT))
    }

    @Test
    fun suggestedLevel() {
        val c = Character("c", "C", listOf(ClassEntry("wizard", 5), ClassEntry("fighter", 2)))
        assertEquals(5, c.suggestedCompanionLevel(CompanionKind.FAMILIAR))
    }

    @Test
    fun moreSpecies() {
        assertTrue(SpeciesCatalog.all.size > 150)
        assertEquals(SpeciesCatalog.all.size, SpeciesCatalog.all.map { it.id }.toSet().size)
        assertNotNull(SpeciesCatalog.get("wolf")) // Core Rulebook ručně (stabilní id)
        val allo = SpeciesCatalog.get("allosaurus")!!
        assertEquals(7, allo.advLevel)
        assertEquals(cz.pfspells.data.CreatureSize.LARGE, allo.advSize)
        assertEquals(8, allo.advStr)
    }
}
