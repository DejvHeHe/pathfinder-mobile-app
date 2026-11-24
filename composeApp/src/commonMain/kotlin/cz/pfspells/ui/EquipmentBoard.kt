package cz.pfspells.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Inventory
import cz.pfspells.data.ItemSlot
import cz.pfspells.data.equippedIn
import cz.pfspells.data.isWorking
import cz.pfspells.data.slotConflicts
import cz.pfspells.str

/*
 * Inventář 2.0 (návrh „Inventory 2.0 – figurka a equip sloty“): sloty ve dvou sloupcích kolem siluety postavy,
 * pod nimi ruce a předměty bez slotu; stavy dlaždic plný / prázdný / vybraný / konflikt; banner konfliktu
 * a sheet slotu (mobil bottom sheet, desktop dialog) s volbou, který předmět platí.
 */

/** Jedna dlaždice: slot a pro prsteny pořadí (Prsten 1 / 2). */
private data class SlotPos(val slot: ItemSlot, val index: Int = 0)

private val LEFT = listOf(ItemSlot.HEAD, ItemSlot.HEADBAND, ItemSlot.EYES, ItemSlot.NECK, ItemSlot.SHOULDERS, ItemSlot.CHEST, ItemSlot.BODY)
    .map { SlotPos(it) }
private val RIGHT = listOf(
    SlotPos(ItemSlot.ARMOR), SlotPos(ItemSlot.BELT), SlotPos(ItemSlot.WRISTS), SlotPos(ItemSlot.HANDS),
    SlotPos(ItemSlot.RING, 0), SlotPos(ItemSlot.RING, 1), SlotPos(ItemSlot.FEET),
)
private val HANDS = listOf(SlotPos(ItemSlot.MAIN_HAND), SlotPos(ItemSlot.SHIELD))

/** Počet dlaždic slotů (bez „bez slotu“) – „8 z 16 slotů“. */
private val SLOT_COUNT = LEFT.size + RIGHT.size + HANDS.size

private enum class TileState { EMPTY, FILLED, SELECTED, CONFLICT }

/** Předmět zobrazený na dlaždici (u prstenů podle pořadí). */
private fun Inventory.tileEntry(p: SlotPos): InvEntry? = equippedIn(p.slot).getOrNull(p.index)

/** Nadpis sekce „Vybavení · 8 z 16 slotů“. */
@Composable
fun EquipmentHeader(inv: Inventory) {
    val s = str
    val filled = (LEFT + RIGHT + HANDS).count { inv.tileEntry(it) != null }
    SectionHeader(s.equipmentHeader) { MonoLabel(s.slotsFilled(filled, SLOT_COUNT)) }
}

/** Banner konfliktu slotu: „Ramena: 2 předměty, platí jen první. Vyřešit“ (pro každý přeplněný slot). */
@Composable
fun SlotConflictBanners(inv: Inventory, onResolve: (ItemSlot) -> Unit) {
    val s = str
    inv.slotConflicts().forEach { slot ->
        val n = inv.equippedIn(slot).sumOf { it.qty.coerceAtLeast(1) }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(Nb.shape)
                .background(Nb.error.copy(alpha = 0.07f)).border(1.5.dp, Nb.error, Nb.shape)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(NbIcons.Warn, null, tint = Nb.error, modifier = Modifier.size(18.dp))
            Text(s.conflictBanner(s.itemSlot(slot), n), Modifier.weight(1f), fontSize = 13.5.sp, color = Nb.error)
            Text(
                s.resolve, Modifier.clip(Nb.shape).clickable { onResolve(slot) }.padding(4.dp),
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Nb.error, textDecoration = TextDecoration.Underline,
            )
        }
    }
}

/**
 * Figurka se sloty: levý sloupec (hlava…tělo), silueta, pravý sloupec (zbroj…nohy), pod tím ruce a bez slotu.
 * [selectedEntryId] = položka otevřená v detailu (dlaždice „vybraná“); klepnutí → [onSlot] (null = bez slotu).
 */
@Composable
fun EquipmentBoard(inv: Inventory, wide: Boolean, selectedEntryId: String?, onSlot: (ItemSlot?) -> Unit) {
    val s = str
    val slotless = inv.entries.filter { it.equipped && it.slot == null }
    val figureW = if (wide) 200.dp else 92.dp
    val body: @Composable ColumnScope.() -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(if (wide) 18.dp else 8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 6.dp)) {
                LEFT.forEach { p -> BoardTile(inv, p, wide, selectedEntryId) { onSlot(p.slot) } }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Silhouette(inv, figureW)
                if (wide) MonoLabel(s.creatureSize(inv.creatureSize) + if (inv.quadruped) " · ${s.quadruped}" else "")
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 6.dp)) {
                RIGHT.forEach { p -> BoardTile(inv, p, wide, selectedEntryId) { onSlot(p.slot) } }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = if (wide) 12.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(if (wide) 8.dp else 6.dp)) {
            HANDS.forEach { p -> Box(Modifier.weight(1f)) { BoardTile(inv, p, wide, selectedEntryId) { onSlot(p.slot) } } }
            // předměty bez slotu (ioun stones…): na desktopu vždy, na mobilu jen když nějaké jsou
            if (wide || slotless.isNotEmpty()) {
                Box(Modifier.weight(1f)) {
                    SlotTile(
                        label = s.noSlot,
                        name = slotless.joinToString { it.name }.ifEmpty { null },
                        state = if (slotless.any { it.id == selectedEntryId }) TileState.SELECTED
                        else if (slotless.isEmpty()) TileState.EMPTY else TileState.FILLED,
                        note = null, wide = wide,
                    ) { onSlot(null) }
                }
            }
        }
    }
    if (wide) {
        Column(Modifier.fillMaxWidth().clip(Nb.shape).background(Nb.inset).dashedBorder().padding(16.dp), content = body)
    } else {
        Column(Modifier.fillMaxWidth(), content = body)
    }
}

@Composable
private fun BoardTile(inv: Inventory, p: SlotPos, wide: Boolean, selectedEntryId: String?, onClick: () -> Unit) {
    val s = str
    val all = inv.equippedIn(p.slot)
    val count = all.sumOf { it.qty.coerceAtLeast(1) }
    val e = inv.tileEntry(p)
    val conflict = count > p.slot.capacity && e != null
    val label = if (p.slot == ItemSlot.RING) s.ringSlot(p.index + 1) else s.itemSlot(p.slot)
    val state = when {
        e == null -> TileState.EMPTY
        conflict -> TileState.CONFLICT
        e.id == selectedEntryId -> TileState.SELECTED
        else -> TileState.FILLED
    }
    SlotTile(label, e?.name, state, note = if (conflict) "$count×" else null, wide = wide, onClick = onClick)
}

/** Dlaždice slotu ve stavech z návrhu (plný, prázdný, vybraný, konflikt). */
@Composable
private fun SlotTile(label: String, name: String?, state: TileState, note: String?, wide: Boolean, onClick: () -> Unit) {
    val s = str
    val base = Modifier.fillMaxWidth().heightIn(min = if (wide) 52.dp else 44.dp).clip(Nb.shape)
    val styled = when (state) {
        TileState.EMPTY -> base.dashedBorder(color = Nb.dash.copy(alpha = 0.55f))
        TileState.FILLED -> base.background(Nb.panel).dashedBorder()
        TileState.SELECTED -> base.background(Nb.inset).border(2.dp, Nb.accent, Nb.shape)
        TileState.CONFLICT -> base.background(Nb.panel).border(1.5.dp, Nb.error, Nb.shape)
    }
    Column(
        styled.clickable(onClick = onClick).padding(horizontal = if (wide) 10.dp else 8.dp, vertical = if (wide) 6.dp else 4.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        MonoLabel(
            label, size = if (wide) 10.5.sp else 10.sp,
            color = when (state) {
                TileState.EMPTY -> Nb.inkMuted
                TileState.CONFLICT -> Nb.error
                else -> Nb.rust
            },
        )
        Text(
            name ?: s.emptySlot, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontSize = if (wide) 14.sp else 12.5.sp,
            fontWeight = if (name == null) FontWeight.Normal else FontWeight.SemiBold,
            color = if (name == null) Nb.placeholder else Nb.ink,
        )
        note?.let { MonoLabel(it, color = Nb.error, size = 10.sp) }
    }
}

// silueta z návrhu (viewBox 200 × 360): hlava + tělo, čárkovaný obrys
private val BODY_PATH = PathParser().parsePathString(
    "M90,62 L60,76 L46,90 L38,150 L32,204 L27,222 L40,228 L46,206 L54,152 L64,112 L70,190 L66,214 L72,290 L76,338 " +
        "L64,352 L96,352 L95,338 L98,236 L100,228 L102,236 L105,338 L104,352 L136,352 L124,338 L128,290 L134,214 " +
        "L130,190 L136,112 L146,152 L154,206 L160,228 L173,222 L168,204 L162,150 L154,90 L140,76 L110,62 Z",
).toPath()

/** Kde na siluetě leží tečka slotu (souřadnice viewBoxu 200 × 360; sloty po dvou mají dvě tečky). */
private fun dots(slot: ItemSlot, index: Int): List<Offset> = when (slot) {
    ItemSlot.HEAD -> listOf(Offset(100f, 16f))
    ItemSlot.HEADBAND -> listOf(Offset(100f, 28f))
    ItemSlot.EYES -> listOf(Offset(100f, 40f))
    ItemSlot.NECK -> listOf(Offset(100f, 66f))
    ItemSlot.SHOULDERS -> listOf(Offset(72f, 82f), Offset(128f, 82f))
    ItemSlot.CHEST -> listOf(Offset(100f, 108f))
    ItemSlot.BODY -> listOf(Offset(100f, 128f))
    ItemSlot.ARMOR -> listOf(Offset(100f, 150f))
    ItemSlot.BELT -> listOf(Offset(100f, 192f))
    ItemSlot.WRISTS -> listOf(Offset(46f, 186f), Offset(154f, 186f))
    ItemSlot.HANDS -> listOf(Offset(36f, 222f), Offset(164f, 222f))
    ItemSlot.RING -> listOf(if (index == 0) Offset(30f, 236f) else Offset(170f, 236f))
    ItemSlot.FEET -> listOf(Offset(84f, 344f), Offset(116f, 344f))
    ItemSlot.MAIN_HAND -> listOf(Offset(22f, 210f))
    ItemSlot.SHIELD -> listOf(Offset(178f, 210f))
}

@Composable
private fun Silhouette(inv: Inventory, width: Dp) {
    val fill = Nb.tag.copy(alpha = 0.65f)
    val dash = Nb.dash
    val ok = Nb.accent
    val bad = Nb.error
    val ring = Nb.panel
    val marks = (LEFT + RIGHT + HANDS).mapNotNull { p ->
        val e = inv.tileEntry(p) ?: return@mapNotNull null
        val conflict = inv.equippedIn(p.slot).sumOf { it.qty.coerceAtLeast(1) } > p.slot.capacity
        dots(p.slot, p.index).map { it to conflict }
            .takeIf { inv.isWorking(e) || conflict }
    }.flatten()
    Canvas(Modifier.size(width, width * 1.8f)) {
        val k = size.width / 200f
        scale(k, k, pivot = Offset.Zero) {
            val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            drawCircle(fill, 24f, Offset(100f, 36f))
            drawCircle(dash, 24f, Offset(100f, 36f), style = stroke)
            drawPath(BODY_PATH, fill)
            drawPath(BODY_PATH, dash, style = stroke)
            marks.forEach { (o, conflict) ->
                drawCircle(ring, 7f, o)
                drawCircle(if (conflict) bad else ok, 5f, o)
            }
        }
    }
}

/**
 * Sheet slotu: které předměty v něm jsou (volba, který platí), odložení ostatních do batohu, nasazení jiného.
 * Mobil: bottom sheet, desktop: dialog. [slot] null = předměty bez slotu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotSheet(state: AppState, inv: Inventory, slot: ItemSlot?, wide: Boolean, onOpen: (InvEntry) -> Unit, onDismiss: () -> Unit) {
    if (wide) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Nb.panel,
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { SlotSheetContent(state, inv, slot, onOpen, onDismiss) } },
            confirmButton = { TextButton(onClick = onDismiss) { Text(str.close, color = Nb.ink) } },
        )
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Nb.panel,
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 22.dp).verticalScroll(rememberScrollState())) {
                SlotSheetContent(state, inv, slot, onOpen, onDismiss)
            }
        }
    }
}

@Composable
private fun SlotSheetContent(state: AppState, inv: Inventory, slot: ItemSlot?, onOpen: (InvEntry) -> Unit, onDismiss: () -> Unit) {
    val s = str
    val worn = if (slot == null) inv.entries.filter { it.equipped && it.slot == null } else inv.equippedIn(slot)
    val conflict = slot != null && worn.sumOf { it.qty.coerceAtLeast(1) } > slot.capacity
    val candidates = inv.entries.filter { !it.equipped && (it.slot == slot || (slot != null && it.slot == null)) }
    var showCandidates by remember(slot) { mutableStateOf(worn.isEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MonoLabel(s.slotSheetLabel, color = Nb.rust)
                Text(
                    slot?.let(s::itemSlot) ?: s.noSlot,
                    fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Nb.ink,
                )
            }
            NbIconButton(NbIcons.Close, s.close, onDismiss, Nb.ink)
        }
        if (conflict) Text(s.conflictExplain, fontSize = 14.sp, color = Nb.error)
        worn.forEach { e ->
            val working = inv.isWorking(e)
            Row(
                Modifier.fillMaxWidth().clip(Nb.shape)
                    .then(if (working) Modifier.background(Nb.inset).dashedBorder() else Modifier)
                    .clickable { if (slot != null) state.preferInSlot(inv.id, e.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // přepínač „tenhle platí“ (u slotů pro jeden předmět)
                Box(
                    Modifier.size(22.dp).clip(CircleShape).border(2.dp, Nb.accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (working) Box(Modifier.size(10.dp).clip(CircleShape).background(Nb.accent)) }
                Column(Modifier.weight(1f)) {
                    Text(e.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink)
                    val bonuses = e.bonuses.joinToString(", ") { bonusText(s, it) }
                    val detail = listOfNotNull(bonuses.ifEmpty { null }, formatWeight(e.weightLb).takeIf { it != "—" }).joinToString(" · ")
                    Text(
                        if (working) detail else listOf(detail, s.ignoredNotWorn).filter { it.isNotEmpty() }.joinToString(" · "),
                        fontSize = 12.5.sp, color = if (working) Nb.inkMuted else Nb.error,
                    )
                }
                Text(
                    s.open, Modifier.clip(Nb.shape).clickable { onOpen(e) }.padding(4.dp),
                    fontSize = 13.sp, color = Nb.accent, textDecoration = TextDecoration.Underline,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (conflict) NbPrimaryButton(s.moveOthersToCarried, { state.unequipExtra(inv.id, slot) }, icon = null)
            if (!showCandidates) NbDashedButton(s.equipAnother, { showCandidates = true }, icon = NbIcons.Plus)
        }
        if (showCandidates) {
            MonoLabel(s.equipAnother)
            if (candidates.isEmpty()) Text(s.noSlotCandidates, fontSize = 13.5.sp, color = Nb.inkMuted)
            candidates.forEach { e ->
                Row(Modifier.fillMaxWidth().bottomRule(Nb.ruleSoft).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val bonuses = e.bonuses.joinToString(", ") { bonusText(s, it) }
                        if (bonuses.isNotEmpty()) Text(bonuses, fontSize = 12.5.sp, color = Nb.inkMuted)
                    }
                    NbDashedButton(s.equip, {
                        // nasadit do slotu; co se pak nevejde, jde do batohu
                        state.preferInSlot(inv.id, e.id, slot)
                        if (slot != null) state.unequipExtra(inv.id, slot)
                    })
                }
            }
        }
    }
}

/** Štítek slotu v seznamu: olivový (platí), červený (navíc v plném slotu), u neseného mono „neseno“. */
@Composable
fun SlotBadge(inv: Inventory, e: InvEntry) {
    val s = str
    if (!e.equipped) {
        Text(s.carriedLabel, fontFamily = LocalNbFonts.current.mono, fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1)
        return
    }
    val label = e.slot?.let(s::itemSlot) ?: s.equippedOnly
    Text(
        label,
        Modifier.clip(Nb.shape).background(if (inv.isWorking(e)) Nb.accent else Nb.error).padding(horizontal = 8.dp, vertical = 3.dp),
        color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}
