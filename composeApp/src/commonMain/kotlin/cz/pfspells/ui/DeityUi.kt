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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cz.pfspells.Strings
import cz.pfspells.data.DeityCatalog
import cz.pfspells.data.DeityInfo
import cz.pfspells.str

/** „LG · Core Deities · Longsword“ */
fun deityDetail(s: Strings, d: DeityInfo): String =
    listOfNotNull(d.alignment?.name, d.pantheon.ifBlank { null }, d.weapon.ifBlank { null }).joinToString(" · ")

/** Text tlačítka božstva v panelu postavy: „Božstvo: Torag (LG)“ / vlastní text / „vybrat…“. */
fun deityButtonText(s: Strings, deity: String): String {
    val d = DeityCatalog.get(deity)
    val name = when {
        d != null -> d.name + (d.alignment?.let { " (${it.name})" } ?: "")
        deity.isNotBlank() -> deity
        else -> s.chooseOption
    }
    return "${s.deityLabel}: $name"
}

/**
 * Výběr božstva ze seznamu (~460 božstev z Archives of Nethys): hledání v jménu, panteonu a oblastech.
 * Vlastní / homebrew božstvo jde zadat textem ([onCustom]).
 */
@Composable
fun DeityPickerDialog(current: String, onDismiss: () -> Unit, onCustom: () -> Unit, onPick: (String) -> Unit) {
    val s = str
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        val q = query.trim().lowercase()
        DeityCatalog.all.filter { d ->
            q.isEmpty() || d.name.lowercase().contains(q) || d.pantheon.lowercase().contains(q) || d.areas.lowercase().contains(q)
        }.sortedWith(compareBy({ !it.name.lowercase().startsWith(q) }, { it.name.lowercase() }))
    }
    val currentKey = current.trim().lowercase()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            val width = minOf(560.dp, maxWidth - 32.dp)
            val maxH = minOf(640.dp, maxHeight - 48.dp)
            Surface(
                Modifier.width(width).heightIn(max = maxH),
                shape = MaterialTheme.shapes.extraLarge, color = Nb.panel, shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 16.dp)) {
                    Text(s.chooseDeityTitle, style = MaterialTheme.typography.headlineSmall, color = Nb.ink, modifier = Modifier.padding(bottom = 14.dp))
                    NbSearchField(query, { query = it }, s.searchDeities, Modifier.fillMaxWidth())
                    LazyColumn(
                        Modifier.weight(1f, fill = false).padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                    ) {
                        items(list, key = { it.name }) { d -> DeityCard(s, d, d.name.lowercase() == currentKey) { onPick(d.name) } }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onCustom) { Text(s.customDeity, color = Nb.accent) }
                        if (current.isNotBlank()) TextButton(onClick = { onPick("") }) { Text(s.noDeity, color = Nb.error) }
                        TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeityCard(s: Strings, d: DeityInfo, selected: Boolean, onClick: () -> Unit) {
    val base = Modifier.fillMaxWidth().clip(Nb.shape)
    val fg = if (selected) Color.White else Nb.ink
    val muted = if (selected) Color.White else Nb.inkMuted
    Column(
        (if (selected) base.background(Nb.accent) else base.background(Nb.paper).dashedBorder())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(d.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(deityDetail(s, d), fontSize = 12.sp, color = if (selected) Color.White else Nb.rust, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (d.areas.isNotBlank()) Text(d.areas, fontSize = 12.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Varování: klerik má doménu / subdoménu, kterou jeho božstvo nedává (ikona místo znaku ⚠ – web nemá záložní fonty). */
@Composable
fun DeityConflictWarning(s: Strings, deity: DeityInfo, items: List<String>, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(NbIcons.Warn, null, tint = Nb.error, modifier = Modifier.padding(end = 6.dp).size(15.dp))
        Text(
            s.deityConflict(deity.name, items.joinToString(", ") { it.replaceFirstChar { ch -> ch.uppercase() } }),
            fontSize = 13.sp, color = Nb.error,
        )
    }
}
