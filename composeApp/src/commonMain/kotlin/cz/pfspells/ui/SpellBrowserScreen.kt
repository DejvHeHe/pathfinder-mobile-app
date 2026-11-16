package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.BrowserFilter
import cz.pfspells.Screen
import cz.pfspells.data.CasterClass
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Spell
import cz.pfspells.data.SpellDatabase

/**
 * deckId == null → kompendium všech spellů (volitelně filtr podle classy).
 * deckId != null → výběr do decku: jen spelly dostupné pro classu a úroveň decku.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellBrowserScreen(
    state: AppState,
    db: SpellDatabase,
    deckId: String?,
    wide: Boolean = false,
    selectedSpellId: String? = null,
    /** na širokém okně je hledání v hlavičce appky */
    showSearch: Boolean = true,
) {
    val deck = deckId?.let { state.deck(it) }
    if (deckId != null && deck == null) {
        LaunchedEffect(deckId) { state.pop() }
        return
    }
    val picker = deck != null
    val filter = if (picker) state.pickerFilter else state.compendiumFilter
    val setFilter: (BrowserFilter) -> Unit = {
        if (picker) state.pickerFilter = it else state.compendiumFilter = it
    }
    val listState = if (picker) state.pickerList else state.compendiumList

    val cls: CasterClass? = if (deck != null) CasterClasses.get(deck.classId) else filter.classId?.let(CasterClasses::get)
    val maxLevel = if (deck != null) cls?.maxSpellLevel(deck.classLevel) ?: -1 else 9
    val minLevel = if (cls != null && !cls.hasCantrips) 1 else 0
    val schools = remember(db) { db.spells.map { it.school }.distinct().sorted() }

    val results: List<Spell> = remember(db, filter, deck?.classId, deck?.classLevel, deck?.spells, cls) {
        val q = filter.query.trim().lowercase()
        db.spells.filter { s ->
            if (deck != null) {
                if (cls == null || !cls.canCast(s, deck.classLevel)) return@filter false
                if (filter.hideInDeck && s.id in deck.spells) return@filter false
            } else if (cls != null && cls.spellLevelOf(s) == null) return@filter false

            if (filter.level != null) {
                val ok = if (cls != null) cls.spellLevelOf(s) == filter.level else filter.level in s.levels.values
                if (!ok) return@filter false
            }
            if (filter.school != null && s.school != filter.school) return@filter false
            if (q.isNotEmpty()) {
                val hit = s.searchName.contains(q) ||
                    (filter.fullText && (
                        s.description.contains(q, ignoreCase = true) ||
                            (s.shortDescription?.contains(q, ignoreCase = true) == true) ||
                            (s.descriptor?.contains(q, ignoreCase = true) == true)
                        ))
                if (!hit) return@filter false
            }
            true
        }
    }

    val back: () -> Unit = {
        when {
            wide && deck != null -> state.openRoot(Screen.DeckDetail(deck.id))
            wide -> state.popMain()
            else -> state.pop()
        }
    }

    NbScreen(
        title = if (picker) "Add to: ${deck!!.name}" else "Compendium",
        subtitle = if (picker) classLevelSummary(cls, deck!!.classLevel) else "${db.spells.size} spells in the database",
        onBack = back,
        wide = wide,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (wide) 0.dp else 16.dp).padding(top = if (wide) 14.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showSearch) {
                NbSearchField(filter.query, { setFilter(filter.copy(query = it)) }, "Search spells…", Modifier.fillMaxWidth())
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SimpleDropdown(
                    label = "School",
                    options = listOf<String?>(null) + schools,
                    selected = filter.school,
                    optionLabel = { it ?: "all" },
                    onSelect = { setFilter(filter.copy(school = it)) },
                )
                if (!picker) {
                    SimpleDropdown(
                        label = "Class",
                        options = listOf<CasterClass?>(null) + CasterClasses.all,
                        selected = cls,
                        optionLabel = { it?.name ?: "all" },
                        onSelect = { setFilter(filter.copy(classId = it?.id, level = null)) },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                NbCheck(filter.fullText, "search descriptions too") { setFilter(filter.copy(fullText = it)) }
                if (picker) NbCheck(filter.hideInDeck, "hide added") { setFilter(filter.copy(hideInDeck = it)) }
            }
            if (maxLevel >= minLevel) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { NbFilterChip("All", filter.level == null) { setFilter(filter.copy(level = null)) } }
                    items((minLevel..maxLevel).toList()) { lvl ->
                        NbFilterChip(if (lvl == 0) "0" else "L$lvl", filter.level == lvl) {
                            setFilter(filter.copy(level = if (filter.level == lvl) null else lvl))
                        }
                    }
                }
            }
            MonoLabel("${results.size} results", Modifier.padding(top = 2.dp), size = 11.sp)
        }

        if (picker && maxLevel < 0) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "${cls?.name ?: "This class"} cannot cast spells at level ${deck!!.classLevel} yet (from level 4).",
                    textAlign = TextAlign.Center, color = Nb.inkMuted,
                )
            }
            return@NbScreen
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = if (wide) 0.dp else 16.dp, end = if (wide) 0.dp else 16.dp, top = 4.dp, bottom = 32.dp),
        ) {
            items(results, key = { it.id }) { spell ->
                SpellRow(
                    spell = spell,
                    levelForClass = cls?.spellLevelOf(spell),
                    countInDeck = deck?.spells?.get(spell.id),
                    selected = spell.id == selectedSpellId,
                    onClick = { state.showSpell(spell.id, deckId) },
                    onAdd = if (deck != null) ({ state.addSpell(deck.id, spell.id) }) else null,
                )
            }
        }
    }
}

@Composable
private fun NbCheck(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.clip(Nb.shape).clickable { onChange(!checked) }.padding(end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked, onChange,
            colors = CheckboxDefaults.colors(checkedColor = Nb.accent, uncheckedColor = Nb.dash, checkmarkColor = androidx.compose.ui.graphics.Color.White),
        )
        Text(label, fontSize = 14.sp, color = Nb.ink)
    }
}

@Composable
private fun SpellRow(
    spell: Spell,
    levelForClass: Int?,
    countInDeck: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    onAdd: (() -> Unit)?,
) {
    val base = Modifier.fillMaxWidth()
    Row(
        (if (selected) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule())
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 10.dp else 0.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    spell.name, Modifier.weight(1f, fill = false),
                    fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (levelForClass != null) NbTag(if (levelForClass == 0) "0" else "L$levelForClass", Nb.rust)
            }
            Text(
                if (levelForClass == null) "${spellSubline(spell)} · ${spell.levelText}" else spellSubline(spell),
                fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
            spell.shortDescription?.let {
                Text(it, fontSize = 13.sp, color = Nb.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (onAdd != null) {
            NbSquareButton("+", onAdd, "Add to deck", size = 40.dp) {
                if (countInDeck != null && countInDeck > 0) {
                    Text("×$countInDeck", fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Nb.accent)
                } else {
                    Icon(NbIcons.Plus, null, tint = Nb.ink, modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}
