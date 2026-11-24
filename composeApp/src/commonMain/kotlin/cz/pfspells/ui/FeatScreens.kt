package cz.pfspells.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.FeatDbState
import cz.pfspells.Screen
import cz.pfspells.Strings
import cz.pfspells.data.Character
import cz.pfspells.data.Feat
import cz.pfspells.data.FeatDatabase
import cz.pfspells.data.FeatPrereqs
import cz.pfspells.data.PrereqStatus
import cz.pfspells.data.featSlots
import cz.pfspells.data.raceInfo
import cz.pfspells.str

// ---------------- sekce v detailu postavy ----------------

/** Featy postavy: počet (vybrané / dostupné + rozpis), seznam, přidání z databáze a vlastní feat. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeatsPanel(state: AppState, c: Character, wide: Boolean, selectedPick: Int?) {
    val s = str
    val fonts = LocalNbFonts.current
    val db = state.featDb
    LaunchedEffect(Unit) { state.ensureFeats() }
    var custom by remember { mutableStateOf(false) }
    var exportFeats by remember { mutableStateOf(false) }
    var importFeats by remember { mutableStateOf(false) }
    val slots = c.featSlots()
    val over = c.feats.size > slots.total

    Column(Modifier.padding(top = 4.dp)) {
        SectionHeader(s.featsHeader) {
            Text(
                s.featCount(c.feats.size, slots.total), fontFamily = fonts.mono, fontSize = 12.sp,
                color = if (over) Nb.error else Nb.inkMuted,
            )
        }
        // rozpis: za úrovně · bonusové featy class · navíc (stepper)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            val parts = listOf(s.featSlotsGeneral(slots.general)) +
                listOfNotNull(c.raceInfo?.takeIf { slots.race > 0 }?.let { "${s.raceNameOf(it)} +${slots.race}" }) +
                slots.bonus.map { (cls, n) -> "${s.classNameOf(cls)} +$n" }
            Text(
                parts.joinToString(" · "), Modifier.align(Alignment.CenterVertically),
                fontSize = 12.5.sp, color = Nb.inkMuted,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.extraFeats, fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(end = 6.dp))
                CountStepper(c.extraFeats, { state.setExtraFeats(c.id, it) }, min = -20, max = 40, size = 28.dp, numberSize = 13.sp)
            }
        }
        if (c.feats.isEmpty()) {
            Text(s.noFeats, color = Nb.inkMuted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        c.feats.forEachIndexed { i, pick ->
            val feat = pick.featId?.let { db?.get(it) }
            val status = feat?.let { FeatPrereqs.overall(FeatPrereqs.evaluate(c, db, it.prereq, state.spellDb)) }
            val base = Modifier.fillMaxWidth()
            Row(
                (if (selectedPick == i) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule(if (wide) Nb.ruleSoft else Nb.rule))
                    .clickable { state.showFeat(pick.featId, c.id, i) }
                    .padding(horizontal = if (wide) 10.dp else 0.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pick.name + (if (pick.note.isNotBlank()) " (${pick.note})" else ""),
                            Modifier.weight(1f, fill = false),
                            fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (status == PrereqStatus.UNMET) {
                            Icon(NbIcons.Warn, s.prereqUnmet, tint = Nb.error, modifier = Modifier.padding(start = 6.dp).size(16.dp))
                        }
                    }
                    Text(
                        when {
                            pick.featId == null -> s.customBadge + (pick.text?.let { " · $it" } ?: "")
                            feat != null -> featSubline(s, feat)
                            else -> ""
                        },
                        fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                NbIconButton(NbIcons.Close, s.removeFeat, { state.removeFeat(c.id, i) }, Nb.inkMuted, 16.dp)
            }
        }
        FlowRow(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NbDashedButton(s.addFeat, {
                if (wide) state.openRoot(Screen.FeatPicker(c.id)) else state.push(Screen.FeatPicker(c.id))
            }, icon = NbIcons.Plus)
            NbDashedButton(s.customFeat, { custom = true }, icon = NbIcons.Pencil)
            if (c.feats.isNotEmpty()) NbDashedButton(s.exportFeats, { exportFeats = true }, icon = NbIcons.Share)
            NbDashedButton(s.importFeats, { importFeats = true })
        }
    }
    if (exportFeats) FeatExportDialog(c) { exportFeats = false }
    if (importFeats) FeatImportDialog(state, c) { importFeats = false }
    if (custom) {
        CustomFeatDialog(s.customFeat, "", "", onDismiss = { custom = false }) { name, text ->
            custom = false
            state.addCustomFeat(c.id, name, text)
        }
    }
}

/** „Combat · Str 13, base attack bonus +1“ */
fun featSubline(s: Strings, f: Feat): String = buildString {
    append(if (f.types.isEmpty()) s.generalFeat else f.types.joinToString(", "))
    if (f.prereq.isNotBlank()) append(" · ").append(f.prereq)
}

@Composable
fun CustomFeatDialog(title: String, name0: String, text0: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val s = str
    var name by remember { mutableStateOf(name0) }
    var text by remember { mutableStateOf(text0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title, color = Nb.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(s.featName) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(text, { text = it }, label = { Text(s.featText) }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { NbPrimaryButton(s.save, { if (name.isNotBlank()) onSave(name, text) }, icon = null, enabled = name.isNotBlank()) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}

// ---------------- výběr featů ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeatPickerScreen(state: AppState, charId: String, wide: Boolean = false, selectedFeatId: String? = null) {
    val c = state.character(charId)
    if (c == null) {
        LaunchedEffect(charId) { state.pop() }
        return
    }
    val s = str
    val back: () -> Unit = { if (wide) state.openRoot(Screen.CharacterDetail(charId)) else state.pop() }
    NbScreen(title = s.addTo(c.name), subtitle = s.featsHeader, onBack = back, wide = wide) {
        val db = when (val st = state.featDbState) {
            is FeatDbState.Ready -> st.db
            is FeatDbState.Failed -> { CenterText(s.featsFailed(st.message)); return@NbScreen }
            else -> { CenterText(s.loadingFeats); return@NbScreen }
        }
        val filter = state.featFilter
        val setFilter: (cz.pfspells.FeatFilter) -> Unit = { state.featFilter = it }
        val taken = c.feats.groupingBy { it.featId }.eachCount()
        val results = remember(db, filter, c) {
            val q = filter.query.trim().lowercase()
            db.feats.filter { f ->
                if (filter.type != null && filter.type !in f.types) return@filter false
                if (filter.pfsOnly && !f.pfs) return@filter false
                if (q.isNotEmpty() && !f.searchName.contains(q) &&
                    !(filter.fullText && (f.summary.contains(q, true) || f.sections.any { it.last().contains(q, true) }))
                ) return@filter false
                if (filter.onlyMet && FeatPrereqs.overall(FeatPrereqs.evaluate(c, db, f.prereq, state.spellDb)) == PrereqStatus.UNMET) return@filter false
                true
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (wide) 0.dp else 16.dp).padding(top = if (wide) 14.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NbSearchField(filter.query, { setFilter(filter.copy(query = it)) }, s.searchFeats, Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SimpleDropdown(
                    label = s.featType,
                    options = listOf<String?>(null) + db.types,
                    selected = filter.type,
                    optionLabel = { it ?: s.anyOption },
                    onSelect = { setFilter(filter.copy(type = it)) },
                )
                NbCheck(filter.onlyMet, s.onlyMet) { setFilter(filter.copy(onlyMet = it)) }
                NbCheck(filter.pfsOnly, s.pfsOnly) { setFilter(filter.copy(pfsOnly = it)) }
                NbCheck(filter.fullText, s.searchDescriptions) { setFilter(filter.copy(fullText = it)) }
                NbCheck(state.strictMode, s.strictMode) { state.changeStrictMode(it) }
            }
            MonoLabel(s.results(results.size), Modifier.padding(top = 2.dp), size = 11.sp)
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            state = state.featList,
            contentPadding = PaddingValues(start = if (wide) 0.dp else 16.dp, end = if (wide) 0.dp else 16.dp, top = 4.dp, bottom = 32.dp),
        ) {
            items(results, key = { it.id }) { f ->
                FeatRow(
                    s, f, FeatPrereqs.overall(FeatPrereqs.evaluate(c, db, f.prereq, state.spellDb)), taken[f.id] ?: 0,
                    selected = f.id == selectedFeatId,
                    onClick = { state.showFeat(f.id, charId) },
                    onAdd = if (f.repeatable || (taken[f.id] ?: 0) == 0) ({ state.addFeat(charId, f) }) else null,
                )
            }
        }
    }
}

@Composable
private fun CenterText(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = Nb.inkMuted)
    }
}

@Composable
private fun FeatRow(s: Strings, f: Feat, status: PrereqStatus, taken: Int, selected: Boolean, onClick: () -> Unit, onAdd: (() -> Unit)?) {
    val base = Modifier.fillMaxWidth()
    Row(
        (if (selected) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule())
            .clickable(onClick = onClick).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PrereqIcon(s, status)
        Column(Modifier.weight(1f)) {
            Text(f.name, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(featSubline(s, f), fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (f.summary.isNotBlank()) {
                Text(f.summary, fontSize = 13.sp, color = Nb.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onAdd == null) {
            // už vybraný a nejde vzít znovu
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(NbIcons.Check, s.featAlreadyTaken, tint = Nb.accent, modifier = Modifier.size(22.dp))
            }
        } else {
            NbSquareButton("+", onAdd, s.addFeatToCharacter, size = 40.dp) {
                if (taken > 0) Text("×$taken", fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Nb.accent)
                else Text("+", fontSize = 18.sp, color = Nb.ink)
            }
        }
    }
}

/** ✓ / ✗ / ? jako ikony (fonty webu nemají fajfku). */
@Composable
fun PrereqIcon(s: Strings, status: PrereqStatus, size: androidx.compose.ui.unit.Dp = 18.dp) {
    Box(Modifier.size(size + 4.dp), contentAlignment = Alignment.Center) {
        when (status) {
            PrereqStatus.MET -> Icon(NbIcons.Check, s.prereqStatus(status), tint = Nb.accent, modifier = Modifier.size(size))
            PrereqStatus.UNMET -> Icon(NbIcons.Close, s.prereqStatus(status), tint = Nb.error, modifier = Modifier.size(size - 2.dp))
            PrereqStatus.UNKNOWN -> Text("?", fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Nb.inkMuted)
        }
    }
}

// ---------------- detail featu ----------------

/**
 * Detail featu z databáze ([featId]) nebo featu postavy ([pick] = index v `Character.feats`, i vlastního).
 * U postavy: prerekvizity ✓/✗/?, poznámka (např. zvolená zbraň), úprava vlastního featu.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeatDetailScreen(state: AppState, featId: String?, charId: String?, pick: Int?, aside: Boolean) {
    val s = str
    val fonts = LocalNbFonts.current
    val c = charId?.let { state.character(it) }
    val p = pick?.let { c?.feats?.getOrNull(it) }
    val db: FeatDatabase? = state.featDb
    val feat = (featId ?: p?.featId)?.let { db?.get(it) }
    if (feat == null && p == null) {
        if (state.featDbState is FeatDbState.Ready || featId == null) LaunchedEffect(Unit) { state.pop() }
        else CenterText(s.loadingFeats)
        return
    }
    var editCustom by remember { mutableStateOf(false) }
    var note by remember(charId, pick) { mutableStateOf(p?.note ?: "") }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = if (aside) 24.dp else 4.dp, end = 8.dp, top = if (aside) 14.dp else 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!aside) BackButton { state.pop() }
                Text(
                    feat?.name ?: p!!.name, Modifier.weight(1f),
                    fontFamily = fonts.serif, fontWeight = FontWeight.Bold,
                    fontSize = if (aside) 32.sp else 24.sp, lineHeight = if (aside) 35.sp else 26.sp,
                    color = Nb.ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (p != null && p.featId == null) NbIconButton(NbIcons.Pencil, s.editFeat, { editCustom = true }, Nb.inkMuted)
                if (aside) NbIconButton(NbIcons.Close, s.closeDetails, { state.pop() }, Nb.inkMuted)
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = if (aside) 28.dp else 18.dp, end = if (aside) 28.dp else 18.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (feat == null) {
                    // vlastní feat
                    NbTag(s.customBadge, Nb.rust)
                    p?.text?.let { Text(it, fontSize = 15.sp, lineHeight = 23.sp, color = Nb.ink) }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (feat.types.isEmpty()) NbTag(s.generalFeat) else feat.types.forEach { NbTag(it) }
                        if (!feat.pfs) NbTag(s.notPfsLegal, Nb.error)
                    }
                    feat.flavor?.let {
                        Text(it, fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = if (aside) 19.sp else 18.sp, lineHeight = 24.sp, color = Nb.ink)
                    }
                    if (feat.prereq.isNotBlank()) {
                        SectionHeader(s.prerequisites, Modifier.padding(top = 0.dp))
                        val list = if (c != null) FeatPrereqs.evaluate(c, db, feat.prereq, state.spellDb) else FeatPrereqs.split(feat.prereq).map { cz.pfspells.data.Prereq(it, PrereqStatus.UNKNOWN) }
                        list.forEach { pr ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (c != null) PrereqIcon(s, pr.status, 16.dp)
                                Text(
                                    pr.text + if (pr.confirmed) " (${s.confirmedByHand})" else "",
                                    Modifier.weight(1f).padding(start = if (c != null) 6.dp else 0.dp),
                                    fontSize = 14.5.sp, color = if (pr.status == PrereqStatus.UNMET) Nb.error else Nb.ink,
                                )
                                // ruční potvrzení – co appka neověří (rasový trait, archetyp…), platí pro všechny featy se stejným textem
                                if (c != null && (pr.status != PrereqStatus.MET || pr.confirmed)) {
                                    Text(
                                        if (pr.confirmed) s.unconfirmPrereq else s.confirmPrereq,
                                        Modifier.padding(start = 8.dp).clip(Nb.shape)
                                            .clickable { state.setPrereqConfirmed(c.id, pr.text, !pr.confirmed) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        fontSize = 13.sp, color = Nb.accent, textDecoration = TextDecoration.Underline,
                                    )
                                }
                            }
                        }
                        if (c != null && list.any { it.status != PrereqStatus.MET || it.confirmed }) {
                            Text(s.confirmPrereqHint, fontSize = 12.5.sp, color = Nb.inkMuted)
                        }
                    }
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            feat.sections.forEach { sec ->
                                if (sec.size >= 2) {
                                    Text(sec[0], fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Nb.rust)
                                    sec[1].split('\n').filter { it.isNotBlank() }.forEach {
                                        Text(it.trim(), fontSize = if (aside) 15.sp else 14.5.sp, lineHeight = 22.5.sp, color = Nb.ink)
                                    }
                                }
                            }
                        }
                    }
                    feat.source?.let { Text(s.sourceLine(it), fontSize = 12.5.sp, color = Nb.inkMuted) }
                    val uri = LocalUriHandler.current
                    Text(
                        s.openOnAon,
                        Modifier.clickable { runCatching { uri.openUri(feat.url) } }.padding(vertical = 4.dp),
                        color = Nb.accent, fontSize = 14.sp, textDecoration = TextDecoration.Underline,
                    )
                }
                if (p != null && c != null && pick != null) {
                    OutlinedTextField(
                        note, { note = it; state.updateFeat(c.id, pick) { fp -> fp.copy(note = it.trim()) } },
                        label = { Text(s.featNote) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                val takenHere = if (c != null && feat != null) c.feats.count { it.featId == feat.id } else 0
                if (c != null && feat != null && p == null && takenHere > 0 && !feat.repeatable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(NbIcons.Check, null, tint = Nb.accent, modifier = Modifier.size(18.dp))
                        Text(s.featAlreadyTaken, Modifier.padding(start = 6.dp), fontSize = 14.sp, color = Nb.accent)
                    }
                } else if (aside && c != null && feat != null && p == null) {
                    Spacer(Modifier.height(4.dp))
                    val taken = takenHere
                    NbPrimaryButton(if (taken > 0) "${s.addFeatToCharacter} (${s.featTaken(taken)})" else s.addFeatToCharacter, { state.addFeat(c.id, feat) })
                }
            }
        }
        val takenFab = if (c != null && feat != null) c.feats.count { it.featId == feat.id } else 0
        if (!aside && c != null && feat != null && p == null && (feat.repeatable || takenFab == 0)) {
            val taken = takenFab
            NbFab(
                if (taken > 0) "${s.addFeatToCharacter} (×$taken)" else s.addFeatToCharacter,
                { state.addFeat(c.id, feat) },
                Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 18.dp),
            )
        }
    }
    if (editCustom && p != null && c != null && pick != null) {
        CustomFeatDialog(s.editFeat, p.name, p.text ?: "", onDismiss = { editCustom = false }) { name, text ->
            editCustom = false
            state.updateFeat(c.id, pick) { it.copy(name = name.trim(), text = text.trim().ifEmpty { null }) }
        }
    }
}
