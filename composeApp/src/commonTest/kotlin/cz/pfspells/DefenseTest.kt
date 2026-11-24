package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.BonusTarget
import cz.pfspells.data.BonusType
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.CreatureSize
import cz.pfspells.data.Feat
import cz.pfspells.data.FeatPick
import cz.pfspells.data.ItemBonus
import cz.pfspells.data.Load
import cz.pfspells.data.Metamagic
import cz.pfspells.data.FeatDatabase
import cz.pfspells.data.armorCheckPenalty
import cz.pfspells.data.currentHp
import cz.pfspells.data.defense
import cz.pfspells.data.loadBonuses
import cz.pfspells.data.maxHp
import cz.pfspells.data.resources
import cz.pfspells.data.skillTotal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefenseTest {
    private val chainShirt = listOf(
        ItemBonus(BonusTarget.AC, 4, BonusType.ARMOR),
        ItemBonus(BonusTarget.MAX_DEX, 4, BonusType.ARMOR),
        ItemBonus(BonusTarget.ARMOR_CHECK, -2, BonusType.ARMOR),
    )
    private val heavyShield = listOf(
        ItemBonus(BonusTarget.AC, 2, BonusType.SHIELD),
        ItemBonus(BonusTarget.ARMOR_CHECK, -2, BonusType.SHIELD),
    )

    private fun fighter(dex: Int, gear: List<ItemBonus> = emptyList()) =
        Character("f", "F", listOf(ClassEntry("fighter", 3)), abilities = Abilities(con = 14, dex = dex), gear = gear)

    @Test
    fun armorClass() {
        val d = fighter(14, chainShirt + heavyShield + ItemBonus(BonusTarget.AC, 1, BonusType.DEFLECTION)).defense()
        assertEquals(10 + 4 + 2 + 2 + 1, d.ac)
        assertEquals(10 + 2 + 1, d.touch)
        assertEquals(d.ac - 2, d.flatFooted)
        // max Dex zbroje omezí Obratnost 18 (+4) na +3
        val breastplate = listOf(ItemBonus(BonusTarget.AC, 6, BonusType.ARMOR), ItemBonus(BonusTarget.MAX_DEX, 3, BonusType.ARMOR))
        assertEquals(3, fighter(18, breastplate).defense().dex)
        // malý tvor +1
        assertEquals(11, fighter(10).copy(size = CreatureSize.SMALL).defense().ac)
    }

    @Test
    fun armorCheckPenaltyAndLoad() {
        val armored = fighter(10, chainShirt + heavyShield)
        assertEquals(-4, armored.armorCheckPenalty()) // zbroj + štít se sčítají
        assertEquals(fighter(10).skillTotal("climb")!! - 4, armored.skillTotal("climb"))
        assertEquals(fighter(10).skillTotal("perception"), armored.skillTotal("perception")) // Moudrost – bez postihu
        // střední zatížení (−3) se se zbrojí (−2) nesčítá – platí horší
        assertEquals(-3, fighter(10, chainShirt + loadBonuses(Load.MEDIUM)).armorCheckPenalty())
        assertEquals(3, fighter(18, loadBonuses(Load.MEDIUM)).defense().maxDex)
    }

    @Test
    fun hitPoints() {
        // fighter 3, d10, Con 14: 10 + 2, pak 2× (6 + 2)
        assertEquals(28, fighter(10).maxHp())
        assertEquals(28 + 3 + 2, fighter(10).copy(feats = listOf(FeatPick("toughness", "Toughness")), hpBonus = 2).maxHp())
        assertEquals(40, fighter(10).copy(hpOverride = 40).maxHp())
        // zvíře 2 HD (d8, průměr 4,5), Con 15: 9 + 2 × 2
        val wolf = Character("w", "Wolf", listOf(ClassEntry("animal", 2)), abilities = Abilities(con = 15))
        assertEquals(13, wolf.maxHp())
        // aktuální HP = max − zranění, může jít pod nulu
        assertEquals(28 - 10, fighter(10).copy(damage = 10).currentHp())
        assertEquals(-2, fighter(10).copy(damage = 30).currentHp())
    }

    @Test
    fun metamagicLevels() {
        fun meta(name: String, text: String) = Feat(name.lowercase(), name, listOf("Metamagic"), sections = listOf(listOf("Benefit", text)))
        assertEquals(2, Metamagic.levels(meta("Empower Spell", "An empowered spell uses up a spell slot two levels higher than the spell's actual level.")))
        assertEquals(1, Metamagic.levels(meta("Shadow Grasp", "A Shadow Grasp spell uses up a slot one level higher than the spell’s actual level.")))
        assertEquals(0, Metamagic.levels(meta("Brackish Spell", "A brackish spell does not use up a higher-level spell slot than the spell’s actual level.")))
        assertNull(Metamagic.levels(meta("Heighten Spell", "A heightened spell has a higher spell level than normal (up to a maximum of 9th level).")))
        assertNull(Metamagic.levels(Feat("power-attack", "Power Attack")))
        // featy postavy: jen metamagické z databáze, bez opakování, vlastní featy ne
        val empower = meta("Empower Spell", "An empowered spell uses up a spell slot two levels higher than the spell's actual level.")
        val db = FeatDatabase(listOf(empower, Feat("power-attack", "Power Attack")))
        val c = fighter(10).copy(feats = listOf(
            FeatPick("empower spell", "Empower Spell"), FeatPick("empower spell", "Empower Spell"),
            FeatPick("power-attack", "Power Attack"), FeatPick(null, "Homebrew Metamagic"),
        ))
        assertEquals(listOf("Empower Spell" to 2), Metamagic.ofCharacter(c, db).map { it.first.name to it.second })
        assertTrue(Metamagic.ofCharacter(c, null).isEmpty())
    }

    @Test
    fun subclassPowers() {
        val cleric = Character(
            "c", "C", listOf(ClassEntry("cleric", 3, subclass = mapOf("domain1" to "fire", "domain2" to "sun"))),
            abilities = Abilities(wis = 14),
        )
        val names = cleric.resources().associate { it.name to it.max }
        assertEquals(5, names["Fire Bolt (Fire)"])
        assertTrue(names.keys.none { it.endsWith("(Sun)") }) // Sun's Blessing je pasivní, Nimbus of Light až od 8.
        val sorc = Character("s", "S", listOf(ClassEntry("sorcerer", 1, subclass = mapOf("bloodline" to "draconic"))), abilities = Abilities(cha = 16))
        assertEquals(6, sorc.resources().first { it.name == "Draconic Claws (Draconic)" }.max)
        val arcane = sorc.copy(classes = listOf(ClassEntry("sorcerer", 1, subclass = mapOf("bloodline" to "arcane"))))
        assertTrue(arcane.resources().none { it.name.endsWith("(Arcane)") }) // arcane bond, Metamagic Adept až od 3.
    }
}
