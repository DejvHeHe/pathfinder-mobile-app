package cz.pfspells.ui

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
    NbScreen(
        title = "Databases",
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
            SectionHeader("Spells", size = 22.sp)
            NbPanel(Modifier.fillMaxWidth(), color = Nb.inset) {
                MonoLabel(if (db.source == SpellSource.BUNDLED) "Bundled version" else "Downloaded version")
                Text(
                    "${db.spells.size} spells",
                    fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Nb.ink,
                    modifier = Modifier.padding(top = 4.dp),
                )
                db.updatedAt?.let { Text("Downloaded $it", fontSize = 14.sp, color = Nb.rust) }
            }

            Text(
                "Spells come from Archives of Nethys (Paizo, Pathfinder 1e) via the open SpellTable-pf1e project. " +
                    "Pathfinder 1e is finished, so new spells practically never appear – an update mostly brings typo fixes.",
                fontSize = 15.sp, lineHeight = 22.sp, color = Nb.ink,
            )

            if (state.downloading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Nb.accent, strokeWidth = 2.dp)
                    Text("  Downloading…", color = Nb.ink)
                }
            } else {
                NbPrimaryButton("Download latest spell database", { state.downloadSpells() }, icon = NbIcons.Refresh)
            }

            state.downloadMessage?.let {
                Text(it, color = if (it.startsWith("Download failed")) Nb.error else Nb.accent, fontSize = 14.sp)
            }

            if (db.source == SpellSource.DOWNLOADED) {
                NbDashedButton("Restore bundled version", { state.resetSpells() }, enabled = !state.downloading)
            }

            Text("Source: " + SpellParser.SOURCE_URL, fontFamily = fonts.mono, fontSize = 12.sp, color = Nb.inkMuted)
            Text("Decks are kept – spells are matched by id and name.", fontSize = 13.sp, color = Nb.inkMuted)

            ItemDatabaseSection(state)
        }
    }
}

@Composable
private fun ItemDatabaseSection(state: AppState) {
    val fonts = LocalNbFonts.current
    LaunchedEffect(Unit) { state.ensureItems() }
    SectionHeader("Items", size = 22.sp)
    val idb = state.itemDb
    NbPanel(Modifier.fillMaxWidth(), color = Nb.inset) {
        if (idb == null) {
            Text(if (state.items is ItemDbState.Failed) "Item database failed to load." else "Loading…", color = Nb.inkMuted)
        } else {
            MonoLabel(if (idb.source == SpellSource.BUNDLED) "Bundled version" else "Downloaded version")
            Text(
                "${idb.items.size} items",
                fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Nb.ink,
                modifier = Modifier.padding(top = 4.dp),
            )
            idb.updatedAt?.let { Text("Downloaded $it", fontSize = 14.sp, color = Nb.rust) }
        }
    }
    Text(
        "Weapons, ammunition and magic items come from the d20pfsrd tables and can be updated here. " +
            "Mundane gear, armor and alchemical items (from the Foundry VTT PF1 system) are bundled with the app – core PF1 equipment does not change.",
        fontSize = 15.sp, lineHeight = 22.sp, color = Nb.ink,
    )
    if (state.itemsDownloading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Nb.accent, strokeWidth = 2.dp)
            Text("  Downloading…", color = Nb.ink)
        }
    } else {
        NbPrimaryButton("Download latest item database", { state.downloadItems() }, icon = NbIcons.Refresh)
    }
    state.itemsMessage?.let {
        Text(it, color = if (it.startsWith("Download failed")) Nb.error else Nb.accent, fontSize = 14.sp)
    }
    if (idb?.source == SpellSource.DOWNLOADED) {
        NbDashedButton("Restore bundled version", { state.resetItems() }, enabled = !state.itemsDownloading)
    }
    Text("Source: " + ItemParser.SOURCE_DESCRIPTION, fontFamily = fonts.mono, fontSize = 12.sp, color = Nb.inkMuted)
    Text("Inventories are kept – entries store their own name, price and weight.", fontSize = 13.sp, color = Nb.inkMuted)
}
