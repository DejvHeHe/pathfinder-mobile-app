package cz.pfspells.data

/*
 * Mechanika archetypů (ClassEntry.archetypes):
 * 1. automaticky ze seznamu nahrazených class features (ArchetypePick.replaces, sloupec „Replaces“ na AoN):
 *    - celá schopnost s počtem použití („Smite Evil“, „Rage“, „Bombs“…) → předvyplněná schopnost zmizí,
 *    - podtřída („Domains“, „Arcane School“, „Bloodline“, „Patron“…) → classa podtřídu nemá; „Bloodline Powers“ jen
 *      schopnosti, „Mystery Bonus Spells“ / „Patron Spells“… jen bonusová kouzla,
 *    - hexy / revelations / bonusové featy („2nd, 6th-level hex“, „5th, 10th-level bonus feats“) → nižší počet,
 *    - „Cantrips“ / „Orisons“ / „Knacks“ → bez triků;
 * 2. ručně zadané efekty známých archetypů (ARCHETYPE_MODS): ztráta sesílání (Skirmisher, Warrior of the Holy Light…),
 *    o kouzlo méně za den / známé (Crusader, Survivor…), jedna doména (Cloistered Cleric), doména paladina
 *    (Sacred Servant), nové schopnosti s počtem použití.
 * „Spells“ / „Spellcasting“ v Replaces automaticky nic nedělá – většinou jde o změněné, ne zrušené sesílání.
 */

/**
 * Změna sesílání archetypem: jiný spell list ([lists] místo původních, [addLists] navíc), atribut, způsob sesílání,
 * tabulka kouzel za den / známých jiné classy ([table] + [progression]), druh magie, kouzla navíc ([spells]: jméno → level).
 */
class CastingChange(
    val lists: List<String>? = null,
    val addLists: List<String> = emptyList(),
    val ability: Ability? = null,
    val kind: CastingKind? = null,
    val table: String? = null,
    val progression: Progression? = null,
    val tradition: MagicTradition? = null,
    /** atribut sesílání se počítá o tolik vyšší (Scarred Witch Doctor: Int +2) */
    val abilityBonus: Int = 0,
    val spells: Map<String, Int> = emptyMap(),
)

/** Ručně zadaný efekt archetypu (id archetypu → efekt); vše volitelné. */
class ArchetypeMod(
    /** classa nesesílá vůbec */
    val noSpells: Boolean = false,
    /** změna počtu kouzel za den na každém levelu od 1. (−1 = „one fewer spell of each level“) */
    val perDayDelta: Int = 0,
    /** změna počtu známých kouzel na každém levelu (spontánní) */
    val knownDelta: Int = 0,
    /** změna platí i pro triky (Crossblooded „including cantrips“; Morphic Savant jen „spell levels 1–6“) */
    val deltaIncludesCantrips: Boolean = true,
    /** úrovně classy, na kterých archetyp dává bonusový feat navíc (Insinuator: 4., 7., 10.…) */
    val bonusFeatLevels: List<Int> = emptyList(),
    /** jen jedna doména místo dvou (cleric) */
    val oneDomain: Boolean = false,
    /** podtřída navíc / místo původní (Sacred Servant: doména paladina) */
    val subclass: SubclassType? = null,
    /** předvyplněné schopnosti, které archetyp ruší, i když je „Replaces“ nejmenuje (jména malými písmeny) */
    val removes: Set<String> = emptySet(),
    /** nové schopnosti s počtem použití (classId doplní [ArchetypeRules]) */
    val presets: List<ArchetypePreset> = emptyList(),
    /** změna sesílání (jiný list, atribut, tabulka…) */
    val casting: CastingChange? = null,
    /** efektivní úroveň pro schopnosti podtřídy = úroveň classy + [subclassLevelOffset] (Sacred Servant: paladin −3) */
    val subclassLevelOffset: Int = 0,
    /** dvě bloodline (Crossblooded) */
    val secondBloodline: Boolean = false,
)

/** Schopnost archetypu s počtem použití: [max] podle úrovně classy a postavy. */
class ArchetypePreset(
    val key: String,
    val name: String,
    val unit: ResourceUnit,
    val minLevel: Int = 1,
    val max: (level: Int, c: Character) -> Int?,
)

/** Výsledný efekt archetypů jedné classy postavy. */
data class ArchetypeEffect(
    /** zrušené předvyplněné schopnosti (jména malými písmeny, jako `FeaturePreset.name`) */
    val removedPresets: Set<String> = emptySet(),
    val noSpells: Boolean = false,
    val noCantrips: Boolean = false,
    val noSubclass: Boolean = false,
    val noSubclassSpells: Boolean = false,
    val noSubclassPowers: Boolean = false,
    /** o kolik méně hexů / revelations (už podle úrovně classy) */
    val lostAbilitySlots: Int = 0,
    /** o kolik méně bonusových featů classy (už podle úrovně classy) */
    val lostBonusFeats: Int = 0,
    val perDayDelta: Int = 0,
    val knownDelta: Int = 0,
    val deltaIncludesCantrips: Boolean = true,
    /** bonusové featy navíc z archetypu (už podle úrovně classy) */
    val gainedBonusFeats: Int = 0,
    val oneDomain: Boolean = false,
    val subclass: SubclassType? = null,
    val presets: List<FeaturePreset> = emptyList(),
    val casting: CastingChange? = null,
    /** kouzla navíc na listu (z textu archetypu i z [casting]): spellId → spell level */
    val addedSpells: Map<String, Int> = emptyMap(),
    val subclassLevelOffset: Int = 0,
    val secondBloodline: Boolean = false,
) {
    val isEmpty: Boolean get() = this == NONE

    companion object {
        val NONE = ArchetypeEffect()
    }
}

object ArchetypeRules {
    /** Normalizace položky Replaces: malá písmena, rovné apostrofy, bez kurzívy / tečky na konci, opravy překlepů AoN. */
    fun norm(s: String): String = s.lowercase().replace('’', '\'').replace('�', '\'').trim().trimEnd('.')
        .replace(Regex("\\s+"), " ")
        .replace("judgement", "judgment").replace("persistant", "persistent").replace("proficieny", "proficiency")
        .replace("levle", "level").replace("--", "-")

    /** Jiná jména předvyplněných schopností v Replaces (normalizované → `FeaturePreset.name` malými). */
    private val PRESET_ALIASES = mapOf(
        "bomb" to "bombs", "alchemist bomb" to "bombs",
        "channel positive energy" to "channel energy",
    )

    /** Položka Replaces, která ruší celou podtřídu classy. */
    private val SUBCLASS_FEATURES: Map<String, Set<String>> = mapOf(
        "cleric" to setOf("domains", "domain"),
        "druid" to setOf("nature bond", "nature's bond", "domains", "domain"),
        "inquisitor" to setOf("domain", "domains"),
        "oracle" to setOf("mystery"),
        "sorcerer" to setOf("bloodline"),
        "bloodrager" to setOf("bloodline"),
        "wizard" to setOf("arcane school"),
        "witch" to setOf("patron"),
        "shaman" to setOf("spirit"),
        "psychic" to setOf("psychic discipline", "discipline"),
        "warpriest" to setOf("blessings"),
    )

    /** Jen bonusová kouzla podtřídy (schopnosti zůstávají). */
    private val SUBCLASS_SPELLS = setOf(
        "bonus spells", "mystery bonus spells", "bloodline spells", "bloodline bonus spells", "bloodline spell",
        "patron spells", "discipline spells", "spirit magic", "spirit magic spells", "domain spells",
    )

    /** Jen schopnosti podtřídy (kouzla zůstávají). */
    private val SUBCLASS_POWERS = setOf(
        "bloodline powers", "domain powers", "discipline powers", "spirit abilities", "altered domain abilities", "arcane school powers",
    )

    private val CANTRIPS = setOf("cantrips", "orisons", "knacks", "0-level spells")

    /** Bonusové featy classy pod jiným jménem (inquisitor / hunter = teamwork, ranger = combat style). */
    private val BONUS_FEAT_WORDS = listOf("bonus feat", "bonus combat feat", "teamwork feat", "combat style feat", "combat feat")

    private val LEVEL = Regex("(\\d+)(?:-?(?:st|nd|rd|th))?")

    /** Úrovně uvedené v položce („2nd, 6th, and 10th-level hexes“ → 2, 6, 10); prázdné = celá schopnost. */
    fun levelsIn(entry: String): List<Int> {
        val head = entry.substringBefore("level", missingDelimiterValue = "")
        if (head.isEmpty()) return emptyList()
        return LEVEL.findAll(head).mapNotNull { it.groupValues[1].toIntOrNull() }.filter { it in 1..20 }.toList()
    }

    private fun mentions(entry: String, words: List<String>): Boolean = words.any { w -> entry.contains(w) }

    /**
     * Kolik kusů schopnosti s úrovněmi [classLevels] (hexy, bonusové featy…) archetyp na úrovni [level] bere:
     * bez úrovní celá schopnost, jinak jen uvedené úrovně, které classa už má.
     */
    private fun lost(entry: String, level: Int, classLevels: List<Int>?): Int {
        val lv = levelsIn(entry)
        return if (lv.isEmpty()) classLevels?.count { it <= level } ?: Int.MAX_VALUE
        else lv.count { it <= level && (classLevels == null || it in classLevels) }
    }

    fun effect(e: ClassEntry): ArchetypeEffect {
        if (e.archetypes.isEmpty()) return ArchetypeEffect.NONE
        val base = Archetypes.baseClass(e.classId)
        val presetNames = FeaturePresets.all.filter { it.classId == e.classId }.map { it.name.lowercase() }.toSet() + "phrenic pool"
        val removed = mutableSetOf<String>()
        var noSubclass = false
        var noSubclassSpells = false
        var noSubclassPowers = false
        var noCantrips = false
        var lostAbilities = 0
        var lostFeats = 0
        val abilityGroup = AbilityRules.groupOf(e.classId)
        val abilityWord = when (abilityGroup) {
            AbilityGroup.HEXES -> "hex"
            AbilityGroup.REVELATIONS -> "revelation"
            null -> null
        }
        val abilityLevels = (1..20).filter { AbilityRules.slots(e.classId, it) > (if (it == 1) 0 else AbilityRules.slots(e.classId, it - 1)) }
        val featLevels = bonusFeatLevels(e.classId)
        for (p in e.archetypes) for (raw in p.replaces) {
            val r = norm(raw)
            val name = PRESET_ALIASES[r] ?: r
            if (name in presetNames) removed += name
            if (r in SUBCLASS_FEATURES[base].orEmpty()) noSubclass = true
            if (r in SUBCLASS_SPELLS) noSubclassSpells = true
            if (r in SUBCLASS_POWERS) noSubclassPowers = true
            if (r in CANTRIPS) noCantrips = true
            if (abilityWord != null && r.contains(abilityWord) && !r.contains("final revelation") && !r.contains("wandering hex")) {
                lostAbilities += lost(r, e.level, abilityLevels)
            }
            if (featLevels.isNotEmpty() && mentions(r, BONUS_FEAT_WORDS) && !r.contains("bloodline")) {
                lostFeats += lost(r, e.level, featLevels)
            }
        }
        val mods = e.archetypes.mapNotNull { ARCHETYPE_MODS[it.id] }
        removed += mods.flatMap { it.removes }
        val presets = e.archetypes.flatMap { p ->
            ARCHETYPE_MODS[p.id]?.presets.orEmpty().map { ap ->
                FeaturePreset("arch:${e.classId}:${p.id}:${ap.key}", e.classId, ap.name, ap.unit, ap.minLevel, ap.max)
            }
        }
        val effect = ArchetypeEffect(
            removedPresets = removed,
            noSpells = mods.any { it.noSpells },
            noCantrips = noCantrips,
            noSubclass = noSubclass,
            noSubclassSpells = noSubclassSpells,
            noSubclassPowers = noSubclassPowers,
            lostAbilitySlots = lostAbilities.coerceAtMost(AbilityRules.slots(e.classId, e.level)),
            lostBonusFeats = lostFeats.coerceAtMost(featLevels.count { it <= e.level }),
            perDayDelta = mods.sumOf { it.perDayDelta },
            knownDelta = mods.sumOf { it.knownDelta },
            deltaIncludesCantrips = mods.filter { it.perDayDelta != 0 || it.knownDelta != 0 }.all { it.deltaIncludesCantrips },
            gainedBonusFeats = mods.sumOf { m -> m.bonusFeatLevels.count { it <= e.level } },
            oneDomain = mods.any { it.oneDomain },
            subclass = mods.firstNotNullOfOrNull { it.subclass },
            presets = presets,
            casting = mods.firstNotNullOfOrNull { it.casting },
            addedSpells = buildMap {
                for (p in e.archetypes) for ((id, lvl) in p.spells) put(id, minOf(lvl, get(id) ?: lvl))
                for (m in mods) for ((name, lvl) in m.casting?.spells.orEmpty()) {
                    val id = SpellParser.slug(name)
                    put(id, minOf(lvl, get(id) ?: lvl))
                }
            },
            subclassLevelOffset = mods.sumOf { it.subclassLevelOffset },
            secondBloodline = mods.any { it.secondBloodline },
        )
        return if (effect == ArchetypeEffect.NONE) ArchetypeEffect.NONE else effect
    }
}

/** Efekt archetypů classy (prázdný bez archetypů). */
val ClassEntry.archetypeEffect: ArchetypeEffect get() = ArchetypeRules.effect(this)

/**
 * Sesílání classy s ohledem na archetypy: null = classa nesesílá nebo o sesílání archetypem přišla; jinak případně
 * jiný spell list, atribut, způsob sesílání, tabulka a kouzla navíc ([CastingChange], kouzla z textu archetypu).
 */
val ClassEntry.caster: CasterClass?
    get() {
        val base = CasterClasses.get(classId) ?: return null
        if (archetypes.isEmpty()) return base
        val fx = archetypeEffect
        if (fx.noSpells) return null
        val ch = fx.casting
        if (ch == null && fx.addedSpells.isEmpty()) return base
        val progression = ch?.progression ?: base.progression
        return base.copy(
            listKeys = (ch?.lists ?: base.listKeys) + ch?.addLists.orEmpty(),
            ability = ch?.ability ?: base.ability,
            kind = ch?.kind ?: base.kind,
            progression = progression,
            hasCantrips = if (ch?.progression != null) progression != Progression.FOUR else base.hasCantrips,
            tableId = ch?.table ?: base.tableId,
            traditionOverride = ch?.tradition,
            extraSpells = fx.addedSpells,
        )
    }

/** Efektivní úroveň classy pro schopnosti podtřídy (Sacred Servant: paladin −3). */
val ClassEntry.subclassLevel: Int
    get() = if (archetypes.isEmpty()) level else level + archetypeEffect.subclassLevelOffset

/**
 * Podtřída classy s ohledem na archetypy: zrušená („Domains“, „Arcane School“…) → null; archetyp, který podtřídu dává
 * (Sacred Servant), ji doplní; Cloistered Cleric a spol. → jedna doména.
 */
val ClassEntry.subclassKind: SubclassType?
    get() {
        val base = subclassType(classId)
        if (archetypes.isEmpty()) return base
        val fx = archetypeEffect
        fx.subclass?.let { return it }
        if (fx.noSubclass) return null
        return if (base == SubclassType.TWO_DOMAINS && fx.oneDomain) SubclassType.ONE_DOMAIN else base
    }

/**
 * Limity spell levelu s ohledem na archetypy ([casterLevel] = sesílací úroveň vč. prestižních class): bez triků; o kouzlo méně za den / známé na každém levelu, který classa má
 * (i triky – „a 4th-level survivor can cast three cantrips“); bez limitu (spontánní triky) zůstane bez limitu.
 */
fun ClassEntry.levelSlots(cls: CasterClass, abilityScore: Int, spellLevel: Int, casterLevel: Int = level): LevelSlots {
    if (archetypes.isEmpty()) return cls.levelSlots(casterLevel, abilityScore, spellLevel)
    val fx = archetypeEffect
    val s = cls.levelSlots(casterLevel, abilityScore + (fx.casting?.abilityBonus ?: 0), spellLevel)
    if (spellLevel == 0 && fx.noCantrips) return s.copy(perDay = 0, limit = 0)
    if (fx.perDayDelta == 0 && fx.knownDelta == 0) return s
    if (spellLevel == 0 && !fx.deltaIncludesCantrips) return s
    val perDay = s.perDay?.let { if (it > 0) (it + fx.perDayDelta).coerceAtLeast(0) else it }
    val limit = when (cls.kind) {
        CastingKind.PREPARED -> perDay?.plus(s.bonus)
        CastingKind.SPONTANEOUS, CastingKind.HYBRID -> s.limit?.let { if (it > 0) (it + fx.knownDelta).coerceAtLeast(0) else it }
    }
    return s.copy(perDay = perDay, limit = limit)
}
