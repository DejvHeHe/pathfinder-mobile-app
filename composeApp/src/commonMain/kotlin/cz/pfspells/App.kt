package cz.pfspells

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.pfspells.data.SpellDatabase
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import cz.pfspells.ui.DatabaseScreen
import cz.pfspells.ui.InventoryScreen
import cz.pfspells.ui.ItemDetailScreen
import cz.pfspells.ui.ItemPickerScreen
import cz.pfspells.ui.DeckDetailScreen
import cz.pfspells.ui.DeckListScreen
import cz.pfspells.ui.Nb
import cz.pfspells.ui.NbPrimaryButton
import cz.pfspells.ui.PfTheme
import cz.pfspells.ui.SpellBrowserScreen
import cz.pfspells.ui.SpellDetailScreen
import cz.pfspells.ui.WideLayout
import cz.pfspells.ui.ruledPaper

/** Od této šířky se appka zobrazí ve sloupcích (desktop, tablet na šířku). */
private val WideBreakpoint = 1000.dp

@Composable
fun App(state: AppState) {
    PfTheme {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Nb.paper).ruledPaper()
                // tajné odhalení cen: 10 rychlých ťuknutí kamkoli (dotyk se nespotřebuje, UI funguje normálně)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        state.registerTap(down.uptimeMillis)
                    }
                },
        ) {
            val wide = maxWidth >= WideBreakpoint
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                when (val db = state.db) {
                    DbState.Loading -> Centered {
                        CircularProgressIndicator(color = Nb.accent)
                        Text("Loading spells…", Modifier.padding(top = 16.dp), color = Nb.inkMuted)
                    }
                    is DbState.Failed -> Centered {
                        Text("Could not load spells:\n${db.message}", color = Nb.error)
                        NbPrimaryButton("Use bundled data", { state.resetSpells() }, Modifier.padding(top = 16.dp), icon = null)
                    }
                    is DbState.Ready -> if (wide) WideLayout(state, db.db) else Router(state, db.db)
                }
                state.revealToast?.let { msg ->
                    Text(
                        msg,
                        Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
                            .clip(Nb.shape).background(Nb.ink).padding(horizontal = 16.dp, vertical = 10.dp),
                        color = Nb.panel, fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun Router(state: AppState, db: SpellDatabase) {
    when (val s = state.current) {
        Screen.Decks -> DeckListScreen(state, db)
        Screen.Compendium -> SpellBrowserScreen(state, db, deckId = null)
        Screen.Database -> DatabaseScreen(state, db)
        is Screen.DeckDetail -> DeckDetailScreen(state, db, s.deckId)
        is Screen.Picker -> SpellBrowserScreen(state, db, deckId = s.deckId)
        is Screen.SpellDetail -> SpellDetailScreen(state, db, s.spellId, s.deckId)
        is Screen.InventoryDetail -> InventoryScreen(state, s.invId)
        is Screen.ItemPicker -> ItemPickerScreen(state, s.invId)
        is Screen.ItemDetail -> ItemDetailScreen(state, s.itemId, s.invId, s.entryId, aside = false)
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            content()
        }
    }
}
