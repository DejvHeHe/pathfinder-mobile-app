package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cz.pfspells.Strings
import cz.pfspells.data.ClassCatalog
import cz.pfspells.data.ClassGroup
import cz.pfspells.data.Ability
import cz.pfspells.data.ClassInfo
import cz.pfspells.data.MagicTradition
import cz.pfspells.data.RaceCatalog
import cz.pfspells.data.DeityCatalog
import cz.pfspells.data.SubclassIndex
import cz.pfspells.data.subclassType
import cz.pfspells.data.withSubclass
import cz.pfspells.data.group
import cz.pfspells.str

/** Výsledek [ClassPickerDialog]; rasa jen u nové postavy ([ClassPickerDialog] s `withName`). */
data class ClassPick(
    val name: String,
    val classId: String,
    val level: Int,
    val subclass: Map<String, String>,
    val race: String? = null,
    val raceBonus: Ability? = null,
    val deity: String = "",
)

/**
 * Výběr classy: hledání, karty seskupené podle počtu úrovní kouzel, úroveň a náhled spell levelů.
 * [withName] = pole pro jméno a rasu postavy (nová postava). [exclude] = classy, které postava už má.
 * [maxLevel] = nejvyšší možná úroveň přidávané classy (celkem max 20).
 * [subclasses] = index podtříd – classa s podtřídou (cleric, druid, oracle, sorcerer, wizard) ji pak vybere rovnou tady.
 */
@Composable
fun ClassPickerDialog(
    title: String,
    confirmLabel: String,
    withName: Boolean,
    exclude: Set<String> = emptySet(),
    maxLevel: Int = 20,
    subclasses: SubclassIndex? = null,
    initialClassId: String? = null,
    initialSubclass: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit,
    initialRace: String? = null,
    /** božstvo postavy (přidání classy) – klerik pak volí jen z jeho domén */
    characterDeity: String = "",
    onPick: (ClassPick) -> Unit,
) {
    val s = str
    val fonts = LocalNbFonts.current
    var name by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initialClassId?.let { id -> ClassCatalog.all.firstOrNull { it.id == id } }) }
    var level by remember { mutableIntStateOf(1) }
    // podtřída patří k vybrané classe – při změně classy se začíná znovu
    var sub by remember(selected?.id) { mutableStateOf(if (selected?.id == initialClassId) initialSubclass else emptyMap()) }
    val subType = selected?.let { subclassType(it.id) }.takeIf { subclasses != null }
    val rows: List<Any> = remember(query, exclude, s) { classRows(s, query, exclude) }
    var race by remember { mutableStateOf(initialRace) }
    var raceBonus by remember { mutableStateOf<Ability?>(null) }
    var pickRace by remember { mutableStateOf(false) }
    var deity by remember { mutableStateOf(characterDeity) }
    var pickDeity by remember { mutableStateOf(false) }
    var customDeity by remember { mutableStateOf(false) }
    if (pickDeity) {
        DeityPickerDialog(deity, onDismiss = { pickDeity = false }, onCustom = { pickDeity = false; customDeity = true }) { name ->
            pickDeity = false
            deity = name
        }
    }
    if (customDeity) {
        TextFieldDialog(s.deityLabel, "", s.deityLabel, onDismiss = { customDeity = false }) { v ->
            customDeity = false
            deity = v
        }
    }
    if (pickRace) {
        RacePickerDialog(race, onDismiss = { pickRace = false }) { id ->
            pickRace = false
            if (id != race) raceBonus = null
            race = id
        }
    }

    val sel = selected
    val lvl = level.coerceAtMost(maxLevel)

    @Composable
    fun classList(modifier: Modifier) {
        Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEach { row ->
                when (row) {
                    is ClassGroup -> SectionHeader(s.classGroup(row), size = 15.sp)
                    is ClassInfo -> ClassCard(s, row, row.id == sel?.id) { selected = row }
                }
            }
        }
    }

    @Composable
    fun nameField() {
        if (!withName) return
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text(s.characterNameLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        // rasa (nepovinná) + volba „+2 k jednomu atributu“
        val r = RaceCatalog.get(race)
        NbDashedButton("${s.raceLabel}: ${r?.let { s.raceNameOf(it) } ?: s.chooseSubclass}", { pickRace = true }, trailing = NbIcons.ChevronDown)
        if (r != null) Text(raceDetail(s, r, raceBonus), fontSize = 12.5.sp, color = Nb.rust, fontFamily = fonts.sans)
        if (r?.anyBonus == true) RaceBonusDropdown(raceBonus) { raceBonus = it }
        NbDashedButton(deityButtonText(s, deity), { pickDeity = true }, trailing = NbIcons.ChevronDown)
    }

    /** Úroveň, souhrn vybrané classy a podtřída. */
    @Composable
    fun settings() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonoLabel(s.levelLabel, Modifier.weight(1f))
            CountStepper(lvl, { level = it }, min = 1, max = maxLevel, size = 32.dp, numberSize = 17.sp)
        }
        Text(
            when {
                sel == null -> s.chooseClass
                sel.caster != null -> "${s.classNameOf(sel.id)} $lvl · ${spellRangeText(s, sel.caster!!, lvl)}"
                else -> "${s.classNameOf(sel.id)} $lvl · ${s.classGroup(sel.group).lowercase()}"
            },
            fontSize = 13.sp, color = if (sel == null) Nb.inkMuted else Nb.rust, fontFamily = fonts.sans,
        )
        if (sel != null && subType != null && subclasses != null) {
            SectionHeader(s.subclassLabel(subType), size = 15.sp)
            SubclassFields(sel.id, subType, sub, subclasses, deity = DeityCatalog.get(deity)) { key, v -> sub = sub.withSubclass(key, v) }
            Text(s.subclassLaterHint, fontSize = 12.5.sp, color = Nb.inkMuted)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            // široké okno: seznam class vlevo, nastavení vpravo; úzké: vše pod sebou a posouvá se
            val wide = maxWidth >= 720.dp
            val width = if (wide) minOf(860.dp, maxWidth - 64.dp) else minOf(560.dp, maxWidth - 32.dp)
            val maxH = if (wide) minOf(620.dp, maxHeight - 64.dp) else maxHeight - 48.dp
            Surface(
                Modifier.width(width).let { if (wide) it.height(maxH) else it.heightIn(max = maxH) },
                shape = MaterialTheme.shapes.extraLarge, color = Nb.panel, shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 16.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = Nb.ink, modifier = Modifier.padding(bottom = 14.dp))
                    if (wide) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                NbSearchField(query, { query = it }, s.searchClasses, Modifier.fillMaxWidth())
                                classList(Modifier.fillMaxWidth().weight(1f))
                            }
                            Column(
                                Modifier.width(340.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                nameField()
                                settings()
                            }
                        }
                    } else {
                        Column(
                            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            nameField()
                            NbSearchField(query, { query = it }, s.searchClasses, Modifier.fillMaxWidth())
                            classList(Modifier.fillMaxWidth().height(240.dp))
                            settings()
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) }
                        NbPrimaryButton(
                            confirmLabel,
                            {
                                sel?.let { cls ->
                                    onPick(
                                        ClassPick(
                                            name.trim().ifEmpty { "${s.classNameOf(cls.id)} $lvl" }, cls.id, lvl,
                                            if (subType != null) sub else emptyMap(),
                                            race = race.takeIf { withName }, raceBonus = raceBonus.takeIf { withName },
                                            deity = deity.takeIf { withName }.orEmpty(),
                                        ),
                                    )
                                }
                            },
                            icon = null, enabled = sel != null,
                        )
                    }
                }
            }
        }
    }
}

/** Řádky seznamu: nadpis skupiny ([ClassGroup]) a pod ním classy seřazené podle názvu v jazyce rozhraní. */
private fun classRows(s: Strings, query: String, exclude: Set<String>): List<Any> {
    val q = query.trim().lowercase()
    val matching = ClassCatalog.all.filter { c ->
        c.id !in exclude && (q.isEmpty() || s.classNameOf(c.id).lowercase().contains(q) || c.name.lowercase().contains(q))
    }
    return ClassGroup.entries.flatMap { g ->
        val list = matching.filter { it.group == g }.sortedBy { s.classNameOf(it.id).lowercase() }
        if (list.isEmpty()) emptyList() else listOf<Any>(g) + list
    }
}

@Composable
private fun ClassCard(s: Strings, c: ClassInfo, selected: Boolean, onClick: () -> Unit) {
    val base = Modifier.fillMaxWidth().clip(Nb.shape)
    Column(
        (if (selected) base.background(Nb.accent) else base.background(Nb.paper).dashedBorder())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val local = s.classNameOf(c.id)
            Text(
                local, Modifier.weight(1f),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Color.White else Nb.ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val detail = buildString {
                if (local != c.name) append(c.name).append(" · ")
                append("d").append(c.hd)
            }
            Text(detail, fontSize = 12.sp, color = if (selected) Color.White else Nb.inkMuted, maxLines = 1)
        }
        // štítky: druh magie (tajemná / božská / psychická / extrakty) a způsob sesílání
        c.caster?.let { cc ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ClassTag(s.magicTradition(cc.tradition), traditionColor(cc.tradition), selected)
                ClassTag(s.castingKind(cc.kind), Nb.inkMuted, selected)
            }
        }
    }
}

private fun traditionColor(t: MagicTradition): Color = when (t) {
    MagicTradition.ARCANE -> Nb.accent
    MagicTradition.DIVINE -> Nb.ochre
    MagicTradition.PSYCHIC -> Nb.rust
    MagicTradition.ALCHEMICAL -> Nb.inkMuted
}

/** Malý štítek na kartě classy (na vybrané kartě bílý text na průsvitném podkladu). */
@Composable
private fun ClassTag(text: String, color: Color, selected: Boolean) {
    Text(
        text,
        Modifier.clip(Nb.shape)
            .background(if (selected) Color.White.copy(alpha = 0.18f) else Nb.tag)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        color = if (selected) Color.White else color,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = LocalNbFonts.current.sans,
        maxLines = 1,
    )
}
