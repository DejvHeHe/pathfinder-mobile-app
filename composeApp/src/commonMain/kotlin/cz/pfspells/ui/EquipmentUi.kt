package cz.pfspells.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pfspells.AppState
import cz.pfspells.Strings
import cz.pfspells.data.Ability
import cz.pfspells.data.BonusTarget
import cz.pfspells.data.BonusType
import cz.pfspells.data.Character
import cz.pfspells.data.InvEntry
import cz.pfspells.data.Inventory
import cz.pfspells.data.ItemBonus
import cz.pfspells.data.ItemSlot
import cz.pfspells.data.SkillInfo
import cz.pfspells.data.Skills
import cz.pfspells.data.gearAc
import cz.pfspells.data.equippedIn
import cz.pfspells.data.isWorking
import cz.pfspells.data.gearMod
import cz.pfspells.data.gearSave
import cz.pfspells.str

private fun signed(v: Int) = if (v < 0) "−${-v}" else "+$v"

private fun abilityOf(t: BonusTarget): Ability? = when (t) {
    BonusTarget.STR -> Ability.STR
    BonusTarget.DEX -> Ability.DEX
    BonusTarget.CON -> Ability.CON
    BonusTarget.INT -> Ability.INT
    BonusTarget.WIS -> Ability.WIS
    BonusTarget.CHA -> Ability.CHA
    else -> null
}

/** „+4 INT (enhancement)“, „+5 Stealth (competence)“, „+2 AC (deflection)“ */
fun bonusText(s: Strings, b: ItemBonus): String {
    val target = abilityOf(b.target)?.let { s.abilityShort(it) }
        ?: if (b.target == BonusTarget.SKILL) b.skill?.let { k -> Skills.get(Skills.idOf(k))?.let { s.skillName(it.id, it.name) } ?: k } ?: s.bonusTarget(b.target)
        else s.bonusTarget(b.target)
    return "${signed(b.value)} $target (${s.bonusType(b.type)})"
}

/** Souhrn vybavení postavy: záchrany a AC (atributy a skills už jsou v hodnotách). Prázdné = nic nenasazeno. */
fun gearSummaryText(s: Strings, c: Character): String {
    val saves = listOf(BonusTarget.FORT, BonusTarget.REF, BonusTarget.WILL)
        .mapNotNull { t -> c.gearSave(t).takeIf { it != 0 }?.let { "${s.bonusTarget(t)} ${signed(it)}" } }
    val ac = c.gearAc().map { (type, v) -> "AC ${signed(v)} ${s.bonusType(type)}" }
    val parts = saves + ac
    return if (parts.isEmpty()) "" else s.gearSummary(parts.joinToString(" · "))
}

/** „+4 INT, +2 CON“ – úpravy atributů z vybavení (pro nápovědu u atributů). */
fun gearAbilityText(s: Strings, c: Character): String =
    Ability.entries.mapNotNull { a -> c.gearMod(a).takeIf { it != 0 }?.let { "${signed(it)} ${s.abilityShort(a)}" } }.joinToString(", ")

/** „Odolnost · resistance“ – na co a typ (bez hodnoty, ta je v řádku zvlášť). */
private fun bonusTargetText(s: Strings, b: ItemBonus): String {
    val target = abilityOf(b.target)?.let { s.abilityLong(it) }
        ?: if (b.target == BonusTarget.SKILL) b.skill?.let { k -> Skills.get(Skills.idOf(k))?.let { s.skillName(it.id, it.name) } ?: k } ?: s.bonusTarget(b.target)
        else s.bonusTarget(b.target)
    return "$target · ${s.bonusType(b.type)}"
}

/**
 * Sekce „Vybavení“ v detailu položky inventáře (návrh Inventory 2.0): vybraný slot, nasazeno + slot,
 * bonusy po řádcích (velké číslo + na co · typ), přidat bonus, karta konfliktu se stejným slotem (sundat druhý / prohodit).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EquipmentSection(state: AppState, inv: Inventory, entry: InvEntry) {
    val s = str
    val fonts = LocalNbFonts.current
    var addBonus by remember { mutableStateOf(false) }
    val working = inv.isWorking(entry)
    SectionHeader(s.equipmentHeader)
    entry.slot?.takeIf { entry.equipped }?.let { MonoLabel(s.selectedSlot(s.itemSlot(it)), color = Nb.rust) }
    Text(s.equipHint, fontSize = 12.5.sp, color = Nb.inkMuted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        NbCheck(entry.equipped, s.equippedLabel) { state.setEquipped(inv.id, entry.id, it) }
        SimpleDropdown<ItemSlot?>(
            label = s.slot,
            options = listOf<ItemSlot?>(null) + ItemSlot.entries,
            selected = entry.slot,
            optionLabel = { it?.let(s::itemSlot) ?: s.noSlot },
            onSelect = { sl -> state.updateEntry(inv.id, entry.id) { it.copy(slot = sl) } },
        )
    }
    if (entry.bonuses.isEmpty()) Text(s.noBonuses(), fontSize = 13.sp, color = Nb.inkMuted)
    entry.bonuses.forEachIndexed { i, b ->
        Row(
            Modifier.fillMaxWidth().bottomRule(Nb.ruleSoft).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // bonus nenasazeného (nebo přebytečného ve slotu) předmětu neplatí – tlumeně
            val active = entry.equipped && working
            Text(
                signed(b.value), Modifier.widthIn(min = 40.dp),
                fontFamily = fonts.serif, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                color = if (active) Nb.accent else Nb.inkMuted,
            )
            Text(bonusTargetText(s, b), Modifier.weight(1f), fontSize = 14.5.sp, color = if (active) Nb.ink else Nb.inkMuted)
            NbIconButton(NbIcons.Close, s.delete, {
                state.updateEntry(inv.id, entry.id) { e -> e.copy(bonuses = e.bonuses.filterIndexed { j, _ -> j != i }) }
            }, Nb.inkMuted, 16.dp)
        }
    }
    NbDashedButton(s.addBonus, { addBonus = true }, icon = NbIcons.Plus)
    // konflikt: jiný nasazený předmět ve stejném slotu
    val slot = entry.slot
    val other = if (entry.equipped && slot != null) inv.equippedIn(slot).firstOrNull { it.id != entry.id } else null
    if (other != null && slot != null && inv.equippedIn(slot).sumOf { it.qty.coerceAtLeast(1) } > slot.capacity) {
        Column(
            Modifier.fillMaxWidth().clip(Nb.shape).background(Nb.error.copy(alpha = 0.07f)).border(1.5.dp, Nb.error, Nb.shape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(s.sameSlotAs(other.name), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Nb.error)
            Text(s.keepThisWorn, fontSize = 13.5.sp, color = Nb.ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // tenhle nechat, druhý do batohu
                NbDashedButton(s.unequipOther(other.name), {
                    state.preferInSlot(inv.id, entry.id)
                    state.setEquipped(inv.id, other.id, false)
                })
                // prohodit: platí ten druhý, tenhle do batohu
                NbPrimaryButton(s.swap, {
                    state.preferInSlot(inv.id, other.id)
                    state.setEquipped(inv.id, entry.id, false)
                }, icon = null)
            }
        }
    }
    if (addBonus) {
        BonusDialog(onDismiss = { addBonus = false }) { b ->
            addBonus = false
            state.updateEntry(inv.id, entry.id) { it.copy(bonuses = it.bonuses + b) }
        }
    }
}

/** Nový bonus předmětu: na co (atribut, záchrana, AC, skill), hodnota a typ. */
@Composable
fun BonusDialog(onDismiss: () -> Unit, onSave: (ItemBonus) -> Unit) {
    val s = str
    var target by remember { mutableStateOf(BonusTarget.INT) }
    var skill by remember { mutableStateOf(Skills.all.first()) }
    var value by remember { mutableIntStateOf(2) }
    var type by remember { mutableStateOf(BonusType.ENHANCEMENT) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nb.panel,
        title = { Text(s.addBonus, color = Nb.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SimpleDropdown(
                    label = s.bonusTargetLabel,
                    options = BonusTarget.entries,
                    selected = target,
                    optionLabel = { s.bonusTarget(it) },
                    onSelect = { t ->
                        target = t
                        // typický typ podle cíle (jde změnit)
                        type = when (t) {
                            BonusTarget.FORT, BonusTarget.REF, BonusTarget.WILL -> BonusType.RESISTANCE
                            BonusTarget.AC -> BonusType.DEFLECTION
                            BonusTarget.SKILL -> BonusType.COMPETENCE
                            BonusTarget.MAX_DEX, BonusTarget.ARMOR_CHECK -> BonusType.ARMOR
                            else -> BonusType.ENHANCEMENT
                        }
                    },
                )
                if (target == BonusTarget.SKILL) {
                    SimpleDropdown<SkillInfo>(
                        label = s.bonusSkillLabel,
                        options = Skills.all,
                        selected = skill,
                        optionLabel = { s.skillName(it.id, it.name) },
                        onSelect = { skill = it },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel(s.bonusValueLabel, Modifier.weight(1f))
                    CountStepper(value, { value = it }, min = -20, max = 20, size = 32.dp, numberSize = 17.sp)
                }
                SimpleDropdown(
                    label = s.bonusTypeLabel,
                    options = BonusType.entries,
                    selected = type,
                    optionLabel = { s.bonusType(it) },
                    onSelect = { type = it },
                )
            }
        },
        confirmButton = {
            NbPrimaryButton(s.add, {
                onSave(ItemBonus(target, value, type, skill.id.takeIf { target == BonusTarget.SKILL }))
            }, icon = null, enabled = value != 0)
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel, color = Nb.ink) } },
    )
}
