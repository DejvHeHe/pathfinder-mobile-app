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
import cz.pfspells.data.CasterClass
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.MAGIC_SCHOOLS
import cz.pfspells.data.Spell
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SubclassKind
import cz.pfspells.data.SubclassSlot
import cz.pfspells.data.SubclassType
import cz.pfspells.data.UNIVERSALIST
import cz.pfspells.data.bonusSpellClassLevel
import cz.pfspells.data.bonusSpellsByLevel
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
    val type = subclassType(e.classId) ?: return null
    val sub = e.subclass
    fun dom(d: String, sd: String) = sub[d]?.let { cap(it) + (sub[sd]?.let { x -> " (${cap(x)})" } ?: "") }
    val text = when (type) {
        SubclassType.TWO_DOMAINS -> listOfNotNull(dom(SubclassSlot.DOMAIN1, SubclassSlot.SUBDOMAIN1), dom(SubclassSlot.DOMAIN2, SubclassSlot.SUBDOMAIN2)).joinToString(", ")
        SubclassType.ONE_DOMAIN -> dom(SubclassSlot.DOMAIN1, SubclassSlot.SUBDOMAIN1) ?: ""
        SubclassType.MYSTERY -> sub[SubclassSlot.MYSTERY]?.let(::cap) ?: ""
        SubclassType.BLOODLINE -> sub[SubclassSlot.BLOODLINE]?.let(::cap) ?: ""
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
    val type = subclassType(e.classId) ?: return
    val idx = db.subclasses
    val set: (String, String?) -> Unit = { key, v -> state.setSubclass(c.id, e.classId, key, v) }
    val sub = e.subclass

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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(s.subclassTitle(s.classNameOf(e.classId)), color = Nb.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (type) {
                    SubclassType.TWO_DOMAINS, SubclassType.ONE_DOMAIN -> {
                        val domains = idx.names(SubclassKind.DOMAIN)
                        val subdomains = idx.names(SubclassKind.SUBDOMAIN)
                        pick(s.domainN(1), SubclassSlot.DOMAIN1, domains)
                        if (sub[SubclassSlot.DOMAIN1] != null) pick(s.subdomain, SubclassSlot.SUBDOMAIN1, subdomains)
                        if (type == SubclassType.TWO_DOMAINS) {
                            pick(s.domainN(2), SubclassSlot.DOMAIN2, domains.filter { it != sub[SubclassSlot.DOMAIN1] })
                            if (sub[SubclassSlot.DOMAIN2] != null) pick(s.subdomain, SubclassSlot.SUBDOMAIN2, subdomains)
                        }
                        Text(s.subdomainHint, fontSize = 12.5.sp, color = Nb.inkMuted)
                    }
                    SubclassType.MYSTERY -> pick(s.subclassLabel(type), SubclassSlot.MYSTERY, idx.names(SubclassKind.MYSTERY))
                    SubclassType.BLOODLINE -> pick(s.subclassLabel(type), SubclassSlot.BLOODLINE, idx.names(SubclassKind.BLOODLINE))
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
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.close, color = Nb.ink) } },
    )
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
    val type = subclassType(e.classId) ?: return
    if (type == SubclassType.SCHOOL) return
    val game = state.gameMode
    val bonus = e.bonusSpellsByLevel(db.subclasses)
    val domainType = type == SubclassType.TWO_DOMAINS || type == SubclassType.ONE_DOMAIN
    val nothingChosen = e.subclass.isEmpty()

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
            if (!domainType && !nothingChosen) Text(s.knownAutomatically, fontSize = 12.5.sp, color = Nb.inkMuted)
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
                    game -> {
                        // mystérium / bloodline: sesílá se ze slotu spell levelu
                        val key = Character.slotKey(e.classId, lvl)
                        val slots = cls.levelSlots(e.level, c.abilities[cls.ability], lvl)
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
