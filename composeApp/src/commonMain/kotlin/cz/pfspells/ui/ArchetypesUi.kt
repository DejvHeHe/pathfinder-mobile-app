package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cz.pfspells.AppState
import cz.pfspells.ArchetypeDbState
import cz.pfspells.Strings
import cz.pfspells.data.Archetype
import cz.pfspells.data.Archetypes
import cz.pfspells.data.ArchetypePick
import cz.pfspells.data.Character
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.FeaturePresets
import cz.pfspells.data.archetypeEffect
import cz.pfspells.data.caster
import cz.pfspells.data.subclassType
import cz.pfspells.str

/** Odkaz pod classou v panelu postavy: „Archetypy (1)“, vybrané archetypy a varování při konfliktu. */
@Composable
fun ArchetypesLink(e: ClassEntry, onOpen: () -> Unit) {
    val s = str
    if (!Archetypes.supports(e.classId)) return
    Column(Modifier.padding(top = 4.dp)) {
        NbDashedButton(s.archetypesButton(e.archetypes.size), onOpen)
        if (e.archetypes.isNotEmpty()) {
            Text(e.archetypes.joinToString(", ") { it.name }, fontSize = 12.5.sp, color = Nb.rust, modifier = Modifier.padding(top = 4.dp))
        }
        archetypeEffectText(s, e)?.let { Text(it, fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 2.dp)) }
        val conflicts = Archetypes.conflicts(e.archetypes)
        if (conflicts.isNotEmpty()) TraitWarning(s.archetypeConflict(conflicts.joinToString(", ")))
    }
}

/** Co archetypy u classy mění: „Ruší: Smite Evil · bez sesílání · −1 hexů / zjevení · Získává: Hunter's Tricks“. */
fun archetypeEffectText(s: Strings, e: ClassEntry): String? {
    val fx = e.archetypeEffect
    if (fx.isEmpty) return null
    val removed = FeaturePresets.all.filter { it.classId == e.classId && it.name.lowercase() in fx.removedPresets }.map { it.name }
        .ifEmpty { fx.removedPresets.filter { it == "phrenic pool" }.map { "Phrenic Pool" } }
    // schopnost, kterou archetyp nahradí upravenou verzí stejného jména (Sacred Servant: Smite Evil) = „Mění“
    val changed = removed.filter { r -> fx.presets.any { it.name.equals(r, ignoreCase = true) } }
    val gained = buildList {
        fx.subclass?.let { add(s.subclassLabel(it)) }
        addAll(fx.presets.map { it.name }.filterNot { n -> changed.any { it.equals(n, ignoreCase = true) } })
    }
    val parts = listOfNotNull(
        (removed - changed.toSet()).takeIf { it.isNotEmpty() }?.let { s.fxRemoves(it.joinToString(", ")) },
        changed.takeIf { it.isNotEmpty() }?.let { s.fxChanges(it.joinToString(", ")) },
        s.fxNoSpells.takeIf { fx.noSpells && CasterClasses.get(e.classId) != null },
        s.fxNoCantrips.takeIf { fx.noCantrips },
        s.fxNoSubclass.takeIf { fx.noSubclass && subclassType(e.classId) != null && fx.subclass == null },
        s.fxNoSubclassSpells.takeIf { fx.noSubclassSpells && !fx.noSubclass },
        s.fxNoSubclassPowers.takeIf { fx.noSubclassPowers && !fx.noSubclass },
        s.fxOneDomain.takeIf { fx.oneDomain && !fx.noSubclass },
        fx.lostAbilitySlots.takeIf { it > 0 }?.let(s::fxLostAbilities),
        fx.lostBonusFeats.takeIf { it > 0 }?.let(s::fxLostFeats),
        fx.gainedBonusFeats.takeIf { it > 0 }?.let(s::fxGainedFeats),
        fx.perDayDelta.takeIf { it != 0 && !fx.noSpells }?.let(s::fxPerDay),
        fx.knownDelta.takeIf { it != 0 && !fx.noSpells }?.let(s::fxKnown),
        castingChangeText(s, e)?.let(s::fxSpellList),
        fx.addedSpells.size.takeIf { it > 0 && !fx.noSpells }?.let(s::fxAddedSpells),
        fx.subclassLevelOffset.takeIf { it != 0 }?.let(s::fxSubclassLevel),
        gained.takeIf { it.isNotEmpty() }?.let { s.fxGains(it.joinToString(", ")) },
    )
    return parts.joinToString(" · ").ifEmpty { null }
}

/** Změněné sesílání: „Bard · spontánní · CHA“ (jen to, co archetyp mění). */
private fun castingChangeText(s: Strings, e: ClassEntry): String? {
    val ch = e.archetypeEffect.casting ?: return null
    val cls = e.caster ?: return null
    val parts = listOfNotNull(
        cls.listKeys.joinToString(" + ") { s.spellListName(it) }.takeIf { ch.lists != null || ch.addLists.isNotEmpty() },
        s.castingKind(cls.kind).takeIf { ch.kind != null },
        s.abilityShort(cls.ability).takeIf { ch.ability != null },
        (if (ch.abilityBonus > 0) "${s.abilityShort(cls.ability)} +${ch.abilityBonus}" else null),
        ch.table?.let { s.fxAsClass(s.classNameOf(it)) },
        ch.tradition?.let { s.magicTradition(it) },
    )
    return parts.joinToString(" · ").ifEmpty { null }
}

/**
 * Archetypy jedné classy postavy: vybrané (×, klepnutím text) a seznam archetypů classy z Archives of Nethys –
 * hledání v názvu, nahrazených schopnostech i textu, klepnutím text, + přidat. Mechanika v `ArchetypeEffects.kt`.
 */
@Composable
fun ArchetypesDialog(state: AppState, c: Character, classId: String, onDismiss: () -> Unit) {
    val s = str
    val e = c.classEntry(classId) ?: return
    LaunchedEffect(Unit) { state.ensureArchetypes() }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val db = state.archetypeDb
    val taken = remember(e.archetypes) { e.archetypes.flatMap { p -> p.replaces.map { it.lowercase() } }.toSet() }
    val list = remember(db, query, e.archetypes, classId) {
        val q = query.trim().lowercase()
        val chosen = e.archetypes.map { it.id }.toSet()
        db?.forClass(classId).orEmpty()
            .filter { it.id !in chosen }
            .filter {
                q.isEmpty() || it.searchName.contains(q) || it.summary.lowercase().contains(q) ||
                    it.replaces.any { r -> r.lowercase().contains(q) } || it.text.lowercase().contains(q)
            }
            // nejdřív ty, které nenahrazují nic, co už nahrazuje vybraný archetyp
            .sortedWith(compareBy({ it.replaces.any { r -> r.lowercase() in taken } }, { it.searchName }))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            val width = minOf(620.dp, maxWidth - 32.dp)
            val maxH = minOf(760.dp, maxHeight - 48.dp)
            Surface(Modifier.width(width).heightIn(max = maxH), shape = MaterialTheme.shapes.extraLarge, color = Nb.panel, shadowElevation = 8.dp) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)) {
                    Text(s.archetypesTitle(s.classNameOf(classId)), style = MaterialTheme.typography.headlineSmall, color = Nb.ink, modifier = Modifier.padding(bottom = 8.dp))
                    if (e.archetypes.isNotEmpty()) {
                        MonoLabel(s.chosenArchetypesLabel)
                        e.archetypes.forEach { p -> ChosenArchetypeRow(s, p, db?.get(p.id), expanded == p.id, { expanded = if (expanded == p.id) null else p.id }) {
                            state.removeArchetype(c.id, classId, p.id)
                        } }
                        val conflicts = Archetypes.conflicts(e.archetypes)
                        if (conflicts.isNotEmpty()) TraitWarning(s.archetypeConflict(conflicts.joinToString(", ")))
                    }
                    Text(s.archetypesHint, fontSize = 12.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                    NbSearchField(query, { query = it }, s.searchAbilities, Modifier.fillMaxWidth())
                    when (val st = state.archetypeDbState) {
                        is ArchetypeDbState.Failed -> Text(st.message, color = Nb.error, modifier = Modifier.padding(vertical = 12.dp))
                        !is ArchetypeDbState.Ready -> Text(s.loadingAbilities, color = Nb.inkMuted, modifier = Modifier.padding(vertical = 12.dp))
                        else -> if (db?.forClass(classId).isNullOrEmpty()) {
                            Text(s.noArchetypes, color = Nb.inkMuted, modifier = Modifier.padding(vertical = 12.dp))
                        } else LazyColumn(
                            Modifier.weight(1f, fill = false).padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 4.dp),
                        ) {
                            items(list, key = { it.id }) { a ->
                                ArchetypeCard(s, a, a.replaces.any { it.lowercase() in taken }, expanded == a.id, onToggle = { expanded = if (expanded == a.id) null else a.id }) {
                                    state.addArchetype(c.id, classId, a)
                                    expanded = null
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text(s.close, color = Nb.ink) }
                    }
                }
            }
        }
    }
}

/** Vybraný archetyp: jméno, nahrazené schopnosti, ×; klepnutím plný text (když je databáze načtená). */
@Composable
private fun ChosenArchetypeRow(s: Strings, p: ArchetypePick, a: Archetype?, expanded: Boolean, onToggle: () -> Unit, onRemove: () -> Unit) {
    Column(Modifier.fillMaxWidth().bottomRule(Nb.rule).clickable(enabled = a != null, onClick = onToggle).padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink)
                if (p.replaces.isNotEmpty()) Text(s.replacesTraits(p.replaces.joinToString(", ")), fontSize = 12.sp, color = Nb.inkMuted)
            }
            NbIconButton(NbIcons.Close, s.removeAbility, onRemove, Nb.inkMuted, 16.dp)
        }
        if (expanded && a != null) ArchetypeText(a)
    }
}

@Composable
private fun ArchetypeCard(s: Strings, a: Archetype, conflict: Boolean, expanded: Boolean, onToggle: () -> Unit, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(Nb.shape).background(Nb.paper).dashedBorder()
            .clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (conflict) Nb.inkMuted else Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        a.replaces.takeIf { it.isNotEmpty() }?.let { s.replacesTraits(it.joinToString(", ")) },
                        "PFS".takeIf { a.pfs },
                    ).joinToString(" · "),
                    fontSize = 12.sp, color = if (conflict) Nb.error else Nb.rust,
                    maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis,
                )
                if (!expanded && a.summary.isNotBlank()) {
                    Text(a.summary, fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            NbIconButton(NbIcons.Plus, s.add, onAdd, Nb.accent, 18.dp)
        }
        if (expanded) ArchetypeText(a)
    }
}

@Composable
private fun ArchetypeText(a: Archetype) {
    Text(a.text.ifBlank { a.summary }, fontSize = 13.5.sp, color = Nb.ink, modifier = Modifier.padding(top = 6.dp))
    if (a.source.isNotBlank()) MonoLabel(a.source, Modifier.padding(top = 4.dp))
}
