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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.Screen
import cz.pfspells.Section
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Deck
import cz.pfspells.data.SpellDatabase

/** Mobilní úvodní obrazovka se seznamem decků. */
@Composable
fun DeckListScreen(state: AppState, db: SpellDatabase) {
    var showNew by remember { mutableStateOf(false) }
    val fonts = LocalNbFonts.current

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "PF1 Spellbook",
                    Modifier.weight(1f),
                    fontFamily = fonts.serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold,
                    fontSize = 28.sp, color = Nb.accent,
                )
                if (state.section == Section.SPELLBOOK) {
                    NbIconButton(NbIcons.Book, "Spell compendium", { state.push(Screen.Compendium) })
                }
                AppMenu(state, db)
            }
            SectionTabs(state, Modifier.padding(start = 14.dp, bottom = 2.dp))
            if (state.section == Section.INVENTORY) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    inventoryListItems(state) { inv -> state.push(Screen.InventoryDetail(inv.id)) }
                }
            } else if (state.decks.isEmpty()) {
                EmptyDecks(db, Modifier.weight(1f))
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { SectionHeader("My decks", size = 20.sp) }
                    items(state.decks, key = { it.id }) { deck ->
                        DeckItem(deck, selected = false, onClick = { state.push(Screen.DeckDetail(deck.id)) }, card = true)
                    }
                }
            }
        }
        NbFab(
            if (state.section == Section.INVENTORY) "New inventory" else "New deck",
            { showNew = true },
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp),
        )
    }

    if (showNew && state.section == Section.INVENTORY) {
        TextFieldDialog("New inventory", "", "Name (character / stash)", onDismiss = { showNew = false }) { name ->
            showNew = false
            val inv = state.createInventory(name)
            state.push(Screen.InventoryDetail(inv.id))
        }
    } else if (showNew) {
        NewDeckDialog(
            onDismiss = { showNew = false },
            onCreate = { name, classId, level ->
                showNew = false
                val deck = state.createDeck(name, classId, level)
                state.push(Screen.DeckDetail(deck.id))
            },
        )
    }
}

/** Boční panel širokého okna. */
@Composable
fun DeckSidebar(state: AppState, selectedDeckId: String?, modifier: Modifier = Modifier) {
    var showNew by remember { mutableStateOf(false) }
    LazyColumn(
        modifier,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionHeader("My decks", size = 20.sp) }
        if (state.decks.isEmpty()) {
            item { Text("No decks yet.", color = Nb.inkMuted, fontSize = 14.sp) }
        }
        items(state.decks, key = { it.id }) { deck ->
            DeckItem(deck, selected = deck.id == selectedDeckId, onClick = { state.openRoot(Screen.DeckDetail(deck.id)) })
        }
        item {
            NbDashedButton("New deck", { showNew = true }, Modifier.padding(top = 8.dp), icon = NbIcons.Plus)
        }
    }
    if (showNew) {
        NewDeckDialog(
            onDismiss = { showNew = false },
            onCreate = { name, classId, level ->
                showNew = false
                val deck = state.createDeck(name, classId, level)
                state.openRoot(Screen.DeckDetail(deck.id))
            },
        )
    }
}

@Composable
private fun DeckItem(deck: Deck, selected: Boolean, onClick: () -> Unit, card: Boolean = false) {
    val cls = CasterClasses.get(deck.classId)
    val fonts = LocalNbFonts.current
    val base = Modifier.fillMaxWidth().clip(Nb.shape)
    Column(
        (if (selected || card) base.background(if (card) Nb.panel else Nb.inset).dashedBorder() else base)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (card) 14.dp else 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                deck.name, Modifier.weight(1f),
                fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = if (card) 22.sp else 18.sp,
                color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (selected) MonoLabel("Open", color = Nb.rust, size = 11.sp)
        }
        Text(
            "${cls?.name ?: deck.classId} ${deck.classLevel} · ${spellsCount(deck.spells.size)}",
            fontSize = 13.5.sp, color = Nb.inkMuted,
        )
        if (card && cls != null) {
            Text(spellRangeText(cls, deck.classLevel), fontSize = 13.sp, color = Nb.rust, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun EmptyDecks(db: SpellDatabase, modifier: Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "You have no decks yet.\nCreate one for your character's class and level – only spells they can cast will be offered.\nLoad an exported deck via the menu (top right) → Import deck.\n\n${db.spells.size} spells in the database.",
            textAlign = TextAlign.Center,
            color = Nb.inkMuted,
            fontSize = 15.sp,
        )
    }
}

/** Menu ⋮: import, export, databáze. */
@Composable
fun AppMenu(state: AppState, db: SpellDatabase, onDatabase: () -> Unit = { state.push(Screen.Database) }) {
    var open by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showInvImport by remember { mutableStateOf(false) }
    var showInvExport by remember { mutableStateOf(false) }
    Box {
        NbIconButton(NbIcons.Menu, "Menu", { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Nb.panel) {
            DropdownMenuItem(text = { Text("Import deck", color = Nb.ink) }, onClick = { open = false; showImport = true })
            if (state.decks.isNotEmpty()) {
                DropdownMenuItem(text = { Text("Export all decks", color = Nb.ink) }, onClick = { open = false; showExport = true })
            }
            DropdownMenuItem(text = { Text("Import inventory", color = Nb.ink) }, onClick = { open = false; state.ensureItems(); showInvImport = true })
            if (state.inventories.isNotEmpty()) {
                DropdownMenuItem(text = { Text("Export all inventories", color = Nb.ink) }, onClick = { open = false; showInvExport = true })
            }
            DropdownMenuItem(text = { Text("Spell & item database / update", color = Nb.ink) }, onClick = { open = false; onDatabase() })
        }
    }
    if (showImport) ImportDialog(state, db) { showImport = false }
    if (showExport) ExportDialog(state.decks.toList(), db) { showExport = false }
    if (showInvImport) InventoryImportDialog(state) { showInvImport = false }
    if (showInvExport) InventoryExportDialog(state.inventories.toList()) { showInvExport = false }
}

@Composable
fun NewDeckDialog(onDismiss: () -> Unit, onCreate: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cls by remember { mutableStateOf(CasterClasses.all.first()) }
    var level by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text("New deck", color = Nb.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (character / build)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                SimpleDropdown(
                    label = "Class",
                    options = CasterClasses.all,
                    selected = cls,
                    optionLabel = { it.name },
                    onSelect = { cls = it },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel("Character level", Modifier.weight(1f))
                    CountStepper(level, { level = it }, min = 1, max = 20, numberSize = 18.sp)
                }
                Text(classLevelSummary(cls, level), fontSize = 13.sp, color = Nb.rust)
            }
        },
        confirmButton = {
            NbPrimaryButton("Create", { onCreate(name.trim().ifEmpty { "${cls.name} $level" }, cls.id, level) }, icon = null)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nb.ink) } },
    )
}
