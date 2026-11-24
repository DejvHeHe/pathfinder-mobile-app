package cz.pfspells.ui

import androidx.compose.foundation.layout.size
import cz.pfspells.SpellSort
import cz.pfspells.str
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
 * charId == null → kompendium všech spellů (volitelně filtr podle classy).
 * charId != null → výběr pro classu [classId] postavy: jen spelly dostupné pro classu a její úroveň.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpellBrowserScreen(
    state: AppState,
    db: SpellDatabase,
    charId: String?,
    classId: String? = null,
    wide: Boolean = false,
    selectedSpellId: String? = null,
    /** na širokém okně je hledání v hlavičce appky */
    showSearch: Boolean = true,
) {
    val deck = charId?.let { state.spellbook(it, classId) }
    if (charId != null && deck == null) {
        LaunchedEffect(charId) { state.pop() }
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

    val favorites = state.favorites
    val results: List<Spell> = remember(db, filter, deck?.classId, deck?.classLevel, deck?.spells, cls, if (filter.favoritesOnly) favorites else null) {
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
            if (filter.pfsOnly && !s.pfsLegal) return@filter false
            if (filter.favoritesOnly && s.id !in favorites) return@filter false
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
        }.let { list ->
            when (filter.sort) {
                SpellSort.NAME -> list // databáze je seřazená podle jména
                SpellSort.LEVEL -> list.sortedWith(
                    compareBy<Spell>({ cls?.spellLevelOf(it) ?: it.levels.values.minOrNull() ?: 99 }, { it.searchName }),
                )
                SpellSort.SCHOOL -> list.sortedWith(compareBy<Spell>({ it.school }, { it.searchName }))
            }
        }
    }

    val back: () -> Unit = {
        when {
            wide && deck != null -> state.openRoot(Screen.CharacterDetail(deck.id))
            wide -> state.popMain()
            else -> state.pop()
        }
    }

    val s = str
    NbScreen(
        title = if (picker) s.addTo(deck!!.name) else s.compendium,
        subtitle = if (picker) classLevelSummary(s, cls, deck!!.classLevel) else s.spellsInDatabase(db.spells.size),
        onBack = back,
        wide = wide,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (wide) 0.dp else 16.dp).padding(top = if (wide) 14.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showSearch) {
                NbSearchField(filter.query, { setFilter(filter.copy(query = it)) }, s.searchSpells, Modifier.fillMaxWidth())
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SimpleDropdown(
                    label = s.school,
                    options = listOf<String?>(null) + schools.sortedBy { s.school(it) },
                    selected = filter.school,
                    optionLabel = { it?.let(s::school) ?: s.anyOption },
                    onSelect = { setFilter(filter.copy(school = it)) },
                )
                if (!picker) {
                    SimpleDropdown(
                        label = s.classLabel,
                        options = listOf<CasterClass?>(null) + CasterClasses.all,
                        selected = cls,
                        optionLabel = { c -> c?.let { s.classOption(it.id, it.name) } ?: s.anyOption },
                        onSelect = { setFilter(filter.copy(classId = it?.id, level = null)) },
                    )
                }
                SimpleDropdown(
                    label = s.sortLabel,
                    options = SpellSort.entries,
                    selected = filter.sort,
                    optionLabel = { s.spellSort(it) },
                    onSelect = { setFilter(filter.copy(sort = it)) },
                )
            }
            FlowRow(verticalArrangement = Arrangement.Center) {
                NbCheck(filter.fullText, s.searchDescriptions) { setFilter(filter.copy(fullText = it)) }
                if (picker) NbCheck(filter.hideInDeck, s.hideAdded) { setFilter(filter.copy(hideInDeck = it)) }
                NbCheck(filter.favoritesOnly, s.favoritesOnly) { setFilter(filter.copy(favoritesOnly = it)) }
                NbCheck(filter.pfsOnly, s.pfsOnly) { setFilter(filter.copy(pfsOnly = it)) }
                // přísný režim (globální, stejný jako v menu) – jen při výběru kouzel postavě
                if (picker) NbCheck(state.strictMode, s.strictMode) { state.changeStrictMode(it) }
            }
            if (maxLevel >= minLevel) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { NbFilterChip(s.all, filter.level == null) { setFilter(filter.copy(level = null)) } }
                    items((minLevel..maxLevel).toList()) { lvl ->
                        NbFilterChip(if (lvl == 0) "0" else "L$lvl", filter.level == lvl) {
                            setFilter(filter.copy(level = if (filter.level == lvl) null else lvl))
                        }
                    }
                }
            }
            MonoLabel(s.results(results.size), Modifier.padding(top = 2.dp), size = 11.sp)
        }

        if (picker && maxLevel < 0) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    s.cannotCastYet(cls?.let { s.nameOf(it) }, deck!!.classLevel),
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
                    favorite = spell.id in favorites,
                    countInDeck = deck?.spells?.get(spell.id),
                    selected = spell.id == selectedSpellId,
                    onClick = { state.showSpell(spell.id, charId, deck?.classId) },
                    onAdd = if (deck != null) ({ state.addSpell(deck.id, deck.classId, spell.id) }) else null,
                )
            }
        }
    }
}

@Composable
fun NbCheck(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
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
    favorite: Boolean,
    countInDeck: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    onAdd: (() -> Unit)?,
) {
    val base = Modifier.fillMaxWidth()
    val s = str
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
                if (favorite) Icon(NbIcons.StarFilled, s.favorite, tint = Nb.rust, modifier = Modifier.size(14.dp))
                if (levelForClass != null) NbTag(if (levelForClass == 0) "0" else "L$levelForClass", Nb.rust)
            }
            Text(
                if (levelForClass == null) "${spellSubline(s, spell)} · ${spell.levels.entries.joinToString { "${s.spellListName(it.key)} ${it.value}" }}" else spellSubline(s, spell),
                fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
            spell.shortDescription?.let {
                Text(it, fontSize = 13.sp, color = Nb.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (onAdd != null) {
            NbSquareButton("+", onAdd, str.addToCharacter, size = 40.dp) {
                if (countInDeck != null && countInDeck > 0) {
                    Text("×$countInDeck", fontFamily = LocalNbFonts.current.mono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Nb.accent)
                } else {
                    Icon(NbIcons.Plus, null, tint = Nb.ink, modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
}
