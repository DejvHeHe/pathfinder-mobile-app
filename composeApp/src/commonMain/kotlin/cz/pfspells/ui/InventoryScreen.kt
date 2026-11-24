package cz.pfspells.ui

import cz.pfspells.str
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
import cz.pfspells.data.ItemSlot
import cz.pfspells.data.equippedIn
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color

// ---------- seznam inventářů ----------

/** Obsah záložky Inventory na mobilní úvodní obrazovce. */
fun LazyListScope.inventoryListItems(state: AppState, onOpen: (Inventory) -> Unit) {
    item { SectionHeader(str.myInventories, size = 20.sp) }
    if (state.inventories.isEmpty()) {
        item {
            Text(
                str.noInventoriesYet,
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
            if (selected) MonoLabel(str.open, color = Nb.rust, size = 11.sp)
        }
        Text("${str.itemTypes(inv.entries.size)} · ${formatWeight(inv.totalWeight).replace("—", "0 lb")}", fontSize = 13.5.sp, color = Nb.inkMuted)
        if (card) Text("${inv.gp} gp · ${worthText(str, inv, state.pricesRevealed)}", fontSize = 13.sp, color = Nb.rust, modifier = Modifier.padding(top = 2.dp))
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
        item { SectionHeader(str.myInventories, size = 20.sp) }
        items(state.inventories, key = { it.id }) { inv ->
            InventoryItemCard(state, inv, selected = inv.id == selected?.id, card = false) {
                state.openRoot(Screen.InventoryDetail(inv.id))
            }
        }
        item { NbDashedButton(str.newInventory, { showNew = true }, Modifier.padding(top = 4.dp), icon = NbIcons.Plus) }
        if (selected != null) {
            item { SectionHeader(str.purse, size = 20.sp) }
            item { CoinRow(state, selected, big = true) }
            item { SectionHeader(str.load, size = 20.sp) }
            item { LoadBar(state, selected) }
            item { SectionHeader(str.categories, size = 20.sp) }
            item { CategoryFilterList(state, selected) }
        }
    }
    if (showNew) {
        TextFieldDialog(str.newInventory, "", str.inventoryNameLabel, onDismiss = { showNew = false }) { name ->
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
        CategoryRow(str.all, inv.entries.size, state.inventoryCategory == null && !state.inventoryEquippedOnly) {
            state.inventoryCategory = null
            state.inventoryEquippedOnly = false
        }
        // jen nasazené (návrh Inventory 2.0)
        CategoryRow(str.equippedOnly, inv.entries.count { it.equipped }, state.inventoryEquippedOnly) {
            state.inventoryEquippedOnly = !state.inventoryEquippedOnly
        }
        ItemCategory.entries.filter { (counts[it] ?: 0) > 0 }.forEach { c ->
            CategoryRow(str.category(c), counts[c] ?: 0, state.inventoryCategory == c) {
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

/**
 * Detail inventáře (návrh „Inventory 2.0“): figurka se sloty vybavení, banner konfliktu slotu, seznam rozdělený
 * na nasazené a nesené (štítek slotu), poznámky. Mobil: záložky Vybavení | Předměty; desktop: vše pod sebou,
 * měšec, zatížení a kategorie jsou v bočním panelu.
 */
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
    // otevřený sheet slotu: Pair(slot) – null uvnitř = předměty bez slotu
    var sheet by remember { mutableStateOf<Pair<ItemSlot?, Unit>?>(null) }
    val s = str
    val revealed = state.pricesRevealed
    val q = state.inventoryQuery.trim().lowercase()
    val catFilter = if (wide) state.inventoryCategory else null
    val visible = inv.entries.filter { e ->
        (catFilter == null || e.cat == catFilter) && (!wide || !state.inventoryEquippedOnly || e.equipped) &&
            (q.isEmpty() || e.name.lowercase().contains(q) || e.note.lowercase().contains(q))
    }
    // nasazené zvlášť, nesené podle kategorie a jména
    val equipped = visible.filter { it.equipped }
    val carried = visible.filter { !it.equipped }.sortedWith(compareBy({ it.cat.ordinal }, { it.name.lowercase() }))
    val subtitle = buildString {
        state.characterOfInventory(inv.id)?.let { append(it.name).append(" · ") }
        append(s.itemTypes(inv.entries.size))
        append(" · ").append(formatWeight(inv.totalWeight).replace("—", "0 lb"))
        append(" · ").append(worthText(s, inv, revealed))
    }
    val openPicker: () -> Unit = { if (wide) state.openRoot(Screen.ItemPicker(invId)) else state.push(Screen.ItemPicker(invId)) }
    val openSlot: (ItemSlot?) -> Unit = { slot ->
        // desktop: plný slot bez konfliktu rovnou otevře předmět v detailu vpravo
        val worn = if (slot == null) inv.entries.filter { it.equipped && it.slot == null } else inv.equippedIn(slot)
        val single = worn.singleOrNull()
        if (wide && single != null) state.showItem(single.itemId, invId, single.id) else sheet = slot to Unit
    }
    val tab = if (wide) -1 else state.inventoryTab

    NbScreen(
        title = inv.name,
        subtitle = subtitle,
        onBack = if (wide) null else ({ state.pop() }),
        wide = wide,
        primary = if (wide) null else s.addItem to openPicker, // na širokém okně je v hlavičce
        actions = {
            NbIconButton(NbIcons.Share, s.exportInventory, { export = true }, Nb.inkMuted, 19.dp)
            NbIconButton(NbIcons.Pencil, s.rename, { rename = true }, Nb.inkMuted, 19.dp)
            NbIconButton(NbIcons.Trash, s.deleteInventory, { confirmDelete = true }, Nb.inkMuted, 19.dp)
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = if (wide) 0.dp else 16.dp, end = if (wide) 0.dp else 16.dp, top = 6.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (!wide) item(key = "tabs") { InventoryTabs(state, inv) }
            // vybavení: desktop vždy nahoře, mobil v záložce Vybavení
            if (wide || tab == 0) {
                item(key = "conflicts-top") { if (!wide) SlotConflictBanners(inv) { sheet = it to Unit } }
                item(key = "equipment") {
                    Column {
                        if (wide) EquipmentHeader(inv) else Box(Modifier.padding(top = 6.dp))
                        EquipmentBoard(inv, wide, selectedEntryId, openSlot)
                    }
                }
                if (wide) item(key = "conflicts") { Box(Modifier.padding(top = 8.dp)) { SlotConflictBanners(inv) { sheet = it to Unit } } }
                if (!wide) item(key = "load") {
                    Column(Modifier.padding(top = 12.dp).clip(Nb.shape).background(Nb.panel).dashedBorder().padding(horizontal = 12.dp, vertical = 10.dp)) {
                        LoadBar(state, inv)
                    }
                }
            }
            if (wide || tab == 1) {
                if (!wide) item(key = "coins") { Box(Modifier.padding(top = 10.dp)) { CoinRow(state, inv) } }
                if (wide) item(key = "table-head") { InventoryTableHeader() }
                if (inv.entries.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            s.inventoryEmpty,
                            Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = Nb.inkMuted,
                        )
                    }
                }
                listOf(s.equippedOnly to equipped, s.carriedGroup to carried).forEach { (label, list) ->
                    if (list.isNotEmpty()) {
                        item(key = "h-$label") {
                            SectionHeader(label, size = 18.sp) {
                                MonoLabel(formatWeight(list.sumOf { (it.weightLb ?: 0.0) * it.qty }).replace("—", "0 lb"))
                            }
                        }
                        items(list, key = { it.id }) { e ->
                            EntryRow(
                                inv = inv, entry = e, wide = wide, revealed = revealed, selected = e.id == selectedEntryId,
                                onClick = { state.showItem(e.itemId, invId, e.id) },
                                onQty = { state.setEntryQty(invId, e.id, it) },
                            )
                        }
                    }
                }
                item(key = "notes") {
                    Column(Modifier.padding(top = 6.dp)) {
                        SectionHeader(s.notes)
                        Text(s.notesHint, fontSize = 13.sp, color = Nb.inkMuted, modifier = Modifier.padding(bottom = 6.dp))
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
    }

    sheet?.let { (slot, _) ->
        SlotSheet(state, inv, slot, wide, onOpen = { e -> sheet = null; state.showItem(e.itemId, invId, e.id) }) { sheet = null }
    }
    if (export) InventoryExportDialog(listOf(inv)) { export = false }
    if (rename) {
        TextFieldDialog(s.renameInventory, inv.name, s.name, onDismiss = { rename = false }) { n ->
            rename = false
            state.updateInventory(invId) { it.copy(name = n) }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Nb.panel,
            title = { Text(s.deleteInventoryTitle, color = Nb.ink) },
            text = { Text(s.deleteInventoryText(inv.name), color = Nb.ink) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (wide) state.openRoot(Screen.Decks) else state.pop()
                    state.deleteInventory(invId)
                }) { Text(s.delete, color = Nb.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(s.cancel, color = Nb.ink) } },
        )
    }
}

/** Segmentové záložky mobilu: Vybavení | Předměty (N) – olivová vybraná, čárkovaný rámeček. */
@Composable
private fun InventoryTabs(state: AppState, inv: Inventory) {
    val s = str
    Row(
        Modifier.fillMaxWidth().padding(bottom = 4.dp).clip(Nb.shape).dashedBorder().padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(s.equipmentHeader, s.itemsTab(inv.entries.size)).forEachIndexed { i, label ->
            val on = state.inventoryTab == i
            Box(
                Modifier.weight(1f).height(38.dp).clip(Nb.shape).then(if (on) Modifier.background(Nb.accent) else Modifier)
                    .clickable { state.inventoryTab = i },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.White else Nb.ink, maxLines = 1)
            }
        }
    }
}

@Composable
private fun InventoryTableHeader() {
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MonoLabel(str.colItem, Modifier.weight(3f))
        MonoLabel(str.slot, Modifier.weight(1.3f))
        MonoLabel(str.colQty, Modifier.width(104.dp))
        MonoLabel(str.colWeight, Modifier.weight(0.9f))
        MonoLabel(str.colPrice, Modifier.weight(1.1f))
    }
}

@Composable
private fun EntryRow(inv: Inventory, entry: InvEntry, wide: Boolean, revealed: Boolean, selected: Boolean, onClick: () -> Unit, onQty: (Int) -> Unit) {
    val mono = LocalNbFonts.current.mono
    val base = Modifier.fillMaxWidth()
    val mod = (if (selected) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule(Nb.ruleSoft))
        .clickable(onClick = onClick)
        .padding(horizontal = if (wide) 10.dp else 0.dp, vertical = 4.dp)
    val notes = listOfNotNull(entry.info, entry.note.ifBlank { null }).joinToString(" · ")
    if (wide) {
        Row(mod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(3f)) {
                Text(entry.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (notes.isNotEmpty()) Text(notes, fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1.3f)) { SlotBadge(inv, entry) }
            CountStepper(entry.qty, onQty, min = 0, max = 9999, size = 32.dp, numberSize = 14.sp, modifier = Modifier.width(104.dp))
            Text(formatWeight(entry.weightLb), Modifier.weight(0.9f), fontFamily = mono, fontSize = 13.sp, color = Nb.ink)
            Text(priceLabel(entry.priceGp, revealed), Modifier.weight(1.1f), fontFamily = mono, fontSize = 13.sp, color = Nb.ink)
        }
    } else {
        Row(mod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry.equipped) SlotBadge(inv, entry)
                    Text(
                        listOfNotNull(formatWeight(entry.weightLb), priceLabel(entry.priceGp, revealed), notes.ifBlank { null }).joinToString(" · "),
                        fontFamily = mono, fontSize = 12.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            CountStepper(entry.qty, onQty, min = 0, max = 9999, size = 32.dp, numberSize = 14.sp)
        }
    }
}
