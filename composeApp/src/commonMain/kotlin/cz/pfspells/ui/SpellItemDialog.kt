package cz.pfspells.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Spell
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.SpellItemKind
import cz.pfspells.str

/**
 * Lektvar / svitek / hůlka ze spellu: výběr druhu, spellu, spell listu (určuje level) a úrovně sesilatele,
 * cena podle pravidel PF1 ([SpellItemKind.price]). Výsledek se přidá jako vlastní položka inventáře.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellItemDialog(state: AppState, db: SpellDatabase, onDismiss: () -> Unit, onAdd: (InvEntry) -> Unit) {
    val s = str
    val fonts = LocalNbFonts.current
    var kind by remember { mutableStateOf(SpellItemKind.POTION) }
    var query by remember { mutableStateOf("") }
    var spell by remember { mutableStateOf<Spell?>(null) }
    var listKey by remember { mutableStateOf<String?>(null) }
    var cl by remember { mutableIntStateOf(1) }
    var qty by remember { mutableIntStateOf(1) }
    val spellLevel = spell?.let { sp -> listKey?.let { sp.levels[it] } } ?: 0
    val minCl = SpellItemKind.minCasterLevel(spellLevel)
    val matches = remember(query, db) {
        val q = query.trim().lowercase()
        if (q.length < 2) emptyList() else db.spells.filter { it.searchName.contains(q) }.take(8)
    }

    fun pick(sp: Spell) {
        spell = sp
        query = ""
        val key = sp.levels.minByOrNull { it.value }?.key
        listKey = key
        cl = SpellItemKind.minCasterLevel(key?.let { sp.levels[it] } ?: 0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(s.spellItemTitle, color = Nb.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpellItemKind.entries.forEach { k -> NbFilterChip(s.spellItemKind(k), kind == k) { kind = k } }
                }
                NbSearchField(query, { query = it }, s.searchSpells, Modifier.fillMaxWidth())
                matches.forEach { sp ->
                    Text(
                        sp.name,
                        Modifier.fillMaxWidth().clip(Nb.shape).clickable { pick(sp) }.bottomRule(Nb.ruleSoft)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        fontSize = 15.sp, color = Nb.ink,
                    )
                }
                val sp = spell
                if (sp == null) {
                    if (matches.isEmpty()) Text(s.pickSpell, color = Nb.inkMuted, fontSize = 14.sp)
                } else {
                    Text(kind.name(sp), fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Nb.ink)
                    val keys = sp.levels.entries.sortedBy { it.value }.map { it.key }
                    SimpleDropdown(
                        s.spellListLabel, keys, listKey ?: keys.first(),
                        { "${s.spellListName(it)} ${sp.levels[it]}" },
                        { k ->
                            listKey = k
                            cl = maxOf(cl, SpellItemKind.minCasterLevel(sp.levels[k] ?: 0))
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel(s.casterLevel, Modifier.weight(1f))
                        CountStepper(cl, { cl = it }, min = minCl, max = 20, size = 32.dp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel(s.quantity, Modifier.weight(1f))
                        CountStepper(qty, { qty = it }, min = 1, max = 99, size = 32.dp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.cost, Modifier.weight(1f), fontSize = 15.sp, color = Nb.ink)
                        Text(
                            priceLabel(kind.price(spellLevel, cl), state.pricesRevealed),
                            fontFamily = fonts.mono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Nb.ink,
                        )
                    }
                    if (!kind.allows(spellLevel)) Text(s.spellLevelTooHigh(kind), color = Nb.error, fontSize = 13.sp)
                    if (kind == SpellItemKind.POTION) Text(s.potionNote, color = Nb.inkMuted, fontSize = 12.5.sp, lineHeight = 17.sp)
                    if (sp.components?.contains(" gp") == true) {
                        Text(s.materialCostNote, color = Nb.rust, fontSize = 12.5.sp, lineHeight = 17.sp)
                    }
                }
            }
        },
        confirmButton = {
            NbPrimaryButton(
                s.addToInventory,
                { spell?.let { onAdd(kind.entry(it, spellLevel, cl, qty)) } },
                icon = null,
                enabled = spell != null && kind.allows(spellLevel),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}
