package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cz.pfspells.Strings
import cz.pfspells.data.Ability
import cz.pfspells.data.RaceCatalog
import cz.pfspells.data.RaceGroup
import cz.pfspells.data.RaceInfo
import cz.pfspells.data.raceInfo
import cz.pfspells.data.raceMod
import cz.pfspells.str

/** Název rasy v jazyce rozhraní. */
fun Strings.raceNameOf(r: RaceInfo): String = raceName(r.id, r.name)

/** „+2 DEX, +2 INT, −2 CON“ / „+2 k jednomu atributu“ (u zvoleného [bonus] „+2 SÍL“). */
fun raceModsText(s: Strings, r: RaceInfo, bonus: Ability? = null): String = when {
    r.anyBonus && bonus == null -> s.anyAbilityBonus
    else -> Ability.entries.mapNotNull { a ->
        r.mod(a, bonus).takeIf { it != 0 }?.let { v -> (if (v < 0) "−${-v}" else "+$v") + " " + s.abilityShort(a) }
    }.joinToString(", ")
}

/** Úpravy atributů rasy postavy včetně alternativních traitů (Dual Talent, Keen Kitsune…). */
fun characterRaceModsText(s: Strings, c: cz.pfspells.data.Character): String {
    val r = c.raceInfo ?: return ""
    if (r.anyBonus && c.raceBonus == null && !cz.pfspells.data.RacialTraits.replacesRaceBonus(c)) return s.anyAbilityBonus
    return Ability.entries.mapNotNull { a ->
        c.raceMod(a).takeIf { it != 0 }?.let { v -> (if (v < 0) "−${-v}" else "+$v") + " " + s.abilityShort(a) }
    }.joinToString(", ")
}

/** „Střední · 30 ft · +2 SÍL, …“ pro rasu postavy (včetně alternativních traitů). */
fun characterRaceDetail(s: Strings, c: cz.pfspells.data.Character): String {
    val r = c.raceInfo ?: return ""
    return listOf(s.creatureSize(r.size), "${r.speed} ft", characterRaceModsText(s, c)).filter { it.isNotEmpty() }.joinToString(" · ")
}

/** „Malá · 20 ft · +2 CON, +2 CHA, −2 SÍL“ */
fun raceDetail(s: Strings, r: RaceInfo, bonus: Ability? = null): String =
    listOf(s.creatureSize(r.size), "${r.speed} ft", raceModsText(s, r, bonus)).filter { it.isNotEmpty() }.joinToString(" · ")

/** Volba atributu pro rasové „+2 k jednomu atributu“ (human, half-elf, half-orc). */
@Composable
fun RaceBonusDropdown(selected: Ability?, modifier: Modifier = Modifier, onSelect: (Ability?) -> Unit) {
    val s = str
    SimpleDropdown(
        label = s.raceBonusLabel,
        options = listOf<Ability?>(null) + Ability.entries,
        selected = selected,
        optionLabel = { it?.let(s::abilityLong) ?: s.chooseOption },
        onSelect = onSelect,
        modifier = modifier,
    )
}

/**
 * Výběr rasy: hledání, rasy po skupinách (základní, významné, neobvyklé, ostatní), velikost, rychlost a úpravy atributů.
 * Klepnutím se rasa vybere a dialog zavře; [current] je zvýrazněná.
 */
@Composable
fun RacePickerDialog(current: String?, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    val s = str
    var query by remember { mutableStateOf("") }
    val rows: List<Any> = remember(query, s) { raceRows(s, query) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            val width = minOf(560.dp, maxWidth - 32.dp)
            val maxH = minOf(640.dp, maxHeight - 48.dp)
            Surface(
                Modifier.width(width).heightIn(max = maxH),
                shape = MaterialTheme.shapes.extraLarge, color = Nb.panel, shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 16.dp)) {
                    Text(s.chooseRaceTitle, style = MaterialTheme.typography.headlineSmall, color = Nb.ink, modifier = Modifier.padding(bottom = 14.dp))
                    NbSearchField(query, { query = it }, s.searchRaces, Modifier.fillMaxWidth())
                    Column(
                        Modifier.weight(1f, fill = false).padding(top = 10.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        rows.forEach { row ->
                            when (row) {
                                is RaceGroup -> SectionHeader(s.raceGroup(row), size = 15.sp)
                                is RaceInfo -> RaceCard(s, row, row.id == current) { onPick(row.id) }
                            }
                        }
                    }
                    Text(s.raceSizeHint, fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 10.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (current != null) TextButton(onClick = { onPick(null) }) { Text(s.noRace, color = Nb.error) }
                        TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) }
                    }
                }
            }
        }
    }
}

/** Řádky seznamu: nadpis skupiny a rasy seřazené podle názvu v jazyce rozhraní. */
private fun raceRows(s: Strings, query: String): List<Any> {
    val q = query.trim().lowercase()
    val matching = RaceCatalog.all.filter { r ->
        q.isEmpty() || s.raceNameOf(r).lowercase().contains(q) || r.name.lowercase().contains(q)
    }
    return RaceGroup.entries.flatMap { g ->
        val list = matching.filter { it.group == g }.sortedBy { s.raceNameOf(it).lowercase() }
        if (list.isEmpty()) emptyList() else listOf<Any>(g) + list
    }
}

@Composable
private fun RaceCard(s: Strings, r: RaceInfo, selected: Boolean, onClick: () -> Unit) {
    val base = Modifier.fillMaxWidth().clip(Nb.shape)
    val fg = if (selected) Color.White else Nb.ink
    val muted = if (selected) Color.White else Nb.inkMuted
    Column(
        (if (selected) base.background(Nb.accent) else base.background(Nb.paper).dashedBorder())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val local = s.raceNameOf(r)
            Text(local, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (local != r.name) Text(r.name, fontSize = 12.sp, color = muted, maxLines = 1)
        }
        Text(raceDetail(s, r), fontSize = 12.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
