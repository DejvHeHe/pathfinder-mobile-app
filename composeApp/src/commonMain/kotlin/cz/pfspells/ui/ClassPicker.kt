package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.Strings
import cz.pfspells.data.ClassCatalog
import cz.pfspells.data.ClassGroup
import cz.pfspells.data.ClassInfo
import cz.pfspells.data.group
import cz.pfspells.str

/**
 * Výběr classy: hledání, karty seskupené podle počtu úrovní kouzel, úroveň a náhled spell levelů.
 * [withName] = pole pro jméno postavy (nová postava). [exclude] = classy, které postava už má.
 * [maxLevel] = nejvyšší možná úroveň přidávané classy (celkem max 20).
 */
@Composable
fun ClassPickerDialog(
    title: String,
    confirmLabel: String,
    withName: Boolean,
    exclude: Set<String> = emptySet(),
    maxLevel: Int = 20,
    onDismiss: () -> Unit,
    onPick: (name: String, classId: String, level: Int) -> Unit,
) {
    val s = str
    val fonts = LocalNbFonts.current
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ClassInfo?>(null) }
    var level by remember { mutableIntStateOf(1) }
    val rows: List<Any> = remember(query, exclude, s) { classRows(s, query, exclude) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title, color = Nb.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (withName) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text(s.characterNameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                NbSearchField(query, { query = it }, s.searchClasses, Modifier.fillMaxWidth())
                // obyčejný scroll místo LazyColumn – v AlertDialogu nejsou podporovaná intrinsic měření
                Column(
                    Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    rows.forEach { row ->
                        when (row) {
                            is ClassGroup -> SectionHeader(s.classGroup(row), size = 15.sp)
                            is ClassInfo -> ClassCard(s, row, row.id == selected?.id) { selected = row }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel(s.levelLabel, Modifier.weight(1f))
                    CountStepper(level.coerceAtMost(maxLevel), { level = it }, min = 1, max = maxLevel, size = 32.dp, numberSize = 17.sp)
                }
                val sel = selected
                Text(
                    when {
                        sel == null -> s.chooseClass
                        sel.caster != null -> "${s.classNameOf(sel.id)} ${level.coerceAtMost(maxLevel)} · ${spellRangeText(s, sel.caster!!, level.coerceAtMost(maxLevel))}"
                        else -> "${s.classNameOf(sel.id)} ${level.coerceAtMost(maxLevel)} · ${s.classGroup(sel.group).lowercase()}"
                    },
                    fontSize = 13.sp, color = if (sel == null) Nb.inkMuted else Nb.rust, fontFamily = fonts.sans,
                )
            }
        },
        confirmButton = {
            NbPrimaryButton(
                confirmLabel,
                {
                    selected?.let { cls ->
                        val lvl = level.coerceAtMost(maxLevel)
                        onPick(name.trim().ifEmpty { "${s.classNameOf(cls.id)} $lvl" }, cls.id, lvl)
                    }
                },
                icon = null, enabled = selected != null,
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}

/** Řádky seznamu: nadpis skupiny ([ClassGroup]) a pod ním classy seřazené podle názvu v jazyce rozhraní. */
private fun classRows(s: Strings, query: String, exclude: Set<String>): List<Any> {
    val q = query.trim().lowercase()
    val matching = ClassCatalog.all.filter { c ->
        c.id !in exclude && (q.isEmpty() || s.classNameOf(c.id).lowercase().contains(q) || c.name.lowercase().contains(q))
    }
    return ClassGroup.entries.flatMap { g ->
        val list = matching.filter { it.group == g }.sortedBy { s.classNameOf(it.id).lowercase() }
        if (list.isEmpty()) emptyList() else listOf<Any>(g) + list
    }
}

@Composable
private fun ClassCard(s: Strings, c: ClassInfo, selected: Boolean, onClick: () -> Unit) {
    val base = Modifier.fillMaxWidth().clip(Nb.shape)
    Row(
        (if (selected) base.background(Nb.accent) else base.background(Nb.paper).dashedBorder())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val local = s.classNameOf(c.id)
        Text(
            local, Modifier.weight(1f),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else Nb.ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        val detail = buildString {
            if (local != c.name) append(c.name).append(" · ")
            append("d").append(c.hd)
        }
        Text(detail, fontSize = 12.sp, color = if (selected) Color.White else Nb.inkMuted, maxLines = 1)
    }
}
