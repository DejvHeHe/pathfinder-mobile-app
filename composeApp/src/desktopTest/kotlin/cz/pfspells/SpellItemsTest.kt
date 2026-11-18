package cz.pfspells

import cz.pfspells.data.CustomItem
import cz.pfspells.data.ItemCategory
import cz.pfspells.data.Spell
import cz.pfspells.data.SpellItemKind
import cz.pfspells.data.isCustom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpellItemsTest {
    private val cureLight = Spell(
        "cure-light-wounds", "Cure Light Wounds", "conjuration", "healing", null, mapOf("cleric/oracle" to 1),
        null, null, null, null, null, null, null, null, null, "", null, null, null,
    )

    @Test
    fun pricesFollowCoreRulebook() {
        // Core Rulebook, Table 15-29: lektvar 50 × SL × CL, svitek 25 × SL × CL, hůlka 750 × SL × CL; 0. level = ½
        assertEquals(50.0, SpellItemKind.POTION.price(1, 1))      // Potion of Cure Light Wounds
        assertEquals(300.0, SpellItemKind.POTION.price(2, 3))     // lektvar 2. levelu, CL 3
        assertEquals(375.0, SpellItemKind.SCROLL.price(3, 5))     // Scroll of Fireball
        assertEquals(750.0, SpellItemKind.WAND.price(1, 1))       // Wand of Magic Missile
        assertEquals(12.5, SpellItemKind.SCROLL.price(0, 1))      // cantrip
        assertEquals(4500.0, SpellItemKind.WAND.price(2, 3))
    }

    @Test
    fun limitsAndCasterLevels() {
        assertTrue(SpellItemKind.POTION.allows(3))
        assertFalse(SpellItemKind.POTION.allows(4))
        assertFalse(SpellItemKind.WAND.allows(5))
        assertTrue(SpellItemKind.SCROLL.allows(9))
        assertEquals(listOf(1, 1, 3, 5, 7, 9, 11, 13, 15, 17), (0..9).map { SpellItemKind.minCasterLevel(it) })
    }

    @Test
    fun entryAndCatalog() {
        val e = SpellItemKind.WAND.entry(cureLight, 1, 1, qty = 2)
        assertEquals("Wand of Cure Light Wounds", e.name)
        assertEquals(ItemCategory.WAND, e.cat)
        assertEquals(2, e.qty)
        assertNull(e.itemId)
        assertEquals("SL 1, CL 1, 50 charges", e.info)

        val c = CustomItem.fromEntry(e, id = "abc")
        val item = c.toItem()
        assertTrue(item.isCustom)
        assertEquals("my-abc", item.id)
        assertEquals("SL 1, CL 1, 50 charges", item.subtitle)
        assertEquals(750.0, item.priceGp)
    }
}
