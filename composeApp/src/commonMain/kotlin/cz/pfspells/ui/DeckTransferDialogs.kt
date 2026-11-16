package cz.pfspells.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cz.pfspells.AppState
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Deck
import cz.pfspells.data.DeckImportException
import cz.pfspells.data.DeckTransfer
import cz.pfspells.data.Inventory
import cz.pfspells.data.InventoryTransfer
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.deckFileIo
import kotlinx.coroutines.launch

// ---------- konkrétní použití ----------

@Composable
fun ExportDialog(decks: List<Deck>, db: SpellDatabase, onDismiss: () -> Unit) {
    val text = remember(decks, db) { DeckTransfer.export(decks, db) }
    TextExportDialog(
        title = if (decks.size == 1) "Export deck" else "Export ${decks.size} decks",
        hint = "The text below contains the deck(s) with all spells. Save or send it – load it on another device via \"Import deck\".",
        text = text,
        fileName = DeckTransfer.fileName(decks.singleOrNull()?.name ?: "pf-decks"),
        onDismiss = onDismiss,
    )
}

@Composable
fun ImportDialog(state: AppState, db: SpellDatabase, onDismiss: () -> Unit) {
    TextImportDialog(
        title = "Import deck",
        parse = { t ->
            val decks = DeckTransfer.parse(t, db)
            ImportPreview(
                rows = decks.map { (deck, missing) ->
                    ImportRow(
                        deck.name,
                        "${classLevelSummary(CasterClasses.get(deck.classId), deck.classLevel)} · ${spellsCount(deck.spells.size)}",
                        missing.takeIf { it.isNotEmpty() }?.let { "Not found in the database (will be skipped): ${it.joinToString()}" },
                    )
                },
                apply = { state.importDecks(decks.map { it.deck }) },
            )
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun InventoryExportDialog(list: List<Inventory>, onDismiss: () -> Unit) {
    val text = remember(list) { InventoryTransfer.export(list) }
    TextExportDialog(
        title = if (list.size == 1) "Export inventory" else "Export ${list.size} inventories",
        hint = "The text below contains the inventory with coins, items and notes. Load it on another device via \"Import inventory\".",
        text = text,
        fileName = InventoryTransfer.fileName(list.singleOrNull()?.name ?: "pf-inventories"),
        onDismiss = onDismiss,
    )
}

@Composable
fun InventoryImportDialog(state: AppState, onDismiss: () -> Unit) {
    TextImportDialog(
        title = "Import inventory",
        parse = { t ->
            val list = InventoryTransfer.parse(t, state.itemDb)
            ImportPreview(
                rows = list.map { inv ->
                    ImportRow(inv.name, "${itemTypesCount(inv.entries.size)} · ${formatWeight(inv.totalWeight)} · ${inv.gp} gp", null)
                },
                apply = { state.importInventories(list) },
            )
        },
        onDismiss = onDismiss,
    )
}

// ---------- obecné dialogy ----------

data class ImportRow(val title: String, val subtitle: String, val warning: String?)
class ImportPreview(val rows: List<ImportRow>, val apply: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextExportDialog(title: String, hint: String, text: String, fileName: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(hint, style = MaterialTheme.typography.bodySmall)
                Text(
                    text,
                    Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NbDashedButton("Copy", {
                        clipboard.setText(AnnotatedString(text))
                        message = "Copied to clipboard."
                    })
                    NbDashedButton(deckFileIo.saveLabel, {
                        scope.launch {
                            message = try {
                                if (deckFileIo.saveText(fileName, text)) "Done." else null
                            } catch (e: Exception) {
                                "Failed: ${e.message}"
                            }
                        }
                    }, icon = NbIcons.Share)
                }
                message?.let { Text(it, color = Nb.accent) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Nb.ink) } },
    )
}

/** [parse] vyhodí [DeckImportException] s hláškou pro uživatele. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextImportDialog(title: String, parse: (String) -> ImportPreview, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<ImportPreview?>(null) }

    fun check(t: String) {
        text = t
        preview = null
        error = null
        if (t.isBlank()) return
        try {
            preview = parse(t)
        } catch (e: DeckImportException) {
            error = e.message
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NbDashedButton("Paste from clipboard", { check(clipboard.getText()?.text ?: "") })
                    if (deckFileIo.canOpen) {
                        NbDashedButton("Open file", {
                            scope.launch {
                                try {
                                    deckFileIo.openText()?.let(::check)
                                } catch (e: Exception) {
                                    error = "Cannot read the file: ${e.message}"
                                }
                            }
                        })
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = ::check,
                    label = { Text("Export text") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                preview?.rows?.forEach { row ->
                    Column {
                        Text(row.title, style = MaterialTheme.typography.titleSmall)
                        Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        row.warning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = preview != null,
                onClick = {
                    preview?.apply?.invoke()
                    onDismiss()
                },
            ) { Text(preview?.rows?.takeIf { it.size > 1 }?.let { "Import (${it.size})" } ?: "Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nb.ink) } },
    )
}
