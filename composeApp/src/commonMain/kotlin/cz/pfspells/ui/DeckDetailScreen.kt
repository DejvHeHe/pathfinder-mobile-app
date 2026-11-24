package cz.pfspells.ui

import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.Screen
import cz.pfspells.Strings
import cz.pfspells.data.resources
import cz.pfspells.data.isOppositionSpell
import cz.pfspells.data.hasSubclassSlot
import cz.pfspells.data.subclassType
import cz.pfspells.data.SubclassType
import cz.pfspells.data.levelSlots
import cz.pfspells.data.LevelSlots
import cz.pfspells.data.CastingKind
import cz.pfspells.data.CasterClass
import cz.pfspells.data.Ability
import cz.pfspells.data.Alignment as CharAlignment
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import cz.pfspells.data.CasterClasses
import cz.pfspells.data.Character
import cz.pfspells.data.ClassEntry
import cz.pfspells.data.Spell
import cz.pfspells.data.SpellDatabase
import cz.pfspells.data.abilityMod
import cz.pfspells.data.stats
import cz.pfspells.data.raceInfo
import cz.pfspells.data.deityInfo
import cz.pfspells.data.deityConflicts
import cz.pfspells.data.raceMod
import cz.pfspells.data.companionMod
import cz.pfspells.data.abilityBonus
import cz.pfspells.data.scores
import cz.pfspells.str

/**
 * Detail postavy: classy (multiclass, úrovně), souhrn (úroveň, BAB, záchrany), atributy
 * a pro každou sesílací classu její kouzla seskupená podle spell levelu.
 */
@Composable
fun DeckDetailScreen(
    state: AppState,
    db: SpellDatabase,
    charId: String,
    wide: Boolean = false,
    selectedSpellId: String? = null,
    selectedFeatPick: Int? = null,
) {
    val c = state.character(charId)
    if (c == null) {
        LaunchedEffect(charId) { if (wide) state.openRoot(Screen.Decks) else state.pop() }
        return
    }
    val s = str
    var confirmDelete by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var export by remember { mutableStateOf(false) }
    var addClass by remember { mutableStateOf(false) }
    var removeClass by remember { mutableStateOf<String?>(null) }
    var editAbility by remember { mutableStateOf<Ability?>(null) }
    var subclassFor by remember { mutableStateOf<String?>(null) }
    var pickRace by remember { mutableStateOf(false) }
    var editDeity by remember { mutableStateOf(false) }
    var customDeity by remember { mutableStateOf(false) }
    if (export) ExportDialog(state, listOf(c), db) { export = false }

    val stats = c.stats()
    val casters = c.classes.filter { CasterClasses.get(it.classId) != null }
    val openPicker: (String) -> Unit = { classId ->
        if (wide) state.openRoot(Screen.Picker(charId, classId)) else state.push(Screen.Picker(charId, classId))
    }
    val missing = c.classes.sumOf { e -> e.spells.keys.count { db.get(it) == null } }

    NbScreen(
        title = c.name,
        subtitle = if (wide) "${classesSummary(s, c)} · ${s.statsLine(stats)}" else classesSummary(s, c),
        onBack = if (wide) null else ({ state.pop() }),
        wide = wide,
        // v herním režimu se kouzla nepřidávají – tlačítko by překrývalo sloty
        primary = if (state.gameMode) null else casters.singleOrNull()?.let { e -> s.addSpells to { openPicker(e.classId) } },
        actions = {
            NbIconButton(NbIcons.Share, s.exportCharacter, { export = true }, Nb.inkMuted, 19.dp)
            NbIconButton(NbIcons.Pencil, s.rename, { rename = true }, Nb.inkMuted, 19.dp)
            NbIconButton(NbIcons.Trash, s.deleteCharacter, { confirmDelete = true }, Nb.inkMuted, 19.dp)
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = if (wide) 0.dp else 16.dp, end = if (wide) 0.dp else 16.dp, top = 6.dp, bottom = 96.dp),
        ) {
            if (c.isCompanion) item(key = "companion") { CompanionHeader(state, c, wide) }
            item(key = "classes") {
                ClassesPanel(
                    state, c, wide,
                    onAdd = { addClass = true }, onRemove = { removeClass = it }, onSubclass = { subclassFor = it }, onRace = { pickRace = true }, onDeity = { editDeity = true },
                )
            }
            if (!wide) {
                item(key = "stats") {
                    Text(s.statsLine(stats), fontSize = 13.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 8.dp))
                }
            }
            item(key = "abilities") {
                Column(Modifier.padding(top = 4.dp)) {
                    SectionHeader(s.abilitiesHeader)
                    AbilityRow(s, c) { editAbility = it }
                    c.raceInfo?.let { r ->
                        val mods = characterRaceModsText(s, c)
                        if (Ability.entries.any { c.raceMod(it) != 0 }) {
                            Text(s.raceIncludedHint(mods), fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    // zvířecí společník: Str/Dex bonus z tabulky a vylepšení druhu
                    val companionMods = Ability.entries.mapNotNull { a ->
                        c.companionMod(a).takeIf { it != 0 }?.let { v -> (if (v < 0) "−${-v}" else "+$v") + " " + s.abilityShort(a) }
                    }
                    if (companionMods.isNotEmpty()) {
                        Text(s.companionIncludedHint(companionMods.joinToString(", ")), fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                    // nasazené vybavení z propojeného inventáře (Headband of Vast Intelligence…)
                    val gearMods = gearAbilityText(s, c)
                    if (gearMods.isNotEmpty()) {
                        Text(s.gearIncludedHint(gearMods), fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            item(key = "defense") { DefensePanel(state, c) }
            item(key = "inventory") { InventoryLink(state, c, wide) }
            // mazlíček nemá vlastní mazlíčky (familiár familiára…)
            if (!c.isCompanion) item(key = "companions") { CompanionsPanel(state, c, wide) }
            item(key = "feats") { FeatsPanel(state, c, wide, selectedFeatPick) }
            item(key = "class-abilities") { ClassAbilitiesPanel(state, c) }
            item(key = "skills") { SkillsPanel(state, c) }
            item(key = "resources") { ResourcesPanel(state, c, wide) }
            if (casters.isNotEmpty() || c.resources().isNotEmpty()) {
                item(key = "game") {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        NbCheck(state.gameMode, s.gameMode) { state.gameMode = it }
                        Spacer(Modifier.weight(1f))
                        if (state.gameMode) NbDashedButton(s.rest, { state.rest(c.id) })
                    }
                }
            }
            if (missing > 0) {
                item(key = "missing") {
                    Text(s.missingSpells(missing), color = Nb.error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            if (casters.isEmpty()) {
                item(key = "no-casters") {
                    Text(s.noCasterClasses, color = Nb.inkMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
                }
            }
            casters.forEach { e ->
                classSpells(state, db, c, e, wide, selectedSpellId, showAddButton = casters.size > 1, openPicker)
                CasterClasses.get(e.classId)?.let { cls ->
                    subclassBonusSpells(state, db, c, e, cls, wide, selectedSpellId) { subclassFor = e.classId }
                }
            }
        }
    }

    if (addClass) {
        ClassPickerDialog(
            title = s.addClass, confirmLabel = s.add, withName = false,
            exclude = c.classes.map { it.classId }.toSet(),
            maxLevel = (20 - c.level).coerceAtLeast(1),
            subclasses = db.subclasses,
            characterDeity = c.deity,
            onDismiss = { addClass = false },
        ) { p ->
            addClass = false
            state.addClass(charId, p.classId, p.level, p.subclass)
        }
    }
    removeClass?.let { classId ->
        AlertDialog(
            onDismissRequest = { removeClass = null },
            containerColor = Nb.panel,
            title = { Text(s.removeClass, color = Nb.ink) },
            text = { Text(s.removeClassText(s.classNameOf(classId)), color = Nb.ink) },
            confirmButton = {
                TextButton(onClick = {
                    removeClass = null
                    state.removeClass(charId, classId)
                }) { Text(s.removeClass, color = Nb.error) }
            },
            dismissButton = { TextButton(onClick = { removeClass = null }) { Text(s.cancel, color = Nb.ink) } },
        )
    }
    if (editDeity) {
        DeityPickerDialog(c.deity, onDismiss = { editDeity = false }, onCustom = { editDeity = false; customDeity = true }) { name ->
            editDeity = false
            state.setDeity(charId, name)
        }
    }
    if (customDeity) {
        // vlastní / homebrew božstvo – jen text (přesvědčení božstva appka nezná)
        TextFieldDialog(s.deityLabel, c.deity.takeIf { cz.pfspells.data.DeityCatalog.get(it) == null }.orEmpty(), s.deityLabel, onDismiss = { customDeity = false }) { v ->
            customDeity = false
            state.setDeity(charId, v)
        }
    }
    if (pickRace) {
        RacePickerDialog(c.race, onDismiss = { pickRace = false }) { id ->
            pickRace = false
            state.setRace(charId, id)
        }
    }
    subclassFor?.let { classId ->
        c.classEntry(classId)?.let { e -> SubclassDialog(state, db, c, e) { subclassFor = null } }
    }
    editAbility?.let { a ->
        NumberDialog(s.abilityBaseTitle(s.abilityLong(a), c.abilityBonus(a)), c.abilities[a], onDismiss = { editAbility = null }) { v ->
            state.setAbility(charId, a, v)
            editAbility = null
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Nb.panel,
            title = { Text(s.deleteCharacterTitle, color = Nb.ink) },
            text = { Text(s.deleteCharacterText(c.name), color = Nb.ink) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    if (wide) state.openRoot(Screen.Decks) else state.pop()
                    state.deleteCharacter(charId)
                }) { Text(s.delete, color = Nb.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(s.cancel, color = Nb.ink) } },
        )
    }

    if (rename) {
        var text by remember { mutableStateOf(c.name) }
        AlertDialog(
            onDismissRequest = { rename = false },
            containerColor = Nb.panel,
            title = { Text(s.renameCharacter, color = Nb.ink) },
            text = { OutlinedTextField(text, { text = it }, singleLine = true) },
            confirmButton = {
                NbPrimaryButton(s.save, {
                    rename = false
                    if (text.isNotBlank()) state.updateCharacter(charId) { it.copy(name = text.trim()) }
                }, icon = null)
            },
            dismissButton = { TextButton(onClick = { rename = false }) { Text(s.cancel, color = Nb.ink) } },
        )
    }
}

/** Seznam class postavy: název, úroveň (stepper), odebrání; tlačítko „Přidat classu“. Celkem max. 20 úrovní. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassesPanel(
    state: AppState,
    c: Character,
    wide: Boolean,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    onSubclass: (String) -> Unit,
    onRace: () -> Unit,
    onDeity: () -> Unit,
) {
    val s = str
    val fonts = LocalNbFonts.current
    NbPanel(Modifier.fillMaxWidth().padding(top = if (wide) 14.dp else 0.dp), color = Nb.inset) {
        // rasa: název + velikost, rychlost, úpravy atributů; klepnutím výběr
        MonoLabel(s.raceLabel)
        val race = c.raceInfo
        Column(Modifier.padding(top = 4.dp, bottom = 10.dp).clip(Nb.shape).clickable(onClick = onRace).padding(vertical = 2.dp)) {
            if (race != null) {
                Text(s.raceNameOf(race), fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Nb.ink)
                Text(characterRaceDetail(s, c), fontSize = 12.5.sp, color = Nb.rust)
            } else {
                Text(
                    "${s.raceLabel}: ${s.chooseSubclass}",
                    fontSize = 13.sp, color = Nb.accent, textDecoration = TextDecoration.Underline,
                )
            }
        }
        if (race?.anyBonus == true && !cz.pfspells.data.RacialTraits.replacesRaceBonus(c)) RaceBonusDropdown(c.raceBonus, Modifier.padding(bottom = 10.dp)) { state.setRaceBonus(c.id, it) }
        // alternativní rasové traity (Archives of Nethys)
        var traitsOpen by remember { mutableStateOf(false) }
        RacialTraitsLink(c) { traitsOpen = true }
        if (traitsOpen) RacialTraitsDialog(state, c) { traitsOpen = false }
        // přesvědčení a božstvo (prerekvizity featů „good alignment“, „worshiper of Iomedae“)
        FlowRow(
            Modifier.padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SimpleDropdown(
                label = s.alignmentLabel,
                options = listOf<CharAlignment?>(null) + CharAlignment.entries,
                selected = c.alignment,
                optionLabel = { it?.let(s::alignmentName) ?: s.chooseOption },
                onSelect = { state.setAlignment(c.id, it) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                NbDashedButton(deityButtonText(s, c.deity), onDeity)
                if (c.deity.isNotBlank()) NbIconButton(NbIcons.Close, s.delete, { state.setDeity(c.id, "") }, Nb.inkMuted, 14.dp)
            }
        }
        MonoLabel(s.classesHeader)
        c.classes.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.classNameOf(e.classId), fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Nb.ink)
                    val cls = CasterClasses.get(e.classId)
                    Text(
                        cls?.let { spellRangeText(s, it, e.level) } ?: s.classGroup(cz.pfspells.data.ClassGroup.NONE).lowercase(),
                        fontSize = 12.5.sp, color = Nb.rust,
                    )
                }
                CountStepper(
                    e.level, { lvl -> state.setClassLevel(c.id, e.classId, lvl) },
                    min = 1, max = (20 - (c.level - e.level)).coerceAtLeast(e.level), size = 32.dp, numberSize = 17.sp,
                )
                if (c.classes.size > 1) {
                    NbIconButton(NbIcons.Close, s.removeClass, { onRemove(e.classId) }, Nb.inkMuted, 16.dp)
                }
            }
            subclassSummary(s, e)?.let { text ->
                Text(
                    text,
                    Modifier.padding(top = 2.dp).clip(Nb.shape).clickable { onSubclass(e.classId) }.padding(vertical = 4.dp),
                    fontSize = 13.sp, color = Nb.accent, textDecoration = TextDecoration.Underline,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            // doména / subdoména, kterou božstvo nedává (třeba po změně božstva)
            c.deityInfo?.let { d ->
                val bad = e.deityConflicts(d)
                if (bad.isNotEmpty()) DeityConflictWarning(s, d, bad, Modifier.clip(Nb.shape).clickable { onSubclass(e.classId) })
            }
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (c.level < 20) NbDashedButton(s.addClass, onAdd, icon = NbIcons.Plus)
            else Text(s.maxLevelReached, fontSize = 13.sp, color = Nb.inkMuted)
        }
    }
}

/** Propojený inventář (STR → nosnost): souhrn a otevření, nebo propojení existujícího / založení nového. */
@Composable
private fun InventoryLink(state: AppState, c: Character, wide: Boolean) {
    val s = str
    val inv = c.inventoryId?.let { state.inventory(it) }
    val open: (String) -> Unit = { id ->
        state.ensureItems()
        if (wide) state.openRoot(Screen.InventoryDetail(id)) else state.push(Screen.InventoryDetail(id))
    }
    Column(Modifier.padding(top = 4.dp)) {
        SectionHeader(s.inventoryHeader)
        if (inv != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.inventorySummary(inv.name, formatWeight(inv.totalWeight), s.loadName(inv.load).lowercase()),
                    Modifier.weight(1f).clickable { open(inv.id) },
                    fontSize = 14.5.sp, color = Nb.ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                NbDashedButton(s.openInventory, { open(inv.id) })
                NbIconButton(NbIcons.Close, s.unlinkInventory, { state.linkInventory(c.id, null) }, Nb.inkMuted, 16.dp)
            }
            // záchrany a AC z nasazeného vybavení (atributy a skills už bonusy obsahují)
            val gear = gearSummaryText(s, c)
            if (gear.isNotEmpty()) Text(gear, fontSize = 13.sp, color = Nb.rust, modifier = Modifier.padding(top = 2.dp))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.inventories.isNotEmpty()) {
                    SimpleDropdown<cz.pfspells.data.Inventory?>(
                        label = s.linkInventory,
                        options = state.inventories.toList(),
                        selected = null,
                        optionLabel = { it?.name ?: s.chooseOption },
                        onSelect = { it?.let { i -> state.linkInventory(c.id, i.id) } },
                    )
                }
                NbDashedButton(s.newInventoryForCharacter, { state.createInventoryFor(c.id) }, icon = NbIcons.Plus)
            }
        }
    }
}

/** Šest dlaždic atributů (hodnota + modifikátor); klepnutím se upraví. */
@Composable
private fun AbilityRow(s: Strings, c: Character, onEdit: (Ability) -> Unit) {
    val fonts = LocalNbFonts.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Ability.entries.forEach { a ->
            val v = c.scores[a]
            val mod = abilityMod(v)
            Column(
                Modifier.weight(1f).clip(Nb.shape).background(Nb.panel).dashedBorder()
                    .clickable(onClickLabel = s.abilityLong(a)) { onEdit(a) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MonoLabel(s.abilityShort(a), color = Nb.rust, size = 10.5.sp)
                Text("$v", fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Nb.ink)
                Text(if (mod < 0) "−${-mod}" else "+$mod", fontFamily = fonts.mono, fontSize = 11.sp, color = Nb.inkMuted)
            }
        }
    }
}

/** Kouzla jedné sesílací classy postavy: nadpis, tlačítko výběru, skupiny podle spell levelu. */
private fun androidx.compose.foundation.lazy.LazyListScope.classSpells(
    state: AppState,
    db: SpellDatabase,
    c: Character,
    e: ClassEntry,
    wide: Boolean,
    selectedSpellId: String?,
    showAddButton: Boolean,
    openPicker: (String) -> Unit,
) {
    val cls = CasterClasses.get(e.classId) ?: return
    val game = state.gameMode
    val entries = e.spells.mapNotNull { (id, count) -> db.get(id)?.let { it to count } }
    val byLevel = entries.groupBy { (spell, _) -> cls.spellLevelOf(spell) ?: -1 }
    val maxLevel = cls.maxSpellLevel(e.level)
    val score = c.scores[cls.ability]
    // všechny dostupné levely (i prázdné – kvůli limitům), pak případné kouzla nad úrovní / mimo list
    val levels = ((if (cls.hasCantrips) 0 else 1)..maxLevel).toList() + byLevel.keys.filter { it > maxLevel || it < 0 }.sorted()
    val grouped: List<Pair<Int, List<Pair<Spell, Int>>>> = levels.map { lvl ->
        lvl to (byLevel[lvl] ?: emptyList()).sortedBy { it.first.searchName }
    }

    item(key = "${e.classId}-header") {
        val s = str
        Column(Modifier.padding(top = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.classSpellsHeader(s.nameOf(cls)),
                        fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Nb.ink,
                    )
                    Text(
                        buildString {
                            append(spellRangeText(s, cls, e.level)).append(" · ").append(s.spells(e.spells.size))
                            if (!cls.distinctSpells) append(", ").append(s.copies(e.spells.values.sum()))
                        },
                        fontSize = 12.5.sp, color = Nb.inkMuted,
                    )
                    val mod = abilityMod(score)
                    Text(
                        s.castingAbility(s.abilityShort(cls.ability), score, if (mod < 0) "−${-mod}" else "+$mod"),
                        fontSize = 12.5.sp, color = Nb.inkMuted,
                    )
                }
                if (showAddButton && !game) {
                    Spacer(Modifier.width(8.dp))
                    NbDashedButton(s.addSpells, { openPicker(e.classId) }, icon = NbIcons.Plus)
                }
            }
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                    Text(s.characterNoSpells, textAlign = TextAlign.Center, color = Nb.inkMuted)
                }
            }
        }
    }
    grouped.forEach { (level, list) ->
        item(key = "${e.classId}-h$level") {
            val s = str
            if (level < 0 || level > maxLevel) {
                SectionHeader(if (level < 0) s.notOnClassList else s.spellLevelLabel(level), size = 16.sp)
            } else {
                val slots = cls.levelSlots(e.level, score, level)
                if (game) {
                    GameLevelHeader(s, cls, slots, c, e.classId, list) { key, n -> state.setCast(c.id, key, n) }
                } else {
                    val chosen = if (cls.distinctSpells) list.size else list.sumOf { it.second }
                    LevelHeader(s, cls, slots, chosen, schoolSlot = if (level >= 1 && e.hasSubclassSlot() && subclassType(e.classId) == SubclassType.SCHOOL) 1 else 0)
                }
            }
        }
        items(list, key = { "${e.classId}-${it.first.id}" }) { (spell, count) ->
            // herní režim: připravující classa odškrtává kopie, spontánní sesílá ze slotu levelu
            val gameControl: (@Composable () -> Unit)? = when {
                !game -> null
                level <= 0 || level > maxLevel -> ({})
                cls.kind == CastingKind.PREPARED -> ({
                    val key = Character.castKey(e.classId, spell.id)
                    CastPips(count, c.cast(key)) { state.setCast(c.id, key, it) }
                })
                else -> ({
                    val key = Character.slotKey(e.classId, level)
                    val slots = cls.levelSlots(e.level, score, level)
                    val total = (slots.perDay ?: 0) + slots.bonus
                    val used = c.cast(key)
                    NbDashedButton(str.castSpell, { state.setCast(c.id, key, used + 1) }, enabled = used < total && slots.abilityOk)
                })
            }
            DeckSpellRow(
                spell = spell,
                count = count,
                single = cls.distinctSpells,
                gameControl = gameControl,
                opposition = e.isOppositionSpell(spell),
                spent = game && cls.kind == CastingKind.PREPARED && level > 0 && c.cast(Character.castKey(e.classId, spell.id)) >= count,
                tooHigh = level > maxLevel,
                favorite = state.isFavorite(spell.id),
                selected = spell.id == selectedSpellId,
                wide = wide,
                onClick = { state.showSpell(spell.id, c.id, e.classId) },
                onCount = { state.setSpellCount(c.id, e.classId, spell.id, it) },
            )
        }
    }
}

/** Nadpis spell levelu s limity: „Úroveň 1 … 3 / 3+1 připraveno“, u spontánních i „5+1 za den“. */
@Composable
private fun LevelHeader(s: Strings, cls: CasterClass, slots: LevelSlots, chosen: Int, schoolSlot: Int = 0) {
    val fonts = LocalNbFonts.current
    val over = slots.limit != null && chosen > slots.limit + schoolSlot
    val limitText = when {
        slots.limit == null -> null
        cls.kind == CastingKind.PREPARED && slots.bonus > 0 -> "${slots.perDay}+${slots.bonus}"
        else -> "${slots.limit}"
    }
    val info = buildList {
        if (limitText != null) {
            add(if (cls.kind == CastingKind.SPONTANEOUS) s.slotsKnown(chosen, limitText) else s.slotsPrepared(chosen, limitText))
        }
        if (cls.kind != CastingKind.PREPARED) {
            add(
                when {
                    slots.perDay == null -> s.atWill
                    slots.bonus > 0 -> s.slotsPerDay("${slots.perDay}+${slots.bonus}")
                    else -> s.slotsPerDay("${slots.perDay}")
                },
            )
        }
        if (schoolSlot > 0) add(s.schoolSlot)
        if (!slots.abilityOk) add(s.abilityNeeded(s.abilityShort(cls.ability), 10 + slots.spellLevel))
    }.joinToString(" · ")
    SectionHeader(s.spellLevelLabel(slots.spellLevel), size = 16.sp) {
        Text(
            info, textAlign = TextAlign.End,
            fontFamily = fonts.mono, fontSize = 11.5.sp,
            color = if (over || !slots.abilityOk) Nb.error else Nb.inkMuted,
        )
    }
}

/**
 * Nadpis spell levelu v herním režimu: připravující – „2 / 4 sesláno“, spontánní / arcanist – klikací sloty
 * (plný = použitý) a „2 / 6+1 sesláno“. Triky libovolně.
 */
@Composable
private fun GameLevelHeader(
    s: Strings,
    cls: CasterClass,
    slots: LevelSlots,
    c: Character,
    classId: String,
    list: List<Pair<Spell, Int>>,
    onCast: (key: String, n: Int) -> Unit,
) {
    val fonts = LocalNbFonts.current
    val level = slots.spellLevel
    if (level == 0 || slots.perDay == null) {
        SectionHeader(s.spellLevelLabel(level), size = 16.sp) {
            Text(s.atWill, fontFamily = fonts.mono, fontSize = 11.5.sp, color = Nb.inkMuted)
        }
        return
    }
    if (cls.kind == CastingKind.PREPARED) {
        val copies = list.sumOf { it.second }
        val used = list.sumOf { (spell, n) -> c.cast(Character.castKey(classId, spell.id)).coerceAtMost(n) }
        SectionHeader(s.spellLevelLabel(level), size = 16.sp) {
            Text(s.castOf(used, copies), fontFamily = fonts.mono, fontSize = 11.5.sp, color = if (used >= copies && copies > 0) Nb.rust else Nb.inkMuted)
        }
        return
    }
    val key = Character.slotKey(classId, level)
    val total = slots.perDay + slots.bonus
    val used = c.cast(key)
    SectionHeader(s.spellLevelLabel(level), size = 16.sp) {
        Text(
            s.castOf(used, total), fontFamily = fonts.mono, fontSize = 11.5.sp,
            color = if (used >= total) Nb.rust else Nb.inkMuted,
        )
    }
    if (total > 0) CastPips(total, used, Modifier.padding(bottom = 4.dp)) { onCast(key, it) }
}

/** Řada slotů / kopií: plný = sesláno. Klepnutí na plný ho (i následující) uvolní, na prázdný doplní až po něj. */
@Composable
internal fun CastPips(total: Int, used: Int, modifier: Modifier = Modifier, onSet: (Int) -> Unit) {
    val s = str
    if (total > 8) {
        // hodně kopií – místo puntíků počítadlo
        CountStepper(used.coerceAtMost(total), onSet, min = 0, max = total, size = 32.dp, numberSize = 14.sp)
        return
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            val filled = i < used
            Box(
                Modifier.size(24.dp).clip(Nb.shape)
                    .background(if (filled) Nb.accent else Nb.inset)
                    .border(1.dp, if (filled) Nb.accent else Nb.rule, Nb.shape)
                    .clickable(onClickLabel = s.slotLabel) { onSet(if (filled) i else i + 1) },
            )
        }
    }
}

/** „Evocation [fire] · 1 standard action“ */
fun spellSubline(s: Strings, spell: Spell): String = buildString {
    append(s.school(spell.school))
    spell.subschool?.let { append(" (${s.subschool(it)})") }
    spell.descriptor?.let { append(" [$it]") }
    spell.castingTime?.let { append(" · ").append(it) }
}

@Composable
private fun DeckSpellRow(
    spell: Spell,
    count: Int,
    /** spontánní / arcanist: místo počtu kopií jen odebrání */
    single: Boolean,
    /** herní režim: ovládání místo počtu kopií (null = režim úprav) */
    gameControl: (@Composable () -> Unit)?,
    /** herní režim: všechny kopie seslané */
    spent: Boolean,
    /** kouzelník: kouzlo opoziční školy (2 sloty) */
    opposition: Boolean = false,
    tooHigh: Boolean,
    favorite: Boolean,
    selected: Boolean,
    wide: Boolean,
    onClick: () -> Unit,
    onCount: (Int) -> Unit,
) {
    val fonts = LocalNbFonts.current
    val base = Modifier.fillMaxWidth()
    val rowMod = if (selected) base.clip(Nb.shape).background(Nb.panel).dashedBorder() else base.bottomRule(if (wide) Nb.ruleSoft else Nb.rule)
    Row(
        rowMod.clickable(onClick = onClick).padding(horizontal = if (wide) 10.dp else 0.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val name: @Composable (Modifier) -> Unit = { m ->
            Row(m, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    spell.name, Modifier.weight(1f, fill = false),
                    fontSize = if (wide) 16.sp else 15.5.sp, fontWeight = FontWeight.SemiBold, color = if (spent) Nb.inkMuted else Nb.ink,
                    textDecoration = if (spent) TextDecoration.LineThrough else null,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (favorite) Icon(NbIcons.StarFilled, str.favorite, tint = Nb.rust, modifier = Modifier.padding(start = 6.dp).size(13.dp))
                if (tooHigh) Icon(NbIcons.Warn, str.notAvailableAtLevel, tint = Nb.error, modifier = Modifier.padding(start = 6.dp).size(16.dp))
                if (opposition) Icon(NbIcons.Warn, str.oppositionWarn, tint = Nb.rust, modifier = Modifier.padding(start = 6.dp).size(16.dp))
            }
        }
        if (wide) {
            name(Modifier.weight(2f))
            Text(
                buildString {
                    append(str.school(spell.school))
                    spell.subschool?.let { append(" (${str.subschool(it)})") }
                    spell.descriptor?.let { append(" [$it]") }
                },
                Modifier.weight(2f), fontSize = 14.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                spell.castingTime?.replace("standard", "std.") ?: "",
                Modifier.weight(1f), fontFamily = fonts.mono, fontSize = 12.5.sp, color = Nb.inkMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            // pevná šířka – sloupce řádků lícují v režimu úprav i ve hře
            Box(Modifier.width(150.dp), contentAlignment = Alignment.CenterEnd) {
                when {
                    gameControl != null -> gameControl()
                    single -> NbIconButton(NbIcons.Close, str.removeSpell, { onCount(0) }, Nb.inkMuted, 16.dp)
                    else -> CountStepper(count, onCount, size = 32.dp, numberSize = 14.sp)
                }
            }
        } else {
            Column(Modifier.weight(1f)) {
                name(Modifier)
                Text(spellSubline(str, spell), fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
            }
            when {
                gameControl != null -> gameControl()
                single -> NbIconButton(NbIcons.Close, str.removeSpell, { onCount(0) }, Nb.inkMuted, 16.dp)
                else -> CountStepper(count, onCount)
            }
        }
    }
}
