package cz.pfspells.ui

import androidx.compose.runtime.LaunchedEffect
import cz.pfspells.data.Eidolons
import cz.pfspells.data.EidolonForm
import cz.pfspells.data.Evolution
import cz.pfspells.data.eidolonFormInfo
import cz.pfspells.data.eidolonRow
import cz.pfspells.data.evolutionPointsUsed
import cz.pfspells.data.eidolonNaturalArmor
import cz.pfspells.data.hasUnchainedSummoner
import cz.pfspells.data.EIDOLON_SUBTYPES
import cz.pfspells.data.Ability
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cz.pfspells.data.FamiliarCatalog
import cz.pfspells.data.FamiliarInfo
import cz.pfspells.data.FamiliarProgression
import cz.pfspells.data.familiarInfo
import cz.pfspells.data.familiarLevel
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
    /** druh familiára ([FamiliarCatalog] id) */
    val familiar: String? = null,
    /** eidolon: základní forma a podtyp */
    val eidolonForm: String? = null,
    val eidolonSubtype: String? = null,
)

private fun openCharacter(state: AppState, id: String, wide: Boolean) {
    if (wide) state.openRoot(Screen.CharacterDetail(id)) else state.push(Screen.CharacterDetail(id))
}

/** Druh mazlíčka, který obvykle postupuje podle tabulky zvířecího společníka. */
private fun CompanionKind.usesProgression() = this != CompanionKind.PET

/** Sekce „Mazlíčci a společníci“ v detailu postavy: seznam (otevře list mazlíčka) a přidání nového. */
@Composable
fun CompanionsPanel(state: AppState, c: Character, wide: Boolean) {
    val s = str
    var add by remember { mutableStateOf(false) }
    val list = state.companionsOf(c.id)
    Column(Modifier.padding(top = 4.dp)) {
        SectionHeader(s.companionsHeader)
        if (list.isEmpty()) NbEmptyState(Sketch.PAW, s.emptyCompanionsTitle, s.companionsHint)
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
                        listOfNotNull(p.companion?.let(s::companionKind), p.speciesInfo?.let(s::speciesName) ?: p.familiarInfo?.name, classesSummary(s, p))
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
            unchainedSummoner = c.hasUnchainedSummoner,
        ) { f ->
            add = false
            val p = state.createCompanion(c.id, f.name, f.kind, f.classId, f.level, f.size, f.quadruped, f.species, f.companionLevel, f.familiar, f.eidolonForm, f.eidolonSubtype)
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
        // familiár: druh, bonus pánovi, úroveň pána, přirozená zbroj / Int / SR a schopnosti
        val fam = c.familiarInfo
        val famLevel = c.familiarLevel
        if (c.companion == CompanionKind.FAMILIAR && (fam != null || famLevel != null)) {
            NbPanel(Modifier.fillMaxWidth().padding(top = 6.dp), color = Nb.inset) {
                fam?.let { f ->
                    Text(f.name, fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Nb.ink)
                    DetailLine(s.masterBonusLabel, f.bonusText + if (f.bonus == null) " (${s.masterBonusConditional})" else "")
                    DetailLine(s.speedLabel, f.speed)
                    DetailLine(s.attacksLabel, f.attacks)
                    DetailLine(s.qualitiesLabel, f.senses)
                    DetailLine(s.familiarNoteLabel, f.note)
                }
                if (famLevel != null) {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel(s.familiarLevelLabel, Modifier.weight(1f))
                        CountStepper(famLevel, { state.setCompanionLevel(c.id, it) }, min = 1, max = 20, size = 30.dp, numberSize = 16.sp)
                    }
                    Text(
                        s.familiarStats(FamiliarProgression.naturalArmor(famLevel), FamiliarProgression.intelligence(famLevel), FamiliarProgression.spellResistance(famLevel)),
                        fontSize = 13.sp, color = Nb.ink, modifier = Modifier.padding(top = 4.dp),
                    )
                    DetailLine(s.specialsLabel, FamiliarProgression.specials(famLevel).joinToString(", ") { s.familiarSpecial(it) })
                }
                Text(s.familiarHint, fontSize = 12.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (c.companion == CompanionKind.EIDOLON) EidolonPanel(state, c)
        val sp = c.speciesInfo
        val row = c.companionRow
        if (sp != null || row != null || (c.size != null && c.companion != CompanionKind.FAMILIAR && c.companion != CompanionKind.EIDOLON)) {
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
                familiar = c.familiar, eidolonForm = c.eidolonForm, eidolonSubtype = c.eidolonSubtype,
            ),
            isNew = false, suggestLevel = { owner.suggestedCompanionLevel(it) }, onDismiss = { edit = false },
            unchainedSummoner = owner.hasUnchainedSummoner,
        ) { f ->
            edit = false
            state.setCompanionInfo(c.id, f.kind, f.size, f.quadruped, f.species, f.companionLevel, f.familiar, f.eidolonForm, f.eidolonSubtype)
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
    /** vlastník je unchained summoner – eidolon nabídne unchained formy a podtypy */
    unchainedSummoner: Boolean = false,
    onSave: (CompanionForm) -> Unit,
) {
    val s = str
    var name by remember { mutableStateOf(initial.name) }
    var kind by remember { mutableStateOf(initial.kind) }
    var species by remember { mutableStateOf(SpeciesCatalog.get(initial.species)) }
    var familiar by remember { mutableStateOf(FamiliarCatalog.get(initial.familiar)) }
    var eidolonForm by remember { mutableStateOf(Eidolons.form(initial.eidolonForm)) }
    var eidolonSubtype by remember { mutableStateOf(initial.eidolonSubtype) }
    var pickSpecies by remember { mutableStateOf(false) }
    var pickFamiliar by remember { mutableStateOf(false) }
    if (pickSpecies) {
        SearchPickDialog(
            s.speciesLabel, SpeciesCatalog.all, s.speciesCustom,
            label = { s.speciesName(it) }, detail = { "${s.creatureSize(it.size)} · ${it.speed} · ${it.attacks}" },
            key = { it.id }, onDismiss = { pickSpecies = false },
        ) { species = it; pickSpecies = false }
    }
    if (pickFamiliar) {
        SearchPickDialog(
            s.familiarLabel, FamiliarCatalog.all, s.speciesCustom,
            label = { it.name }, detail = { it.bonusText + if (it.note.isNotBlank()) " · ${it.note}" else "" },
            key = { it.id }, onDismiss = { pickFamiliar = false },
        ) { familiar = it; pickFamiliar = false }
    }
    var progression by remember { mutableStateOf(initial.companionLevel != null) }
    var companionLevel by remember { mutableIntStateOf(initial.companionLevel ?: suggestLevel(initial.kind)) }
    var creature by remember { mutableStateOf(ClassCatalog.get(initial.classId) ?: ClassCatalog.creatures.first()) }
    var level by remember { mutableIntStateOf(initial.level) }
    var size by remember { mutableStateOf(initial.size) }
    var quadruped by remember { mutableStateOf(initial.quadruped) }
    // nový druh familiára → velikost z bestiáře (Cat Tiny, Bat Diminutive)
    LaunchedEffect(familiar?.id) { familiar?.takeIf { it.id != initial.familiar }?.size?.let { size = it } }
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
                val isFamiliar = kind == CompanionKind.FAMILIAR
                val isEidolon = kind == CompanionKind.EIDOLON
                // familiár: druh z tabulky familiárů (bonus pánovi); eidolon: základní forma (+ podtyp unchained);
                // ostatní: zvíře ze seznamu (157 druhů, hledání)
                if (isEidolon) {
                    val forms = Eidolons.forms.filter { it.unchained == unchainedSummoner }
                    SimpleDropdown<EidolonForm?>(
                        label = s.eidolonFormLabel,
                        options = listOf<EidolonForm?>(null) + forms,
                        selected = eidolonForm,
                        optionLabel = { it?.name ?: s.speciesCustom },
                        onSelect = { eidolonForm = it },
                    )
                    if (unchainedSummoner) {
                        SimpleDropdown<String?>(
                            label = s.eidolonSubtypeLabel,
                            options = listOf<String?>(null) + EIDOLON_SUBTYPES,
                            selected = eidolonSubtype,
                            optionLabel = { it ?: s.chooseOption },
                            onSelect = { eidolonSubtype = it },
                        )
                    }
                } else if (isFamiliar) {
                    PickField(s.familiarLabel, familiar?.name ?: s.speciesCustom) { pickFamiliar = true }
                    familiar?.let { Text(it.bonusText, fontSize = 12.5.sp, color = Nb.rust) }
                } else {
                    PickField(s.speciesLabel, species?.let(s::speciesName) ?: s.speciesCustom) { pickSpecies = true }
                }
                NbCheck(progression, s.companionProgression) { progression = it }
                if (progression) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonoLabel(if (isFamiliar) s.familiarLevelLabel else if (isEidolon) s.summonerLevelLabel else s.companionLevelLabel, Modifier.weight(1f))
                        CountStepper(companionLevel, { companionLevel = it }, min = 1, max = 20, size = 32.dp, numberSize = 17.sp)
                    }
                    Text(
                        "${s.hitDiceLabel}: ${when {
                            isFamiliar -> companionLevel
                            isEidolon -> Eidolons.row(companionLevel, eidolonForm?.unchained ?: unchainedSummoner).hd
                            else -> CompanionProgression.at(companionLevel).hd
                        }}",
                        fontSize = 12.5.sp, color = Nb.inkMuted,
                    )
                } else if (isNew) {
                    if ((species == null || isFamiliar) && !isEidolon) {
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
                val sp = species.takeIf { !isFamiliar && !isEidolon }
                val ef = eidolonForm.takeIf { isEidolon }
                if (ef != null) {
                    Text("${s.sizeLabel}: ${s.creatureSize(ef.size)} · ${ef.speed} · ${ef.attack}", fontSize = 12.5.sp, color = Nb.rust)
                } else if (sp != null) {
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
                val fam = kind == CompanionKind.FAMILIAR
                val eid = kind == CompanionKind.EIDOLON
                val sp = species.takeIf { !fam && !eid }
                onSave(
                    CompanionForm(
                        name = name,
                        kind = kind,
                        // šablona zvířete / progrese = rasové HD „animal“
                        classId = if (eid) "eidolon" else if (sp != null || progression) "animal" else creature.id,
                        level = level,
                        size = size,
                        quadruped = quadruped && size != null,
                        species = sp?.id,
                        companionLevel = lvl,
                        rarity = rarity,
                        // výchozí text se neukládá – změní se s druhem i jazykem
                        respawnRules = respawn.trim().takeIf { it.isNotEmpty() && it != s.defaultRespawn(kind) },
                        familiar = familiar?.id?.takeIf { fam },
                        eidolonForm = eidolonForm?.id?.takeIf { eid },
                        eidolonSubtype = eidolonSubtype?.takeIf { eid },
                    ),
                )
            }, icon = null)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}

/** Pole „Zvíře: Wolf ▾“ – klepnutím otevře výběr s hledáním. */
@Composable
private fun PickField(label: String, value: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(Nb.shape).background(Nb.paper).dashedBorder().clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        MonoLabel(label)
        Text(value, fontSize = 15.sp, color = Nb.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Výběr ze seznamu s hledáním (zvíře, familiár); [noneLabel] = volba „vlastní / žádný“ (null). */
@Composable
private fun <T> SearchPickDialog(
    title: String,
    items: List<T>,
    noneLabel: String,
    label: (T) -> String,
    detail: (T) -> String,
    key: (T) -> String,
    onDismiss: () -> Unit,
    onPick: (T?) -> Unit,
) {
    val s = str
    var query by remember { mutableStateOf("") }
    val list = remember(query, items) {
        val q = query.trim().lowercase()
        items.filter { q.isEmpty() || label(it).lowercase().contains(q) || detail(it).lowercase().contains(q) }
            .sortedWith(compareBy({ !label(it).lowercase().startsWith(q) }, { label(it).lowercase() }))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            val width = minOf(560.dp, maxWidth - 32.dp)
            val maxH = minOf(680.dp, maxHeight - 48.dp)
            Surface(Modifier.width(width).heightIn(max = maxH), shape = MaterialTheme.shapes.extraLarge, color = Nb.panel, shadowElevation = 8.dp) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = Nb.ink, modifier = Modifier.padding(bottom = 10.dp))
                    NbSearchField(query, { query = it }, s.searchAbilities, Modifier.fillMaxWidth())
                    LazyColumn(
                        Modifier.weight(1f, fill = false).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                    ) {
                        items(list, key = key) { item ->
                            Column(
                                Modifier.fillMaxWidth().clip(Nb.shape).background(Nb.paper).dashedBorder()
                                    .clickable { onPick(item) }.padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Text(label(item), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Nb.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(detail(item), fontSize = 12.sp, color = Nb.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        TextButton(onClick = { onPick(null) }) { Text(noneLabel, color = Nb.accent) }
                        TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) }
                    }
                }
            }
        }
    }
}

/**
 * Eidolon v listu mazlíčka: forma, podtyp, úroveň summonera (stepper), HD / přirozená zbroj / Str-Dex / max. útoků,
 * schopnosti z tabulky, volné evoluce a vybrané evoluce s body (Ability Increase s výběrem atributu).
 */
@Composable
private fun EidolonPanel(state: AppState, c: Character) {
    val s = str
    val form = c.eidolonFormInfo
    val row = c.eidolonRow
    var pick by remember { mutableStateOf(false) }
    NbPanel(Modifier.fillMaxWidth().padding(top = 6.dp), color = Nb.inset) {
        Text(
            listOfNotNull(form?.name, c.eidolonSubtype, "unchained".takeIf { form?.unchained == true }).joinToString(" · ").ifEmpty { s.speciesCustom },
            fontFamily = LocalNbFonts.current.serif, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Nb.ink,
        )
        form?.let {
            DetailLine(s.speedLabel, it.speed)
            DetailLine(s.attacksLabel, it.attack)
            DetailLine(s.freeEvolutionsLabel, it.freeEvolutions)
        }
        if (row != null) {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                MonoLabel(s.summonerLevelLabel, Modifier.weight(1f))
                CountStepper(row.level, { state.setCompanionLevel(c.id, it) }, min = 1, max = 20, size = 30.dp, numberSize = 16.sp)
            }
            Text(s.eidolonStats(row.hd, c.eidolonNaturalArmor, row.strDex, row.maxAttacks), fontSize = 13.sp, color = Nb.ink, modifier = Modifier.padding(top = 4.dp))
            val specials = (if (form?.unchained == true) cz.pfspells.data.EIDOLON_PROGRESSION_UNCHAINED else cz.pfspells.data.EIDOLON_PROGRESSION)
                .take(row.level).map { it.special }.filter { it.isNotBlank() }
            DetailLine(s.specialsLabel, specials.joinToString(", "))
        }
        // evoluce a body
        SectionHeader(s.evolutionsHeader, size = 15.sp)
        val used = c.evolutionPointsUsed
        Text(s.evolutionPool(used, row?.evolutionPool), fontSize = 13.sp, color = if (row != null && used > row.evolutionPool) Nb.error else Nb.rust)
        if (row != null && used > row.evolutionPool) TraitWarning(s.evolutionsOver)
        c.evolutions.forEachIndexed { i, p ->
            Row(Modifier.fillMaxWidth().bottomRule(Nb.ruleSoft).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${p.name} (${s.evolutionCost(p.cost)})", fontSize = 14.sp, color = Nb.ink)
                    if (p.id == Eidolons.ABILITY_INCREASE) {
                        SimpleDropdown<Ability?>(
                            label = s.abilityChoiceLabel(2),
                            options = listOf<Ability?>(null) + Ability.entries,
                            selected = p.ability,
                            optionLabel = { it?.let(s::abilityLong) ?: s.chooseOption },
                            onSelect = { state.setEvolutionAbility(c.id, i, it) },
                        )
                    }
                }
                NbIconButton(NbIcons.Close, s.delete, { state.removeEvolution(c.id, i) }, Nb.inkMuted, 16.dp)
            }
        }
        NbDashedButton(s.addEvolution, { pick = true }, icon = NbIcons.Plus, modifier = Modifier.padding(top = 6.dp))
        Text(s.eidolonHint, fontSize = 12.sp, color = Nb.inkMuted, modifier = Modifier.padding(top = 4.dp))
    }
    if (pick) EvolutionPickerDialog(state, c) { pick = false }
}

/** Výběr evoluce: hledání, cena, klepnutím text; evoluce dražší než zbývající body jsou tlumené (přidat jde). */
@Composable
private fun EvolutionPickerDialog(state: AppState, c: Character, onDismiss: () -> Unit) {
    val s = str
    LaunchedEffect(Unit) { state.ensureEidolonData() }
    val data = state.eidolonData
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val free = (c.eidolonRow?.evolutionPool ?: Int.MAX_VALUE) - c.evolutionPointsUsed
    val list = remember(data, query) {
        val q = query.trim().lowercase()
        data?.evolutions.orEmpty().filter { q.isEmpty() || it.name.lowercase().contains(q) || it.text.lowercase().contains(q) }
            .sortedWith(compareBy({ it.cost }, { it.name }))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints {
            val width = minOf(600.dp, maxWidth - 32.dp)
            val maxH = minOf(720.dp, maxHeight - 48.dp)
            Surface(Modifier.width(width).heightIn(max = maxH), shape = MaterialTheme.shapes.extraLarge, color = Nb.panel, shadowElevation = 8.dp) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)) {
                    Text(s.addEvolution, style = MaterialTheme.typography.headlineSmall, color = Nb.ink)
                    Text(s.evolutionPool(c.evolutionPointsUsed, c.eidolonRow?.evolutionPool), fontSize = 13.sp, color = Nb.rust, modifier = Modifier.padding(bottom = 8.dp))
                    NbSearchField(query, { query = it }, s.searchAbilities, Modifier.fillMaxWidth())
                    if (data == null) Text(s.loadingAbilities, color = Nb.inkMuted, modifier = Modifier.padding(vertical = 12.dp))
                    LazyColumn(
                        Modifier.weight(1f, fill = false).padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                    ) {
                        items(list, key = { it.id }) { evo: Evolution ->
                            val affordable = evo.cost <= free
                            Column(
                                Modifier.fillMaxWidth().clip(Nb.shape).background(Nb.paper).dashedBorder()
                                    .clickable { expanded = if (expanded == evo.id) null else evo.id }.padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(evo.name, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (affordable) Nb.ink else Nb.inkMuted)
                                    Text(s.evolutionCost(evo.cost), fontFamily = LocalNbFonts.current.mono, fontSize = 12.sp, color = if (affordable) Nb.rust else Nb.error)
                                    NbIconButton(NbIcons.Plus, s.add, { state.addEvolution(c.id, evo); onDismiss() }, Nb.accent, 18.dp)
                                }
                                if (expanded == evo.id) Text(evo.text, fontSize = 13.sp, color = Nb.ink, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text(s.close, color = Nb.ink) }
                    }
                }
            }
        }
    }
}

