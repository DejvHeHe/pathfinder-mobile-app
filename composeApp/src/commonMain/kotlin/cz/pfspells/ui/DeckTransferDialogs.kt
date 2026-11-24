package cz.pfspells.ui

import androidx.compose.runtime.LaunchedEffect
import cz.pfspells.data.CharacterTransfer
import cz.pfspells.data.toDeck
import cz.pfspells.data.toCharacter
import cz.pfspells.data.Character
import cz.pfspells.str
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
import cz.pfspells.data.DeckImportException
import cz.pfspells.data.DeckTransfer
import cz.pfspells.data.Inventory
import cz.pfspells.data.InventoryTransfer
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.deckFileIo
import cz.pfspells.data.trackEvent
import kotlinx.coroutines.launch

// ---------- konkrétní použití ----------

/** Export postav (`pf-spellbook-characters` v1), volitelně i s propojenými inventáři. */
@Composable
fun ExportDialog(state: AppState, selected: List<Character>, db: SpellDatabase, onDismiss: () -> Unit) {
    val s = str
    // s postavou jdou i její mazlíčci (v exportu odkazují na vlastníka pořadím)
    val characters = remember(selected) {
        val ids = selected.map { it.id }.toSet()
        selected + selected.flatMap { state.companionsOf(it.id) }.filter { it.id !in ids }
    }
    val linked = characters.any { c -> c.inventoryId?.let { state.inventory(it) } != null }
    var withInventory by remember { mutableStateOf(true) }
    val text = remember(characters, db, withInventory) {
        CharacterTransfer.export(characters, db) { c -> if (withInventory) c.inventoryId?.let { state.inventory(it) } else null }
    }
    TextExportDialog(
        title = s.exportCharactersTitle(characters.size),
        hint = s.exportCharactersHint,
        text = text,
        fileName = CharacterTransfer.fileName(characters.singleOrNull()?.name ?: "pf-characters"),
        kind = "characters",
        onDismiss = onDismiss,
        options = if (linked) ({ NbCheck(withInventory, s.includeInventory) { withInventory = it } }) else null,
    )
}

/** Import postav – nový formát i staré exporty decků. */
@Composable
fun ImportDialog(state: AppState, db: SpellDatabase, onDismiss: () -> Unit) {
    val s = str
    LaunchedEffect(Unit) { state.ensureFeats(); state.ensureItems() }
    TextImportDialog(
        title = s.importCharacter,
        parse = { t ->
            val list = CharacterTransfer.parse(t, db, state.featDb, state.itemDb)
            ImportPreview(
                rows = list.map { ic ->
                    val c = ic.character
                    ImportRow(
                        c.name,
                        buildString {
                            append(classesSummary(s, c)).append(" · ").append(s.spells(c.classes.sumOf { it.spells.size }))
                            if (c.feats.isNotEmpty()) append(" · ").append(s.featsHeader.lowercase()).append(" ").append(c.feats.size)
                            ic.inventory?.let { append(" · ").append(s.withInventory(it.name)) }
                        },
                        ic.missingSpells.takeIf { it.isNotEmpty() }?.let { s.notFoundSkipped(it.joinToString()) },
                    )
                },
                apply = { state.importCharacters(list) },
            )
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun FeatExportDialog(c: Character, onDismiss: () -> Unit) {
    val s = str
    val text = remember(c.feats) { CharacterTransfer.exportFeats(c.feats) }
    TextExportDialog(
        title = s.exportFeatsTitle(c.feats.size),
        hint = s.exportFeatsHint,
        text = text,
        fileName = CharacterTransfer.featsFileName(c.name + "-feats"),
        kind = "feats",
        onDismiss = onDismiss,
    )
}

@Composable
fun FeatImportDialog(state: AppState, c: Character, onDismiss: () -> Unit) {
    val s = str
    TextImportDialog(
        title = s.importFeatsTitle(c.name),
        parse = { t ->
            val picks = CharacterTransfer.parseFeats(t, state.featDb)
            val add = state.featsToImport(c.id, picks)
            ImportPreview(
                rows = listOf(
                    ImportRow(
                        s.featsImportSummary(add.size, picks.size - add.size),
                        add.joinToString { it.name + if (it.note.isNotBlank()) " (${it.note})" else "" },
                        null,
                    ),
                ),
                apply = { state.importFeats(c.id, picks) },
            )
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun InventoryExportDialog(list: List<Inventory>, onDismiss: () -> Unit) {
    val text = remember(list) { InventoryTransfer.export(list) }
    TextExportDialog(
        title = str.exportInventoriesTitle(list.size),
        hint = str.exportInventoriesHint,
        text = text,
        fileName = InventoryTransfer.fileName(list.singleOrNull()?.name ?: "pf-inventories"),
        kind = "inventories",
        onDismiss = onDismiss,
    )
}

@Composable
fun InventoryImportDialog(state: AppState, onDismiss: () -> Unit) {
    val s = str
    TextImportDialog(
        title = s.importInventory,
        parse = { t ->
            val list = InventoryTransfer.parse(t, state.itemDb)
            ImportPreview(
                rows = list.map { inv ->
                    ImportRow(inv.name, "${s.itemTypes(inv.entries.size)} · ${formatWeight(inv.totalWeight)} · ${inv.gp} gp", null)
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
fun TextExportDialog(
    title: String,
    hint: String,
    text: String,
    fileName: String,
    onDismiss: () -> Unit,
    /** druh exportu pro anonymní analytiku (`characters`, `feats`, `inventories`) */
    kind: String,
    /** volby exportu pod nápovědou (např. „včetně inventáře“) */
    options: (@Composable () -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val s = str
    var message by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(hint, style = MaterialTheme.typography.bodySmall)
                options?.invoke()
                Text(
                    text,
                    Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NbDashedButton(s.copy, {
                        clipboard.setText(AnnotatedString(text))
                        trackEvent("export", mapOf("kind" to kind, "method" to "copy"))
                        message = s.copiedToClipboard
                    })
                    NbDashedButton(s.saveAction(deckFileIo.saveAction), {
                        scope.launch {
                            message = try {
                                if (deckFileIo.saveText(fileName, text)) {
                                    trackEvent("export", mapOf("kind" to kind, "method" to "file"))
                                    s.done
                                } else null
                            } catch (e: Exception) {
                                s.failed(e.message)
                            }
                        }
                    }, icon = NbIcons.Share)
                }
                message?.let { Text(it, color = Nb.accent) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.close, color = Nb.ink) } },
    )
}

/** [parse] vyhodí [DeckImportException] s hláškou pro uživatele. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextImportDialog(title: String, parse: (String) -> ImportPreview, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val s = str
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
            error = s.importError(e.reason)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NbDashedButton(s.pasteFromClipboard, { check(clipboard.getText()?.text ?: "") })
                    if (deckFileIo.canOpen) {
                        NbDashedButton(s.openFile, {
                            scope.launch {
                                try {
                                    deckFileIo.openText()?.let(::check)
                                } catch (e: Exception) {
                                    error = s.cannotReadFile(e.message)
                                }
                            }
                        })
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = ::check,
                    label = { Text(s.exportText) },
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
            ) { Text(s.importButton(preview?.rows?.size ?: 0)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}
