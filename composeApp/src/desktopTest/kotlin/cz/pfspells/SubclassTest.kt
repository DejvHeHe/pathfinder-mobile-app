package cz.pfspells

import cz.pfspells.data.ClassEntry
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SpellParser
import cz.pfspells.data.SpellSource
import cz.pfspells.data.SubclassKind
import cz.pfspells.data.SubclassSlot
import cz.pfspells.data.bonusSpellsByLevel
import cz.pfspells.data.hasSubclassSlot
import cz.pfspells.data.isOppositionSpell
import cz.pfspells.data.isSchoolSpell
import cz.pfspells.data.withSubclass
import cz.pfspells.data.DRUID_DOMAINS
import cz.pfspells.data.domainsFor
import cz.pfspells.data.subdomainParents
import cz.pfspells.data.subdomainsOf
import cz.pfspells.data.BLESSINGS
import cz.pfspells.data.BLESSING_DOMAINS
import cz.pfspells.data.DeityCatalog
import cz.pfspells.data.SUBCLASS_SPELL_TABLE
import cz.pfspells.data.SubclassPowers
import cz.pfspells.data.blessingAllowed
import cz.pfspells.data.deityConflicts
import cz.pfspells.data.Ability
import cz.pfspells.data.AbilityRules
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Character
import cz.pfspells.data.ClassAbilityRepository
import cz.pfspells.data.FeatPick
import cz.pfspells.data.INQUISITIONS
import cz.pfspells.data.disciplinePoolAbility
import cz.pfspells.data.inquisitionAllowed
import cz.pfspells.data.resources
import cz.pfspells.data.spellLevelFor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Podtřídy z access_ways v přibaleném SpellList.json. */
class SubclassTest {
    private val db by lazy {
        SpellDatabase(SpellParser.parse(File("src/commonMain/composeResources/files/SpellList.json").readText()), SpellSource.BUNDLED, null)
    }
    private val idx get() = db.subclasses
    private fun names(kind: SubclassKind, name: String, lvl: Int) = idx.spells(kind, name)[lvl].orEmpty().map { it.name }

    @Test
    fun parsesSubclassLists() {
        assertTrue("Burning Hands" in names(SubclassKind.DOMAIN, "fire", 1))
        assertTrue("Fireball" in names(SubclassKind.DOMAIN, "fire", 3))
        assertTrue("Identify" in names(SubclassKind.BLOODLINE, "arcane", 1))
        assertTrue("Burning Hands" in names(SubclassKind.MYSTERY, "flame", 1))
        assertTrue("Fireball" in names(SubclassKind.ELEMENT, "fire", 3))
        // čištění zdroje: mezery, slepené záznamy, překlepy
        val domains = idx.names(SubclassKind.DOMAIN)
        assertTrue("repose" in domains && "war" in domains && " war" !in domains)
        assertFalse(idx.names(SubclassKind.BLOODLINE).any { "elemental school" in it })
        assertTrue("aristocracy" in idx.names(SubclassKind.SUBDOMAIN) && "aristocra" !in idx.names(SubclassKind.SUBDOMAIN))
        assertTrue("juju" in idx.names(SubclassKind.MYSTERY))
        println("domains=${domains.size} subdomains=${idx.names(SubclassKind.SUBDOMAIN).size} mysteries=${idx.names(SubclassKind.MYSTERY).size} bloodlines=${idx.names(SubclassKind.BLOODLINE).size}")
    }

    @Test
    fun clericDomainsWithSubdomain() {
        val e = ClassEntry("cleric", 7, subclass = mapOf(SubclassSlot.DOMAIN1 to "fire", SubclassSlot.DOMAIN2 to "magic", SubclassSlot.SUBDOMAIN2 to "arcane"))
        val bonus = e.bonusSpellsByLevel(idx)
        assertEquals((1..4).toList(), bonus.keys.sorted()) // cleric 7 → max 4. level
        assertTrue(bonus[1]!!.any { it.name == "Burning Hands" })
        assertTrue(bonus[1]!!.any { it.name == "Magic Aura" }) // arcane subdoména nahrazuje 1. level magic
        assertTrue(e.hasSubclassSlot())
    }

    @Test
    fun sorcererBloodlineByLevel() {
        val e = ClassEntry("sorcerer", 5, subclass = mapOf(SubclassSlot.BLOODLINE to "arcane"))
        // bloodline spell N od úrovně 2N+1 → na 5. úrovni levely 1 a 2
        assertEquals(listOf(1, 2), e.bonusSpellsByLevel(idx).keys.sorted())
        assertFalse(e.hasSubclassSlot())
    }

    @Test
    fun oracleMysteryByLevel() {
        val e = ClassEntry("oracle", 4, subclass = mapOf(SubclassSlot.MYSTERY to "flame"))
        assertEquals(listOf(1, 2), e.bonusSpellsByLevel(idx).keys.sorted())
    }

    @Test
    fun wizardSchools() {
        val fireball = db.get("fireball")!!
        val charm = db.get("charm-person")!!
        val evoker = ClassEntry("wizard", 5, subclass = mapOf(SubclassSlot.SCHOOL to "evocation", SubclassSlot.OPPOSITION1 to "enchantment"))
        assertTrue(evoker.isSchoolSpell(fireball))
        assertTrue(evoker.isOppositionSpell(charm))
        assertTrue(evoker.hasSubclassSlot())
        val fireSchool = ClassEntry("wizard", 5, subclass = mapOf(SubclassSlot.SCHOOL to "element:fire", SubclassSlot.OPPOSITION1 to "element:water"))
        assertTrue(fireSchool.isSchoolSpell(fireball))
        assertFalse(ClassEntry("wizard", 5, subclass = mapOf(SubclassSlot.SCHOOL to "universalist")).hasSubclassSlot())
    }

    @Test
    fun subclassMapChanges() {
        var sub = emptyMap<String, String>()
            .withSubclass(SubclassSlot.DOMAIN1, "fire").withSubclass(SubclassSlot.SUBDOMAIN1, "ash")
        assertEquals("ash", sub[SubclassSlot.SUBDOMAIN1])
        // zrušená doména ruší i subdoménu
        sub = sub.withSubclass(SubclassSlot.DOMAIN1, null)
        assertTrue(sub.isEmpty())
        // jiná specializace ruší opoziční školy
        sub = emptyMap<String, String>().withSubclass(SubclassSlot.SCHOOL, "evocation")
            .withSubclass(SubclassSlot.OPPOSITION1, "illusion").withSubclass(SubclassSlot.SCHOOL, "abjuration")
        assertEquals(mapOf(SubclassSlot.SCHOOL to "abjuration"), sub)
    }

    @Test
    fun subdomainsBelongToTheirDomain() {
        assertEquals(listOf("arson", "ash", "smoke"), idx.subdomainsOf("fire", "cleric"))
        assertTrue(idx.subdomainsOf("law", "cleric").any { it.startsWith("archon") })
        assertTrue(idx.subdomainsOf("good", "cleric").any { it.startsWith("archon") })
        assertTrue("metal" in idx.subdomainsOf("earth", "cleric"))
        assertFalse("metal" in idx.subdomainsOf("earth", "druid"))
        // druid jen přírodní domény
        assertEquals(DRUID_DOMAINS, idx.domainsFor("druid"))
        assertTrue(idx.domainsFor("cleric").size > 30)
        // téměř každá subdoména ze zdroje má známou doménu
        val unknown = idx.names(SubclassKind.SUBDOMAIN).filter { subdomainParents(it).isEmpty() }
        assertTrue(unknown.size <= 3, "neznámé subdomény: $unknown")
        // jiná doména zruší cizí subdoménu, vlastní nechá
        val sub = mapOf(SubclassSlot.DOMAIN1 to "fire", SubclassSlot.SUBDOMAIN1 to "ash")
        assertEquals(null, sub.withSubclass(SubclassSlot.DOMAIN1, "water")[SubclassSlot.SUBDOMAIN1])
        assertEquals("ash", sub.withSubclass(SubclassSlot.DOMAIN1, "fire")[SubclassSlot.SUBDOMAIN1])
    }

    @Test
    fun foundrySubclassSpellsResolve() {
        // všechna kouzla z tabulky Foundry se najdou v SpellList.json
        val byName = db.spells.map { it.name.lowercase() }.toSet()
        val missing = SUBCLASS_SPELL_TABLE.flatMap { (k, l) -> l.filter { it.isNotEmpty() && it.lowercase() !in byName }.map { "$k: $it" } }
        assertTrue(missing.isEmpty(), "chybí: $missing")
        assertTrue(idx.names(SubclassKind.PATRON).size >= 50)
        assertTrue("flame" in idx.names(SubclassKind.SPIRIT))
        assertTrue("abyssal" in idx.names(SubclassKind.BLOODRAGER_BLOODLINE))
        assertTrue("dream" in idx.names(SubclassKind.DISCIPLINE))
        assertEquals(listOf("Summon Monster 5"), names(SubclassKind.SPIRIT, "flame", 5))
    }

    @Test
    fun witchPatronByLevel() {
        val e = ClassEntry("witch", 5, subclass = mapOf(SubclassSlot.PATRON to "agility"))
        // kouzlo patrona N od úrovně 2N → na 5. úrovni levely 1 a 2 (3. až na 6.)
        val bonus = e.bonusSpellsByLevel(idx)
        assertEquals(listOf(1, 2), bonus.keys.sorted())
        assertEquals("Jump", bonus[1]!!.single().name)
        assertFalse(e.hasSubclassSlot())
    }

    @Test
    fun shamanSpiritBloodragerPsychic() {
        // shaman: spirit magic na každém levelu, který umí (5. úroveň → 1.–3.)
        assertEquals(listOf(1, 2, 3), ClassEntry("shaman", 5, subclass = mapOf(SubclassSlot.SPIRIT to "flame")).bonusSpellsByLevel(idx).keys.sorted())
        // bloodrager: 7. / 10. / 13. / 16. úroveň
        assertTrue(ClassEntry("bloodrager", 6, subclass = mapOf(SubclassSlot.BLOODLINE to "abyssal")).bonusSpellsByLevel(idx).isEmpty())
        val br10 = ClassEntry("bloodrager", 10, subclass = mapOf(SubclassSlot.BLOODLINE to "abyssal")).bonusSpellsByLevel(idx)
        assertEquals(listOf(1, 2), br10.keys.sorted())
        assertEquals("Ray of Enfeeblement", br10[1]!!.single().name)
        // psychic: 1. úroveň → 1. level, 4. úroveň → 2. level
        assertEquals(listOf(1), ClassEntry("psychic", 1, subclass = mapOf(SubclassSlot.DISCIPLINE to "dream")).bonusSpellsByLevel(idx).keys.sorted())
        assertEquals(listOf(1, 2), ClassEntry("psychic", 4, subclass = mapOf(SubclassSlot.DISCIPLINE to "dream")).bonusSpellsByLevel(idx).keys.sorted())
    }

    @Test
    fun warpriestBlessingsAndInquisitorDomain() {
        val wp = ClassEntry("warpriest", 10, subclass = mapOf(SubclassSlot.BLESSING1 to "fire", SubclassSlot.BLESSING2 to "war"))
        assertEquals(listOf("blessing:fire" to "Fire", "blessing:war" to "War"), SubclassPowers.keysOf(wp))
        assertEquals(listOf("Fire Strike", "Armor of Flame"), SubclassPowers.of("blessing:fire")!!.map { it.name })
        assertTrue(wp.bonusSpellsByLevel(idx).isEmpty())
        assertTrue("death" in BLESSINGS && BLESSING_DOMAINS["death"] == "death")
        // blessingy podle božstva: Sarenrae (Fire…) ano, Death ne; bez božstva vše
        val sarenrae = DeityCatalog.get("Sarenrae")!!
        assertTrue(blessingAllowed("fire", sarenrae))
        assertFalse(blessingAllowed("death", sarenrae))
        assertTrue(blessingAllowed("death", null))
        assertEquals(listOf("death"), wp.copy(subclass = mapOf(SubclassSlot.BLESSING1 to "fire", SubclassSlot.BLESSING2 to "death")).deityConflicts(sarenrae))
        // inquisitor: doména jen se schopnostmi (bez kouzel a slotu), subdoména má přednost
        val inq = ClassEntry("inquisitor", 8, subclass = mapOf(SubclassSlot.DOMAIN1 to "fire", SubclassSlot.SUBDOMAIN1 to "ash"))
        assertTrue(inq.bonusSpellsByLevel(idx).isEmpty())
        assertFalse(inq.hasSubclassSlot())
        assertTrue(SubclassPowers.keysOf(inq).single().first.startsWith("subdomain:ash"))
        assertEquals(listOf("water"), inq.copy(subclass = mapOf(SubclassSlot.DOMAIN1 to "water")).deityConflicts(sarenrae))
    }

    @Test
    fun aonPowersSpiritBloodragerDiscipline() {
        val shaman = ClassEntry("shaman", 8, subclass = mapOf(SubclassSlot.SPIRIT to "flame"))
        assertEquals(listOf("spirit:flame" to "Flame"), SubclassPowers.keysOf(shaman))
        val flame = SubclassPowers.of("spirit:flame")!!
        assertEquals(listOf(1, 8, 16, 20), flame.map { it.level })
        assertEquals("3 + @abilities.cha.mod", flame.first { it.name == "Touch of Flame" }.uses)
        val br = SubclassPowers.of(SubclassPowers.keysOf(ClassEntry("bloodrager", 4, subclass = mapOf(SubclassSlot.BLOODLINE to "abyssal"))).single().first)!!
        assertEquals("Demonic Bulk", br.first { it.level == 4 }.name)
        assertTrue(SubclassPowers.of("discipline:dream")!!.any { it.name == "Oneiromancy" })
        // phrenic pool: ½ úrovně + Cha (dream)
        val psychic = Character("p", "Mira", listOf(ClassEntry("psychic", 6, subclass = mapOf(SubclassSlot.DISCIPLINE to "dream"))))
            .copy(abilities = cz.pfspells.data.Abilities(cha = 14))
        assertEquals(3 + 2, psychic.resources().first { it.name == "Phrenic Pool" }.max)
        assertEquals(Ability.WIS, disciplinePoolAbility("faith"))
    }

    @Test
    fun wanderingSpirit() {
        val sub = mapOf(SubclassSlot.SPIRIT to "flame", SubclassSlot.WANDERING_SPIRIT to "life")
        // 3. úroveň: wandering spirit ještě není
        assertEquals(listOf("spirit:flame"), SubclassPowers.keysOf(ClassEntry("shaman", 3, subclass = sub)).map { it.first })
        val s12 = ClassEntry("shaman", 12, subclass = sub)
        assertEquals(listOf("spirit:flame", "wspirit:life"), SubclassPowers.keysOf(s12).map { it.first })
        // schopnost ducha od 4., větší od 12., pravá od 20.; bez manifestace
        assertEquals(listOf(4, 12, 20), SubclassPowers.of("wspirit:life")!!.map { it.level }.distinct())
        assertTrue(SubclassPowers.of("wspirit:life")!!.none { it.name == "Manifestation" })
        // kouzla magie ducha z obou duchů
        val lvl1 = s12.bonusSpellsByLevel(idx)[1]!!.map { it.name }
        assertTrue("Burning Hands" in lvl1 && lvl1.size == 2, "$lvl1")
        // wandering hex: 1 od 6., 2 od 14.
        assertEquals(listOf(0, 1, 2), listOf(5, 6, 14).map { AbilityRules.wanderingHexes(ClassEntry("shaman", it, subclass = sub)) })
        // stejný duch jako hlavní → wandering spirit odpadne
        assertEquals(null, sub.withSubclass(SubclassSlot.SPIRIT, "life")[SubclassSlot.WANDERING_SPIRIT])
    }

    @Test
    fun bloodlineBonusFeatsAndSorcererPowers() {
        val br = ClassEntry("bloodrager", 6, subclass = mapOf(SubclassSlot.BLOODLINE to "abyssal"))
        assertTrue("Power Attack" in SubclassPowers.bonusFeats(br))
        val sorc = ClassEntry("sorcerer", 7, subclass = mapOf(SubclassSlot.BLOODLINE to "draconic"))
        assertTrue("Quicken Spell" in SubclassPowers.bonusFeats(sorc))
        // Draconic zná Foundry → schopnosti jen z Foundry (bez zdvojení z AoN)
        val draconic = SubclassPowers.of("bloodline:draconic")!!
        assertEquals(draconic.size, draconic.map { it.name }.distinct().size)
        // Accursed Foundry nezná → schopnosti z AoN
        assertTrue(SubclassPowers.of("bloodline:accursed")!!.any { it.name == "Horrific Visage" })
        // „fire“ ze zdroje kouzel = Elemental
        assertEquals("bloodline:elemental", SubclassPowers.keysOf(ClassEntry("sorcerer", 3, subclass = mapOf(SubclassSlot.BLOODLINE to "fire"))).single().first)
    }

    @Test
    fun inquisitionReplacesDomain() {
        val iomedae = DeityCatalog.get("Iomedae")!!
        assertTrue(inquisitionAllowed("banishment", iomedae))
        assertFalse(inquisitionAllowed("anger", iomedae))
        assertTrue(inquisitionAllowed("black powder", iomedae)) // „Any“
        assertTrue(INQUISITIONS.size >= 35)
        val sub = mapOf(SubclassSlot.DOMAIN1 to "sun").withSubclass(SubclassSlot.INQUISITION, "banishment")
        assertEquals(mapOf(SubclassSlot.INQUISITION to "banishment"), sub)
        assertEquals(mapOf(SubclassSlot.DOMAIN1 to "sun"), sub.withSubclass(SubclassSlot.DOMAIN1, "sun"))
        val inq = ClassEntry("inquisitor", 8, subclass = sub)
        assertEquals(listOf("inquisition:banishment" to "Banishment"), SubclassPowers.keysOf(inq))
        assertEquals(listOf("anger"), inq.copy(subclass = mapOf(SubclassSlot.INQUISITION to "anger")).deityConflicts(iomedae))
    }

    @Test
    fun patronSpellsCanBePrepared() {
        val haste = db.findByName("Haste")!!
        val witchCls = CasterClasses.get("witch")!!
        assertEquals(null, witchCls.spellLevelOf(haste)) // haste na spell listu čarodějnice není
        val w6 = ClassEntry("witch", 6, subclass = mapOf(SubclassSlot.PATRON to "agility"))
        assertEquals(3, w6.spellLevelFor(witchCls, haste, idx))
        assertEquals(null, w6.copy(level = 5).spellLevelFor(witchCls, haste, idx))
        assertEquals(null, ClassEntry("witch", 6).spellLevelFor(witchCls, haste, idx))
    }

    @Test
    fun spiritHexesAndExtraHexFeats() {
        val adb = ClassAbilityRepository.parse(
            File("src/commonMain/composeResources/files/ClassAbilities.json").readText(),
            File("src/commonMain/composeResources/files/SubclassAbilities.json").readText(),
        )
        val shamanHexes = adb.forClass("shaman")
        val spiritHexes = shamanHexes.filter { it.spirits.isNotEmpty() }
        assertTrue(spiritHexes.size >= 60, "spirit hexes: ${spiritHexes.size}")
        assertEquals(shamanHexes.size, shamanHexes.map { it.searchName }.distinct().size) // žádné duplicitní jméno
        val fireNimbus = spiritHexes.first { it.name == "Fire Nimbus" }
        assertTrue(AbilityRules.matchesSpirit(fireNimbus, "flame"))
        assertFalse(AbilityRules.matchesSpirit(fireNimbus, "life"))
        assertTrue(adb.forClass("witch").none { it.spirits.isNotEmpty() })
        // Extra Hex: +1 za každý feat
        val e = ClassEntry("witch", 4)
        val c = Character("w", "Morgana", listOf(e), feats = listOf(FeatPick("extra-hex", "Extra Hex"), FeatPick("extra-hex", "Extra Hex"), FeatPick(null, "Extra Hex")))
        assertEquals(AbilityRules.slots("witch", 4) + 3, AbilityRules.slots(c, e))
        assertEquals(0, AbilityRules.extraFeats(c.copy(feats = emptyList()), "witch"))
    }
}
