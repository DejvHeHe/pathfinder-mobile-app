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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.edgeSwipeBack
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
    val s = state.strings
    PfTheme {
        val reduceMotion = remember { cz.pfspells.data.systemReducedMotion() }
        CompositionLocalProvider(LocalStrings provides s, cz.pfspells.ui.LocalReduceMotion provides reduceMotion) { AppContent(state, s) }
    }
}

@Composable
private fun AppContent(state: AppState, s: Strings) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Nb.paper).ruledPaper()
            // tajné odhalení cen: 10 rychlých ťuknutí kamkoli (dotyk se nespotřebuje, UI funguje normálně)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    state.registerTap(down.uptimeMillis)
                }
            }
            .then(if (edgeSwipeBack) Modifier.swipeFromEdgeBack { state.pop() } else Modifier),
    ) {
        val wide = maxWidth >= WideBreakpoint
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            when (val db = state.db) {
                DbState.Loading -> Centered {
                    CircularProgressIndicator(color = Nb.accent)
                    Text(s.loadingSpells, Modifier.padding(top = 16.dp), color = Nb.inkMuted)
                }
                is DbState.Failed -> Centered {
                    Text(s.couldNotLoadSpells(db.message), color = Nb.error)
                    NbPrimaryButton(s.useBundledData, { state.resetSpells() }, Modifier.padding(top = 16.dp), icon = null)
                }
                is DbState.Ready -> if (wide) WideLayout(state, db.db) else Router(state, db.db)
            }
            // nová verze appky (automatická kontrola GitHub Releases) – dole nad toastem
            cz.pfspells.ui.UpdateBanner(state, Modifier.align(Alignment.BottomCenter).padding(bottom = 84.dp, start = 16.dp, end = 16.dp))
            // dotaz před instalací aktualizace (s nabídkou exportu postav)
            (state.db as? DbState.Ready)?.let { cz.pfspells.ui.UpdateConfirmDialog(state, it.db) }
            state.toast?.let { msg ->
                Text(
                    msg.text(s),
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
                        .clip(Nb.shape).background(if (msg.isError) Nb.rust else Nb.ink).padding(horizontal = 16.dp, vertical = 10.dp),
                    color = Nb.panel, fontSize = 14.sp,
                )
            }
        }
    }
}

/**
 * Zpět tažením od levého okraje (jako na iOS): dotyk začne do 24 dp od okraje a táhne se doprava aspoň 80 dp,
 * víc vodorovně než svisle. Dotyky se nespotřebují, takže seznamy a tlačítka fungují normálně.
 */
private fun Modifier.swipeFromEdgeBack(onBack: () -> Unit): Modifier = pointerInput(Unit) {
    val edge = 24.dp.toPx()
    val trigger = 80.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.position.x > edge) return@awaitEachGesture
        var dx = 0f
        var dy = 0f
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
            dx = change.position.x - down.position.x
            dy = change.position.y - down.position.y
            if (!change.pressed) break
        }
        if (dx > trigger && kotlin.math.abs(dy) < dx) onBack()
    }
}

@Composable
private fun Router(state: AppState, db: SpellDatabase) {
    when (val s = state.current) {
        Screen.Decks -> DeckListScreen(state, db)
        Screen.Compendium -> SpellBrowserScreen(state, db, charId = null)
        Screen.Database -> DatabaseScreen(state, db)
        Screen.About -> cz.pfspells.ui.AboutScreen(state)
        is Screen.CharacterDetail -> DeckDetailScreen(state, db, s.charId)
        is Screen.Picker -> SpellBrowserScreen(state, db, charId = s.charId, classId = s.classId)
        is Screen.SpellDetail -> SpellDetailScreen(state, db, s.spellId, s.charId, s.classId)
        is Screen.FeatPicker -> cz.pfspells.ui.FeatPickerScreen(state, s.charId)
        is Screen.FeatDetail -> cz.pfspells.ui.FeatDetailScreen(state, s.featId, s.charId, s.pick, aside = false)
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
