package cz.pfspells

import cz.pfspells.data.Abilities
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.Feat
import cz.pfspells.data.FeatDatabase
import cz.pfspells.data.FeatPick
import cz.pfspells.data.FeatPrereqs
import cz.pfspells.data.PrereqStatus
import cz.pfspells.data.PrereqStatus.MET
import cz.pfspells.data.PrereqStatus.UNKNOWN
import cz.pfspells.data.PrereqStatus.UNMET
import cz.pfspells.data.featSlots
import cz.pfspells.data.Ability
import cz.pfspells.data.scores
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatPrereqTest {
    private val db = FeatDatabase(
        listOf("Power Attack", "Cleave", "Weapon Focus", "Dodge", "Mobility").map { Feat(it.lowercase().replace(' ', '-'), it) },
    )

    // Fighter 4 / Wizard 3: BAB 5, STR 15, INT 14, Power Attack, Weapon Focus (longsword)
    private val c = Character(
        "1", "Test",
        listOf(ClassEntry("fighter", 4), ClassEntry("wizard", 3)),
        abilities = Abilities(str = 15, int = 14),
        feats = listOf(FeatPick("power-attack", "Power Attack"), FeatPick("weapon-focus", "Weapon Focus", note = "longsword")),
    )

    private fun st(text: String): PrereqStatus = FeatPrereqs.status(c, db, text)

    @Test
    fun splitRespectsParentheses() {
        assertEquals(
            listOf("Str 13", "Weapon Focus (longsword, scimitar)", "base attack bonus +1"),
            FeatPrereqs.split("Str 13, Weapon Focus (longsword, scimitar), base attack bonus +1."),
        )
    }

    @Test
    fun evaluatesCommonPrerequisites() {
        assertEquals(MET, st("Str 13"))
        assertEquals(UNMET, st("Dex 13"))
        assertEquals(MET, st("Intelligence 13"))
        assertEquals(MET, st("base attack bonus +5"))
        assertEquals(UNMET, st("base attack bonus +6"))
        assertEquals(MET, st("fighter level 4th"))
        assertEquals(UNMET, st("fighter level 8th"))
        assertEquals(MET, st("caster level 3rd"))
        assertEquals(MET, st("character level 7th"))
        assertEquals(MET, st("ability to cast 2nd-level spells"))
        assertEquals(UNMET, st("ability to cast 3rd-level spells"))
        assertEquals(MET, st("Power Attack"))
        assertEquals(UNMET, st("Dodge"))
        assertEquals(MET, st("Weapon Focus (longsword)"))
        assertEquals(UNKNOWN, st("Acrobatics 5 ranks"))
        assertEquals(MET, st("Dodge or Power Attack"))
    }

    @Test
    fun overall() {
        assertEquals(UNMET, FeatPrereqs.overall(FeatPrereqs.evaluate(c, db, "Str 13, Power Attack, Dodge")))
        assertEquals(UNKNOWN, FeatPrereqs.overall(FeatPrereqs.evaluate(c, db, "Str 13, Acrobatics 1 rank")))
        assertEquals(MET, FeatPrereqs.overall(FeatPrereqs.evaluate(c, db, "Str 13, Power Attack, base attack bonus +1")))
    }

    @Test
    fun repeatableFeats() {
        val wf = Feat("weapon-focus", "Weapon Focus", sections = listOf(listOf("Special", "You can gain Weapon Focus multiple times. Its effects do not stack.")))
        val pa = Feat("power-attack", "Power Attack", sections = listOf(listOf("Benefit", "You can choose to take a penalty…")))
        assertEquals(true, wf.repeatable)
        assertEquals(false, pa.repeatable)
    }

    @Test
    fun featCounts() {
        // úroveň 7 → 4 obecné; fighter 4 → 3 bonusové (1, 2, 4); wizard 3 → 0
        val slots = c.featSlots()
        assertEquals(4, slots.general)
        assertEquals(mapOf("fighter" to 3), slots.bonus)
        assertEquals(7, slots.total)
        assertEquals(8, c.copy(extraFeats = 1).featSlots().total)
    }

    @Test
    fun racePrerequisites() {
        fun race(id: String?, text: String) = FeatPrereqs.status(c.copy(race = id), db, text)
        // bez rasy: ověř ručně
        assertEquals(UNKNOWN, race(null, "Dwarf"))
        assertEquals(MET, race("dwarf", "Dwarf"))
        assertEquals(UNMET, race("elf", "Dwarf"))
        // half-elf platí jako elf i člověk, half-orc jako orc
        assertEquals(MET, race("half-elf", "Elf"))
        assertEquals(MET, race("half-elf", "Human"))
        assertEquals(MET, race("half-orc", "Half-orc or orc"))
        assertEquals(MET, race("orc", "Half-orc or orc"))
        assertEquals(UNMET, race("human", "Half-orc or orc"))
        assertEquals(MET, race("drow", "elf"))
        assertEquals(MET, race("svirfneblin", "gnome subtype"))
        // etnikum / varianta: se správnou rasou ověř ručně, s jinou nesplněno
        assertEquals(UNKNOWN, race("human", "Human (Varisian)"))
        assertEquals(UNMET, race("dwarf", "Human (Varisian)"))
        // rasové traity rasa neověří
        assertEquals(UNKNOWN, race("halfling", "halfling jinx trait"))
    }

    @Test
    fun raceAbilitiesAndBonusFeat() {
        // elf: +2 DEX, +2 INT, −2 CON k základu
        val elf = c.copy(race = "elf")
        assertEquals(16, elf.scores.int)
        assertEquals(8, elf.scores.con)
        assertEquals(MET, FeatPrereqs.status(elf, db, "Int 15"))
        assertEquals(UNMET, FeatPrereqs.status(c, db, "Int 15"))
        // human: +2 podle volby, bonusový feat
        val human = c.copy(race = "human", raceBonus = Ability.STR)
        assertEquals(17, human.scores.str)
        assertEquals(15, c.copy(race = "human").scores.str)
        assertEquals(c.featSlots().total + 1, human.featSlots().total)
        assertEquals(c.featSlots().total, elf.featSlots().total)
    }

    @Test
    fun sourceQuirks() {
        // „—“ = bez prerekvizit
        assertEquals(emptyList(), FeatPrereqs.split("—"))
        assertEquals(MET, FeatPrereqs.overall(FeatPrereqs.evaluate(c, db, "—")))
        // zkratka knihy přilepená ke jménu featu, „… with the chosen weapon“
        assertEquals(MET, st("Power AttackAPG"))
        assertEquals(UNMET, st("DodgeUC"))
        assertEquals(MET, st("Weapon Focus with the chosen weapon"))
    }
}
