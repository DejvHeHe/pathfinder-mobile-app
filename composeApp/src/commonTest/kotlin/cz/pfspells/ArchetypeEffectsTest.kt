package cz.pfspells

import cz.pfspells.data.AbilityRules
import cz.pfspells.data.ArchetypePick
import cz.pfspells.data.ArchetypeRules
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.SubclassPowers
import cz.pfspells.data.SubclassSlot
import cz.pfspells.data.SubclassType
import cz.pfspells.data.archetypeEffect
import cz.pfspells.data.caster
import cz.pfspells.data.featSlots
import cz.pfspells.data.levelSlots
import cz.pfspells.data.resources
import cz.pfspells.data.subclassKind
import cz.pfspells.data.subclassLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArchetypeEffectsTest {
    private fun arch(vararg replaces: String) = ArchetypePick("x:test", "Test", replaces.toList())
    private fun char(e: ClassEntry) = Character("c", "C", listOf(e))

    @Test
    fun levelsInEntry() {
        assertEquals(listOf(2, 6, 10), ArchetypeRules.levelsIn("2nd, 6th, and 10th-level hexes"))
        assertEquals(listOf(1), ArchetypeRules.levelsIn("1st-level bonus feat"))
        assertEquals(listOf(4, 12), ArchetypeRules.levelsIn("4th, 12-th level rogue talents"))
        assertEquals(emptyList(), ArchetypeRules.levelsIn("hexes"))
    }

    @Test
    fun noArchetypeNoEffect() {
        assertTrue(ClassEntry("paladin", 5).archetypeEffect.isEmpty)
    }

    @Test
    fun replacedPresetDisappears() {
        val e = ClassEntry("paladin", 5, archetypes = listOf(arch("Smite Evil", "Divine Bond")))
        val names = char(e).resources().map { it.name }
        assertFalse("Smite Evil" in names)
        assertTrue("Lay on Hands" in names)
        // „Bomb“ / „Alchemist Bomb“ = Bombs, „Judgement“ = Judgment
        assertTrue("bombs" in ClassEntry("alchemist", 3, archetypes = listOf(arch("Bomb"))).archetypeEffect.removedPresets)
        assertTrue("judgment" in ClassEntry("inquisitor", 3, archetypes = listOf(arch("Judgement"))).archetypeEffect.removedPresets)
    }

    @Test
    fun partialHexesByLevel() {
        val a = arch("2nd, 6th-level hex")
        assertEquals(1, ClassEntry("witch", 4, archetypes = listOf(a)).archetypeEffect.lostAbilitySlots)
        assertEquals(2, ClassEntry("witch", 8, archetypes = listOf(a)).archetypeEffect.lostAbilitySlots)
        val e = ClassEntry("witch", 8, archetypes = listOf(a))
        assertEquals(AbilityRules.slots("witch", 8) - 2, AbilityRules.slots(char(e), e))
        // celé „Hexes“ = všechny
        assertEquals(AbilityRules.slots("witch", 5), ClassEntry("witch", 5, archetypes = listOf(arch("Hexes"))).archetypeEffect.lostAbilitySlots)
        // final revelation není revelation slot
        assertEquals(0, ClassEntry("oracle", 20, archetypes = listOf(arch("Final Revelation"))).archetypeEffect.lostAbilitySlots)
    }

    @Test
    fun bonusFeats() {
        val f = ClassEntry("fighter", 4, archetypes = listOf(arch("1st-level bonus feat")))
        assertEquals(2, char(f).featSlots().bonus["fighter"])
        val w = ClassEntry("wizard", 12, archetypes = listOf(arch("5th, 10th, 15th-level bonus feat")))
        assertNull(char(w).featSlots().bonus["wizard"])
        // ranger: combat style feat = bonusový feat
        assertEquals(1, ClassEntry("ranger", 6, archetypes = listOf(arch("6th-level combat style feat"))).archetypeEffect.lostBonusFeats)
    }

    @Test
    fun subclassRemoved() {
        val e = ClassEntry("cleric", 5, subclass = mapOf(SubclassSlot.DOMAIN1 to "fire"), archetypes = listOf(arch("Domains")))
        assertNull(e.subclassKind)
        assertTrue(SubclassPowers.keysOf(e).isEmpty())
        assertFalse(char(e).resources().any { "Fire" in it.name })
        // jen schopnosti bloodline – podtřída i kouzla zůstávají
        val s = ClassEntry("sorcerer", 5, subclass = mapOf(SubclassSlot.BLOODLINE to "arcane"), archetypes = listOf(arch("Bloodline Powers")))
        assertEquals(SubclassType.BLOODLINE, s.subclassKind)
        assertTrue(s.archetypeEffect.noSubclassPowers)
        // wizard bez školy nemá ani obecnou „Arcane school power“
        val w = ClassEntry("wizard", 3, archetypes = listOf(arch("Arcane School")))
        assertFalse(char(w).resources().any { it.name.startsWith("Arcane school") })
    }

    @Test
    fun cantrips() {
        val e = ClassEntry("wizard", 3, archetypes = listOf(arch("Cantrips")))
        val cls = CasterClasses.get("wizard")!!
        assertEquals(0, e.levelSlots(cls, 14, 0).limit)
        assertEquals(cls.levelSlots(3, 14, 1), e.levelSlots(cls, 14, 1))
    }

    private fun known(id: String) = ArchetypePick(id, id)

    @Test
    fun curatedNoSpells() {
        val e = ClassEntry("paladin", 6, archetypes = listOf(known("paladin:warrior-of-the-holy-light")))
        assertNull(e.caster)
        assertFalse(cz.pfspells.data.PrereqRules.castersOf(char(e), null).any())
    }

    @Test
    fun curatedOneFewerSpell() {
        val cls = CasterClasses.get("magus")!!
        val e = ClassEntry("magus", 7, archetypes = listOf(known("magus:kensai")))
        val base = cls.levelSlots(7, 16, 2)
        val s = e.levelSlots(cls, 16, 2)
        assertEquals(base.perDay!! - 1, s.perDay)
        assertEquals(base.limit!! - 1, s.limit)
        // 1. úroveň: 1 kouzlo 1. levelu → 0
        assertEquals(cls.levelSlots(1, 16, 1).perDay, ClassEntry("magus", 1, archetypes = listOf(known("magus:kensai"))).levelSlots(cls, 16, 1).perDay?.plus(1))
    }

    @Test
    fun curatedOneDomain() {
        val e = ClassEntry(
            "cleric", 5, subclass = mapOf(SubclassSlot.DOMAIN1 to "fire", SubclassSlot.DOMAIN2 to "sun"),
            archetypes = listOf(known("cleric:cloistered-cleric")),
        )
        assertEquals(SubclassType.ONE_DOMAIN, e.subclassKind)
        assertEquals(listOf("domain:fire"), SubclassPowers.keysOf(e).map { it.first })
        assertEquals(-1, e.archetypeEffect.perDayDelta)
    }

    @Test
    fun sacredServant() {
        val e = ClassEntry("paladin", 7, subclass = mapOf(SubclassSlot.DOMAIN1 to "fire"), archetypes = listOf(known("paladin:sacred-servant")))
        assertEquals(SubclassType.ONE_DOMAIN, e.subclassKind)
        assertEquals(listOf("domain:fire"), SubclassPowers.keysOf(e).map { it.first })
        val res = char(e).resources()
        assertEquals(listOf(2), res.filter { it.name == "Smite Evil" }.map { it.max })
        assertEquals(listOf(1), res.filter { it.name == "Divine Bond (holy symbol)" }.map { it.max })
        assertTrue(res.any { it.name.startsWith("Fire Bolt") })
    }

    @Test
    fun insinuatorBonusFeats() {
        val e = ClassEntry("antipaladin", 7, archetypes = listOf(known("antipaladin:insinuator")))
        assertNull(e.caster)
        assertEquals(2, char(e).featSlots().bonus["antipaladin"])
        assertEquals(listOf(3), char(e).resources().filter { it.name == "Smite Impudence" }.map { it.max })
    }

    @Test
    fun knownDeltaWithoutCantrips() {
        val cls = CasterClasses.get("summoner")!!
        val e = ClassEntry("summoner", 7, archetypes = listOf(known("summoner:morphic-savant")))
        assertEquals(cls.levelSlots(7, 16, 0), e.levelSlots(cls, 16, 0))
        assertEquals(cls.levelSlots(7, 16, 1).limit!! - 1, e.levelSlots(cls, 16, 1).limit)
    }

    @Test
    fun castingChange() {
        val e = ClassEntry("magus", 7, archetypes = listOf(known("magus:eldritch-scion")))
        val cls = e.caster!!
        assertEquals(cz.pfspells.data.CastingKind.SPONTANEOUS, cls.kind)
        assertEquals(cz.pfspells.data.Ability.CHA, cls.ability)
        assertEquals(listOf("magus"), cls.listKeys)
        val bard = CasterClasses.get("bard")!!
        assertEquals(bard.levelSlots(7, 16, 2), e.levelSlots(cls, 16, 2))
        // jiný spell list
        assertEquals(listOf("witch"), ClassEntry("arcanist", 3, archetypes = listOf(known("arcanist:unlettered-arcanist"))).caster!!.listKeys)
        // druh magie pro prerekvizity („ability to cast divine spells“)
        val filidh = char(ClassEntry("bard", 3, archetypes = listOf(known("bard:filidh"))))
        assertEquals(1, cz.pfspells.data.PrereqRules.castersOf(filidh, "divine").size)
        assertTrue(cz.pfspells.data.PrereqRules.castersOf(filidh, "arcane").isEmpty())
        // Scarred Witch Doctor: Int +2 pro sesílání
        val w = ClassEntry("witch", 5, archetypes = listOf(known("witch:scarred-witch-doctor")))
        assertEquals(CasterClasses.get("witch")!!.levelSlots(5, 18, 3), w.levelSlots(w.caster!!, 16, 3))
    }

    @Test
    fun addedSpells() {
        val e = ClassEntry("ranger", 7, archetypes = listOf(ArchetypePick("ranger:corpse-hunter", "Corpse Hunter", spells = mapOf("detect-undead" to 1))))
        assertEquals(mapOf("detect-undead" to 1), e.caster!!.extraSpells)
        // ručně zadaná kouzla (jména → id)
        val p = ClassEntry("paladin", 7, archetypes = listOf(known("paladin:pearl-seeker")))
        assertEquals(1, p.caster!!.extraSpells["hydraulic-push"])
        assertEquals("bloodrager", p.caster!!.tableId)
    }

    @Test
    fun subclassLevelOffset() {
        val at3 = ClassEntry("paladin", 3, subclass = mapOf(SubclassSlot.DOMAIN1 to "fire"), archetypes = listOf(known("paladin:sacred-servant")))
        assertEquals(0, at3.subclassLevel)
        assertFalse(char(at3).resources().any { it.name.startsWith("Fire Bolt") })
        assertTrue(char(at3.copy(level = 4)).resources().any { it.name.startsWith("Fire Bolt") })
    }

    @Test
    fun crossbloodedSecondBloodline() {
        val e = ClassEntry(
            "sorcerer", 5, subclass = mapOf(SubclassSlot.BLOODLINE to "arcane", SubclassSlot.BLOODLINE2 to "draconic"),
            archetypes = listOf(known("sorcerer:crossblooded")),
        )
        assertEquals(listOf("bloodline:arcane", "bloodline:draconic"), SubclassPowers.keysOf(e).map { it.first })
        // bez archetypu se druhá bloodline ignoruje
        assertEquals(listOf("bloodline:arcane"), SubclassPowers.keysOf(e.copy(archetypes = emptyList())).map { it.first })
        assertEquals(-1, e.archetypeEffect.knownDelta)
    }

    @Test
    fun spellsInReplacesKeepsCasting() {
        // „Spells“ v Replaces většinou znamená změněné sesílání – automaticky se nic neruší
        assertNotNull(ClassEntry("bard", 3, archetypes = listOf(arch("Spells"))).caster)
    }
}
