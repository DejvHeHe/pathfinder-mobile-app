package cz.pfspells.ui

import cz.pfspells.data.casterLevel
import cz.pfspells.data.caster
import cz.pfspells.data.Character
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import cz.pfspells.DbState
import cz.pfspells.Screen
import cz.pfspells.Section
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.SpellDatabase

/** Mobilní úvodní obrazovka se seznamem postav (záložka Spellbook) nebo inventářů. */
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
                LangToggle(state, Modifier.padding(end = 4.dp))
                if (state.section == Section.SPELLBOOK) {
                    NbIconButton(NbIcons.Book, str.spellCompendium, { state.push(Screen.Compendium) })
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
            } else if (state.characters.isEmpty()) {
                EmptyDecks(db, Modifier.weight(1f))
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { SectionHeader(str.myCharacters, size = 20.sp) }
                    items(state.rootCharacters, key = { it.id }) { c ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            CharacterItem(c, selected = false, onClick = { state.push(Screen.CharacterDetail(c.id)) }, card = true)
                            state.companionsOf(c.id).forEach { p ->
                                CompanionItem(p, selected = false) { state.push(Screen.CharacterDetail(p.id)) }
                            }
                        }
                    }
                }
            }
        }
        NbFab(
            if (state.section == Section.INVENTORY) str.newInventory else str.newCharacter,
            { showNew = true },
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp),
        )
    }

    if (showNew && state.section == Section.INVENTORY) {
        TextFieldDialog(str.newInventory, "", str.inventoryNameLabel, onDismiss = { showNew = false }) { name ->
            showNew = false
            val inv = state.createInventory(name)
            state.push(Screen.InventoryDetail(inv.id))
        }
    } else if (showNew) {
        ClassPickerDialog(
            str.newCharacter, str.create, withName = true,
            subclasses = (state.db as? DbState.Ready)?.db?.subclasses, onDismiss = { showNew = false },
        ) { p ->
            showNew = false
            val c = state.createCharacter(p.name, p.classId, p.level, p.subclass, p.race, p.raceBonus)
            if (p.deity.isNotBlank()) state.setDeity(c.id, p.deity)
            state.push(Screen.CharacterDetail(c.id))
        }
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
        item { SectionHeader(str.myCharacters, size = 20.sp) }
        if (state.characters.isEmpty()) {
            item { Text(str.noCharactersYet, color = Nb.inkMuted, fontSize = 14.sp) }
        }
        items(state.rootCharacters, key = { it.id }) { c ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CharacterItem(c, selected = c.id == selectedDeckId, onClick = { state.openRoot(Screen.CharacterDetail(c.id)) })
                state.companionsOf(c.id).forEach { p ->
                    CompanionItem(p, selected = p.id == selectedDeckId) { state.openRoot(Screen.CharacterDetail(p.id)) }
                }
            }
        }
        item {
            NbDashedButton(str.newCharacter, { showNew = true }, Modifier.padding(top = 8.dp), icon = NbIcons.Plus)
        }
    }
    if (showNew) {
        ClassPickerDialog(
            str.newCharacter, str.create, withName = true,
            subclasses = (state.db as? DbState.Ready)?.db?.subclasses, onDismiss = { showNew = false },
        ) { p ->
            showNew = false
            val c = state.createCharacter(p.name, p.classId, p.level, p.subclass, p.race, p.raceBonus)
            if (p.deity.isNotBlank()) state.setDeity(c.id, p.deity)
            state.openRoot(Screen.CharacterDetail(c.id))
        }
    }
}

@Composable
private fun CharacterItem(character: Character, selected: Boolean, onClick: () -> Unit, card: Boolean = false) {
    // spell levely ukazuje první sesílací classa
    val casterEntry = character.classes.firstOrNull { it.caster != null }
    val cls = casterEntry?.caster
    val fonts = LocalNbFonts.current
    val base = Modifier.fillMaxWidth().clip(Nb.shape)
    Column(
        (if (selected || card) base.background(if (card) Nb.panel else Nb.inset).dashedBorder() else base)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (card) 14.dp else 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                character.name, Modifier.weight(1f),
                fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = if (card) 22.sp else 18.sp,
                color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (selected) MonoLabel(str.open, color = Nb.rust, size = 11.sp)
        }
        Text(
            "${classesSummary(str, character)} · ${str.spells(character.classes.sumOf { it.spells.size })}",
            fontSize = 13.5.sp, color = Nb.inkMuted,
        )
        if (card && cls != null) {
            Text("${str.nameOf(cls)}: ${spellRangeText(str, cls, character.casterLevel(casterEntry))}", fontSize = 13.sp, color = Nb.rust, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** Mazlíček pod vlastníkem v seznamu postav: odsazený, menší, s druhem (zvířecí společník, familiár…). */
@Composable
private fun CompanionItem(c: Character, selected: Boolean, onClick: () -> Unit) {
    val s = str
    // sponka: mazlíček visí pod pánem jako připnutý lístek
    val base = Modifier.padding(start = 22.dp).fillMaxWidth().paperclip(right = 12.dp).clip(Nb.shape)
    Column(
        (if (selected) base.background(Nb.inset).dashedBorder() else base.background(Nb.panel.copy(alpha = 0.6f)))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                c.name, Modifier.weight(1f),
                fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (selected) MonoLabel(s.open, color = Nb.rust, size = 11.sp)
        }
        Text(
            listOfNotNull(c.companion?.let(s::companionKind), classesSummary(s, c)).joinToString(" · "),
            fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyDecks(db: SpellDatabase, modifier: Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            str.emptyCharacters(db.spells.size),
            textAlign = TextAlign.Center,
            color = Nb.inkMuted,
            fontSize = 15.sp,
        )
    }
}

/** Menu ⋮: import, export, databáze. */
@Composable
fun AppMenu(
    state: AppState,
    db: SpellDatabase,
    onDatabase: () -> Unit = { state.push(Screen.Database) },
    onAbout: () -> Unit = { state.push(Screen.About) },
) {
    var open by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var showInvImport by remember { mutableStateOf(false) }
    var showInvExport by remember { mutableStateOf(false) }
    Box {
        NbIconButton(NbIcons.Menu, str.menu, { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = Nb.panel) {
            DropdownMenuItem(text = { Text(str.importCharacter, color = Nb.ink) }, onClick = { open = false; showImport = true })
            if (state.characters.isNotEmpty()) {
                DropdownMenuItem(text = { Text(str.exportAllCharacters, color = Nb.ink) }, onClick = { open = false; showExport = true })
            }
            DropdownMenuItem(text = { Text(str.importInventory, color = Nb.ink) }, onClick = { open = false; state.ensureItems(); showInvImport = true })
            if (state.inventories.isNotEmpty()) {
                DropdownMenuItem(text = { Text(str.exportAllInventories, color = Nb.ink) }, onClick = { open = false; showInvExport = true })
            }
            // přísný režim: platí pro všechny postavy (menu zůstane otevřené, ať je vidět změna)
            DropdownMenuItem(
                text = {
                    Column {
                        Text(str.strictMode, color = Nb.ink)
                        Text(str.strictModeHint, color = Nb.inkMuted, fontSize = 12.sp, modifier = Modifier.widthIn(max = 260.dp))
                    }
                },
                trailingIcon = {
                    Checkbox(
                        state.strictMode, { state.changeStrictMode(it) },
                        colors = CheckboxDefaults.colors(checkedColor = Nb.accent, uncheckedColor = Nb.dash, checkmarkColor = Color.White),
                    )
                },
                onClick = { state.changeStrictMode(!state.strictMode) },
            )
            DropdownMenuItem(text = { Text(str.menuDatabase, color = Nb.ink) }, onClick = { open = false; onDatabase() })
            DropdownMenuItem(text = { Text(str.menuAbout, color = Nb.ink) }, onClick = { open = false; onAbout() })
        }
    }
    if (showImport) ImportDialog(state, db) { showImport = false }
    if (showExport) ExportDialog(state, state.characters.toList(), db) { showExport = false }
    if (showInvImport) InventoryImportDialog(state) { showInvImport = false }
    if (showInvExport) InventoryExportDialog(state.inventories.toList()) { showInvExport = false }
}
