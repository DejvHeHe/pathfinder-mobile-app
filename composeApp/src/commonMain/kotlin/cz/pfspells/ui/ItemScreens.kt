package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.ItemDbState
import cz.pfspells.ItemFilter
import cz.pfspells.Screen
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Inventory
import cz.pfspells.data.Item
import cz.pfspells.data.ItemCategory
import cz.pfspells.data.ItemDatabase
import cz.pfspells.data.newEntryId

/** Obal: počká na načtení databáze itemů. */
@Composable
fun WithItems(state: AppState, content: @Composable (ItemDatabase) -> Unit) {
    LaunchedEffect(Unit) { state.ensureItems() }
    when (val s = state.items) {
        is ItemDbState.Ready -> content(s.db)
        is ItemDbState.Failed -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Could not load items:\n${s.message}", color = Nb.error, textAlign = TextAlign.Center)
                NbPrimaryButton("Use bundled data", { state.resetItems() }, icon = null)
            }
        }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Nb.accent)
                Text("Loading item database…", Modifier.padding(top = 12.dp), color = Nb.inkMuted)
            }
        }
    }
}

// ---------- výběr itemu ----------

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ItemPickerScreen(state: AppState, invId: String, wide: Boolean = false, selectedItemId: String? = null) {
    val inv = state.inventory(invId)
    if (inv == null) {
        LaunchedEffect(invId) { state.pop() }
        return
    }
    var sheetItem by remember { mutableStateOf<Item?>(null) }
    var custom by remember { mutableStateOf(false) }
    val back: () -> Unit = { if (wide) state.openRoot(Screen.InventoryDetail(invId)) else state.pop() }

    WithItems(state) { db ->
        val filter = state.itemFilter
        val set: (ItemFilter) -> Unit = { state.itemFilter = it }
        val results = remember(db, filter) { filterItems(db, filter) }
        val owned = inv.entries.filter { it.itemId != null }.groupingBy { it.itemId!! }.fold(0) { a, e -> a + e.qty }

        NbScreen(
            title = "Add item",
            subtitle = "PF1 item database · ${db.items.size} items · to ${inv.name}",
            onBack = back,
            wide = wide,
            actions = { if (wide) NbDashedButton("Custom item", { custom = true }, icon = NbIcons.Pencil) },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = if (wide) 0.dp else 16.dp).padding(top = if (wide) 14.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NbSearchField(filter.query, { set(filter.copy(query = it)) }, "Search ${db.items.size} items…", Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { NbFilterChip("All", filter.category == null) { set(filter.copy(category = null, proficiency = emptySet())) } }
                    items(ItemCategory.entries.filter { (db.counts[it] ?: 0) > 0 }) { c ->
                        NbFilterChip(c.label, filter.category == c) {
                            set(filter.copy(category = if (filter.category == c) null else c, proficiency = emptySet()))
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (filter.category == ItemCategory.WEAPON) {
                        listOf("Simple", "Martial", "Exotic").forEach { p ->
                            NbFilterChip(p, p in filter.proficiency) {
                                set(filter.copy(proficiency = if (p in filter.proficiency) filter.proficiency - p else filter.proficiency + p))
                            }
                        }
                    }
                    MaxPriceField(filter.maxPrice) { set(filter.copy(maxPrice = it)) }
                    if (!wide) NbDashedButton("Custom item", { custom = true }, icon = NbIcons.Pencil)
                }
                Text(resultsSummary(results.size, filter), fontSize = 14.sp, color = Nb.inkMuted)
            }
            val weaponCols = filter.category == ItemCategory.WEAPON || filter.category == ItemCategory.AMMO
            if (wide) PickerTableHeader(weaponCols)
            LazyColumn(
                Modifier.fillMaxSize(),
                state = state.itemPickerList,
                contentPadding = PaddingValues(start = if (wide) 0.dp else 16.dp, end = if (wide) 0.dp else 16.dp, top = 4.dp, bottom = 32.dp),
            ) {
                items(results, key = { it.id }) { item ->
                    PickerRow(
                        item = item, wide = wide, weaponCols = weaponCols, owned = owned[item.id] ?: 0,
                        revealed = state.pricesRevealed, selected = item.id == selectedItemId,
                        onClick = { if (wide) state.showItem(item.id, invId) else sheetItem = item },
                        onAdd = { state.addItem(invId, item) },
                    )
                }
            }
        }

        sheetItem?.let { item ->
            ModalBottomSheet(
                onDismissRequest = { sheetItem = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Nb.panel,
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            ) {
                ItemDetailContent(state, item, inv, entry = null, aside = false, sheet = true, onAdded = { sheetItem = null })
            }
        }
    }

    if (custom) {
        CustomItemDialog(null, onDismiss = { custom = false }) { entry ->
            custom = false
            state.addCustomEntry(invId, entry)
        }
    }
}

private fun filterItems(db: ItemDatabase, f: ItemFilter): List<Item> {
    val q = f.query.trim().lowercase()
    return db.items.filter { i ->
        (f.category == null || i.category == f.category) &&
            (f.proficiency.isEmpty() || i.proficiency in f.proficiency) &&
            (f.maxPrice == null || (i.priceGp != null && i.priceGp <= f.maxPrice)) &&
            (q.isEmpty() || i.searchName.contains(q))
    }
}

private fun resultsSummary(n: Int, f: ItemFilter) = buildList {
    add(if (n == 1) "1 result" else "$n results")
    f.category?.let { add(it.label) }
    if (f.proficiency.isNotEmpty()) add(f.proficiency.sorted().joinToString(", "))
    f.maxPrice?.let { add("up to ${formatGp(it)}") }
}.joinToString(" · ")

@Composable
private fun MaxPriceField(value: Double?, onChange: (Double?) -> Unit) {
    var text by remember(value == null) { mutableStateOf(value?.let { formatGp(it).removeSuffix(" gp") } ?: "") }
    val fonts = LocalNbFonts.current
    Row(
        Modifier.height(34.dp).clip(Nb.shape).background(Nb.panel).dashedBorder().padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("max", fontFamily = fonts.mono, fontSize = 12.sp, color = Nb.inkMuted)
        Box(Modifier.width(64.dp)) {
            if (text.isEmpty()) Text("any", fontFamily = fonts.mono, fontSize = 13.sp, color = Nb.placeholder)
            BasicTextField(
                text,
                { t ->
                    text = t.filter { it.isDigit() || it == '.' }.take(8)
                    onChange(text.toDoubleOrNull())
                },
                singleLine = true,
                textStyle = TextStyle(fontFamily = fonts.mono, fontSize = 13.sp, color = Nb.ink),
                cursorBrush = SolidColor(Nb.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        Text("gp", fontFamily = fonts.mono, fontSize = 12.sp, color = Nb.inkMuted)
    }
}

@Composable
private fun PickerTableHeader(weaponCols: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp).dashedEdge(Side.Bottom, Nb.rule),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MonoLabel("Name", Modifier.weight(2.2f))
        MonoLabel("Type", Modifier.weight(2f))
        if (weaponCols) {
            MonoLabel("Dmg", Modifier.weight(0.8f))
            MonoLabel("Crit", Modifier.weight(1.1f))
        }
        MonoLabel("Weight", Modifier.weight(0.8f))
        MonoLabel("Price", Modifier.weight(0.9f))
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun PickerRow(
    item: Item, wide: Boolean, weaponCols: Boolean, owned: Int, revealed: Boolean, selected: Boolean,
    onClick: () -> Unit, onAdd: () -> Unit,
) {
    val mono = LocalNbFonts.current.mono
    val base = Modifier.fillMaxWidth()
    val mod = (if (selected) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule(Nb.ruleSoft))
        .clickable(onClick = onClick)
        .padding(horizontal = if (wide) 10.dp else 0.dp, vertical = if (wide) 6.dp else 5.dp)
    val addButton = @Composable {
        NbSquareButton("+", onAdd, "Add ${item.name}", size = if (wide) 36.dp else 40.dp) {
            Icon(NbIcons.Plus, null, tint = Nb.ink, modifier = Modifier.size(16.dp))
        }
    }
    if (wide) {
        Row(mod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, Modifier.weight(1f, fill = false), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (owned > 0) MonoLabel("$owned owned", color = Nb.rust, size = 10.5.sp)
            }
            Text(item.subtitle ?: item.category.label, Modifier.weight(2f), fontSize = 14.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (weaponCols) {
                Text(item.damageMedium ?: "—", Modifier.weight(0.8f), fontFamily = mono, fontSize = 13.sp, color = Nb.ink)
                Text(item.critical ?: "—", Modifier.weight(1.1f), fontFamily = mono, fontSize = 13.sp, color = Nb.ink)
            }
            Text(formatWeight(item.weightLb), Modifier.weight(0.8f), fontFamily = mono, fontSize = 13.sp, color = Nb.ink)
            Text(priceLabel(item.priceGp, revealed), Modifier.weight(0.9f), fontFamily = mono, fontSize = 13.sp, color = Nb.ink)
            addButton()
        }
    } else {
        Row(mod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(item.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        append(listOfNotNull(item.summary, formatWeight(item.weightLb).takeIf { it != "—" }, priceLabel(item.priceGp, revealed)).joinToString(" · "))
                        if (owned > 0) append(" · $owned owned")
                    },
                    fontFamily = mono, fontSize = 12.sp, color = if (owned > 0) Nb.rust else Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(item.subtitle ?: item.category.label, fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            addButton()
        }
    }
}

// ---------- detail itemu / položky inventáře ----------

/** Pravý panel (široké okno) nebo celá obrazovka (mobil) pro [Screen.ItemDetail]. */
@Composable
fun ItemDetailScreen(state: AppState, itemId: String?, invId: String?, entryId: String?, aside: Boolean) {
    val inv = invId?.let { state.inventory(it) }
    val entry = entryId?.let { id -> inv?.entries?.firstOrNull { it.id == id } }
    if (entryId != null && entry == null) {
        LaunchedEffect(entryId) { state.pop() } // položka byla odebrána
        return
    }
    val fonts = LocalNbFonts.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = if (aside) 24.dp else 4.dp, end = 8.dp, top = if (aside) 14.dp else 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!aside) BackButton { state.pop() }
            Text(
                entry?.name ?: itemId?.let { state.itemDb?.get(it)?.name } ?: "Item", Modifier.weight(1f),
                fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = if (aside) 32.sp else 24.sp,
                lineHeight = if (aside) 35.sp else 27.sp, color = Nb.ink, maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            if (aside) NbIconButton(NbIcons.Close, "Close details", { state.pop() }, Nb.inkMuted)
        }
        if (itemId == null) {
            ItemDetailContent(state, null, inv, entry, aside, sheet = false, showTitle = false)
        } else {
            WithItems(state) { db ->
                ItemDetailContent(state, db.get(itemId), inv, entry, aside, sheet = false, showTitle = false)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDetailContent(
    state: AppState,
    item: Item?,
    inv: Inventory?,
    entry: InvEntry?,
    aside: Boolean,
    sheet: Boolean,
    showTitle: Boolean = true,
    onAdded: () -> Unit = {},
) {
    val fonts = LocalNbFonts.current
    val revealed = state.pricesRevealed
    var qty by remember(item?.id) { mutableIntStateOf(1) }
    var note by remember(item?.id) { mutableStateOf("") }
    var editCustom by remember { mutableStateOf(false) }
    val price = entry?.priceGp ?: item?.priceGp
    val weight = entry?.weightLb ?: item?.weightLb

    Column(
        Modifier.fillMaxWidth().then(if (sheet) Modifier else Modifier.verticalScroll(rememberScrollState()))
            .padding(start = if (aside) 28.dp else 18.dp, end = if (aside) 28.dp else 18.dp, top = 4.dp, bottom = if (sheet) 22.dp else 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showTitle && item != null) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.name, Modifier.weight(1f), fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 30.sp, color = Nb.ink)
                Text(priceLabel(price, revealed), fontFamily = fonts.mono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Nb.ink)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val cat = item?.category ?: entry?.cat ?: ItemCategory.OTHER
            NbTag(cat.label)
            item?.subtitle?.split(" · ")
                ?.filterNot { it.take(5).equals(cat.label.take(5), ignoreCase = true) } // „Wondrous Item“ vs „Wondrous items“
                ?.forEach { NbTag(it) }
            item?.damageType?.let { NbTag(it, Nb.rust) }
            if (item == null) NbTag("Custom item", Nb.rust)
        }
        NbPanel(Modifier.fillMaxWidth(), color = if (aside) Nb.paper else if (sheet) Nb.paper else Nb.panel) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Stat("Cost", priceLabel(price, revealed))
                Stat("Weight", formatWeight(weight))
                Stat("Damage (S)", item?.damageSmall)
                Stat("Damage (M)", item?.damageMedium)
                Stat("Critical", item?.critical)
                Stat("Range", item?.range)
                Stat("Type", item?.damageType)
                Stat("Special", item?.special)
                Stat("Armor bonus", item?.armorBonus?.let { "+$it" })
                Stat("Max Dex", item?.maxDex?.let { "+$it" })
                Stat("Check penalty", item?.armorCheck?.let { if (it == 0) "0" else "−${kotlin.math.abs(it)}" })
                Stat("Spell failure", item?.spellFailure?.let { "$it%" })
                Stat("Aura", item?.aura)
                Stat("Caster level", item?.casterLevel)
                Stat("Slot", item?.slot)
                Stat("Requirements", item?.requirements)
                if (item == null) Stat("Info", entry?.info)
            }
        }
        if (item != null && item.paragraphs.isNotEmpty()) {
            SectionHeader("Description")
            item.paragraphs.forEach { Text(it, fontSize = if (aside) 15.5.sp else 14.5.sp, lineHeight = 23.sp, color = Nb.ink) }
        }
        item?.source?.let {
            val owned = inv?.entries?.filter { it.itemId == item.id }?.sumOf { it.qty } ?: 0
            Text("Source: $it" + if (owned > 0 && entry == null) " · You own $owned" else "", fontSize = 13.sp, color = Nb.inkMuted)
        }

        if (entry != null && inv != null) {
            SectionHeader("In inventory")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quantity", Modifier.weight(1f), fontSize = 15.sp, color = Nb.ink)
                CountStepper(entry.qty, { state.setEntryQty(inv.id, entry.id, it) }, min = 0, max = 9999, size = 32.dp, numberSize = 19.sp)
            }
            OutlinedTextField(
                value = entry.note,
                onValueChange = { t -> state.updateEntry(inv.id, entry.id) { it.copy(note = t) } },
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. masterwork, found in crypt") },
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item == null) NbDashedButton("Edit item", { editCustom = true }, icon = NbIcons.Pencil)
                NbDashedButton("Remove from inventory", { state.removeEntry(inv.id, entry.id) }, icon = NbIcons.Trash)
            }
        } else if (item != null && inv != null) {
            SectionHeader("Add to inventory")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quantity", Modifier.weight(1f), fontSize = 15.sp, color = Nb.ink)
                CountStepper(qty, { qty = it }, min = 1, max = 999, size = 32.dp, numberSize = 19.sp)
            }
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Note (optional)") }, placeholder = { Text("e.g. masterwork, found in crypt") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            NbPrimaryButton(
                "Add to inventory",
                {
                    state.addItem(inv.id, item, qty, note)
                    qty = 1; note = ""
                    onAdded()
                },
                Modifier.fillMaxWidth(), height = 48.dp,
            )
        }
    }

    if (editCustom && entry != null && inv != null) {
        CustomItemDialog(entry, onDismiss = { editCustom = false }) { updated ->
            editCustom = false
            state.updateEntry(inv.id, entry.id) { updated.copy(id = entry.id, qty = entry.qty, note = entry.note) }
        }
    }
}

@Composable
private fun Stat(label: String, value: String?) {
    if (value.isNullOrBlank() || value == "—" && label != "Cost" && label != "Weight") return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.width(120.dp).padding(top = 1.dp), fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Nb.rust)
        Text(value, Modifier.weight(1f), fontSize = 14.sp, color = Nb.ink)
    }
}

// ---------- vlastní item ----------

@Composable
fun CustomItemDialog(existing: InvEntry?, onDismiss: () -> Unit, onSave: (InvEntry) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var cat by remember { mutableStateOf(existing?.cat ?: ItemCategory.GEAR) }
    var price by remember { mutableStateOf(existing?.priceGp?.toString()?.removeSuffix(".0") ?: "") }
    var weight by remember { mutableStateOf(existing?.weightLb?.toString()?.removeSuffix(".0") ?: "") }
    var info by remember { mutableStateOf(existing?.info ?: "") }
    var qty by remember { mutableIntStateOf(existing?.qty ?: 1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(if (existing == null) "Custom item" else "Edit item", color = Nb.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, placeholder = { Text("e.g. Scroll of Mage Armor") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SimpleDropdown("Category", ItemCategory.entries, cat, { it.label }, { cat = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(price, { t -> price = t.filter { it.isDigit() || it == '.' || it == ',' }.take(10) }, label = { Text("Price (gp)") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(weight, { t -> weight = t.filter { it.isDigit() || it == '.' }.take(8) }, label = { Text("Weight (lb)") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                OutlinedTextField(info, { info = it }, label = { Text("Short info (optional)") }, placeholder = { Text("e.g. 1d8+1, CL 1") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (existing == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel("Quantity", Modifier.weight(1f))
                        CountStepper(qty, { qty = it }, min = 1, max = 999, size = 32.dp)
                    }
                }
            }
        },
        confirmButton = {
            NbPrimaryButton(
                if (existing == null) "Add" else "Save",
                {
                    onSave(InvEntry(
                        id = existing?.id ?: newEntryId(), itemId = null, name = name.trim(), category = cat.name, qty = qty,
                        priceGp = price.replace(",", "").toDoubleOrNull(), weightLb = weight.toDoubleOrNull(),
                        note = existing?.note ?: "", info = info.trim().ifEmpty { null },
                    ))
                },
                icon = null, enabled = name.isNotBlank(),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nb.ink) } },
    )
}
