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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Spell
import cz.pfspells.data.SpellDatabase

@Composable
fun DeckDetailScreen(
    state: AppState,
    db: SpellDatabase,
    deckId: String,
    wide: Boolean = false,
    selectedSpellId: String? = null,
) {
    val deck = state.deck(deckId)
    if (deck == null) {
        LaunchedEffect(deckId) { if (wide) state.openRoot(Screen.Decks) else state.pop() }
        return
    }
    val cls = CasterClasses.get(deck.classId)
    var confirmDelete by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var export by remember { mutableStateOf(false) }
    var levelFilter by remember(deckId) { mutableStateOf<Int?>(null) }
    if (export) ExportDialog(listOf(deck), db) { export = false }

    val entries = deck.spells.mapNotNull { (id, count) -> db.get(id)?.let { it to count } }
    val missing = deck.spells.keys.count { db.get(it) == null }
    val grouped: List<Pair<Int, List<Pair<Spell, Int>>>> = entries
        .groupBy { (spell, _) -> cls?.spellLevelOf(spell) ?: -1 }
        .map { (lvl, list) -> lvl to list.sortedBy { it.first.searchName } }
        .sortedBy { it.first }
    val maxLevel = cls?.maxSpellLevel(deck.classLevel) ?: -1
    val copies = deck.spells.values.sum()
    val subtitle = if (wide) {
        "${cls?.name ?: deck.classId} · ${cls?.let { spellRangeText(it, deck.classLevel) } ?: ""} · ${spellsCount(deck.spells.size)}, ${copiesCount(copies)}"
    } else {
        "${cls?.name ?: deck.classId} ${deck.classLevel} · ${spellsCount(deck.spells.size)}"
    }

    NbScreen(
        title = deck.name,
        subtitle = subtitle,
        onBack = if (wide) null else ({ state.pop() }),
        wide = wide,
        primary = "Add spells" to {
            if (wide) state.openRoot(Screen.Picker(deckId)) else state.push(Screen.Picker(deckId))
        },
        headerExtra = {
            if (wide) {
                Row(
                    Modifier.clip(Nb.shape).background(Nb.panel).dashedBorder().padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MonoLabel("Level")
                    CountStepper(deck.classLevel, { lvl -> state.updateDeck(deckId) { it.copy(classLevel = lvl) } }, 1, 20, size = 32.dp, numberSize = 19.sp)
                }
            }
        },
        actions = {
            NbIconButton(NbIcons.Share, "Export deck", { export = true }, Nb.inkMuted, 19.dp)
            NbIconButton(NbIcons.Pencil, "Rename", { rename = true }, Nb.inkMuted, 19.dp)
            NbIconButton(NbIcons.Trash, "Delete deck", { confirmDelete = true }, Nb.inkMuted, 19.dp)
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = if (wide) 0.dp else 16.dp, end = if (wide) 0.dp else 16.dp, top = 6.dp, bottom = 96.dp),
        ) {
            if (!wide) {
                item {
                    NbPanel(Modifier.fillMaxWidth().padding(bottom = 4.dp), color = Nb.inset) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                MonoLabel("Character level")
                                Text(
                                    cls?.let { spellRangeText(it, deck.classLevel).replaceFirstChar { c -> c.uppercase() } } ?: "",
                                    fontSize = 14.sp, color = Nb.ink, modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                            CountStepper(deck.classLevel, { lvl -> state.updateDeck(deckId) { it.copy(classLevel = lvl) } }, 1, 20, numberSize = 20.sp)
                        }
                    }
                }
            } else if (grouped.size > 1) {
                item {
                    Row(Modifier.padding(top = 16.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NbFilterChip("All", levelFilter == null) { levelFilter = null }
                        grouped.map { it.first }.filter { it >= 0 }.forEach { lvl ->
                            NbFilterChip(if (lvl == 0) "0" else "L$lvl", levelFilter == lvl) { levelFilter = if (levelFilter == lvl) null else lvl }
                        }
                    }
                }
            }
            if (missing > 0) {
                item {
                    Text(
                        "$missing spells from this deck are no longer in the database (e.g. after an update).",
                        color = Nb.error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            if (entries.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("This deck is empty. Add spells with \"Add spells\".", textAlign = TextAlign.Center, color = Nb.inkMuted)
                    }
                }
            }
            grouped.filter { levelFilter == null || it.first == levelFilter }.forEach { (level, list) ->
                item(key = "h$level") {
                    SectionHeader(if (level < 0) "Not on the class spell list" else spellLevelLabel(level))
                }
                items(list, key = { it.first.id }) { (spell, count) ->
                    DeckSpellRow(
                        spell = spell,
                        count = count,
                        tooHigh = level > maxLevel,
                        selected = spell.id == selectedSpellId,
                        wide = wide,
                        onClick = { state.showSpell(spell.id, deckId) },
                        onCount = { state.setSpellCount(deckId, spell.id, it) },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Nb.panel,
            title = { Text("Delete deck?", color = Nb.ink) },
            text = { Text("Deck \"${deck.name}\" will be permanently deleted.", color = Nb.ink) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (wide) state.openRoot(Screen.Decks) else state.pop()
                    state.deleteDeck(deckId)
                }) { Text("Delete", color = Nb.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = Nb.ink) } },
        )
    }

    if (rename) {
        var text by remember { mutableStateOf(deck.name) }
        AlertDialog(
            onDismissRequest = { rename = false },
            containerColor = Nb.panel,
            title = { Text("Rename deck", color = Nb.ink) },
            text = { OutlinedTextField(text, { text = it }, singleLine = true) },
            confirmButton = {
                NbPrimaryButton("Save", {
                    rename = false
                    if (text.isNotBlank()) state.updateDeck(deckId) { it.copy(name = text.trim()) }
                }, icon = null)
            },
            dismissButton = { TextButton(onClick = { rename = false }) { Text("Cancel", color = Nb.ink) } },
        )
    }
}

/** „Evocation [fire] · 1 standard action“ */
fun spellSubline(spell: Spell): String = buildString {
    append(spell.school.replaceFirstChar { it.uppercase() })
    spell.subschool?.let { append(" ($it)") }
    spell.descriptor?.let { append(" [$it]") }
    spell.castingTime?.let { append(" · ").append(it) }
}

@Composable
private fun DeckSpellRow(
    spell: Spell,
    count: Int,
    tooHigh: Boolean,
    selected: Boolean,
    wide: Boolean,
    onClick: () -> Unit,
    onCount: (Int) -> Unit,
) {
    val fonts = LocalNbFonts.current
    val base = Modifier.fillMaxWidth()
    val rowMod = if (selected) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule(if (wide) Nb.ruleSoft else Nb.rule)
    Row(
        rowMod.clickable(onClick = onClick).padding(horizontal = if (wide) 10.dp else 0.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val name: @Composable (Modifier) -> Unit = { m ->
            Row(m, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    spell.name, Modifier.weight(1f, fill = false),
                    fontSize = if (wide) 16.sp else 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (tooHigh) Icon(NbIcons.Warn, "Not available at this level", tint = Nb.error, modifier = Modifier.padding(start = 6.dp).size(16.dp))
            }
        }
        if (wide) {
            name(Modifier.weight(2f))
            Text(
                buildString {
                    append(spell.school.replaceFirstChar { it.uppercase() })
                    spell.subschool?.let { append(" ($it)") }
                    spell.descriptor?.let { append(" [$it]") }
                },
                Modifier.weight(2f), fontSize = 14.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                spell.castingTime?.replace("standard", "std.") ?: "",
                Modifier.weight(1f), fontFamily = fonts.mono, fontSize = 12.5.sp, color = Nb.inkMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            CountStepper(count, onCount, size = 32.dp, numberSize = 14.sp)
        } else {
            Column(Modifier.weight(1f)) {
                name(Modifier)
                Text(spellSubline(spell), fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
            }
            CountStepper(count, onCount)
        }
    }
}
