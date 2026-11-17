package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.Screen
import cz.pfspells.Section
import cz.pfspells.data.SpellDatabase

/**
 * Široké okno (desktop/tablet): hlavička (záložky Spellbook / Inventory) | boční panel · hlavní obrazovka · detail.
 * Vše se odvozuje ze stejného back stacku jako na mobilu.
 */
@Composable
fun WideLayout(state: AppState, db: SpellDatabase) {
    val stack = state.stack
    val aside = stack.lastOrNull()?.takeIf { it.isAside }
    val main = stack.lastOrNull { !it.isAside && it != Screen.Decks }
    val fonts = LocalNbFonts.current
    val inventorySection = state.section == Section.INVENTORY
    if (inventorySection) LaunchedEffect(Unit) { state.ensureItems() }

    Column(Modifier.fillMaxSize()) {
        // hlavička
        Row(
            Modifier.fillMaxWidth().background(Nb.panel).dashedEdge(Side.Bottom).padding(horizontal = 28.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                "PF1 Spellbook",
                fontFamily = fonts.serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold,
                fontSize = 28.sp, color = Nb.accent,
            )
            SectionTabs(state, Modifier.weight(1f))
            if (inventorySection) {
                val inv = (main as? Screen.InventoryDetail)?.invId?.let { state.inventory(it) }
                if (inv != null) {
                    NbSearchField(
                        value = state.inventoryQuery,
                        onValueChange = { state.inventoryQuery = it },
                        placeholder = "Search item…",
                        modifier = Modifier.width(340.dp),
                        background = Nb.paper,
                    )
                    NbPrimaryButton("Add item", { state.openRoot(Screen.ItemPicker(inv.id)) })
                } else if (main is Screen.ItemPicker) {
                    NbDashedButton("Back to inventory", { state.openRoot(Screen.InventoryDetail(main.invId)) })
                }
            } else {
                NbSearchField(
                    value = state.compendiumFilter.query,
                    onValueChange = {
                        state.compendiumFilter = state.compendiumFilter.copy(query = it)
                        if (main != Screen.Compendium) state.openRoot(Screen.Compendium)
                    },
                    placeholder = "Search spells…",
                    modifier = Modifier.width(340.dp),
                    background = Nb.paper,
                )
                NbDashedButton("Compendium", { state.openRoot(Screen.Compendium) }, icon = NbIcons.Book)
            }
            AppMenu(state, db, onDatabase = { state.openRoot(Screen.Database) }, onAbout = { state.openRoot(Screen.About) })
        }

        Row(Modifier.weight(1f).fillMaxWidth()) {
            val sidebarMod = Modifier.width(300.dp).fillMaxHeight().dashedEdge(Side.End)
            if (inventorySection) {
                val invId = when (main) {
                    is Screen.InventoryDetail -> main.invId
                    is Screen.ItemPicker -> main.invId
                    else -> null
                }
                InventorySidebar(state, invId?.let { state.inventory(it) }, sidebarMod)
            } else {
                val selectedDeckId = when (main) {
                    is Screen.DeckDetail -> main.deckId
                    is Screen.Picker -> main.deckId
                    else -> null
                }
                DeckSidebar(state, selectedDeckId, sidebarMod)
            }
            val selectedSpell = (aside as? Screen.SpellDetail)?.spellId
            val selectedItem = aside as? Screen.ItemDetail
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (main) {
                    is Screen.DeckDetail -> DeckDetailScreen(state, db, main.deckId, wide = true, selectedSpellId = selectedSpell)
                    is Screen.Picker -> SpellBrowserScreen(state, db, main.deckId, wide = true, selectedSpellId = selectedSpell)
                    Screen.Compendium -> SpellBrowserScreen(state, db, null, wide = true, selectedSpellId = selectedSpell, showSearch = false)
                    Screen.Database -> DatabaseScreen(state, db, wide = true)
                    Screen.About -> AboutScreen(state, wide = true)
                    is Screen.InventoryDetail -> InventoryScreen(state, main.invId, wide = true, selectedEntryId = selectedItem?.entryId)
                    is Screen.ItemPicker -> ItemPickerScreen(state, main.invId, wide = true, selectedItemId = selectedItem?.itemId)
                    else -> Placeholder(db, inventorySection, state.inventories.isEmpty())
                }
            }
            if (aside != null) {
                Box(
                    Modifier.widthIn(min = 380.dp, max = 500.dp).fillMaxWidth(0.34f).fillMaxHeight()
                        .background(Nb.panel).dashedEdge(Side.Start),
                ) {
                    when (aside) {
                        is Screen.SpellDetail -> SpellDetailScreen(state, db, aside.spellId, aside.deckId, aside = true)
                        is Screen.ItemDetail -> ItemDetailScreen(state, aside.itemId, aside.invId, aside.entryId, aside = true)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun Placeholder(db: SpellDatabase, inventory: Boolean, noInventories: Boolean) {
    Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(
            when {
                inventory && noInventories -> "Create an inventory on the left – one per character, or a shared party stash."
                inventory -> "Pick an inventory on the left, or create a new one."
                else -> "Pick a deck on the left or create a new one.\nThe compendium has all ${db.spells.size} spells."
            },
            textAlign = TextAlign.Center, color = Nb.inkMuted, fontSize = 16.sp, lineHeight = 24.sp,
        )
    }
}
