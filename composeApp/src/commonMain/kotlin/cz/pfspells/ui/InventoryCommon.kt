package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.Section
import cz.pfspells.data.CarryingCapacity
import cz.pfspells.data.Inventory
import kotlin.math.abs
import kotlin.math.roundToLong

/** Hranice, nad kterou se cena bez odemčení nezobrazuje. */
const val HIDDEN_PRICE_LIMIT = 200.0

private fun groupThousands(n: Long): String {
    val s = abs(n).toString()
    val out = StringBuilder()
    s.forEachIndexed { i, c ->
        if (i > 0 && (s.length - i) % 3 == 0) out.append(',')
        out.append(c)
    }
    return (if (n < 0) "-" else "") + out
}

private fun trimNumber(v: Double): String {
    val r = (v * 100).roundToLong()
    return if (r % 100 == 0L) groupThousands(r / 100)
    else {
        val whole = groupThousands(r / 100)
        val frac = (abs(r) % 100).toString().padStart(2, '0').trimEnd('0')
        "$whole.$frac"
    }
}

/** 15 → „15 gp“, 0.1 → „1 sp“, 0.01 → „1 cp“. */
fun formatGp(gp: Double): String = when {
    gp >= 1 || gp == 0.0 -> "${trimNumber(gp)} gp"
    gp >= 0.1 -> "${trimNumber(gp * 10)} sp"
    else -> "${trimNumber(gp * 100)} cp"
}

/** Cena s ohledem na skryté drahé itemy. */
fun priceLabel(gp: Double?, revealed: Boolean): String = when {
    gp == null -> "—"
    gp > HIDDEN_PRICE_LIMIT && !revealed -> "??? gp"
    else -> formatGp(gp)
}

fun formatWeight(lb: Double?): String = if (lb == null || lb == 0.0) "—" else "${trimNumber(lb)} lb"

fun itemTypesCount(n: Int) = if (n == 1) "1 item type" else "$n item types"

/** „worth ≈ 187 gp“ – když je v inventáři skrytá cena, celková hodnota se taky skryje. */
fun worthText(inv: Inventory, revealed: Boolean): String {
    val hidden = !revealed && inv.entries.any { (it.priceGp ?: 0.0) > HIDDEN_PRICE_LIMIT }
    if (hidden) return "worth ≈ ???"
    val total = inv.entries.sumOf { (it.priceGp ?: 0.0) * it.qty }
    return "worth ≈ ${formatGp(kotlin.math.round(total))}"
}

// ---------- komponenty ----------

/** Záložky hlavních sekcí (mobil: pod nadpisem; desktop: v hlavičce). */
@Composable
fun SectionTabs(state: AppState, modifier: Modifier = Modifier, size: TextUnit = 18.sp) {
    val fonts = LocalNbFonts.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        listOf(Section.SPELLBOOK to "Spellbook", Section.INVENTORY to "Inventory").forEach { (s, label) ->
            val selected = state.section == s
            Column(
                Modifier.width(IntrinsicSize.Max).clip(Nb.shape).clickable { if (!selected) state.switchSection(s) }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(label, fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = size, color = if (selected) Nb.ink else Nb.inkMuted)
                Box(Modifier.padding(top = 4.dp).height(2.dp).fillMaxWidth().background(if (selected) Nb.rust else androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
}

/** Dlaždice mince (PP/GP/SP/CP); klepnutím se upraví. */
@Composable
fun CoinTile(label: String, value: Int, onEdit: () -> Unit, modifier: Modifier = Modifier, big: Boolean = false) {
    val fonts = LocalNbFonts.current
    Column(
        modifier.clip(Nb.shape).background(Nb.panel).dashedBorder().clickable(onClickLabel = "Edit $label", onClick = onEdit)
            .padding(vertical = if (big) 10.dp else 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MonoLabel(label, color = Nb.rust, size = 11.sp)
        Text(
            value.toString(),
            fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = if (big) 26.sp else 22.sp,
            color = Nb.ink, maxLines = 1, textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun CoinRow(state: AppState, inv: Inventory, big: Boolean = false) {
    var editing by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CoinTile("PP", inv.pp, { editing = "PP" }, Modifier.weight(1f), big)
        CoinTile("GP", inv.gp, { editing = "GP" }, Modifier.weight(1f), big)
        CoinTile("SP", inv.sp, { editing = "SP" }, Modifier.weight(1f), big)
        CoinTile("CP", inv.cp, { editing = "CP" }, Modifier.weight(1f), big)
    }
    editing?.let { coin ->
        val current = when (coin) { "PP" -> inv.pp; "GP" -> inv.gp; "SP" -> inv.sp; else -> inv.cp }
        NumberDialog("$coin – coins", current, onDismiss = { editing = null }) { v ->
            state.updateInventory(inv.id) {
                when (coin) { "PP" -> it.copy(pp = v); "GP" -> it.copy(gp = v); "SP" -> it.copy(sp = v); else -> it.copy(cp = v) }
            }
            editing = null
        }
    }
}

/** Celková váha + ukazatel zatížení podle STR. */
@Composable
fun LoadBar(state: AppState, inv: Inventory, showStrength: Boolean = true) {
    val heavy = CarryingCapacity.heavyLoad(inv.strength)
    val w = inv.totalWeight
    val fraction = if (heavy > 0) (w / heavy).toFloat().coerceIn(0f, 1f) else 1f
    val over = w > heavy
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("Total weight", Modifier.weight(1f), fontSize = 14.sp, color = Nb.ink)
            Text(formatWeight(w).replace("—", "0 lb"), fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (over) Nb.error else Nb.ink)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).dashedBorder(radius = 3.dp).padding(2.dp)) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(Nb.shape).background(if (over) Nb.error else Nb.accent))
        }
        Text(
            "${CarryingCapacity.loadName(inv.strength, w)} · light ≤ ${formatWeight(CarryingCapacity.lightLoad(inv.strength))}, " +
                "medium ≤ ${formatWeight(CarryingCapacity.mediumLoad(inv.strength))}, heavy ≤ ${formatWeight(heavy)}" +
                if (inv.coinWeight > 0) " · coins ${formatWeight(inv.coinWeight)}" else "",
            fontSize = 12.5.sp, color = Nb.inkMuted, lineHeight = 17.sp,
        )
        if (showStrength) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonoLabel("Strength", Modifier.weight(1f))
                CountStepper(inv.strength, { s -> state.updateInventory(inv.id) { it.copy(strength = s) } }, min = 1, max = 60, size = 32.dp, numberSize = 16.sp)
            }
        }
    }
}

@Composable
fun NumberDialog(title: String, initial: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(initial.toString()) }
    val value = text.trim().toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title, color = Nb.ink) },
        text = {
            OutlinedTextField(
                text, { t -> text = t.filter { it.isDigit() }.take(9) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { NbPrimaryButton("Save", { value?.let(onSave) }, icon = null, enabled = value != null) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nb.ink) } },
    )
}

@Composable
fun TextFieldDialog(title: String, initial: String, label: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title, color = Nb.ink) },
        text = { OutlinedTextField(text, { text = it }, label = { Text(label) }, singleLine = true) },
        confirmButton = { NbPrimaryButton("Save", { onSave(text.trim()) }, icon = null, enabled = text.isNotBlank()) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nb.ink) } },
    )
}
