package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.Strings
import cz.pfspells.data.subclassKind
import cz.pfspells.data.subclassLevel
import cz.pfspells.data.archetypeEffect
import cz.pfspells.data.CasterClass
import cz.pfspells.data.Character
import cz.pfspells.data.DeityInfo
import cz.pfspells.data.deityConflicts
import cz.pfspells.data.deityInfo
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.MAGIC_SCHOOLS
import cz.pfspells.data.Spell
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SubclassIndex
import cz.pfspells.data.SubclassKind
import cz.pfspells.data.SubclassSlot
import cz.pfspells.data.SubclassType
import cz.pfspells.data.UNIVERSALIST
import cz.pfspells.data.BLESSINGS
import cz.pfspells.data.DEITY_DOMAIN_CLASSES
import cz.pfspells.data.SubclassPowers
import cz.pfspells.data.blessingAllowed
import cz.pfspells.data.INQUISITIONS
import cz.pfspells.data.inquisitionAllowed
import cz.pfspells.data.hasBonusSpells
import cz.pfspells.data.singleSlotOf
import cz.pfspells.data.tableNames
import cz.pfspells.data.bonusSpellClassLevel
import cz.pfspells.data.bonusSpellsByLevel
import cz.pfspells.data.scores
import cz.pfspells.data.domainsFor
import cz.pfspells.data.subdomainsOf
import cz.pfspells.data.levelSlots
import cz.pfspells.data.subclassType
import cz.pfspells.str

private fun cap(x: String) = x.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

/** „Evocation“, „Universalist“, „Elemental – fire“ */
fun schoolOptionName(s: Strings, v: String): String = when {
    v == UNIVERSALIST -> s.universalist
    v.startsWith("element:") -> s.elementalSchool(s.element(v.substringAfter(':')))
    else -> s.school(v)
}

/** Souhrn podtřídy pod classou: „Domény: Fire, Magic (Arcane)“, „Bloodline: vybrat…“; null = classa podtřídu nemá. */
fun subclassSummary(s: Strings, e: ClassEntry): String? {
    val type = e.subclassKind ?: return null
    val sub = e.subclass
    fun dom(d: String, sd: String) = sub[d]?.let { cap(it) + (sub[sd]?.let { x -> " (${cap(x)})" } ?: "") }
    val text = when (type) {
        SubclassType.TWO_DOMAINS -> listOfNotNull(dom(SubclassSlot.DOMAIN1, SubclassSlot.SUBDOMAIN1), dom(SubclassSlot.DOMAIN2, SubclassSlot.SUBDOMAIN2)).joinToString(", ")
        SubclassType.ONE_DOMAIN -> dom(SubclassSlot.DOMAIN1, SubclassSlot.SUBDOMAIN1) ?: ""
        SubclassType.INQUISITOR_DOMAIN -> sub[SubclassSlot.INQUISITION]?.let { s.inquisitionName(cap(it)) }
            ?: dom(SubclassSlot.DOMAIN1, SubclassSlot.SUBDOMAIN1) ?: ""
        SubclassType.BLOODLINE -> listOfNotNull(
            sub[SubclassSlot.BLOODLINE],
            sub[SubclassSlot.BLOODLINE2]?.takeIf { e.archetypes.isNotEmpty() && e.archetypeEffect.secondBloodline },
        ).joinToString(" / ") { cap(it) }
        SubclassType.MYSTERY, SubclassType.PATRON,
        SubclassType.BLOODRAGER_BLOODLINE, SubclassType.DISCIPLINE -> singleSlotOf(type)?.let { sub[it] }?.let(::cap) ?: ""
        SubclassType.SPIRIT -> listOfNotNull(
            sub[SubclassSlot.SPIRIT]?.let(::cap),
            sub[SubclassSlot.WANDERING_SPIRIT]?.let { "${s.wanderingSpirit.substringBefore(" (")}: ${cap(it)}" },
        ).joinToString(" · ")
        SubclassType.BLESSINGS -> listOfNotNull(sub[SubclassSlot.BLESSING1], sub[SubclassSlot.BLESSING2]).joinToString(", ") { cap(it) }
        SubclassType.SCHOOL -> sub[SubclassSlot.SCHOOL]?.let { school ->
            val opp = listOfNotNull(sub[SubclassSlot.OPPOSITION1], sub[SubclassSlot.OPPOSITION2]).map { schoolOptionName(s, it) }
            schoolOptionName(s, school) + if (opp.isNotEmpty()) " · ${s.oppositionShort}: " + opp.joinToString(", ") else ""
        } ?: ""
    }
    return "${s.subclassLabel(type)}: ${text.ifEmpty { s.chooseSubclass }}"
}

/** Výběr podtřídy classy – změny se ukládají hned. */
@Composable
fun SubclassDialog(state: AppState, db: SpellDatabase, c: Character, e: ClassEntry, onDismiss: () -> Unit) {
    val s = str
    val type = e.subclassKind ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(s.subclassTitle(s.classNameOf(e.classId)), color = Nb.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                c.deityInfo?.let { d ->
                    val bad = e.deityConflicts(d)
                    if (bad.isNotEmpty()) DeityConflictWarning(s, d, bad)
                }
                SubclassFields(
                    e.classId, type, e.subclass, db.subclasses, deity = c.deityInfo,
                    secondBloodline = e.archetypes.isNotEmpty() && e.archetypeEffect.secondBloodline,
                ) { key, v -> state.setSubclass(c.id, e.classId, key, v) }
                SubclassPowersList(s, e)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.close, color = Nb.ink) } },
    )
}

/**
 * Schopnosti zvolených podtříd z Foundry (domény, bloodline, školy, blessingy) s úrovní, od které je classa má;
 * ještě nedostupné jsou tlumené. Počty použití jsou v sekci „Schopnosti“.
 */
@Composable
private fun SubclassPowersList(s: Strings, e: ClassEntry) {
    val powers = SubclassPowers.keysOf(e).flatMap { (key, label) -> SubclassPowers.of(key).orEmpty().map { label to it } }
    val feats = SubclassPowers.bonusFeats(e)
    if (feats.isNotEmpty()) Text(s.bloodlineBonusFeats(feats.joinToString(", ")), fontSize = 12.5.sp, color = Nb.inkMuted)
    if (powers.isEmpty()) return
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        MonoLabel(s.subclassPowersHeader)
        powers.forEach { (label, p) ->
            val has = e.subclassLevel >= p.level
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${p.name} ($label)", Modifier.weight(1f),
                    fontSize = 13.5.sp, color = if (has) Nb.ink else Nb.inkMuted,
                    fontWeight = if (has) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(s.knownFromLevel(p.level), fontFamily = LocalNbFonts.current.mono, fontSize = 11.sp, color = Nb.inkMuted)
            }
        }
    }
}

/**
 * Pole pro výběr podtřídy podle [type] (domény + subdomény, mystérium, bloodline, škola + opoziční školy).
 * Sdílí je [SubclassDialog] (u existující classy) i [ClassPickerDialog] (rovnou při výběru classy).
 */
@Composable
fun SubclassFields(
    classId: String,
    type: SubclassType,
    sub: Map<String, String>,
    idx: SubclassIndex,
    deity: DeityInfo? = null,
    /** Crossblooded: výběr druhé bloodline */
    secondBloodline: Boolean = false,
    set: (key: String, value: String?) -> Unit,
) {
    val s = str

    @Composable
    fun pick(label: String, key: String, options: List<String>, name: (String) -> String = ::cap) {
        SimpleDropdown(
            label = label,
            options = listOf<String?>(null) + options,
            selected = sub[key],
            optionLabel = { it?.let(name) ?: s.noneChosen },
            onSelect = { set(key, it) },
        )
    }

    when (type) {
        SubclassType.TWO_DOMAINS, SubclassType.ONE_DOMAIN, SubclassType.INQUISITOR_DOMAIN -> {
            // klerik a inquisitor volí z domén svého božstva (ze seznamu); bez božstva nebo s vlastním všechny
            val deityDomains = deity?.domains?.takeIf { classId in DEITY_DOMAIN_CLASSES && it.isNotEmpty() }
            val domains = idx.domainsFor(classId).filter { deityDomains == null || it in deityDomains }
            // subdomény jen ty, které božstvo dává („archon (good, law)“ ↔ „archon“)
            val deitySubs = deity?.subdomains?.takeIf { deityDomains != null }
            fun subsOf(d: String) = idx.subdomainsOf(d, classId).filter { sd -> deitySubs == null || sd.substringBefore(" (") in deitySubs }
            val d1 = sub[SubclassSlot.DOMAIN1]
            val d2 = sub[SubclassSlot.DOMAIN2]
            pick(s.domainN(1), SubclassSlot.DOMAIN1, domains.filter { it != d2 })
            // subdomény jen ke zvolené doméně; stejnou subdoménu nejde vzít dvakrát
            d1?.let { d -> subsOf(d).filter { it != sub[SubclassSlot.SUBDOMAIN2] } }
                ?.takeIf { it.isNotEmpty() }?.let { pick(s.subdomain, SubclassSlot.SUBDOMAIN1, it) }
            if (type == SubclassType.TWO_DOMAINS) {
                pick(s.domainN(2), SubclassSlot.DOMAIN2, domains.filter { it != d1 })
                d2?.let { d -> subsOf(d).filter { it != sub[SubclassSlot.SUBDOMAIN1] } }
                    ?.takeIf { it.isNotEmpty() }?.let { pick(s.subdomain, SubclassSlot.SUBDOMAIN2, it) }
            }
            // doména jen se schopnostmi má i paladin / occultist z archetypu (Temple Champion, Reliquarian) – inquisition jen inquisitor
            if (type == SubclassType.INQUISITOR_DOMAIN && classId == "inquisitor") {
                // inquisition místo domény (Ultimate Magic) – jen ty, které božstvo dovoluje
                pick(s.inquisitionLabel, SubclassSlot.INQUISITION, INQUISITIONS.filter { inquisitionAllowed(it, deity) })
            }
            if (deityDomains != null) Text(s.deityDomainsHint(deity.name), fontSize = 12.5.sp, color = Nb.inkMuted)
            if (type == SubclassType.INQUISITOR_DOMAIN && classId == "inquisitor") Text(s.inquisitorDomainHint, fontSize = 12.5.sp, color = Nb.inkMuted)
            Text(if (classId == "druid") s.druidDomainHint else s.subdomainHint, fontSize = 12.5.sp, color = Nb.inkMuted)
        }
        SubclassType.MYSTERY -> pick(s.subclassLabel(type), SubclassSlot.MYSTERY, idx.names(SubclassKind.MYSTERY))
        SubclassType.BLOODLINE -> {
            pick(s.subclassLabel(type), SubclassSlot.BLOODLINE, idx.names(SubclassKind.BLOODLINE))
            if (secondBloodline) {
                pick(s.secondBloodline, SubclassSlot.BLOODLINE2, idx.names(SubclassKind.BLOODLINE))
                Text(s.crossbloodedHint, fontSize = 12.5.sp, color = Nb.inkMuted)
            }
        }
        SubclassType.PATRON, SubclassType.SPIRIT, SubclassType.BLOODRAGER_BLOODLINE, SubclassType.DISCIPLINE -> {
            pick(s.subclassLabel(type), singleSlotOf(type)!!, idx.tableNames(type))
            if (type == SubclassType.SPIRIT) {
                pick(s.wanderingSpirit, SubclassSlot.WANDERING_SPIRIT, idx.tableNames(type).filter { it != sub[SubclassSlot.SPIRIT] })
            }
            val hint = when (type) {
                SubclassType.PATRON -> s.patronHint
                SubclassType.SPIRIT -> s.spiritHint + "\n\n" + s.wanderingSpiritHint
                SubclassType.BLOODRAGER_BLOODLINE -> s.bloodragerBloodlineHint
                else -> s.disciplineHint
            }
            Text(hint, fontSize = 12.5.sp, color = Nb.inkMuted)
        }
        SubclassType.BLESSINGS -> {
            // warpriest: dvě požehnání z domén božstva (ze seznamu); bez božstva nebo s vlastním všechna
            val limited = deity != null && deity.domains.isNotEmpty()
            val options = BLESSINGS.filter { blessingAllowed(it, deity) }
            val b1 = sub[SubclassSlot.BLESSING1]
            val b2 = sub[SubclassSlot.BLESSING2]
            pick(s.blessingN(1), SubclassSlot.BLESSING1, options.filter { it != b2 })
            pick(s.blessingN(2), SubclassSlot.BLESSING2, options.filter { it != b1 })
            if (limited) Text(s.deityBlessingsHint(deity!!.name), fontSize = 12.5.sp, color = Nb.inkMuted)
            Text(s.blessingHint, fontSize = 12.5.sp, color = Nb.inkMuted)
        }
        SubclassType.SCHOOL -> {
            val elements = idx.names(SubclassKind.ELEMENT).map { "element:$it" }
            val school = sub[SubclassSlot.SCHOOL]
            pick(s.subclassLabel(type), SubclassSlot.SCHOOL, listOf(UNIVERSALIST) + MAGIC_SCHOOLS + elements) { schoolOptionName(s, it) }
            when {
                school == null || school == UNIVERSALIST -> {}
                school.startsWith("element:") ->
                    // elementární škola: jeden opoziční element
                    pick(s.oppositionN(1), SubclassSlot.OPPOSITION1, elements.filter { it != school }) { schoolOptionName(s, it) }
                else -> {
                    val others = MAGIC_SCHOOLS.filter { it != school }
                    pick(s.oppositionN(1), SubclassSlot.OPPOSITION1, others.filter { it != sub[SubclassSlot.OPPOSITION2] }) { schoolOptionName(s, it) }
                    pick(s.oppositionN(2), SubclassSlot.OPPOSITION2, others.filter { it != sub[SubclassSlot.OPPOSITION1] }) { schoolOptionName(s, it) }
                }
            }
            Text(s.schoolHint, fontSize = 12.5.sp, color = Nb.inkMuted)
        }
    }
}

/**
 * Bonusová kouzla podtřídy pod kouzly classy: doménová kouzla (cleric, druid – 1 doménový slot na spell level),
 * kouzla mystéria / bloodline (automaticky známá). V herním režimu se dají sesílat / odškrtávat.
 */
fun LazyListScope.subclassBonusSpells(
    state: AppState,
    db: SpellDatabase,
    c: Character,
    e: ClassEntry,
    cls: CasterClass,
    wide: Boolean,
    selectedSpellId: String?,
    onEditSubclass: () -> Unit,
) {
    val type = e.subclassKind ?: return
    if (!type.hasBonusSpells) return
    val game = state.gameMode
    val bonus = e.bonusSpellsByLevel(db.subclasses)
    val domainType = type == SubclassType.TWO_DOMAINS || type == SubclassType.ONE_DOMAIN
    val spirit = type == SubclassType.SPIRIT
    val patron = type == SubclassType.PATRON
    val nothingChosen = e.subclass.isEmpty()
    fun spiritKey(lvl: Int) = Character.castKey(e.classId, "spirit:$lvl")

    item(key = "${e.classId}-bonus-header") {
        val s = str
        Column(Modifier.padding(top = 18.dp)) {
            Text(
                s.bonusSpellsHeader(type),
                fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Nb.ink,
            )
            Text(
                subclassSummary(s, e) ?: "",
                Modifier.clickable(onClick = onEditSubclass).padding(vertical = 2.dp),
                fontSize = 12.5.sp, color = Nb.accent,
            )
            val note = when {
                domainType || nothingChosen -> null
                patron -> s.patronSpellsNote
                spirit -> s.spiritSlot
                else -> s.knownAutomatically
            }
            if (note != null) Text(note, fontSize = 12.5.sp, color = Nb.inkMuted)
            if (patron && !nothingChosen && !game) Text(s.patronPrepareHint, fontSize = 12.5.sp, color = Nb.inkMuted)
            if (!nothingChosen && bonus.isEmpty()) Text(s.subclassNotAvailable, fontSize = 13.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 4.dp))
        }
    }
    bonus.forEach { (lvl, spells) ->
        item(key = "${e.classId}-bonus-h$lvl") {
            val s = str
            val fonts = LocalNbFonts.current
            val text = when {
                domainType && game -> {
                    val prepared = spells.sumOf { e.bonusSpells[it.id] ?: 0 }
                    val used = spells.sumOf { c.cast(Character.castKey(e.classId, "bonus:" + it.id)).coerceAtMost(e.bonusSpells[it.id] ?: 0) }
                    s.castOf(used, prepared)
                }
                domainType -> s.domainSlot(spells.sumOf { e.bonusSpells[it.id] ?: 0 })
                spirit && game -> s.castOf(c.cast(spiritKey(lvl)).coerceAtMost(1), 1)
                else -> s.knownFromLevel(bonusSpellClassLevel(type, lvl) ?: lvl)
            }
            val over = domainType && !game && spells.sumOf { e.bonusSpells[it.id] ?: 0 } > 1
            SectionHeader(s.spellLevelLabel(lvl), size = 16.sp) {
                Text(text, fontFamily = fonts.mono, fontSize = 11.5.sp, color = if (over) Nb.error else Nb.inkMuted)
            }
        }
        items(spells, key = { "${e.classId}-bonus-${it.id}" }) { spell ->
            BonusSpellRow(
                spell = spell,
                selected = spell.id == selectedSpellId,
                wide = wide,
                onClick = { state.showSpell(spell.id, c.id, e.classId) },
            ) {
                when {
                    domainType && game -> {
                        val key = Character.castKey(e.classId, "bonus:" + spell.id)
                        val n = e.bonusSpells[spell.id] ?: 0
                        if (n > 0) CastPips(n, c.cast(key)) { state.setCast(c.id, key, it) }
                    }
                    domainType -> CountStepper(
                        e.bonusSpells[spell.id] ?: 0, { state.setBonusSpellCount(c.id, e.classId, spell.id, it) },
                        max = 9, size = 32.dp, numberSize = 14.sp,
                    )
                    spirit && game -> {
                        // magie ducha: 1 spontánní seslání za den na spell level (vlastní slot)
                        val used = c.cast(spiritKey(lvl))
                        NbDashedButton(str.castSpell, { state.setCast(c.id, spiritKey(lvl), used + 1) }, enabled = used < 1)
                    }
                    // kouzla patrona se připravují do běžných slotů čarodějnice (pak jsou i mezi jejími kouzly)
                    patron && !game -> CountStepper(
                        e.spells[spell.id] ?: 0, { state.setSpellCount(c.id, e.classId, spell.id, it) },
                        max = 9, size = 32.dp, numberSize = 14.sp,
                    )
                    patron -> {}
                    game -> {
                        // mystérium / bloodline / disciplína: sesílá se ze slotu spell levelu
                        val key = Character.slotKey(e.classId, lvl)
                        val slots = e.levelSlots(cls, c.scores[cls.ability], lvl)
                        val total = (slots.perDay ?: 0) + slots.bonus
                        val used = c.cast(key)
                        NbDashedButton(str.castSpell, { state.setCast(c.id, key, used + 1) }, enabled = used < total && slots.abilityOk)
                    }
                }
            }
        }
    }
}

@Composable
private fun BonusSpellRow(spell: Spell, selected: Boolean, wide: Boolean, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    val s = str
    val base = Modifier.fillMaxWidth()
    Row(
        (if (selected) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule(if (wide) Nb.ruleSoft else Nb.rule))
            .clickable(onClick = onClick).padding(horizontal = if (wide) 10.dp else 0.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(spell.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(spellSubline(s, spell), fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}
