package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.BonusTarget
import cz.pfspells.data.BonusType
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Inventory
import cz.pfspells.data.ItemBonus
import cz.pfspells.data.ItemBonusGuess
import cz.pfspells.data.ItemSlot
import cz.pfspells.data.gearBonuses
import cz.pfspells.data.isWorking
import cz.pfspells.data.workingEquipped
import cz.pfspells.data.gearSave
import cz.pfspells.data.scores
import cz.pfspells.data.skillTotal
import cz.pfspells.data.slotConflicts
import cz.pfspells.data.stackBonuses
import kotlin.test.Test
import kotlin.test.assertEquals

class EquipmentTest {
    @Test
    fun stackingRules() {
        // stejný typ → nejvyšší; dodge se sčítá; postihy se sčítají vždy
        assertEquals(4, stackBonuses(listOf(ItemBonus(BonusTarget.INT, 2), ItemBonus(BonusTarget.INT, 4))))
        assertEquals(2, stackBonuses(listOf(ItemBonus(BonusTarget.AC, 1, BonusType.DODGE), ItemBonus(BonusTarget.AC, 1, BonusType.DODGE))))
        assertEquals(5, stackBonuses(listOf(ItemBonus(BonusTarget.INT, 4), ItemBonus(BonusTarget.INT, 1, BonusType.INSIGHT))))
        assertEquals(1, stackBonuses(listOf(ItemBonus(BonusTarget.STR, 2), ItemBonus(BonusTarget.STR, -1, BonusType.UNTYPED))))
    }

    @Test
    fun guessFromItemName() {
        assertEquals(listOf(ItemBonus(BonusTarget.INT, 4)), ItemBonusGuess.of("Headband Of Vast Intelligence +4"))
        assertEquals(3, ItemBonusGuess.of("Headband Of Mental Superiority +2").size)
        assertEquals(
            listOf(BonusTarget.FORT, BonusTarget.REF, BonusTarget.WILL).map { ItemBonus(it, 2, BonusType.RESISTANCE) },
            ItemBonusGuess.of("Cloak Of Resistance +2"),
        )
        assertEquals(listOf(ItemBonus(BonusTarget.AC, 1, BonusType.DEFLECTION)), ItemBonusGuess.of("Ring Of Protection +1"))
        assertEquals(emptyList(), ItemBonusGuess.of("Belt Of Physical Might +4")) // dva atributy podle volby
        assertEquals(emptyList(), ItemBonusGuess.of("Bag of Holding (type I)"))
        assertEquals(ItemSlot.WRISTS, ItemSlot.parse("wrist"))
    }

    @Test
    fun gearCountsOnlyWhenEquipped() {
        val headband = InvEntry("h", name = "Headband", equipped = true, slot = ItemSlot.HEADBAND, bonuses = listOf(ItemBonus(BonusTarget.INT, 4)))
        val cloak = InvEntry("c", name = "Cloak of Elvenkind", equipped = false, slot = ItemSlot.SHOULDERS,
            bonuses = listOf(ItemBonus(BonusTarget.SKILL, 5, BonusType.COMPETENCE, "stealth")))
        val inv = Inventory("i", "Bag", entries = listOf(headband, cloak))
        assertEquals(listOf(ItemBonus(BonusTarget.INT, 4)), inv.gearBonuses())

        val c = Character("x", "X", listOf(ClassEntry("rogue", 1)), abilities = Abilities(int = 14), gear = inv.gearBonuses())
        assertEquals(18, c.scores.int)
        val worn = c.copy(gear = inv.copy(entries = listOf(headband, cloak.copy(equipped = true))).gearBonuses())
        assertEquals(c.skillTotal("stealth")!! + 5, worn.skillTotal("stealth"))
        assertEquals(0, worn.gearSave(BonusTarget.FORT))
    }

    @Test
    fun onlyWhatFitsTheSlotWorks() {
        // dva pláště na ramenou: platí jen první v pořadí, druhý se ignoruje (návrh Inventory 2.0)
        val resistance = InvEntry("r", name = "Cloak of Resistance +1", equipped = true, slot = ItemSlot.SHOULDERS,
            bonuses = ItemBonusGuess.of("Cloak Of Resistance +1"))
        val elvenkind = InvEntry("e", name = "Cloak of Elvenkind", equipped = true, slot = ItemSlot.SHOULDERS,
            bonuses = listOf(ItemBonus(BonusTarget.SKILL, 5, BonusType.COMPETENCE, "stealth")))
        val inv = Inventory("i", "I", entries = listOf(resistance, elvenkind))
        assertEquals(listOf("r"), inv.workingEquipped().map { it.id })
        assertEquals(resistance.bonuses, inv.gearBonuses())
        assertEquals(false, inv.isWorking(elvenkind))
        // prsteny: dva platí, třetí ne
        fun ring(id: String) = InvEntry(id, name = id, equipped = true, slot = ItemSlot.RING, bonuses = listOf(ItemBonus(BonusTarget.AC, 1, BonusType.DEFLECTION)))
        assertEquals(listOf("a", "b"), Inventory("i", "I", entries = listOf(ring("a"), ring("b"), ring("c"))).workingEquipped().map { it.id })
    }

    @Test
    fun slotConflicts() {
        fun ring(id: String) = InvEntry(id, name = "Ring $id", equipped = true, slot = ItemSlot.RING)
        assertEquals(emptyList(), Inventory("i", "I", entries = listOf(ring("a"), ring("b"))).slotConflicts())
        assertEquals(listOf(ItemSlot.RING), Inventory("i", "I", entries = listOf(ring("a"), ring("b"), ring("c"))).slotConflicts())
    }
}
