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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cz.pfspells.AppState
import cz.pfspells.Strings
import cz.pfspells.TraitDbState
import cz.pfspells.data.Character
import cz.pfspells.data.RacialTrait
import cz.pfspells.data.RacialTraits
import cz.pfspells.data.raceInfo
import cz.pfspells.str

/** Odkaz pod rasou v panelu postavy: „Alternativní rasové traity (2)“ + varování při konfliktu. */
@Composable
fun RacialTraitsLink(c: Character, onOpen: () -> Unit) {
    val s = str
    if (c.raceInfo == null || RacialTraits.defaults(c.race) == null) return
    Column(Modifier.padding(bottom = 10.dp)) {
        NbDashedButton(s.racialTraitsButton(c.racialTraits.size), onOpen)
        if (c.racialTraits.isNotEmpty()) {
            Text(c.racialTraits.joinToString(", ") { it.name }, fontSize = 12.5.sp, color = Nb.rust, modifier = Modifier.padding(top = 4.dp))
        }
        val conflicts = RacialTraits.conflicts(c.racialTraits)
        if (conflicts.isNotEmpty()) TraitWarning(s.traitConflict(conflicts.joinToString(", ")))
        RacialTraits.missingChoices(c).forEach { TraitWarning(s.missingAbilityChoice(it.name)) }
    }
}

/** Změna atributů alternativním traitem: „+2 INT, −2 WIS“ + výběr atributu pro každou volbu. */
@Composable
private fun AbilityVariantEditor(state: AppState, c: Character, p: cz.pfspells.data.RacialTraitPick) {
    val s = str
    val v = RacialTraits.abilityVariant(p.id) ?: return
    if (v.fixed.isNotEmpty()) {
        Text(
            v.fixed.entries.joinToString(", ") { (a, n) -> (if (n < 0) "−${-n}" else "+$n") + " " + s.abilityShort(a) },
            fontSize = 12.5.sp, color = Nb.rust,
        )
    }
    v.choices.forEachIndexed { i, ch ->
        val chosen = p.abilities.getOrNull(i)
        SimpleDropdown(
            label = s.abilityChoiceLabel(ch.amount),
            options = listOf<cz.pfspells.data.Ability?>(null) + ch.options,
            selected = chosen,
            optionLabel = { it?.let(s::abilityLong) ?: s.chooseOption },
            onSelect = { state.setRacialTraitAbility(c.id, p.id, i, it) },
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun TraitWarning(text: String) {
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(NbIcons.Warn, null, tint = Nb.error, modifier = Modifier.padding(end = 6.dp).size(15.dp))
        Text(text, fontSize = 13.sp, color = Nb.error)
    }
}

/**
 * Rasové traity postavy: výchozí (nahrazené přeškrtnuté), vybrané alternativní (×) a seznam alternativních traitů
 * rasy z Archives of Nethys – hledání, klepnutím text, + přidat.
 */
@Composable
fun RacialTraitsDialog(state: AppState, c: Character, onDismiss: () -> Unit) {
    val s = str
    val race = c.raceInfo ?: return
    LaunchedEffect(Unit) { state.ensureRacialTraits() }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val db = state.traitDb
    val replaced = RacialTraits.replaced(c)
    val list = remember(db, query, c.racialTraits, race.id) {
        val q = query.trim().lowercase()
        val chosen = c.racialTraits.map { it.id }.toSet()
        db?.forRace(race.id).orEmpty()
            .filter { it.id !in chosen }
            .filter { q.isEmpty() || it.searchName.contains(q) || it.text.lowercase().contains(q) || it.replaces.any { r -> r.contains(q) } }
            .sortedWith(compareBy({ it.replaces.any { r -> r in replaced } }, { it.searchName }))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            val width = minOf(620.dp, maxWidth - 32.dp)
            val maxH = minOf(720.dp, maxHeight - 48.dp)
            Surface(Modifier.width(width).heightIn(max = maxH), shape = MaterialTheme.shapes.extraLarge, color = Nb.panel, shadowElevation = 8.dp) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)) {
                    Text(s.racialTraitsTitle(s.raceNameOf(race)), style = MaterialTheme.typography.headlineSmall, color = Nb.ink, modifier = Modifier.padding(bottom = 8.dp))
                    // výchozí traity – nahrazené přeškrtnuté
                    MonoLabel(s.defaultTraitsLabel)
                    Text(
                        buildAnnotatedString {
                            RacialTraits.defaults(race.id).orEmpty().forEachIndexed { i, n ->
                                if (i > 0) append(", ")
                                if (n.lowercase() in replaced) withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Nb.inkMuted)) { append(n) }
                                else append(n)
                            }
                        },
                        fontSize = 13.5.sp, color = Nb.ink, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                    if (c.racialTraits.isNotEmpty()) {
                        MonoLabel(s.chosenTraitsLabel)
                        c.racialTraits.forEach { p ->
                            Row(Modifier.fillMaxWidth().bottomRule(Nb.rule).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink)
                                    if (p.replaces.isNotEmpty()) Text(s.replacesTraits(p.replaces.joinToString(", ")), fontSize = 12.sp, color = Nb.inkMuted)
                                    AbilityVariantEditor(state, c, p)
                                }
                                NbIconButton(NbIcons.Close, s.removeAbility, { state.removeRacialTrait(c.id, p.id) }, Nb.inkMuted, 16.dp)
                            }
                        }
                        val conflicts = RacialTraits.conflicts(c.racialTraits)
                        if (conflicts.isNotEmpty()) TraitWarning(s.traitConflict(conflicts.joinToString(", ")))
                    }
                    Text(s.racialTraitsHint, fontSize = 12.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                    NbSearchField(query, { query = it }, s.searchAbilities, Modifier.fillMaxWidth())
                    when (val st = state.traitDbState) {
                        is TraitDbState.Failed -> Text(st.message, color = Nb.error, modifier = Modifier.padding(vertical = 12.dp))
                        !is TraitDbState.Ready -> Text(s.loadingAbilities, color = Nb.inkMuted, modifier = Modifier.padding(vertical = 12.dp))
                        else -> if (db?.forRace(race.id).isNullOrEmpty()) {
                            Text(s.noAlternateTraits, color = Nb.inkMuted, modifier = Modifier.padding(vertical = 12.dp))
                        } else LazyColumn(
                            Modifier.weight(1f, fill = false).padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(bottom = 4.dp),
                        ) {
                            items(list, key = { it.id }) { t ->
                                TraitCard(s, t, t.replaces.any { it in replaced }, expanded == t.id, onToggle = { expanded = if (expanded == t.id) null else t.id }) {
                                    state.addRacialTrait(c.id, t)
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
private fun TraitCard(s: Strings, t: RacialTrait, conflict: Boolean, expanded: Boolean, onToggle: () -> Unit, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(Nb.shape).background(Nb.paper).dashedBorder()
            .clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (conflict) Nb.inkMuted else Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        t.replaces.takeIf { it.isNotEmpty() }?.let { s.replacesTraits(it.joinToString(", ")) },
                        "PFS".takeIf { t.pfs },
                    ).joinToString(" · "),
                    fontSize = 12.sp, color = if (conflict) Nb.error else Nb.rust, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            NbIconButton(NbIcons.Plus, s.add, onAdd, Nb.accent, 18.dp)
        }
        if (expanded) {
            Text(t.text, fontSize = 13.5.sp, color = Nb.ink, modifier = Modifier.padding(top = 6.dp))
            if (t.source.isNotBlank()) MonoLabel(t.source, Modifier.padding(top = 4.dp))
        }
    }
}
