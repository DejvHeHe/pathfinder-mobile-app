package cz.pfspells.ui

import cz.pfspells.data.CreatureSize
import cz.pfspells.data.Load
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import cz.pfspells.Strings
import cz.pfspells.str
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

/** „worth ≈ 187 gp“ – když je v inventáři skrytá cena, celková hodnota se taky skryje. */
fun worthText(s: Strings, inv: Inventory, revealed: Boolean): String {
    val hidden = !revealed && inv.entries.any { (it.priceGp ?: 0.0) > HIDDEN_PRICE_LIMIT }
    if (hidden) return s.worth("???")
    val total = inv.entries.sumOf { (it.priceGp ?: 0.0) * it.qty }
    return s.worth(formatGp(kotlin.math.round(total)))
}

// ---------- komponenty ----------

/** Záložky hlavních sekcí (mobil: pod nadpisem; desktop: v hlavičce). */
@Composable
fun SectionTabs(state: AppState, modifier: Modifier = Modifier, size: TextUnit = 18.sp) {
    val fonts = LocalNbFonts.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        listOf(Section.SPELLBOOK to str.tabSpellbook, Section.INVENTORY to str.tabInventory).forEach { (s, label) ->
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
        modifier.clip(Nb.shape).background(Nb.panel).dashedBorder().clickable(onClickLabel = str.editCoin(label), onClick = onEdit)
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
        NumberDialog(str.coinsTitle(coin), current, onDismiss = { editing = null }) { v ->
            state.updateInventory(inv.id) {
                when (coin) { "PP" -> it.copy(pp = v); "GP" -> it.copy(gp = v); "SP" -> it.copy(sp = v); else -> it.copy(cp = v) }
            }
            editing = null
        }
    }
}

/** Barva stupně zatížení (barvy stavů: rezavá jen pro nadpisy): olivová → okrová → červená. */
fun loadColor(l: Load): Color = when (l) {
    Load.LIGHT -> Nb.accent
    Load.MEDIUM -> Nb.ochre
    Load.HEAVY, Load.OVERLOADED -> Nb.error
}

/** Celková váha, pruh zatížení s úseky light / medium / heavy, štítek stavu, postihy, STR a velikost tvora. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoadBar(state: AppState, inv: Inventory, showStrength: Boolean = true) {
    val s = str
    val w = inv.totalWeight
    val load = inv.load
    val color = loadColor(load)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(s.totalWeight, Modifier.weight(1f), fontSize = 14.sp, color = Nb.ink)
            // „58 / 130 lb“ – váha / hranice těžkého zatížení
            Text(
                "${formatWeight(w).replace("—", "0 lb").removeSuffix(" lb")} / ${formatWeight(inv.heavyLoad)}",
                fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                color = if (load == Load.OVERLOADED) Nb.error else Nb.ink,
            )
        }
        LoadMeter(w, inv.lightLoad, inv.mediumLoad, inv.heavyLoad, color)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Text(
                    s.loadName(load), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    color = if (load == Load.MEDIUM) Nb.ochreInk else if (load == Load.LIGHT) Nb.ink else Nb.error,
                )
                if (load == Load.OVERLOADED) NbStamp(s.stampOverloaded)
            }
            if (inv.coinWeight > 0) {
                Text(s.coinsWeight(formatWeight(inv.coinWeight)), Modifier.padding(top = 4.dp), fontSize = 12.5.sp, color = Nb.inkMuted)
            }
        }
        s.loadPenalties(load)?.let {
            Text(it, fontSize = 12.5.sp, lineHeight = 17.sp, color = Nb.inkMuted)
        }
        Text(
            s.loadLimits(formatWeight(inv.lightLoad), formatWeight(inv.mediumLoad), formatWeight(inv.heavyLoad)),
            fontSize = 12.sp, color = Nb.inkMuted, lineHeight = 16.sp,
        )
        if (showStrength) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val owner = state.characterOfInventory(inv.id)
                Column(Modifier.weight(1f)) {
                    MonoLabel(s.strength)
                    if (owner != null) Text(s.strengthFromCharacter(owner.name), fontSize = 12.sp, color = Nb.inkMuted)
                }
                CountStepper(inv.strength, { v -> state.setInventoryStrength(inv.id, v) }, min = 1, max = 60, size = 32.dp, numberSize = 16.sp)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SimpleDropdown(
                    s.sizeLabel, CreatureSize.entries, inv.creatureSize, { s.creatureSize(it) },
                    { c -> state.updateInventory(inv.id) { it.copy(size = c.name) } },
                )
                Box(Modifier.padding(top = 5.dp)) {
                    NbFilterChip(s.quadruped, inv.quadruped) { state.updateInventory(inv.id) { it.copy(quadruped = !it.quadruped) } }
                }
            }
        }
    }
}

/**
 * Pruh zatížení (Deník dobrodruha): úseky light / medium / heavy s čárkovanými předěly, přes ně šrafování do
 * aktuální váhy a nad pruhem trojúhelníkový ukazatel; změna váhy se animuje.
 */
@Composable
private fun LoadMeter(w: Double, light: Double, medium: Double, heavy: Double, @Suppress("UNUSED_PARAMETER") color: Color) {
    val scaleMax = maxOf(heavy, w).takeIf { it > 0 } ?: 1.0
    val frac by androidx.compose.animation.core.animateFloatAsState(
        (w / scaleMax).toFloat().coerceIn(0f, 1f),
        androidx.compose.animation.core.tween(if (LocalReduceMotion.current) 0 else 350), label = "load",
    )
    val ink = Nb.ink
    val edge = Nb.inkMuted
    val zones = listOf(Color(0xFFB9C29A), Color(0xFFD9B76A), Color(0xFFD7A899), Color(0xFFD7A899))
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val width = size.width
        val top = 10.dp.toPx()
        val barH = 16.dp.toPx()
        fun x(v: Double) = (v / scaleMax).toFloat().coerceIn(0f, 1f) * width
        val bounds = listOf(0.0, light, medium, heavy, scaleMax)
        for (i in 0 until 4) {
            val x0 = x(bounds[i]); val x1 = x(bounds[i + 1])
            if (x1 > x0) drawRect(zones[i], Offset(x0, top), Size(x1 - x0, barH))
            // čárkovaný předěl
            if (i in 1..3 && x0 > 0f && x0 < width) {
                drawLine(edge, Offset(x0, top), Offset(x0, top + barH), 1.5.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())))
            }
        }
        // šrafování do aktuální váhy
        translate(0f, top) {
            clipRect(0f, 0f, width, barH) { drawHatchRect(width * frac, barH, ink.copy(alpha = .6f)) }
        }
        drawRoundRect(edge, Offset(0f, top), Size(width, barH), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        // trojúhelníkový ukazatel nad pruhem
        val px = (width * frac).coerceIn(7.dp.toPx(), width - 7.dp.toPx())
        val tri = androidx.compose.ui.graphics.Path().apply {
            moveTo(px - 6.dp.toPx(), 0f); lineTo(px + 6.dp.toPx(), 0f); lineTo(px, 8.dp.toPx()); close()
        }
        drawPath(tri, ink)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHatchRect(width: Float, h: Float, color: Color) {
    if (width <= 0f) return
    val g = 5.dp.toPx()
    val dx = h / kotlin.math.tan(55.0 * kotlin.math.PI / 180).toFloat()
    clipRect(0f, 0f, width, h) {
        var x = -dx
        while (x < width + dx) { drawLine(color, Offset(x, h), Offset(x + dx, 0f), 1.5.dp.toPx()); x += g }
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
        confirmButton = { NbPrimaryButton(str.save, { value?.let(onSave) }, icon = null, enabled = value != null) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str.cancel, color = Nb.ink) } },
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
        confirmButton = { NbPrimaryButton(str.save, { onSave(text.trim()) }, icon = null, enabled = text.isNotBlank()) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str.cancel, color = Nb.ink) } },
    )
}
