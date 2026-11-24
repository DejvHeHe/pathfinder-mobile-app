package cz.pfspells.ui

import androidx.compose.foundation.layout.Arrangement
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

/** Sekce „Vybavení“ v detailu položky inventáře: nasazeno, slot, bonusy (přidat / odebrat). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EquipmentSection(state: AppState, inv: Inventory, entry: InvEntry) {
    val s = str
    var addBonus by remember { mutableStateOf(false) }
    SectionHeader(s.equipmentHeader)
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
    MonoLabel(s.bonusesLabel)
    if (entry.bonuses.isEmpty()) Text(s.noBonuses(), fontSize = 13.sp, color = Nb.inkMuted)
    entry.bonuses.forEachIndexed { i, b ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                bonusText(s, b), Modifier.weight(1f), fontSize = 14.5.sp,
                color = if (entry.equipped) Nb.ink else Nb.inkMuted,
            )
            NbIconButton(NbIcons.Close, s.delete, {
                state.updateEntry(inv.id, entry.id) { e -> e.copy(bonuses = e.bonuses.filterIndexed { j, _ -> j != i }) }
            }, Nb.inkMuted, 16.dp)
        }
    }
    NbDashedButton(s.addBonus, { addBonus = true }, icon = NbIcons.Plus)
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
