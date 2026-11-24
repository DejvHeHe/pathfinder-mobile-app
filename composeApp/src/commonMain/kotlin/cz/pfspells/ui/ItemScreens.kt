package cz.pfspells.ui

import cz.pfspells.data.isCustom
import cz.pfspells.data.CUSTOM_ITEM_PREFIX
import cz.pfspells.data.CustomItem
import cz.pfspells.UiMessage
import cz.pfspells.DbState
import cz.pfspells.Strings
import cz.pfspells.str
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
                Text(str.couldNotLoadItems(s.message), color = Nb.error, textAlign = TextAlign.Center)
                NbPrimaryButton(str.useBundledData, { state.resetItems() }, icon = null)
            }
        }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Nb.accent)
                Text(str.loadingItems, Modifier.padding(top = 12.dp), color = Nb.inkMuted)
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
    var fromSpell by remember { mutableStateOf(false) }
    val spellDb = (state.db as? DbState.Ready)?.db
    val back: () -> Unit = { if (wide) state.openRoot(Screen.InventoryDetail(invId)) else state.pop() }

    WithItems(state) { db ->
        val filter = state.itemFilter
        val set: (ItemFilter) -> Unit = { state.itemFilter = it }
        val customs = state.customItems.map { it.toItem() }
        val results = remember(db, filter, customs) { filterItems(db, customs, filter) }
        val owned = inv.entries.filter { it.itemId != null }.groupingBy { it.itemId!! }.fold(0) { a, e -> a + e.qty }
        val ownedCustom = inv.entries.filter { it.itemId == null }.groupingBy { it.name }.fold(0) { a, e -> a + e.qty }
        val s = str

        NbScreen(
            title = s.addItem,
            subtitle = s.itemPickerSubtitle(db.items.size, inv.name),
            onBack = back,
            wide = wide,
            actions = {
                if (wide) {
                    if (spellDb != null) NbDashedButton(s.fromSpell, { fromSpell = true }, icon = NbIcons.Book)
                    Spacer(Modifier.width(8.dp))
                    NbDashedButton(s.customItem, { custom = true }, icon = NbIcons.Pencil)
                }
            },
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = if (wide) 0.dp else 16.dp).padding(top = if (wide) 14.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NbSearchField(filter.query, { set(filter.copy(query = it)) }, s.searchItems(db.items.size), Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        NbFilterChip(s.all, filter.category == null && !filter.mine) {
                            set(filter.copy(category = null, proficiency = emptySet(), mine = false))
                        }
                    }
                    item { NbFilterChip(s.myItems, filter.mine) { set(filter.copy(mine = !filter.mine, category = null, proficiency = emptySet())) } }
                    items(ItemCategory.entries.filter { (db.counts[it] ?: 0) > 0 }) { c ->
                        NbFilterChip(s.category(c), filter.category == c) {
                            set(filter.copy(category = if (filter.category == c) null else c, proficiency = emptySet(), mine = false))
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (filter.category == ItemCategory.WEAPON) {
                        listOf("Simple", "Martial", "Exotic").forEach { p ->
                            NbFilterChip(s.proficiency(p), p in filter.proficiency) {
                                set(filter.copy(proficiency = if (p in filter.proficiency) filter.proficiency - p else filter.proficiency + p))
                            }
                        }
                    }
                    MaxPriceField(filter.maxPrice) { set(filter.copy(maxPrice = it)) }
                    if (!wide) NbDashedButton(s.customItem, { custom = true }, icon = NbIcons.Pencil)
                    if (!wide && spellDb != null) NbDashedButton(s.fromSpell, { fromSpell = true }, icon = NbIcons.Book)
                }
                Text(resultsSummary(s, results.size, filter), fontSize = 14.sp, color = Nb.inkMuted)
                if (filter.mine && state.customItems.isEmpty()) {
                    Text(s.myItemsEmpty, fontSize = 14.sp, lineHeight = 20.sp, color = Nb.inkMuted, modifier = Modifier.padding(vertical = 12.dp))
                }
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
                        item = item, wide = wide, weaponCols = weaponCols,
                        owned = (if (item.isCustom) ownedCustom[item.name] else owned[item.id]) ?: 0,
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
        CustomItemDialog(null, onDismiss = { custom = false }) { entry, saveToCatalog ->
            custom = false
            state.addCustomEntry(invId, entry)
            if (saveToCatalog) state.saveCustomItem(CustomItem.fromEntry(entry))
        }
    }
    if (fromSpell && spellDb != null) {
        SpellItemDialog(state, spellDb, onDismiss = { fromSpell = false }) { entry ->
            fromSpell = false
            state.addCustomEntry(invId, entry)
        }
    }
}

/** Vlastní katalog je v seznamu nahoře; s filtrem „My items“ jen on. */
private fun filterItems(db: ItemDatabase, customs: List<Item>, f: ItemFilter): List<Item> {
    val q = f.query.trim().lowercase()
    val pool = if (f.mine) customs else customs + db.items
    return pool.filter { i ->
        (f.category == null || i.category == f.category) &&
            (f.proficiency.isEmpty() || i.proficiency in f.proficiency) &&
            (f.maxPrice == null || (i.priceGp != null && i.priceGp <= f.maxPrice)) &&
            (q.isEmpty() || i.searchName.contains(q))
    }
}

private fun resultsSummary(s: Strings, n: Int, f: ItemFilter) = buildList {
    add(s.results(n))
    if (f.mine) add(s.myItems)
    f.category?.let { add(s.category(it)) }
    if (f.proficiency.isNotEmpty()) add(f.proficiency.sorted().joinToString(", ") { s.proficiency(it) })
    f.maxPrice?.let { add(s.upTo(formatGp(it))) }
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
        Text(str.maxPrice, fontFamily = fonts.mono, fontSize = 12.sp, color = Nb.inkMuted)
        Box(Modifier.width(64.dp)) {
            if (text.isEmpty()) Text(str.anyPrice, fontFamily = fonts.mono, fontSize = 13.sp, color = Nb.placeholder)
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
        MonoLabel(str.colName, Modifier.weight(2.2f))
        MonoLabel(str.colType, Modifier.weight(2f))
        if (weaponCols) {
            MonoLabel(str.colDmg, Modifier.weight(0.8f))
            MonoLabel(str.colCrit, Modifier.weight(1.1f))
        }
        MonoLabel(str.colWeight, Modifier.weight(0.8f))
        MonoLabel(str.colPrice, Modifier.weight(0.9f))
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun PickerRow(
    item: Item, wide: Boolean, weaponCols: Boolean, owned: Int, revealed: Boolean, selected: Boolean,
    onClick: () -> Unit, onAdd: () -> Unit,
) {
    val mono = LocalNbFonts.current.mono
    val s = str
    val base = Modifier.fillMaxWidth()
    val mod = (if (selected) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule(Nb.ruleSoft))
        .clickable(onClick = onClick)
        .padding(horizontal = if (wide) 10.dp else 0.dp, vertical = if (wide) 6.dp else 5.dp)
    val addButton = @Composable {
        NbSquareButton("+", onAdd, s.addNamed(item.name), size = if (wide) 36.dp else 40.dp) {
            Icon(NbIcons.Plus, null, tint = Nb.ink, modifier = Modifier.size(16.dp))
        }
    }
    if (wide) {
        Row(mod, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, Modifier.weight(1f, fill = false), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (owned > 0) MonoLabel(s.owned(owned), color = Nb.rust, size = 10.5.sp)
            }
            Text(item.subtitle ?: s.category(item.category), Modifier.weight(2f), fontSize = 14.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        if (owned > 0) append(" · ").append(s.owned(owned))
                    },
                    fontFamily = mono, fontSize = 12.sp, color = if (owned > 0) Nb.rust else Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(item.subtitle ?: s.category(item.category), fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val customItem = itemId?.takeIf { it.startsWith(CUSTOM_ITEM_PREFIX) }?.let { state.findItem(it) }
    if (itemId != null && itemId.startsWith(CUSTOM_ITEM_PREFIX) && customItem == null && entry == null) {
        LaunchedEffect(itemId) { state.pop() } // item byl odebrán z katalogu
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
                entry?.name ?: itemId?.let { state.findItem(it)?.name } ?: str.itemFallback, Modifier.weight(1f),
                fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = if (aside) 32.sp else 24.sp,
                lineHeight = if (aside) 35.sp else 27.sp, color = Nb.ink, maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
            if (aside) NbIconButton(NbIcons.Close, str.closeDetails, { state.pop() }, Nb.inkMuted)
        }
        if (itemId == null) {
            ItemDetailContent(state, null, inv, entry, aside, sheet = false, showTitle = false)
        } else if (customItem != null) {
            ItemDetailContent(state, customItem, inv, entry, aside, sheet = false, showTitle = false)
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
    val s = str

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
            NbTag(s.category(cat))
            item?.subtitle?.split(" · ")
                ?.filterNot { it.take(5).equals(cat.label.take(5), ignoreCase = true) } // „Wondrous Item“ vs „Wondrous items“
                ?.forEach { NbTag(it) }
            item?.damageType?.let { NbTag(it, Nb.rust) }
            if (item == null || item.isCustom) NbTag(if (item?.isCustom == true) s.myItems else s.customItem, Nb.rust)
        }
        NbPanel(Modifier.fillMaxWidth(), color = if (aside) Nb.paper else if (sheet) Nb.paper else Nb.panel) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Stat(s.cost, priceLabel(price, revealed), always = true)
                Stat(s.weight, formatWeight(weight), always = true)
                Stat(s.damageS, item?.damageSmall)
                Stat(s.damageM, item?.damageMedium)
                Stat(s.critical, item?.critical)
                Stat(s.range, item?.range)
                Stat(s.type, item?.damageType)
                Stat(s.special, item?.special)
                Stat(s.armorBonus, item?.armorBonus?.let { "+$it" })
                Stat(s.maxDex, item?.maxDex?.let { "+$it" })
                Stat(s.checkPenalty, item?.armorCheck?.let { if (it == 0) "0" else "−${kotlin.math.abs(it)}" })
                Stat(s.spellFailure, item?.spellFailure?.let { "$it%" })
                Stat(s.aura, item?.aura)
                Stat(s.casterLevel, item?.casterLevel)
                Stat(s.slot, item?.slot)
                Stat(s.requirements, item?.requirements)
                if (item == null) Stat(s.info, entry?.info)
            }
        }
        if (item != null && item.paragraphs.isNotEmpty()) {
            SectionHeader(s.description)
            item.paragraphs.forEach { Text(it, fontSize = if (aside) 15.5.sp else 14.5.sp, lineHeight = 23.sp, color = Nb.ink) }
        }
        item?.source?.let {
            val owned = inv?.entries?.filter { it.itemId == item.id }?.sumOf { it.qty } ?: 0
            Text(s.sourceLine(it) + if (owned > 0 && entry == null) " · ${s.youOwn(owned)}" else "", fontSize = 13.sp, color = Nb.inkMuted)
        }

        if (entry != null && inv != null) {
            SectionHeader(s.inInventory)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.quantity, Modifier.weight(1f), fontSize = 15.sp, color = Nb.ink)
                CountStepper(entry.qty, { state.setEntryQty(inv.id, entry.id, it) }, min = 0, max = 9999, size = 32.dp, numberSize = 19.sp)
            }
            OutlinedTextField(
                value = entry.note,
                onValueChange = { t -> state.updateEntry(inv.id, entry.id) { it.copy(note = t) } },
                label = { Text(s.noteOptional) },
                placeholder = { Text(s.notePlaceholder) },
                modifier = Modifier.fillMaxWidth(),
            )
            EquipmentSection(state, inv, entry)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item == null) NbDashedButton(s.editItem, { editCustom = true }, icon = NbIcons.Pencil)
                if (item == null && state.customItems.none { it.name == entry.name }) {
                    NbDashedButton(s.saveToMyItems, {
                        state.saveCustomItem(CustomItem.fromEntry(entry))
                        state.showToast(UiMessage { it.savedToMyItems })
                    }, icon = NbIcons.Plus)
                }
                NbDashedButton(s.removeFromInventory, { state.removeEntry(inv.id, entry.id) }, icon = NbIcons.Trash)
            }
        } else if (item != null && inv != null) {
            SectionHeader(s.addToInventory)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.quantity, Modifier.weight(1f), fontSize = 15.sp, color = Nb.ink)
                CountStepper(qty, { qty = it }, min = 1, max = 999, size = 32.dp, numberSize = 19.sp)
            }
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text(s.noteOptional) }, placeholder = { Text(s.notePlaceholder) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            NbPrimaryButton(
                s.addToInventory,
                {
                    state.addItem(inv.id, item, qty, note)
                    qty = 1; note = ""
                    onAdded()
                },
                Modifier.fillMaxWidth(), height = 48.dp,
            )
            if (item.isCustom) {
                NbDashedButton(s.removeFromMyItems, {
                    state.deleteCustomItem(item.id.removePrefix(CUSTOM_ITEM_PREFIX))
                    onAdded()
                }, icon = NbIcons.Trash)
            }
        }
    }

    if (editCustom && entry != null && inv != null) {
        CustomItemDialog(entry, onDismiss = { editCustom = false }) { updated, _ ->
            editCustom = false
            state.updateEntry(inv.id, entry.id) {
                updated.copy(id = entry.id, qty = entry.qty, note = entry.note, equipped = entry.equipped, slot = entry.slot, bonuses = entry.bonuses)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String?, always: Boolean = false) {
    if (value.isNullOrBlank() || value == "—" && !always) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.width(120.dp).padding(top = 1.dp), fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Nb.rust)
        Text(value, Modifier.weight(1f), fontSize = 14.sp, color = Nb.ink)
    }
}

// ---------- vlastní item ----------

@Composable
fun CustomItemDialog(existing: InvEntry?, onDismiss: () -> Unit, onSave: (InvEntry, saveToCatalog: Boolean) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var cat by remember { mutableStateOf(existing?.cat ?: ItemCategory.GEAR) }
    var price by remember { mutableStateOf(existing?.priceGp?.toString()?.removeSuffix(".0") ?: "") }
    var weight by remember { mutableStateOf(existing?.weightLb?.toString()?.removeSuffix(".0") ?: "") }
    var info by remember { mutableStateOf(existing?.info ?: "") }
    var qty by remember { mutableIntStateOf(existing?.qty ?: 1) }
    var saveToCatalog by remember { mutableStateOf(true) }
    val s = str
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(if (existing == null) s.customItem else s.editItem, color = Nb.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(s.name) }, placeholder = { Text(s.itemNamePlaceholder) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                SimpleDropdown(s.categoryLabel, ItemCategory.entries, cat, { s.category(it) }, { cat = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(price, { t -> price = t.filter { it.isDigit() || it == '.' || it == ',' }.take(10) }, label = { Text(s.priceGp) }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(weight, { t -> weight = t.filter { it.isDigit() || it == '.' }.take(8) }, label = { Text(s.weightLb) }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                OutlinedTextField(info, { info = it }, label = { Text(s.shortInfo) }, placeholder = { Text(s.shortInfoPlaceholder) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (existing == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel(s.quantity, Modifier.weight(1f))
                        CountStepper(qty, { qty = it }, min = 1, max = 999, size = 32.dp)
                    }
                    NbCheck(saveToCatalog, s.saveToMyItems) { saveToCatalog = it }
                }
            }
        },
        confirmButton = {
            NbPrimaryButton(
                if (existing == null) s.add else s.save,
                {
                    onSave(InvEntry(
                        id = existing?.id ?: newEntryId(), itemId = null, name = name.trim(), category = cat.name, qty = qty,
                        priceGp = price.replace(",", "").toDoubleOrNull(), weightLb = weight.toDoubleOrNull(),
                        note = existing?.note ?: "", info = info.trim().ifEmpty { null },
                    ), existing == null && saveToCatalog)
                },
                icon = null, enabled = name.isNotBlank(),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}
