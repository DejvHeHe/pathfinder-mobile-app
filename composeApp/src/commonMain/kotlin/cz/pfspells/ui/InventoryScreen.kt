package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.Screen
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Inventory
import cz.pfspells.data.ItemCategory

// ---------- seznam inventářů ----------

/** Obsah záložky Inventory na mobilní úvodní obrazovce. */
fun LazyListScope.inventoryListItems(state: AppState, onOpen: (Inventory) -> Unit) {
    item { SectionHeader("My inventories", size = 20.sp) }
    if (state.inventories.isEmpty()) {
        item {
            Text(
                "No inventories yet.\nCreate one for each character (or a shared party stash) and add items from the PF1 item database.",
                Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, color = Nb.inkMuted, fontSize = 15.sp,
            )
        }
    }
    items(state.inventories, key = { "inv-" + it.id }) { inv ->
        InventoryItemCard(state, inv, selected = false, card = true) { onOpen(inv) }
    }
}

@Composable
fun InventoryItemCard(state: AppState, inv: Inventory, selected: Boolean, card: Boolean, onClick: () -> Unit) {
    val fonts = LocalNbFonts.current
    val base = Modifier.fillMaxWidth().clip(Nb.shape)
    Column(
        (if (selected || card) base.background(if (card) Nb.panel else Nb.inset).dashedBorder() else base)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (card) 14.dp else 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                inv.name, Modifier.weight(1f),
                fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = if (card) 22.sp else 18.sp,
                color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (selected) MonoLabel("Open", color = Nb.rust, size = 11.sp)
        }
        Text("${itemTypesCount(inv.entries.size)} · ${formatWeight(inv.totalWeight).replace("—", "0 lb")}", fontSize = 13.5.sp, color = Nb.inkMuted)
        if (card) Text("${inv.gp} gp · ${worthText(inv, state.pricesRevealed)}", fontSize = 13.sp, color = Nb.rust, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Boční panel širokého okna v sekci Inventory: seznam inventářů + měšec, zatížení a kategorie otevřeného. */
@Composable
fun InventorySidebar(state: AppState, selected: Inventory?, modifier: Modifier = Modifier) {
    var showNew by remember { mutableStateOf(false) }
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionHeader("My inventories", size = 20.sp) }
        items(state.inventories, key = { it.id }) { inv ->
            InventoryItemCard(state, inv, selected = inv.id == selected?.id, card = false) {
                state.openRoot(Screen.InventoryDetail(inv.id))
            }
        }
        item { NbDashedButton("New inventory", { showNew = true }, Modifier.padding(top = 4.dp), icon = NbIcons.Plus) }
        if (selected != null) {
            item { SectionHeader("Purse", size = 20.sp) }
            item { CoinRow(state, selected, big = true) }
            item { SectionHeader("Load", size = 20.sp) }
            item { LoadBar(state, selected) }
            item { SectionHeader("Categories", size = 20.sp) }
            item { CategoryFilterList(state, selected) }
        }
    }
    if (showNew) {
        TextFieldDialog("New inventory", "", "Name (character / stash)", onDismiss = { showNew = false }) { name ->
            showNew = false
            val inv = state.createInventory(name)
            state.openRoot(Screen.InventoryDetail(inv.id))
        }
    }
}

@Composable
private fun CategoryFilterList(state: AppState, inv: Inventory) {
    val counts = inv.entries.groupingBy { it.cat }.eachCount()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CategoryRow("All", inv.entries.size, state.inventoryCategory == null) { state.inventoryCategory = null }
        ItemCategory.entries.filter { (counts[it] ?: 0) > 0 }.forEach { c ->
            CategoryRow(c.label, counts[c] ?: 0, state.inventoryCategory == c) {
                state.inventoryCategory = if (state.inventoryCategory == c) null else c
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val base = Modifier.fillMaxWidth().clip(Nb.shape)
    Row(
        (if (selected) base.background(Nb.inset).dashedBorder() else base).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp, color = Nb.ink)
        Text("$count", fontFamily = LocalNbFonts.current.mono, fontSize = 13.sp, color = Nb.inkMuted)
    }
}

// ---------- detail inventáře ----------

@Composable
fun InventoryScreen(state: AppState, invId: String, wide: Boolean = false, selectedEntryId: String? = null) {
    val inv = state.inventory(invId)
    if (inv == null) {
        LaunchedEffect(invId) { if (wide) state.openRoot(Screen.Decks) else state.pop() }
        return
    }
    LaunchedEffect(Unit) { state.ensureItems() }
    var rename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var export by remember { mutableStateOf(false) }
    val revealed = state.pricesRevealed
    val q = state.inventoryQuery.trim().lowercase()
    val catFilter = if (wide) state.inventoryCategory else null
    val visible = inv.entries.filter { e ->
        (catFilter == null || e.cat == catFilter) && (q.isEmpty() || e.name.lowercase().contains(q) || e.note.lowercase().contains(q))
    }
    val grouped = visible.groupBy { it.cat }.entries.sortedBy { it.key.ordinal } // toSortedMap není v common
    val subtitle = buildString {
        append(itemTypesCount(inv.entries.size))
        if (wide) append(" · ").append(formatWeight(inv.totalWeight).replace("—", "0 lb"))
        append(" · ").append(worthText(inv, revealed))
    }
    val openPicker: () -> Unit = { if (wide) state.openRoot(Screen.ItemPicker(invId)) else state.push(Screen.ItemPicker(invId)) }

    NbScreen(
        title = inv.name,
        subtitle = subtitle,
        onBack = if (wide) null else ({ state.pop() }),
        wide = wide,
        primary = if (wide) null else "Add item" to openPicker, // na širokém okně je v hlavičce
        actions = {
            NbIconButton(NbIcons.Share, "Export inventory", { export = true }, Nb.inkMuted, 19.dp)
            NbIconButton(NbIcons.Pencil, "Rename", { rename = true }, Nb.inkMuted, 19.dp)
            NbIconButton(NbIcons.Trash, "Delete inventory", { confirmDelete = true }, Nb.inkMuted, 19.dp)
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = if (wide) 0.dp else 16.dp, end = if (wide) 0.dp else 16.dp, top = 6.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (!wide) {
                item { CoinRow(state, inv) }
                item { Box(Modifier.padding(top = 12.dp)) { LoadBar(state, inv) } }
            } else {
                item { InventoryTableHeader() }
            }
            if (inv.entries.isEmpty()) {
                item {
                    Text(
                        "This inventory is empty. Add items from the database with \"Add item\".",
                        Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = Nb.inkMuted,
                    )
                }
            }
            grouped.forEach { (cat, list) ->
                item(key = "h-${cat.name}") { SectionHeader(cat.label) }
                items(list, key = { it.id }) { e ->
                    EntryRow(
                        entry = e, wide = wide, revealed = revealed, selected = e.id == selectedEntryId,
                        onClick = { state.showItem(e.itemId, invId, e.id) },
                        onQty = { state.setEntryQty(invId, e.id, it) },
                    )
                }
            }
            item(key = "notes") {
                Column(Modifier.padding(top = 6.dp)) {
                    SectionHeader("Notes")
                    Text("Free notes (keys, debts, stash locations…)", fontSize = 13.sp, color = Nb.inkMuted, modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = inv.notes,
                        onValueChange = { t -> state.updateInventory(invId) { it.copy(notes = t) } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = LocalNbFonts.current.sans, fontSize = 15.sp, lineHeight = 28.sp, color = Nb.ink),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Nb.dash, focusedBorderColor = Nb.accent),
                    )
                }
            }
        }
    }

    if (export) InventoryExportDialog(listOf(inv)) { export = false }
    if (rename) {
        TextFieldDialog("Rename inventory", inv.name, "Name", onDismiss = { rename = false }) { n ->
            rename = false
            state.updateInventory(invId) { it.copy(name = n) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Nb.panel,
            title = { Text("Delete inventory?", color = Nb.ink) },
            text = { Text("Inventory \"${inv.name}\" will be permanently deleted.", color = Nb.ink) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (wide) state.openRoot(Screen.Decks) else state.pop()
                    state.deleteInventory(invId)
                }) { Text("Delete", color = Nb.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = Nb.ink) } },
        )
    }
}

@Composable
private fun InventoryTableHeader() {
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MonoLabel("Item", Modifier.weight(3f))
        MonoLabel("Qty", Modifier.width(108.dp))
        MonoLabel("Weight", Modifier.weight(1f))
        MonoLabel("Price", Modifier.weight(1f))
        MonoLabel("Notes", Modifier.weight(2f))
    }
}

@Composable
private fun EntryRow(entry: InvEntry, wide: Boolean, revealed: Boolean, selected: Boolean, onClick: () -> Unit, onQty: (Int) -> Unit) {
    val mono = LocalNbFonts.current.mono
    val base = Modifier.fillMaxWidth()
    val mod = (if (selected) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule(Nb.ruleSoft))
        .clickable(onClick = onClick)
        .padding(horizontal = if (wide) 10.dp else 0.dp, vertical = if (wide) 6.dp else 4.dp)
    val notes = listOfNotNull(entry.info, entry.note.ifBlank { null }).joinToString(" · ")
    if (wide) {
        Row(mod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(entry.name, Modifier.weight(3f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            CountStepper(entry.qty, onQty, min = 0, max = 9999, size = 32.dp, numberSize = 14.sp, modifier = Modifier.width(108.dp))
            Text(formatWeight(entry.weightLb), Modifier.weight(1f), fontFamily = mono, fontSize = 13.sp, color = Nb.ink)
            Text(priceLabel(entry.priceGp, revealed), Modifier.weight(1f), fontFamily = mono, fontSize = 13.sp, color = Nb.ink)
            Text(notes, Modifier.weight(2f), fontSize = 14.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Row(mod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(formatWeight(entry.weightLb), priceLabel(entry.priceGp, revealed), notes.ifBlank { null }).joinToString(" · "),
                    fontFamily = mono, fontSize = 12.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            CountStepper(entry.qty, onQty, min = 0, max = 9999, size = 32.dp, numberSize = 14.sp)
        }
    }
}
