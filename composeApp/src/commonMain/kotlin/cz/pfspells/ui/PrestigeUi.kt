package cz.pfspells.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cz.pfspells.AppState
import cz.pfspells.Strings
import cz.pfspells.data.Character
import cz.pfspells.data.ClassCatalog
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.Prestige
import cz.pfspells.data.PrestigeInfo
import cz.pfspells.data.PrestigeRequirements
import cz.pfspells.data.PrereqStatus
import cz.pfspells.data.casterLevel
import cz.pfspells.str

/** Název druhu sesílání: „tajemné“, „libovolné“, u konkrétní classy její jméno („Čarodějnice“). */
fun prestigeKindName(s: Strings, kind: String): String =
    if (kind.startsWith("class:")) s.classNameOf(kind.removePrefix("class:")) else s.prestigeKind(kind)

/** „tajemné + božské“ – druhy sesílání, které prestižní classa zvyšuje. */
fun prestigeKinds(s: Strings, info: PrestigeInfo): String = info.advanceKinds.joinToString(" + ") { prestigeKindName(s, it) }

/** Podtitulek prestižní classy v panelu class: „prestižní · max 10 · + sesílání (tajemné)“. */
fun prestigeSubtitle(s: Strings, info: PrestigeInfo): String =
    listOfNotNull(s.prestigeTag(info.maxLevel), info.advanceKinds.takeIf { it.isNotEmpty() }?.let { s.prestigeAdvancesTag(prestigeKinds(s, info)) })
        .joinToString(" · ")

/**
 * Pod prestižní classou: pro každý druh sesílání výběr classy, kterou zvyšuje (a kolik úrovní), varování, když chybí,
 * a odkaz na požadavky a schopnosti.
 */
@Composable
fun PrestigeEntryControls(state: AppState, c: Character, e: ClassEntry, onDetails: () -> Unit) {
    val s = str
    val info = ClassCatalog.get(e.classId)?.prestige ?: return
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        info.advanceKinds.forEach { kind ->
            val options = Prestige.candidates(c, kind)
            SimpleDropdown(
                label = s.advanceLabel(prestigeKindName(s, kind)),
                options = listOf<ClassEntry?>(null) + options,
                selected = options.firstOrNull { it.classId == e.advances[kind] },
                optionLabel = { it?.let { o -> s.classNameOf(o.classId) } ?: s.advanceNone },
                onSelect = { state.setPrestigeAdvance(c.id, e.classId, kind, it?.classId) },
            )
            val n = info.advancesAt(e.level, kind)
            e.advances[kind]?.let { target ->
                if (n > 0) Text(s.advanceSummary(s.classNameOf(target), n), fontSize = 12.5.sp, color = Nb.rust)
            }
        }
        if (Prestige.missingAdvances(e).isNotEmpty() && info.advanceKinds.any { Prestige.candidates(c, it).isNotEmpty() }) {
            TraitWarning(s.missingAdvance)
        }
        Text(
            s.prestigeDetails,
            Modifier.clip(Nb.shape).clickable(onClick = onDetails).padding(vertical = 4.dp),
            fontSize = 13.sp, color = Nb.accent, textDecoration = TextDecoration.Underline,
        )
    }
}

/** U sesílající classy, kterou zvyšuje prestižní classa: „sesílací úroveň 9“. */
@Composable
fun CasterLevelNote(c: Character, e: ClassEntry) {
    val cl = c.casterLevel(e)
    if (cl > e.level) Text(str.casterLevelInfo(cl), fontSize = 12.5.sp, color = Nb.rust)
}

/**
 * Detail prestižní classy: zdroj, požadavky (u postavy ✓ / ✗ / ? s ručním potvrzením), tabulka BAB / záchran /
 * sesílání po úrovních, popis a schopnosti (text z AoN).
 */
@Composable
fun PrestigeDialog(state: AppState, c: Character?, info: PrestigeInfo, onDismiss: () -> Unit) {
    val s = str
    val fonts = LocalNbFonts.current
    LaunchedEffect(Unit) {
        state.ensurePrestigeTexts()
        if (c != null) state.ensureFeats()
    }
    val text = state.prestigeTexts?.get(info.id)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            val width = minOf(640.dp, maxWidth - 32.dp)
            val maxH = minOf(780.dp, maxHeight - 48.dp)
            Surface(Modifier.width(width).heightIn(max = maxH), shape = MaterialTheme.shapes.extraLarge, color = Nb.panel, shadowElevation = 8.dp) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)) {
                    Text(s.classNameOf(info.id), style = MaterialTheme.typography.headlineSmall, color = Nb.ink)
                    Text(
                        listOfNotNull(prestigeSubtitle(s, info), "d${info.hd}", "PFS".takeIf { info.pfs }).joinToString(" · "),
                        fontSize = 12.5.sp, color = Nb.rust, modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (info.source.isNotBlank()) MonoLabel(info.source)
                        Text(s.prestigeHint, fontSize = 12.sp, color = Nb.inkMuted)
                        // požadavky
                        SectionHeader(s.prestigeRequirementsHeader)
                        val reqs = c?.let { PrestigeRequirements.evaluate(it, info, state.featDb, state.spellDb) }
                        info.requirements.forEachIndexed { i, r ->
                            val parts = reqs?.getOrNull(i)?.parts
                            Column {
                                Row(verticalAlignment = Alignment.Top) {
                                    if (reqs != null) PrereqIcon(s, reqs[i].status, 16.dp)
                                    Text(r, Modifier.padding(start = if (reqs != null) 6.dp else 0.dp), fontSize = 14.sp,
                                        color = if (reqs?.getOrNull(i)?.status == PrereqStatus.UNMET) Nb.error else Nb.ink)
                                }
                                // části, které appka neověří, jde potvrdit ručně (stejně jako u featů)
                                if (c != null && parts != null) parts.filter { it.status != PrereqStatus.MET || it.confirmed }.forEach { pr ->
                                    Text(
                                        (if (pr.confirmed) s.unconfirmPrereq else s.confirmPrereq) + ": " + pr.text,
                                        Modifier.padding(start = 28.dp).clip(Nb.shape)
                                            .clickable { state.setPrereqConfirmed(c.id, pr.text, !pr.confirmed) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontSize = 12.5.sp, color = Nb.accent, textDecoration = TextDecoration.Underline,
                                    )
                                }
                            }
                        }
                        // tabulka po úrovních
                        SectionHeader(s.prestigeTableHeader)
                        val cols = s.prestigeColumns
                        PrestigeTableRow(cols, fonts.mono, header = true)
                        (1..info.maxLevel).forEach { l ->
                            fun sv(v: Int) = if (v < 0) "−${-v}" else "+$v"
                            PrestigeTableRow(
                                listOf(
                                    "$l", sv(info.at(info.babs, l)), sv(info.at(info.forts, l)), sv(info.at(info.refs, l)), sv(info.at(info.wills, l)),
                                    info.castingKinds.getOrNull(l - 1).orEmpty().joinToString(" + ") { "+1 " + prestigeKindName(s, it) }.ifEmpty { "—" },
                                ),
                                fonts.mono, header = false,
                            )
                        }
                        // popis a schopnosti
                        if (text == null) Text(s.loadingAbilities, color = Nb.inkMuted)
                        text?.description?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 13.5.sp, color = Nb.ink, modifier = Modifier.padding(top = 6.dp)) }
                        text?.features?.takeIf { it.isNotBlank() }?.let {
                            SectionHeader(s.prestigeFeaturesHeader)
                            Text(it, fontSize = 13.5.sp, color = Nb.ink)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text(s.close, color = Nb.ink) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrestigeTableRow(cells: List<String>, mono: androidx.compose.ui.text.font.FontFamily, header: Boolean) {
    Row(Modifier.fillMaxWidth().bottomRule(Nb.ruleSoft).padding(vertical = 2.dp)) {
        cells.forEachIndexed { i, t ->
            Text(
                t,
                Modifier.weight(if (i == cells.lastIndex) 2.4f else 1f),
                fontFamily = mono, fontSize = 12.sp,
                color = if (header) Nb.inkMuted else Nb.ink,
                maxLines = 2,
            )
        }
    }
}
