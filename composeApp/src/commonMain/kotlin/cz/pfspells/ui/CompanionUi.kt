package cz.pfspells.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.Screen
import cz.pfspells.data.AnimalSpecies
import cz.pfspells.data.Character
import cz.pfspells.data.ClassCatalog
import cz.pfspells.data.ClassInfo
import cz.pfspells.data.CompanionKind
import cz.pfspells.data.CompanionProgression
import cz.pfspells.data.CompanionRarity
import cz.pfspells.data.CompanionSpecial
import cz.pfspells.data.CreatureSize
import cz.pfspells.data.SpeciesCatalog
import cz.pfspells.data.companionRow
import cz.pfspells.data.companionSize
import cz.pfspells.data.naturalArmor
import cz.pfspells.data.speciesAdvanced
import cz.pfspells.data.speciesInfo
import cz.pfspells.data.suggestedCompanionLevel
import cz.pfspells.str

/**
 * Hodnoty z [CompanionDialog]. [classId] a [level] (HD) jen u nového mazlíčka bez progrese;
 * [companionLevel] = efektivní úroveň druida (null = bez progrese zvířecího společníka).
 */
data class CompanionForm(
    val name: String = "",
    val kind: CompanionKind = CompanionKind.ANIMAL_COMPANION,
    val classId: String = "animal",
    val level: Int = 1,
    val size: CreatureSize? = CreatureSize.MEDIUM,
    val quadruped: Boolean = true,
    val species: String? = null,
    val companionLevel: Int? = null,
    val rarity: CompanionRarity? = null,
    /** vlastní pravidla pro návrat po smrti (null = výchozí text podle druhu) */
    val respawnRules: String? = null,
)

private fun openCharacter(state: AppState, id: String, wide: Boolean) {
    if (wide) state.openRoot(Screen.CharacterDetail(id)) else state.push(Screen.CharacterDetail(id))
}

/** Druh mazlíčka, který obvykle postupuje podle tabulky zvířecího společníka. */
private fun CompanionKind.usesProgression() = this == CompanionKind.ANIMAL_COMPANION || this == CompanionKind.MOUNT

/** Sekce „Mazlíčci a společníci“ v detailu postavy: seznam (otevře list mazlíčka) a přidání nového. */
@Composable
fun CompanionsPanel(state: AppState, c: Character, wide: Boolean) {
    val s = str
    var add by remember { mutableStateOf(false) }
    val list = state.companionsOf(c.id)
    Column(Modifier.padding(top = 4.dp)) {
        SectionHeader(s.companionsHeader)
        if (list.isEmpty()) Text(s.companionsHint, fontSize = 13.sp, color = Nb.inkMuted)
        list.forEach { p ->
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp).clip(Nb.shape).background(Nb.panel).dashedBorder()
                    .clickable { openCharacter(state, p.id, wide) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        p.name, fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(p.companion?.let(s::companionKind), p.speciesInfo?.let(s::speciesName), classesSummary(s, p))
                            .joinToString(" · "),
                        fontSize = 12.5.sp, color = Nb.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                MonoLabel(s.open, color = Nb.rust, size = 11.sp)
            }
        }
        Row(Modifier.padding(top = 10.dp)) {
            NbDashedButton(s.addCompanion, { add = true }, icon = NbIcons.Plus)
        }
    }
    if (add) {
        val kind = CompanionKind.ANIMAL_COMPANION
        CompanionDialog(
            s.newCompanion, s.create,
            CompanionForm(kind = kind, companionLevel = c.suggestedCompanionLevel(kind)),
            isNew = true, suggestLevel = { c.suggestedCompanionLevel(it) }, onDismiss = { add = false },
        ) { f ->
            add = false
            val p = state.createCompanion(c.id, f.name, f.kind, f.classId, f.level, f.size, f.quadruped, f.species, f.companionLevel)
            if (f.rarity != null || f.respawnRules != null) state.setCompanionNotes(p.id, f.rarity, f.respawnRules)
            openCharacter(state, p.id, wide)
        }
    }
}

/**
 * Hlavička listu mazlíčka: druh a vlastník (odkaz), nastavení; se šablonou zvířete rychlost, útoky a schopnosti,
 * s progresí zvířecího společníka úroveň (stepper), přirozená zbroj, Str/Dex bonus, triky a zvláštní schopnosti.
 */
@Composable
fun CompanionHeader(state: AppState, c: Character, wide: Boolean) {
    val s = str
    val owner = c.ownerId?.let { state.character(it) } ?: return
    var edit by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = if (wide) 14.dp else 0.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.companionOf(c.companion?.let(s::companionKind) ?: s.companionKind(CompanionKind.PET), owner.name),
                Modifier.weight(1f).clip(Nb.shape).clickable { openCharacter(state, owner.id, wide) }.padding(vertical = 4.dp),
                fontSize = 14.sp, color = Nb.accent, textDecoration = TextDecoration.Underline,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            c.rarity?.let { NbTag(s.rarity(it), rarityColor(it), Modifier.padding(end = 8.dp)) }
            NbDashedButton(s.companionSettings, { edit = true }, icon = NbIcons.Pencil)
        }
        val sp = c.speciesInfo
        val row = c.companionRow
        if (sp != null || row != null || c.size != null) {
            NbPanel(Modifier.fillMaxWidth().padding(top = 6.dp), color = Nb.inset) {
                sp?.let { Text(s.speciesName(it), fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Nb.ink) }
                c.size?.let { size ->
                    Text(
                        "${s.sizeLabel}: ${s.creatureSize(size)}" + if (c.quadruped) " · ${s.quadruped.lowercase()}" else "",
                        fontSize = 12.5.sp, color = Nb.rust,
                    )
                }
                if (row != null) {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel(s.companionLevelLabel, Modifier.weight(1f))
                        CountStepper(row.level, { state.setCompanionLevel(c.id, it) }, min = 1, max = 20, size = 30.dp, numberSize = 16.sp)
                    }
                }
                val stats = s.companionStats(c.naturalArmor, row?.strDex, row?.bonusTricks)
                if (stats.isNotEmpty()) Text(stats, fontSize = 13.sp, color = Nb.ink, modifier = Modifier.padding(top = 4.dp))
                if (sp != null) {
                    val adv = c.speciesAdvanced
                    DetailLine(s.speedLabel, sp.speed)
                    DetailLine(s.attacksLabel, if (adv) sp.advAttacks ?: sp.attacks else sp.attacks)
                    DetailLine(s.qualitiesLabel, listOfNotNull(sp.qualities, sp.advQualities.takeIf { adv }).joinToString("; "))
                    if (!adv && row != null) {
                        Text(s.advancementAt(sp.advLevel), fontSize = 12.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                if (row != null) {
                    val specials = CompanionProgression.specialsUpTo(row.level).filter { it != CompanionSpecial.ABILITY_INCREASE }
                    if (specials.isNotEmpty()) {
                        DetailLine(s.companionProgression, specials.joinToString(", ") { s.companionSpecial(it) })
                    }
                    val inc = CompanionProgression.abilityIncreases(row.level)
                    if (inc > 0) Text(s.abilityIncreases(inc), fontSize = 12.5.sp, color = Nb.rust, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        // pravidla pro návrat / náhradu po smrti (vlastní text nebo výchozí podle druhu)
        val respawn = c.respawnRules ?: s.defaultRespawn(c.companion ?: CompanionKind.PET)
        MonoLabel(s.respawnLabel, Modifier.padding(top = 10.dp), color = Nb.rust)
        Text(respawn, fontSize = 13.sp, color = Nb.ink, modifier = Modifier.padding(top = 2.dp))
    }
    if (edit) {
        CompanionDialog(
            s.companionSettings, s.save,
            CompanionForm(
                c.name, c.companion ?: CompanionKind.PET, size = c.size, quadruped = c.quadruped,
                species = c.species, companionLevel = c.companionLevel, rarity = c.rarity, respawnRules = c.respawnRules,
            ),
            isNew = false, suggestLevel = { owner.suggestedCompanionLevel(it) }, onDismiss = { edit = false },
        ) { f ->
            edit = false
            state.setCompanionInfo(c.id, f.kind, f.size, f.quadruped, f.species, f.companionLevel)
            state.setCompanionNotes(c.id, f.rarity, f.respawnRules)
        }
    }
}

private fun rarityColor(r: CompanionRarity) = when (r) {
    CompanionRarity.COMMON -> Nb.inkMuted
    CompanionRarity.UNCOMMON -> Nb.accent
    CompanionRarity.RARE -> Nb.ochre
    CompanionRarity.UNIQUE -> Nb.rust
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Text("$label: $value", fontSize = 12.5.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 2.dp))
}

/**
 * Nový mazlíček ([isNew]: jméno a – bez progrese – druh tvora a HD) nebo úprava existujícího.
 * Zvíře ze seznamu doplní atributy, velikost a „čtyřnohý“; progrese zvířecího společníka odvodí HD z úrovně druida.
 * Classy a úrovně existujícího mazlíčka se mění v jeho listu jako u postavy.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionDialog(
    title: String,
    confirmLabel: String,
    initial: CompanionForm,
    isNew: Boolean,
    suggestLevel: (CompanionKind) -> Int,
    onDismiss: () -> Unit,
    onSave: (CompanionForm) -> Unit,
) {
    val s = str
    var name by remember { mutableStateOf(initial.name) }
    var kind by remember { mutableStateOf(initial.kind) }
    var species by remember { mutableStateOf(SpeciesCatalog.get(initial.species)) }
    var progression by remember { mutableStateOf(initial.companionLevel != null) }
    var companionLevel by remember { mutableIntStateOf(initial.companionLevel ?: suggestLevel(initial.kind)) }
    var creature by remember { mutableStateOf(ClassCatalog.get(initial.classId) ?: ClassCatalog.creatures.first()) }
    var level by remember { mutableIntStateOf(initial.level) }
    var size by remember { mutableStateOf(initial.size) }
    var quadruped by remember { mutableStateOf(initial.quadruped) }
    var rarity by remember { mutableStateOf(initial.rarity) }
    var respawn by remember { mutableStateOf(initial.respawnRules ?: s.defaultRespawn(initial.kind)) }
    val lvl = companionLevel.takeIf { progression }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(title, color = Nb.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isNew) {
                    OutlinedTextField(
                        name, { name = it }, label = { Text(s.characterNameLabel) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SimpleDropdown(
                    label = s.companionKindLabel,
                    options = CompanionKind.entries,
                    selected = kind,
                    optionLabel = { s.companionKind(it) },
                    onSelect = { k ->
                        // výchozí pravidla návratu se mění s druhem (vlastní text zůstane)
                        if (respawn.trim() == s.defaultRespawn(kind)) respawn = s.defaultRespawn(k)
                        kind = k
                        // nový mazlíček: progrese podle druhu a úroveň podle class vlastníka
                        if (isNew) {
                            progression = k.usesProgression()
                            companionLevel = suggestLevel(k)
                        }
                    },
                )
                SimpleDropdown<AnimalSpecies?>(
                    label = s.speciesLabel,
                    options = listOf<AnimalSpecies?>(null) + SpeciesCatalog.all,
                    selected = species,
                    optionLabel = { it?.let(s::speciesName) ?: s.speciesCustom },
                    onSelect = { species = it },
                )
                NbCheck(progression, s.companionProgression) { progression = it }
                if (progression) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel(s.companionLevelLabel, Modifier.weight(1f))
                        CountStepper(companionLevel, { companionLevel = it }, min = 1, max = 20, size = 32.dp, numberSize = 17.sp)
                    }
                    Text(
                        "${s.hitDiceLabel}: ${CompanionProgression.at(companionLevel).hd}",
                        fontSize = 12.5.sp, color = Nb.inkMuted,
                    )
                } else if (isNew) {
                    if (species == null) {
                        SimpleDropdown<ClassInfo>(
                            label = s.creatureLabel,
                            options = ClassCatalog.creatures,
                            selected = creature,
                            optionLabel = { s.classNameOf(it.id) },
                            onSelect = { creature = it },
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel(s.hitDiceLabel, Modifier.weight(1f))
                        CountStepper(level, { level = it }, min = 1, max = 20, size = 32.dp, numberSize = 17.sp)
                    }
                }
                val sp = species
                if (sp != null) {
                    // velikost a čtyřnohost dává druh (po vylepšení větší)
                    val derived = companionSize(sp, lvl)
                    Text(
                        "${s.sizeLabel}: ${derived?.let(s::creatureSize).orEmpty()}" + if (sp.quadruped) " · ${s.quadruped.lowercase()}" else "",
                        fontSize = 12.5.sp, color = Nb.rust,
                    )
                } else {
                    SimpleDropdown<CreatureSize?>(
                        label = s.sizeLabel,
                        options = listOf<CreatureSize?>(null) + CreatureSize.entries,
                        selected = size,
                        optionLabel = { it?.let(s::creatureSize) ?: s.sizeByRace },
                        onSelect = { size = it },
                    )
                    if (size != null) {
                        FlowRow { NbFilterChip(s.quadruped, quadruped) { quadruped = !quadruped } }
                    }
                }
                SimpleDropdown<CompanionRarity?>(
                    label = s.rarityLabel,
                    options = listOf<CompanionRarity?>(null) + CompanionRarity.entries,
                    selected = rarity,
                    optionLabel = { it?.let(s::rarity) ?: s.chooseOption },
                    onSelect = { rarity = it },
                )
                OutlinedTextField(
                    respawn, { respawn = it }, label = { Text(s.respawnLabel) },
                    minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth(),
                )
                if (respawn.trim() != s.defaultRespawn(kind)) {
                    NbDashedButton(s.resetToDefault, { respawn = s.defaultRespawn(kind) })
                }
            }
        },
        confirmButton = {
            NbPrimaryButton(confirmLabel, {
                val sp = species
                onSave(
                    CompanionForm(
                        name = name,
                        kind = kind,
                        // šablona zvířete / progrese = rasové HD „animal“
                        classId = if (sp != null || progression) "animal" else creature.id,
                        level = level,
                        size = size,
                        quadruped = quadruped && size != null,
                        species = sp?.id,
                        companionLevel = lvl,
                        rarity = rarity,
                        // výchozí text se neukládá – změní se s druhem i jazykem
                        respawnRules = respawn.trim().takeIf { it.isNotEmpty() && it != s.defaultRespawn(kind) },
                    ),
                )
            }, icon = null)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}
