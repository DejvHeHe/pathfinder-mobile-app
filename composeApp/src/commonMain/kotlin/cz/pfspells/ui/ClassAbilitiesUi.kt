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
import cz.pfspells.AbilityDbState
import cz.pfspells.AppState
import cz.pfspells.Strings
import cz.pfspells.data.AbilityGroup
import cz.pfspells.data.AbilityPick
import cz.pfspells.data.AbilityRules
import cz.pfspells.data.Character
import cz.pfspells.data.ClassAbility
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.FoundryFormula
import cz.pfspells.data.SubclassSlot
import cz.pfspells.str

/**
 * Hexy (witch, shaman) a zjevení (oracle) u class postavy: vybrané s počtem „x / y“ podle úrovně classy,
 * klepnutím text, × odebrat, výběr ze seznamu (Foundry VTT PF1). Počet použití jde do sekce Schopnosti.
 */
@Composable
fun ClassAbilitiesPanel(state: AppState, c: Character) {
    val entries = c.classes.filter { AbilityRules.groupOf(it.classId) != null }
    if (entries.isEmpty()) return
    LaunchedEffect(Unit) { state.ensureClassAbilities() }
    entries.forEach { e -> ClassAbilitySection(state, c, e, AbilityRules.groupOf(e.classId)!!) }
}

@Composable
private fun ClassAbilitySection(state: AppState, c: Character, e: ClassEntry, group: AbilityGroup) {
    val s = str
    val fonts = LocalNbFonts.current
    val total = AbilityRules.slots(e.classId, e.level)
    var picking by remember { mutableStateOf(false) }
    var showing by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(top = 4.dp)) {
        SectionHeader("${s.abilityGroupName(group)} · ${s.classNameOf(e.classId)}") {
            Text(
                s.featCount(e.abilities.size, total), fontFamily = fonts.mono, fontSize = 12.sp,
                color = if (e.abilities.size > total) Nb.error else Nb.inkMuted,
            )
        }
        if (e.abilities.isEmpty()) Text(s.noClassAbilities(group), fontSize = 13.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(vertical = 4.dp))
        e.abilities.forEach { p ->
            PickRow(s, c, e, p, state.abilityDb?.get(p.id), onOpen = { showing = p.id }) { state.removeClassAbility(c.id, e.classId, p.id) }
        }
        Text(s.abilitiesHint(group), fontSize = 12.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 4.dp))
        NbDashedButton(s.addClassAbility(group), { picking = true }, Modifier.padding(top = 8.dp), icon = NbIcons.Plus)
    }
    if (picking) {
        ClassAbilityPickerDialog(state, c, e, group, onDismiss = { picking = false }) { a -> state.addClassAbility(c.id, e.classId, a) }
    }
    showing?.let { id ->
        val a = state.abilityDb?.get(id)
        if (a == null) showing = null
        else AbilityTextDialog(s, c, e, a, onDismiss = { showing = null })
    }
}

/** „Hex · Su · 5 / den“ */
private fun abilityLine(s: Strings, c: Character, e: ClassEntry, kind: String, type: String?, uses: String?, unit: cz.pfspells.data.ResourceUnit?): String =
    listOfNotNull(
        kind, type,
        uses?.let { f -> FoundryFormula.uses(f, e.level, c)?.let { s.resourceAmount(it, unit ?: cz.pfspells.data.ResourceUnit.PER_DAY) } },
    ).joinToString(" · ")

@Composable
private fun PickRow(s: Strings, c: Character, e: ClassEntry, p: AbilityPick, a: ClassAbility?, onOpen: () -> Unit, onRemove: () -> Unit) {
    val tooHigh = a != null && e.level < a.minLevel
    Row(
        Modifier.fillMaxWidth().bottomRule(Nb.rule).clickable(enabled = a != null, onClick = onOpen).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                abilityLine(s, c, e, s.abilityKind(p.kind), a?.type, p.uses, p.unit) + if (tooHigh) " · " + s.fromClassLevel(a!!.minLevel) else "",
                fontSize = 12.5.sp, color = if (tooHigh) Nb.error else Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        NbIconButton(NbIcons.Close, s.removeAbility, onRemove, Nb.inkMuted, 16.dp)
    }
}

/** Výběr hexu / zjevení: hledání v názvu i textu, zjevení podle mystéria orákula (přepínač „všechna mystéria“). */
@Composable
internal fun ClassAbilityPickerDialog(
    state: AppState,
    c: Character,
    e: ClassEntry,
    group: AbilityGroup,
    onDismiss: () -> Unit,
    onPick: (ClassAbility) -> Unit,
) {
    val s = str
    var query by remember { mutableStateOf("") }
    var allMysteries by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<String?>(null) }
    val mystery = e.subclass[SubclassSlot.MYSTERY]?.takeIf { group == AbilityGroup.REVELATIONS }
    val db = state.abilityDb
    val list = remember(db, query, allMysteries, e) {
        val q = query.trim().lowercase()
        val chosen = e.abilities.map { it.id }.toSet()
        db?.forClass(e.classId).orEmpty()
            .filter { it.id !in chosen }
            .filter { allMysteries || AbilityRules.matchesMystery(it, mystery) }
            .filter { q.isEmpty() || it.searchName.contains(q) || it.text.lowercase().contains(q) }
            .sortedWith(compareBy({ it.minLevel > e.level }, { q.isNotEmpty() && !it.searchName.contains(q) }, { it.kind }, { it.searchName }))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            val width = minOf(600.dp, maxWidth - 32.dp)
            val maxH = minOf(700.dp, maxHeight - 48.dp)
            Surface(Modifier.width(width).heightIn(max = maxH), shape = MaterialTheme.shapes.extraLarge, color = Nb.panel, shadowElevation = 8.dp) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)) {
                    Text(
                        "${s.addClassAbility(group)} · ${s.classNameOf(e.classId)} ${e.level}",
                        style = MaterialTheme.typography.headlineSmall, color = Nb.ink, modifier = Modifier.padding(bottom = 12.dp),
                    )
                    NbSearchField(query, { query = it }, s.searchAbilities, Modifier.fillMaxWidth())
                    if (mystery != null) NbCheck(allMysteries, s.allMysteries) { allMysteries = it }
                    when (val st = state.abilityDbState) {
                        is AbilityDbState.Failed -> Text(st.message, color = Nb.error, modifier = Modifier.padding(vertical = 12.dp))
                        !is AbilityDbState.Ready -> Text(s.loadingAbilities, color = Nb.inkMuted, modifier = Modifier.padding(vertical = 12.dp))
                        else -> LazyColumn(
                            Modifier.weight(1f, fill = false).padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 4.dp),
                        ) {
                            items(list, key = { it.id }) { a ->
                                AbilityCard(s, c, e, a, expanded == a.id, onToggle = { expanded = if (expanded == a.id) null else a.id }) {
                                    onPick(a)
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

@Composable
private fun AbilityCard(s: Strings, c: Character, e: ClassEntry, a: ClassAbility, expanded: Boolean, onToggle: () -> Unit, onAdd: () -> Unit) {
    val locked = e.level < a.minLevel
    Column(
        Modifier.fillMaxWidth().clip(Nb.shape).background(Nb.paper).dashedBorder()
            .clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (locked) Nb.inkMuted else Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        abilityLine(s, c, e, s.abilityKind(a.kind), a.type, a.uses, a.unit),
                        a.minLevel.takeIf { it > 1 }?.let { s.fromClassLevel(it) },
                        a.mysteries.takeIf { it.isNotEmpty() }?.joinToString(", ") { cz.pfspells.data.SubclassPowers.title(it) },
                    ).joinToString(" · "),
                    fontSize = 12.sp, color = if (locked) Nb.error else Nb.rust, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            NbIconButton(NbIcons.Plus, s.add, onAdd, Nb.accent, 18.dp)
        }
        if (expanded) {
            Text(a.text, fontSize = 13.5.sp, color = Nb.ink, modifier = Modifier.padding(top = 6.dp))
            if (a.source.isNotBlank()) MonoLabel(a.source, Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun AbilityTextDialog(s: Strings, c: Character, e: ClassEntry, a: ClassAbility, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(a.name, color = Nb.ink) },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                item {
                    Text(
                        listOfNotNull(abilityLine(s, c, e, s.abilityKind(a.kind), a.type, a.uses, a.unit), a.minLevel.takeIf { it > 1 }?.let { s.fromClassLevel(it) })
                            .joinToString(" · "),
                        fontSize = 12.5.sp, color = Nb.rust,
                    )
                    Text(a.text, fontSize = 14.sp, color = Nb.ink, modifier = Modifier.padding(top = 8.dp))
                    if (a.source.isNotBlank()) MonoLabel(a.source, Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.close, color = Nb.ink) } },
    )
}
