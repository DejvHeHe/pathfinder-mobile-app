package cz.pfspells.data

/** Proč přísný režim nedovolí přidat kouzlo nebo feat (text dodá `Strings.strictBlocked`). */
sealed interface StrictBlock {
    /** na spell levelu je vybráno tolik kouzel, kolik classa smí (připravené kopie / známá) */
    data class SpellLevelFull(val spellLevel: Int, val limit: Int) : StrictBlock
    /** atribut sesílání je nižší než 10 + spell level */
    data class AbilityTooLow(val spellLevel: Int) : StrictBlock
    /** kouzlo je nad úrovní classy (nebo trik u classy bez triků) */
    data object SpellNotAvailable : StrictBlock
    /** všechny featy podle úrovně, class a rasy jsou vybrané */
    data class FeatSlotsFull(val total: Int) : StrictBlock
    /** některá prerekvizita featu je nesplněná (✗; neověřitelné „?“ se nepočítají) */
    data object FeatPrereqsUnmet : StrictBlock
    /** všechny hexy / revelations podle úrovně classy jsou vybrané */
    data class ClassAbilitySlotsFull(val total: Int) : StrictBlock
    /** hex / revelation vyžaduje vyšší úroveň classy */
    data class ClassAbilityLevel(val minLevel: Int) : StrictBlock
    /** výchozí rasový trait už nahrazuje jiný vybraný alternativní trait */
    data class RacialTraitReplaced(val trait: String) : StrictBlock
    /** class feature už nahrazuje jiný vybraný archetyp */
    data class ArchetypeConflict(val feature: String) : StrictBlock
}

/**
 * Přísný režim (`AppState.strictMode`): kontrola před přidáním kouzla / featu.
 * Limity jsou stejné jako v detailu postavy (nadpisy spell levelů, počet featů); null = přidat jde.
 */
object StrictRules {
    /** Ještě jedna kopie (připravující classa) / nové známé kouzlo (spontánní, arcanist) na levelu kouzla. */
    fun spellBlock(c: Character, classId: String, spell: Spell, db: SpellDatabase): StrictBlock? {
        val e = c.classEntry(classId) ?: return null
        val cls = e.caster ?: return null
        val lvl = e.spellLevelFor(cls, spell, db.subclasses) ?: return null
        // známé kouzlo se podruhé nepřidává – nic se nemění
        if (cls.distinctSpells && spell.id in e.spells) return null
        // kouzlo patrona (mimo spell list) má úroveň ověřenou už v spellLevelFor
        if (cls.spellLevelOf(spell) != null && !cls.canCast(spell, e.level)) return StrictBlock.SpellNotAvailable
        val slots = e.levelSlots(cls, c.scores[cls.ability], lvl)
        if (lvl == 0 && slots.limit == 0) return StrictBlock.SpellNotAvailable
        if (!slots.abilityOk) return StrictBlock.AbilityTooLow(lvl)
        val base = slots.limit ?: return null
        // specialista (wizard) má na každém levelu od 1. slot navíc pro kouzlo své školy
        val schoolSlot = if (lvl >= 1 && e.hasSubclassSlot() && e.subclassKind == SubclassType.SCHOOL) 1 else 0
        val limit = base + schoolSlot
        val chosen = e.spells.entries.sumOf { (id, n) ->
            val sp = db.get(id)
            if (sp != null && e.spellLevelFor(cls, sp, db.subclasses) == lvl) (if (cls.distinctSpells) 1 else n) else 0
        }
        return if (chosen >= limit) StrictBlock.SpellLevelFull(lvl, limit) else null
    }

    /** Další feat: volný slot a žádná nesplněná prerekvizita ([feat] null = vlastní feat – jen počet). */
    fun featBlock(c: Character, feat: Feat?, featDb: FeatDatabase?, spells: SpellDatabase?): StrictBlock? {
        val total = c.featSlots().total
        if (c.feats.size >= total) return StrictBlock.FeatSlotsFull(total)
        if (feat != null && feat.prereq.isNotBlank() &&
            FeatPrereqs.overall(FeatPrereqs.evaluate(c, featDb, feat.prereq, spells)) == PrereqStatus.UNMET
        ) return StrictBlock.FeatPrereqsUnmet
        return null
    }

    /** Ještě jeden hex / revelation u classy [classId]. */
    fun classAbilityBlock(c: Character, classId: String, a: ClassAbility): StrictBlock? {
        val e = c.classEntry(classId) ?: return null
        if (e.level < a.minLevel) return StrictBlock.ClassAbilityLevel(a.minLevel)
        val total = AbilityRules.slots(c, e)
        if (e.abilities.size >= total) return StrictBlock.ClassAbilitySlotsFull(total)
        return null
    }
}
