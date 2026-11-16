package cz.pfspells

import cz.pfspells.data.CarryingCapacity
import cz.pfspells.data.Csv
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Inventory
import cz.pfspells.data.InventoryTransfer
import cz.pfspells.data.ItemCategory
import cz.pfspells.data.ItemDatabase
import cz.pfspells.data.ItemParser
import cz.pfspells.data.SpellSource
import cz.pfspells.data.decodeText
import cz.pfspells.ui.priceLabel
import cz.pfspells.ui.worthText
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ItemDataTest {
    private fun res(name: String) = decodeText(File("src/commonMain/composeResources/files/$name").readBytes())

    private val db by lazy {
        val t0 = System.currentTimeMillis()
        val items = ItemParser.combine(
            ItemParser.parseBase(res("ItemsBase.json")),
            ItemParser.parseWeapons(res("Weapons.csv"), ammo = false),
            ItemParser.parseWeapons(res("Ammunition.csv"), ammo = true),
            ItemParser.parseMagic(res("MagicItems.csv")),
        )
        println("Parsed ${items.size} items in ${System.currentTimeMillis() - t0} ms")
        ItemDatabase(items, SpellSource.BUNDLED, null)
    }

    @Test
    fun bundledItemsParse() {
        assertTrue(db.items.size > 5500, "only ${db.items.size} items")
        assertEquals(db.items.size, db.items.map { it.id }.toSet().size)
        println(db.counts.entries.sortedBy { it.key.ordinal }.joinToString { "${it.key}=${it.value}" })

        val longsword = db.findByName("Longsword")!!
        assertEquals(ItemCategory.WEAPON, longsword.category)
        assertEquals(15.0, longsword.priceGp)
        assertEquals(4.0, longsword.weightLb)
        assertEquals("1d8", longsword.damageMedium)
        assertEquals("19-20/×2", longsword.critical)

        val chain = db.findByName("Chainmail")!!
        assertEquals(ItemCategory.ARMOR, chain.category)
        assertEquals(6, chain.armorBonus)

        val bag = db.items.first { it.name.startsWith("Bag of Holding") }
        assertEquals(ItemCategory.WONDROUS, bag.category)
        assertTrue((bag.priceGp ?: 0.0) > 200)

        // žádné rozbité apostrofy z kódování
        assertTrue(db.items.none { '�' in it.name }, db.items.filter { '�' in it.name }.take(5).joinToString { it.name })
        assertTrue(db.findByName("Cat-o'-nine-tails") != null)
    }

    @Test
    fun csvParserHandlesQuotes() {
        val rows = Csv.parse("a,b\n\"x, y\",\"he said \"\"hi\"\"\nnext line\"\n")
        assertEquals(listOf("x, y", "he said \"hi\"\nnext line"), rows[1])
    }

    @Test
    fun pricesParse() {
        assertEquals(1500.0, ItemParser.parsePrice("1,500 gp"))
        assertEquals(0.5, ItemParser.parsePrice("5 sp"))
        assertEquals(0.01, ItemParser.parsePrice("1 cp"))
        assertEquals(0.5, ItemParser.parseWeight("1/2 lb."))
        assertEquals("Slashing", ItemParser.damageTypeName("S"))
        assertEquals("Bludgeoning or Piercing", ItemParser.damageTypeName("B or P"))
    }

    @Test
    fun carryingCapacity() {
        assertEquals(100.0, CarryingCapacity.heavyLoad(10))
        assertEquals(33.0, CarryingCapacity.lightLoad(10))
        assertEquals(66.0, CarryingCapacity.mediumLoad(10))
        assertEquals(300.0, CarryingCapacity.heavyLoad(18))
        assertEquals(1600.0, CarryingCapacity.heavyLoad(30)) // 400 × 4
        assertEquals("Medium load", CarryingCapacity.loadName(10, 49.0))
    }

    @Test
    fun hiddenPrices() {
        assertEquals("15 gp", priceLabel(15.0, revealed = false))
        assertEquals("200 gp", priceLabel(200.0, revealed = false))
        assertEquals("??? gp", priceLabel(1000.0, revealed = false))
        assertEquals("1,000 gp", priceLabel(1000.0, revealed = true))
        assertEquals("1 sp", priceLabel(0.1, revealed = false))
        val inv = Inventory("i", "X", entries = listOf(InvEntry("e", null, "Ring", qty = 1, priceGp = 2000.0)))
        assertEquals("worth ≈ ???", worthText(inv, revealed = false))
        assertEquals("worth ≈ 2,000 gp", worthText(inv, revealed = true))
    }

    @Test
    fun inventoryRoundTrip() {
        val ls = db.findByName("Longsword")!!
        val inv = Inventory(
            "id1", "Elara", strength = 12, gp = 124, sp = 6, cp = 12, notes = "Chest at the inn",
            entries = listOf(
                InvEntry("a", ls.id, ls.name, ls.category.name, 1, ls.priceGp, ls.weightLb, "masterwork", ls.summary),
                InvEntry("b", null, "Scroll of Mage Armor", ItemCategory.SCROLL.name, 2, 25.0, null),
            ),
        )
        val text = InventoryTransfer.export(listOf(inv))
        assertTrue("pf-spellbook-inventories" in text)
        val back = InventoryTransfer.parse(text, db).single()
        assertEquals("Elara", back.name)
        assertEquals(12, back.strength)
        assertEquals(124, back.gp)
        assertEquals("Chest at the inn", back.notes)
        assertEquals(listOf("Longsword", "Scroll of Mage Armor"), back.entries.map { it.name })
        assertEquals(ls.id, back.entries[0].itemId)
        assertEquals("masterwork", back.entries[0].note)
        assertEquals(2, back.entries[1].qty)
    }
}
