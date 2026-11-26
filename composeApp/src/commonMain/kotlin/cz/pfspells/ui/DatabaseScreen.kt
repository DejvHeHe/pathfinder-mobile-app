package cz.pfspells.ui

import cz.pfspells.str
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SpellParser
import cz.pfspells.data.ItemParser
import cz.pfspells.ItemDbState
import androidx.compose.runtime.LaunchedEffect
import cz.pfspells.data.SpellSource

@Composable
fun DatabaseScreen(state: AppState, db: SpellDatabase, wide: Boolean = false) {
    val fonts = LocalNbFonts.current
    val s = str
    NbScreen(
        title = s.databases,
        subtitle = null,
        onBack = { if (wide) state.popMain() else state.pop() },
        wide = wide,
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) 0.dp else 16.dp, vertical = 12.dp)
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionHeader(s.spellsHeader, size = 22.sp)
            NbPanel(Modifier.fillMaxWidth(), color = Nb.inset) {
                MonoLabel(if (db.source == SpellSource.BUNDLED) s.bundledVersion else s.downloadedVersion)
                Text(
                    s.spells(db.spells.size),
                    fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Nb.ink,
                    modifier = Modifier.padding(top = 4.dp),
                )
                db.updatedAt?.let { Text(s.downloadedAt(it), fontSize = 14.sp, color = Nb.rust) }
            }

            Text(
                s.spellsDbInfo,
                fontSize = 15.sp, lineHeight = 22.sp, color = Nb.ink,
            )

            if (state.downloading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Nb.accent, strokeWidth = 2.dp)
                    Text("  " + s.downloading, color = Nb.ink)
                }
            } else {
                NbPrimaryButton(s.downloadLatestSpells, { state.downloadSpells() }, icon = NbIcons.Refresh)
            }

            state.downloadMessage?.let {
                Text(it.text(s), color = if (it.isError) Nb.error else Nb.accent, fontSize = 14.sp)
            }

            if (db.source == SpellSource.DOWNLOADED) {
                NbDashedButton(s.restoreBundled, { state.resetSpells() }, enabled = !state.downloading)
            }

            Text(s.sourceLine(SpellParser.SOURCE_URL), fontFamily = fonts.mono, fontSize = 12.sp, color = Nb.inkMuted)
            Text(s.charactersKept, fontSize = 13.sp, color = Nb.inkMuted)

            ItemDatabaseSection(state)
            AppUpdateSection(state)
        }
    }
}

@Composable
private fun ItemDatabaseSection(state: AppState) {
    val fonts = LocalNbFonts.current
    LaunchedEffect(Unit) { state.ensureItems() }
    val s = str
    SectionHeader(s.itemsHeader, size = 22.sp)
    val idb = state.itemDb
    NbPanel(Modifier.fillMaxWidth(), color = Nb.inset) {
        if (idb == null) {
            Text(if (state.items is ItemDbState.Failed) s.itemDbFailed else s.loading, color = Nb.inkMuted)
        } else {
            MonoLabel(if (idb.source == SpellSource.BUNDLED) s.bundledVersion else s.downloadedVersion)
            Text(
                s.itemsCount(idb.items.size),
                fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Nb.ink,
                modifier = Modifier.padding(top = 4.dp),
            )
            idb.updatedAt?.let { Text(s.downloadedAt(it), fontSize = 14.sp, color = Nb.rust) }
        }
    }
    Text(
        s.itemsDbInfo,
        fontSize = 15.sp, lineHeight = 22.sp, color = Nb.ink,
    )
    if (state.itemsDownloading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Nb.accent, strokeWidth = 2.dp)
            Text("  " + s.downloading, color = Nb.ink)
        }
    } else {
        NbPrimaryButton(s.downloadLatestItems, { state.downloadItems() }, icon = NbIcons.Refresh)
    }
    state.itemsMessage?.let {
        Text(it.text(s), color = if (it.isError) Nb.error else Nb.accent, fontSize = 14.sp)
    }
    if (idb?.source == SpellSource.DOWNLOADED) {
        NbDashedButton(s.restoreBundled, { state.resetItems() }, enabled = !state.itemsDownloading)
    }
    Text(s.sourceLine(ItemParser.SOURCE_DESCRIPTION), fontFamily = fonts.mono, fontSize = 12.sp, color = Nb.inkMuted)
    Text(s.inventoriesKept, fontSize = 13.sp, color = Nb.inkMuted)
}
