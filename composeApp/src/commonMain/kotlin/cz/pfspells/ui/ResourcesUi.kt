package cz.pfspells.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.data.Character
import cz.pfspells.data.Resource
import cz.pfspells.data.ResourceUnit
import cz.pfspells.data.Tracker
import cz.pfspells.data.resources
import cz.pfspells.str

/**
 * Schopnosti s počtem použití: předvyplněné podle class (Smite Evil, Rage, Ki Pool…, maximum podle úrovně a atributu)
 * + vlastní počítadla. V herním režimu se odklikává použití, Odpočinek je obnoví.
 */
@Composable
fun ResourcesPanel(state: AppState, c: Character, wide: Boolean) {
    val s = str
    val game = state.gameMode
    val list = c.resources()
    val hidden = c.resources(includeHidden = true).count { it.preset != null && it.preset.id in c.hiddenPresets }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Tracker?>(null) }

    Column(Modifier.padding(top = 4.dp)) {
        SectionHeader(s.resourcesHeader)
        if (list.isEmpty()) Text(s.noResources, fontSize = 13.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(vertical = 4.dp))
        list.forEach { r -> ResourceRow(state, c, r, game, wide) { r.tracker?.let { editing = it } } }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NbDashedButton(s.addTracker, { adding = true }, icon = NbIcons.Plus)
            if (hidden > 0) {
                Text(
                    s.showHidden(hidden),
                    Modifier.clickable { c.hiddenPresets.forEach { state.setPresetHidden(c.id, it, false) } }.padding(8.dp),
                    fontSize = 13.sp, color = Nb.accent,
                )
            }
        }
    }
    if (adding) {
        TrackerDialog(s.addTracker, null, onDismiss = { adding = false }) { t ->
            adding = false
            state.addTracker(c.id, t.name, t.max, t.unit, t.note)
        }
    }
    editing?.let { t ->
        TrackerDialog(s.editTracker, t, onDismiss = { editing = null }, onDelete = { editing = null; state.removeTracker(c.id, t.id) }) { nt ->
            editing = null
            state.updateTracker(c.id, nt)
        }
    }
}

@Composable
private fun ResourceRow(state: AppState, c: Character, r: Resource, game: Boolean, wide: Boolean, onEdit: () -> Unit) {
    val s = str
    val used = c.cast(r.key)
    val max = r.max
    Row(
        Modifier.fillMaxWidth().bottomRule(if (wide) Nb.ruleSoft else Nb.rule)
            .then(if (r.tracker != null && !game) Modifier.clickable(onClick = onEdit) else Modifier)
            .padding(horizontal = if (wide) 10.dp else 0.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                r.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold,
                color = if (game && max != null && used >= max) Nb.inkMuted else Nb.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    r.classId?.let { append(s.classNameOf(it)).append(" · ") }
                    if (game && max != null) append(s.remaining((max - used).coerceAtLeast(0), max))
                    else append(s.resourceAmount(max, r.unit))
                    r.tracker?.note?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                },
                fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            game && max != null && max <= 8 -> CastPips(max, used) { state.setCast(c.id, r.key, it) }
            game && max != null -> CountStepper(used.coerceAtMost(max), { state.setCast(c.id, r.key, it) }, min = 0, max = max, size = 32.dp, numberSize = 14.sp)
            game -> {}
            r.preset != null -> NbIconButton(NbIcons.Close, s.hideFeature, { state.setPresetHidden(c.id, r.preset.id, true) }, Nb.inkMuted, 16.dp)
            else -> NbIconButton(NbIcons.Pencil, s.editTracker, onEdit, Nb.inkMuted, 16.dp)
        }
    }
}

@Composable
private fun TrackerDialog(
    title: String,
    initial: Tracker?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (Tracker) -> Unit,
) {
    val s = str
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var max by remember { mutableIntStateOf(initial?.max ?: 3) }
    var unit by remember { mutableStateOf(initial?.unit ?: ResourceUnit.PER_DAY) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title, color = Nb.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(s.trackerName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel(s.trackerMax, Modifier.weight(1f))
                    CountStepper(max, { max = it }, min = 1, max = 999, size = 32.dp, numberSize = 16.sp)
                }
                SimpleDropdown(s.trackerUnit, ResourceUnit.entries, unit, { s.unitName(it) }, { unit = it })
                OutlinedTextField(note, { note = it }, label = { Text(s.trackerNote) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(s.removeTracker, color = Nb.error) }
                }
            }
        },
        confirmButton = {
            NbPrimaryButton(s.save, {
                if (name.isNotBlank()) onSave(Tracker(initial?.id ?: "", name.trim(), max, unit, note.trim()))
            }, icon = null, enabled = name.isNotBlank())
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}
